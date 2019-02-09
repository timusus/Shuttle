package com.simplecity.amp_library.utils.playlists

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import com.crashlytics.android.Crashlytics
import com.simplecity.amp_library.R
import com.simplecity.amp_library.data.SongsRepository
import com.simplecity.amp_library.interfaces.FileType
import com.simplecity.amp_library.model.BaseFileObject
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Query
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.sql.SqlUtils
import com.simplecity.amp_library.sql.providers.PlayCountTable
import com.simplecity.amp_library.utils.LogUtils
import com.simplecity.amp_library.utils.SettingsManager
import com.simplecity.amp_library.utils.ShuttleUtils
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.ArrayList
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class PlaylistManager @Inject constructor(
    private val applicationContext: Context,
    private val songsRepository: SongsRepository,
    private val settingsManager: SettingsManager
) {

    interface PlaylistIds {
        companion object {
            const val RECENTLY_ADDED_PLAYLIST: Long = -2
            const val MOST_PLAYED_PLAYLIST: Long = -3
            const val PODCASTS_PLAYLIST: Long = -4
            const val RECENTLY_PLAYED_PLAYLIST: Long = -5
        }
    }

    fun clearMostPlayed() {
        applicationContext.contentResolver.delete(PlayCountTable.URI, null, null)
    }

    fun addToPlaylist(playlist: Playlist, songs: List<Song>, callback: ((Int) -> Unit)?): Disposable? {
        if (songs.isEmpty()) {
            return null
        }

        val mutableSongList = ArrayList(songs)

        return songsRepository.getSongs(playlist)
            .first(emptyList())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { existingSongs ->
                    if (!settingsManager.ignoreDuplicates()) {

                        val duplicates = existingSongs
                            .filter { mutableSongList.contains(it) }
                            .distinct()
                            .toMutableList()

                        if (!duplicates.isEmpty()) {
                            @SuppressLint("InflateParams")
                            val customView = LayoutInflater.from(applicationContext).inflate(R.layout.dialog_playlist_duplicates, null)
                            val messageText = customView.findViewById<TextView>(R.id.textView)
                            val applyToAll = customView.findViewById<CheckBox>(R.id.applyToAll)
                            val alwaysAdd = customView.findViewById<CheckBox>(R.id.alwaysAdd)

                            if (duplicates.size <= 1) {
                                applyToAll.visibility = View.GONE
                                applyToAll.isChecked = false
                            }

                            messageText.text = getPlaylistRemoveString(duplicates[0])
                            applyToAll.text = String.format(applicationContext.getString(R.string.dialog_checkbox_playlist_duplicate_apply_all), duplicates.size)

                            // Fixme: Should not use application context to present dialog.
                            MaterialDialog.Builder(applicationContext)
                                .title(R.string.dialog_title_playlist_duplicates)
                                .customView(customView, false)
                                .positiveText(R.string.dialog_button_playlist_duplicate_add)
                                .autoDismiss(false)
                                .onPositive { dialog, which ->
                                    //If we've only got one item, or we're applying it to all items
                                    if (duplicates.size != 1 && !applyToAll.isChecked) {
                                        //If we're 'adding' this song, we remove it from the 'duplicates' list
                                        duplicates.removeAt(0)
                                        messageText.text = getPlaylistRemoveString(duplicates[0])
                                        applyToAll.text = String.format(applicationContext.getString(R.string.dialog_checkbox_playlist_duplicate_apply_all), duplicates.size)
                                    } else {
                                        //Add all songs to the playlist
                                        insertPlaylistItems(playlist, mutableSongList, existingSongs.size, callback)
                                        settingsManager.setIgnoreDuplicates(alwaysAdd.isChecked)
                                        dialog.dismiss()
                                    }
                                }
                                .negativeText(R.string.dialog_button_playlist_duplicate_skip)
                                .onNegative { dialog, which ->
                                    //If we've only got one item, or we're applying it to all items
                                    if (duplicates.size != 1 && !applyToAll.isChecked) {
                                        //If we're 'skipping' this song, we remove it from the 'duplicates' list,
                                        // and from the ids to be added
                                        mutableSongList.remove(duplicates.removeAt(0))
                                        messageText.text = getPlaylistRemoveString(duplicates[0])
                                        applyToAll.text = String.format(applicationContext.getString(R.string.dialog_checkbox_playlist_duplicate_apply_all), duplicates.size)
                                    } else {
                                        //Remove duplicates from our set of ids
                                        duplicates
                                            .filter { mutableSongList.contains(it) }
                                            .forEach { mutableSongList.remove(it) }
                                        insertPlaylistItems(playlist, mutableSongList, existingSongs.size, callback)
                                        settingsManager.setIgnoreDuplicates(alwaysAdd.isChecked)
                                        dialog.dismiss()
                                    }
                                }
                                .show()
                        } else {
                            insertPlaylistItems(playlist, mutableSongList, existingSongs.size, callback)
                        }
                    } else {
                        insertPlaylistItems(playlist, mutableSongList, existingSongs.size, callback)
                    }
                },
                { error -> LogUtils.logException(TAG, "PlaylistManager: Error determining existing songs", error) }
            )
    }

    private fun insertPlaylistItems(playlist: Playlist, songs: List<Song>, songCount: Int, callback: ((Int) -> Unit)?) {
        if (songs.isEmpty()) {
            return
        }

        val contentValues = arrayOfNulls<ContentValues>(songs.size)
        var i = 0
        val length = songs.size
        while (i < length) {
            contentValues[i] = ContentValues()
            contentValues[i]!!.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, songCount + i)
            contentValues[i]!!.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, songs[i].id)
            i++
        }

        val uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlist.id)
        if (uri != null) {
            applicationContext.contentResolver.bulkInsert(uri, contentValues)
            callback?.invoke(songs.size)
        }
    }

    private fun getPlaylistRemoveString(song: Song): SpannableStringBuilder {
        val spannableString = SpannableStringBuilder(String.format(applicationContext.getString(R.string.dialog_message_playlist_add_duplicate), song.artistName, song.name))
        val boldSpan = StyleSpan(android.graphics.Typeface.BOLD)
        spannableString.setSpan(boldSpan, 0, song.artistName.length + song.name.length + 3, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        return spannableString
    }

    fun clearPlaylist(playlistId: Long) {
        val uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId)
        applicationContext.contentResolver.delete(uri, null, null)
    }

    fun createPlaylist(name: String): Playlist? {
        var playlist: Playlist? = null
        var id: Long = -1

        if (!TextUtils.isEmpty(name)) {
            val query = Query.Builder()
                .uri(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI)
                .projection(arrayOf(MediaStore.Audio.PlaylistsColumns.NAME))
                .selection(MediaStore.Audio.PlaylistsColumns.NAME + " = '" + name + "'")
                .build()

            SqlUtils.createQuery(applicationContext, query)?.use { cursor ->
                val count = cursor.count

                if (count <= 0) {
                    val values = ContentValues(1)
                    values.put(MediaStore.Audio.PlaylistsColumns.NAME, name)
                    //Catch NPE occurring on Amazon devices.
                    try {
                        val uri = applicationContext.contentResolver.insert(
                            MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                            values
                        )
                        if (uri != null) {
                            id = java.lang.Long.parseLong(uri.lastPathSegment!!)
                        }
                    } catch (e: NullPointerException) {
                        Crashlytics.log("Failed to create playlist: " + e.message)
                    }

                }
            }
        }

        if (id != -1L) {
            playlist = Playlist(Playlist.Type.USER_CREATED, id, name, true, false, true, true, true)
        } else {
            Crashlytics.log(String.format("Failed to create playlist. Name: %s, id: %d", name, id))
        }

        return playlist
    }

    fun removeFromPlaylist(playlist: Playlist, song: Song, callback: Function1<Boolean, Unit>?): Disposable {
        return Single.fromCallable {
            var numTracksRemoved = 0
            if (playlist.id >= 0) {
                val uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlist.id)
                numTracksRemoved = applicationContext.contentResolver.delete(uri, MediaStore.Audio.Playlists.Members.AUDIO_ID + "=" + song.id, null)
            }
            numTracksRemoved
        }
            .delay(150, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { numTracksRemoved -> callback?.invoke(numTracksRemoved > 0) },
                { error -> LogUtils.logException(TAG, "PlaylistManager: Error Removing from favorites", error) }
            )
    }

    fun addFileObjectsToPlaylist(context: Context, playlist: Playlist, fileObjects: List<BaseFileObject>, callback: Function1<Int, Unit>): Disposable {
        val progressDialog = ProgressDialog.show(context, "", context.getString(R.string.gathering_songs), false)

        val folderCount = fileObjects
            .filter { value -> value.fileType == FileType.FOLDER }.count()

        if (folderCount > 0) {
            progressDialog!!.show()
        }

        return ShuttleUtils.getSongsForFileObjects(songsRepository, fileObjects)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { songs ->
                    if (progressDialog != null && progressDialog.isShowing) {
                        progressDialog.dismiss()
                    }
                    addToPlaylist(playlist, songs, callback)
                },
                { error -> LogUtils.logException(TAG, "Error getting songs for file object", error) }
            )
    }

    companion object {
        private const val TAG = "PlaylistManager"

        const val ARG_PLAYLIST = "playlist"
    }
}