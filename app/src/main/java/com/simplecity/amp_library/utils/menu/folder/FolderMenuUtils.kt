package com.simplecity.amp_library.utils.menu.folder

import android.annotation.SuppressLint
import android.content.Context
import android.support.annotation.StringRes
import android.support.v4.app.Fragment
import android.support.v7.widget.PopupMenu
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.simplecity.amp_library.R
import com.simplecity.amp_library.data.Repository.SongsRepository
import com.simplecity.amp_library.interfaces.FileType
import com.simplecity.amp_library.model.BaseFileObject
import com.simplecity.amp_library.model.FileObject
import com.simplecity.amp_library.model.FolderObject
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.playback.MediaManager
import com.simplecity.amp_library.playback.MediaManager.Defs
import com.simplecity.amp_library.ui.modelviews.FolderView
import com.simplecity.amp_library.ui.screens.playlist.dialog.CreatePlaylistDialog
import com.simplecity.amp_library.utils.CustomMediaScanner
import com.simplecity.amp_library.utils.FileHelper
import com.simplecity.amp_library.utils.LogUtils
import com.simplecity.amp_library.utils.menu.MenuUtils
import com.simplecity.amp_library.utils.playlists.PlaylistManager
import com.simplecity.amp_library.utils.playlists.PlaylistMenuHelper
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import java.io.File

object FolderMenuUtils {

    private val TAG = "FolderMenuUtils"

    interface Callbacks {

        fun showToast(message: String)

        fun showToast(@StringRes messageResId: Int)

        fun onSongsAddedToQueue(numSongs: Int)

        fun onPlaybackFailed()

        fun shareSong(song: Song)

        fun setRingtone(song: Song)

        fun showSongInfo(song: Song)

        fun onPlaylistItemsInserted()

        fun showTagEditor(song: Song)

        fun onFileNameChanged(folderView: FolderView)

        fun onFileDeleted(folderView: FolderView)

        fun playNext(songsSingle: Single<List<Song>>)

        fun whitelist(songsSingle: Single<List<Song>>)

        fun blacklist(songsSingle: Single<List<Song>>)

        fun whitelist(song: Song)

        fun blacklist(song: Song)
    }

    private fun getSongForFile(songsRepository: SongsRepository, fileObject: FileObject): Single<Song> {
        return FileHelper.getSong(songsRepository, File(fileObject.path))
            .observeOn(AndroidSchedulers.mainThread())
    }

    private fun getSongsForFolderObject(songsRepository: SongsRepository, folderObject: FolderObject): Single<List<Song>> {
        return FileHelper.getSongList(songsRepository, File(folderObject.path), true, false)
    }

    private fun scanFile(context: Context, fileObject: FileObject, callbacks: Callbacks) {
        CustomMediaScanner.scanFile(context, fileObject.path, { callbacks.showToast(it) })
    }

    // Todo: Remove context requirement.
    private fun scanFolder(context: Context, folderObject: FolderObject) {
        CustomMediaScanner.scanFile(context, folderObject)
    }

    // Todo: Remove context requirement.
    private fun renameFile(context: Context, folderView: FolderView, fileObject: BaseFileObject, callbacks: Callbacks) {

        @SuppressLint("InflateParams")
        val customView = LayoutInflater.from(context).inflate(R.layout.dialog_rename, null)

        val editText = customView.findViewById<EditText>(R.id.editText)
        editText.setText(fileObject.name)

        val builder = MaterialDialog.Builder(context)
        if (fileObject.fileType == FileType.FILE) {
            builder.title(R.string.rename_file)
        } else {
            builder.title(R.string.rename_folder)
        }

        builder.customView(customView, false)
        builder.positiveText(R.string.save)
            .onPositive { materialDialog, dialogAction ->
                if (editText.text != null) {
                    if (FileHelper.renameFile(context, fileObject, editText.text.toString())) {
                        callbacks.onFileNameChanged(folderView)
                    } else {
                        callbacks.showToast(if (fileObject.fileType == FileType.FOLDER) R.string.rename_folder_failed else R.string.rename_file_failed)
                    }
                }
            }
        builder.negativeText(R.string.cancel)
            .show()
    }

    // Todo: Remove context requirement.
    private fun deleteFile(context: Context, folderView: FolderView, fileObject: BaseFileObject, callbacks: Callbacks) {
        val builder = MaterialDialog.Builder(context)
            .title(R.string.delete_item)
            .iconRes(R.drawable.ic_warning_24dp)
        if (fileObject.fileType == FileType.FILE) {
            builder.content(
                String.format(
                    context.resources.getString(
                        R.string.delete_file_confirmation_dialog
                    ), fileObject.name
                )
            )
        } else {
            builder.content(
                String.format(
                    context.resources.getString(
                        R.string.delete_folder_confirmation_dialog
                    ), fileObject.path
                )
            )
        }
        builder.positiveText(R.string.button_ok)
            .onPositive { materialDialog, dialogAction ->
                if (FileHelper.deleteFile(File(fileObject.path))) {
                    callbacks.onFileDeleted(folderView)
                    CustomMediaScanner.scanFiles(context, listOf(fileObject.path), null)
                } else {
                    Toast.makeText(
                        context,
                        if (fileObject.fileType == FileType.FOLDER) R.string.delete_folder_failed else R.string.delete_file_failed,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        builder.negativeText(R.string.cancel)
            .show()
    }

    fun setupFolderMenu(menu: PopupMenu, fileObject: BaseFileObject, playlistMenuHelper: PlaylistMenuHelper) {

        menu.inflate(R.menu.menu_file)

        // Add playlist menu
        val subMenu = menu.menu.findItem(R.id.addToPlaylist).subMenu
        playlistMenuHelper.createPlaylistMenu(subMenu)

        if (!fileObject.canReadWrite()) {
            menu.menu.findItem(R.id.rename).isVisible = false
        }

        when (fileObject.fileType) {
            FileType.FILE -> menu.menu.findItem(R.id.play).isVisible = false
            FileType.FOLDER -> {
                menu.menu.findItem(R.id.songInfo).isVisible = false
                menu.menu.findItem(R.id.ringtone).isVisible = false
                menu.menu.findItem(R.id.share).isVisible = false
                menu.menu.findItem(R.id.editTags).isVisible = false
            }
            FileType.PARENT -> {
            }
        }
    }

    fun getFolderMenuClickListener(
        fragment: Fragment,
        mediaManager: MediaManager,
        songsRepository: SongsRepository,
        folderView: FolderView,
        playlistManager: PlaylistManager,
        callbacks: Callbacks
    ): PopupMenu.OnMenuItemClickListener? {
        when (folderView.baseFileObject.fileType) {
            FileType.FILE -> return getFileMenuClickListener(fragment, mediaManager, songsRepository, folderView, folderView.baseFileObject as FileObject, playlistManager, callbacks)
            FileType.FOLDER -> return getFolderMenuClickListener(fragment, mediaManager, songsRepository, folderView, folderView.baseFileObject as FolderObject, playlistManager, callbacks)
        }
        return null
    }

    private fun getFolderMenuClickListener(
        fragment: Fragment,
        mediaManager: MediaManager,
        songsRepository: SongsRepository,
        folderView: FolderView,
        folderObject: FolderObject,
        playlistManager: PlaylistManager,
        callbacks: Callbacks
    ): PopupMenu.OnMenuItemClickListener {

        return PopupMenu.OnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.play -> {
                    MenuUtils.play(mediaManager, getSongsForFolderObject(songsRepository, folderObject)) { callbacks.onPlaybackFailed() }
                    return@OnMenuItemClickListener true
                }
                R.id.playNext -> {
                    callbacks.playNext(getSongsForFolderObject(songsRepository, folderObject))
                    return@OnMenuItemClickListener true
                }
                Defs.NEW_PLAYLIST -> {
                    MenuUtils.newPlaylist(
                        fragment,
                        getSongsForFolderObject(songsRepository, folderObject)
                    )
                    return@OnMenuItemClickListener true
                }
                Defs.PLAYLIST_SELECTED -> {
                    getSongsForFolderObject(songsRepository, folderObject).subscribe { songs ->
                        MenuUtils.addToPlaylist(
                            playlistManager,
                            menuItem.intent.getSerializableExtra(PlaylistManager.ARG_PLAYLIST) as Playlist,
                            songs
                        ) { callbacks.onPlaylistItemsInserted() }
                    }
                    return@OnMenuItemClickListener true
                }
                R.id.addToQueue -> {
                    MenuUtils.addToQueue(mediaManager, getSongsForFolderObject(songsRepository, folderObject)) { callbacks.onSongsAddedToQueue(it) }
                    return@OnMenuItemClickListener true
                }
                R.id.scan -> {
                    scanFolder(fragment.context!!, folderObject)
                    return@OnMenuItemClickListener true
                }
                R.id.whitelist -> {
                    callbacks.whitelist(getSongsForFolderObject(songsRepository, folderObject))
                    return@OnMenuItemClickListener true
                }
                R.id.blacklist -> {
                    callbacks.blacklist(getSongsForFolderObject(songsRepository, folderObject))
                    return@OnMenuItemClickListener true
                }
                R.id.rename -> {
                    renameFile(fragment.context!!, folderView, folderObject, callbacks)
                    return@OnMenuItemClickListener true
                }
                R.id.delete -> {
                    deleteFile(fragment.context!!, folderView, folderObject, callbacks)
                    return@OnMenuItemClickListener true
                }
            }
            false
        }
    }

    private fun getFileMenuClickListener(
        fragment: Fragment,
        mediaManager: MediaManager,
        songsRepository: SongsRepository,
        folderView: FolderView,
        fileObject: FileObject,
        playlistManager: PlaylistManager,
        callbacks: Callbacks
    ): PopupMenu.OnMenuItemClickListener {
        return PopupMenu.OnMenuItemClickListener { menuItem ->

            val errorHandler: (Throwable) -> Unit = { e -> LogUtils.logException(TAG, "getFileMenuClickListener threw error", e) }

            when (menuItem.itemId) {
                R.id.playNext -> {
                    getSongForFile(songsRepository, fileObject)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            { song -> mediaManager.playNext(listOf(song)) { callbacks.onSongsAddedToQueue(it) } },
                            errorHandler
                        )
                    return@OnMenuItemClickListener true
                }
                Defs.NEW_PLAYLIST -> {
                    getSongForFile(songsRepository, fileObject).subscribe(
                        { song ->
                            CreatePlaylistDialog.newInstance(listOf(song)).show(fragment.childFragmentManager, "CreatePlaylistDialog")
                        },
                        errorHandler
                    )
                    return@OnMenuItemClickListener true
                }
                Defs.PLAYLIST_SELECTED -> {
                    getSongForFile(songsRepository, fileObject).subscribe({ song ->
                        MenuUtils.addToPlaylist(
                            playlistManager,
                            menuItem.intent.getSerializableExtra(PlaylistManager.ARG_PLAYLIST) as Playlist,
                            listOf(song)
                        ) { callbacks.onPlaylistItemsInserted() }
                    }, errorHandler)
                    return@OnMenuItemClickListener true
                }
                R.id.addToQueue -> {
                    getSongForFile(songsRepository, fileObject).subscribe({ song -> MenuUtils.addToQueue(mediaManager, listOf(song), { callbacks.onSongsAddedToQueue(it) }) }, errorHandler)
                    return@OnMenuItemClickListener true
                }
                R.id.scan -> {
                    scanFile(fragment.context!!, fileObject, callbacks)
                    return@OnMenuItemClickListener true
                }
                R.id.editTags -> {
                    getSongForFile(songsRepository, fileObject).subscribe({ callbacks.showTagEditor(it) }, errorHandler)
                    return@OnMenuItemClickListener true
                }
                R.id.share -> {
                    getSongForFile(songsRepository, fileObject).subscribe({ callbacks.shareSong(it) }, errorHandler)
                    return@OnMenuItemClickListener true
                }
                R.id.ringtone -> {
                    getSongForFile(songsRepository, fileObject).subscribe({ callbacks.setRingtone(it) }, errorHandler)
                    return@OnMenuItemClickListener true
                }
                R.id.songInfo -> {
                    getSongForFile(songsRepository, fileObject).subscribe({ callbacks.showSongInfo(it) }, errorHandler)
                    return@OnMenuItemClickListener true
                }
                R.id.blacklist -> {
                    getSongForFile(songsRepository, fileObject).subscribe({ callbacks.blacklist(it) }, errorHandler)
                    return@OnMenuItemClickListener true
                }
                R.id.whitelist -> {
                    getSongForFile(songsRepository, fileObject).subscribe({ callbacks.whitelist(it) }, errorHandler)
                    return@OnMenuItemClickListener true
                }
                R.id.rename -> {
                    renameFile(fragment.context!!, folderView, fileObject, callbacks)
                    return@OnMenuItemClickListener true
                }
                R.id.delete -> {
                    deleteFile(fragment.context!!, folderView, fileObject, callbacks)
                    return@OnMenuItemClickListener true
                }
            }
            false
        }
    }
}
