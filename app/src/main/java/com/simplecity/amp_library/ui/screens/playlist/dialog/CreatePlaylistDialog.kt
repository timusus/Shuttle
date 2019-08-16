package com.simplecity.amp_library.ui.screens.playlist.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.annotation.WorkerThread
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.simplecity.amp_library.R
import com.simplecity.amp_library.data.SongsRepository
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Playlist.Type
import com.simplecity.amp_library.model.Query
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.sql.SqlUtils
import com.simplecity.amp_library.sql.sqlbrite.SqlBriteUtils
import com.simplecity.amp_library.utils.LogUtils
import com.simplecity.amp_library.utils.SettingsManager
import com.simplecity.amp_library.utils.playlists.PlaylistManager
import dagger.android.support.AndroidSupportInjection
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.io.Serializable
import javax.inject.Inject

class CreatePlaylistDialog : DialogFragment() {

    private val disposable = CompositeDisposable()

    @Inject lateinit var songsRepository: SongsRepository

    @Inject lateinit var settingsManager: SettingsManager

    @Inject lateinit var playlistManager: PlaylistManager

    interface OnSavePlaylistListener {
        fun onSave(playlist: Playlist)
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val songsToAdd: List<Song>? = arguments!!.getSerializable(ARG_SONGS) as? List<Song>

        @SuppressLint("InflateParams")
        val customView = LayoutInflater.from(context).inflate(R.layout.dialog_playlist, null)
        val editText = customView.findViewById<EditText>(R.id.editText)

        disposable.add(Observable.fromCallable<String> { makePlaylistName() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { name ->
                    editText.setText(name)
                    if (!TextUtils.isEmpty(name)) {
                        editText.setSelection(name.length)
                    }
                },
                { error ->
                    LogUtils.logException(TAG, "PlaylistManager: Error Setting playlist name", error)
                }
            ))

        val activity = activity

        val builder = MaterialDialog.Builder(context!!)
            .customView(customView, false)
            .title(R.string.menu_playlist)
            .positiveText(R.string.create_playlist_create_text)
            .onPositive { materialDialog, dialogAction ->
                val name = editText.text.toString()
                if (!name.isEmpty()) {
                    idForPlaylistObservable(name)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            { id ->
                                val uri: Uri?
                                if (id >= 0) {
                                    uri = ContentUris.withAppendedId(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, id!!.toLong())
                                    val uri1 = MediaStore.Audio.Playlists.Members.getContentUri("external", id as Long)
                                    context!!.contentResolver.delete(uri1, null, null)
                                } else {
                                    val values = ContentValues(1)
                                    values.put(MediaStore.Audio.Playlists.NAME, name)
                                    uri = try {
                                        context!!.contentResolver.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, values)
                                    } catch (e: IllegalArgumentException) {
                                        if (activity != null) {
                                            Toast.makeText(activity, R.string.dialog_create_playlist_error, Toast.LENGTH_LONG).show()
                                        }
                                        null
                                    } catch (e: NullPointerException) {
                                        if (activity != null) {
                                            Toast.makeText(activity, R.string.dialog_create_playlist_error, Toast.LENGTH_LONG).show()
                                        }
                                        null
                                    }
                                }

                                if (uri != null) {
                                    val playlist = Playlist(Type.USER_CREATED, uri.lastPathSegment!!.toLong(), name, true, false, true, true, true)

                                    songsToAdd?.let {
                                        playlistManager.addToPlaylist(playlist, songsToAdd) { numSongs ->
                                            if (activity != null) {
                                                Toast.makeText(activity, activity.resources.getQuantityString(R.plurals.NNNtrackstoplaylist, numSongs, numSongs), Toast.LENGTH_LONG).show()
                                            }
                                            (parentFragment as? OnSavePlaylistListener)?.onSave(playlist)
                                        }
                                    } ?: run {
                                        if (activity != null) {
                                            Toast.makeText(activity, activity.resources.getQuantityString(R.plurals.NNNtrackstoplaylist, 0, 0), Toast.LENGTH_LONG).show()
                                        }
                                        (parentFragment as? OnSavePlaylistListener)?.onSave(playlist)
                                    }
                                }
                            },
                            { error ->
                                LogUtils.logException(
                                    TAG,
                                    "PlaylistManager: Error Saving playlist",
                                    error
                                )
                            }
                        )
                }
            }
            .negativeText(R.string.cancel)

        val dialog = builder.build()

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // don't care about this one
            }

            //Fixme: It's probably best to just query all playlist names first, and then check against hat list, rather than requerying for each char change.
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val newText = editText.text.toString()
                if (newText.trim { it <= ' ' }.isEmpty()) {
                    dialog.getActionButton(DialogAction.POSITIVE).isEnabled = false
                } else {
                    dialog.getActionButton(DialogAction.POSITIVE).isEnabled = true
                    // check if playlist with current name exists already, and warn the user if so.
                    disposable.add(idForPlaylistObservable(newText)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            { id ->
                                if (id >= 0) {
                                    dialog.getActionButton(DialogAction.POSITIVE).setText(R.string.create_playlist_overwrite_text)
                                } else {
                                    dialog.getActionButton(DialogAction.POSITIVE).setText(R.string.create_playlist_create_text)
                                }
                            },
                            { error ->
                                LogUtils.logException(
                                    TAG,
                                    "PlaylistManager: Error handling text change",
                                    error
                                )
                            }
                        ))
                }
            }

            override fun afterTextChanged(s: Editable) {
                // don't care about this one
            }
        }

        editText.addTextChangedListener(textWatcher)

        return dialog
    }

    fun idForPlaylistObservable(name: String): Single<Int> {
        val query = Query.Builder()
            .uri(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI)
            .projection(arrayOf(MediaStore.Audio.Playlists._ID))
            .selection(MediaStore.Audio.Playlists.NAME + "='" + name.replace("'".toRegex(), "\''") + "'")
            .sort(MediaStore.Audio.Playlists.NAME)
            .build()

        return SqlBriteUtils.createSingle(context!!, { cursor -> cursor.getInt(0) }, query, -1)
    }

    @WorkerThread
    fun makePlaylistName(): String? {

        val template = context!!.getString(R.string.new_playlist_name_template)
        var num = 1

        val query = Query.Builder()
            .uri(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI)
            .projection(arrayOf(MediaStore.Audio.Playlists.NAME))
            .sort(MediaStore.Audio.Playlists.NAME)
            .build()

        SqlUtils.createQuery(context, query)?.use { cursor ->
            var suggestedName = String.format(template, num++)

            // Need to loop until we've made 1 full pass through without finding a match.
            // Looping more than once shouldn't happen very often, but will happen
            // if you have playlists named "New Playlist 1"/10/2/3/4/5/6/7/8/9, where
            // making only one pass would result in "New Playlist 10" being erroneously
            // picked for the new name.
            var done = false
            while (!done) {
                done = true
                cursor.moveToFirst()
                while (!cursor.isAfterLast) {
                    val playlistName = cursor.getString(0)
                    if (playlistName.compareTo(suggestedName, ignoreCase = true) == 0) {
                        suggestedName = String.format(template, num++)
                        done = false
                    }
                    cursor.moveToNext()
                }
            }
            return suggestedName
        }
        return null
    }

    fun show(fragmentManager: FragmentManager) {
        show(fragmentManager, TAG)
    }

    companion object {

        private const val TAG = "CreatePlaylistDialog"

        private const val ARG_SONGS = "songs"

        fun newInstance(songsToAdd: List<Song>?): CreatePlaylistDialog {
            val dialogFragment = CreatePlaylistDialog()
            songsToAdd?.let {
                val args = Bundle()
                args.putSerializable(ARG_SONGS, songsToAdd as Serializable)
                dialogFragment.arguments = args
            }
            return dialogFragment
        }
    }
}