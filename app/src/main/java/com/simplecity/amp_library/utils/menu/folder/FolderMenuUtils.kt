package com.simplecity.amp_library.utils.menu.folder

import android.annotation.SuppressLint
import android.content.Context
import android.support.annotation.StringRes
import android.support.v7.widget.PopupMenu
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import com.simplecity.amp_library.R
import com.simplecity.amp_library.interfaces.FileType
import com.simplecity.amp_library.model.BaseFileObject
import com.simplecity.amp_library.model.FileObject
import com.simplecity.amp_library.model.FolderObject
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.playback.MediaManager
import com.simplecity.amp_library.ui.modelviews.FolderView
import com.simplecity.amp_library.utils.CustomMediaScanner
import com.simplecity.amp_library.utils.DialogUtils
import com.simplecity.amp_library.utils.FileHelper
import com.simplecity.amp_library.utils.LogUtils
import com.simplecity.amp_library.utils.PlaylistUtils
import com.simplecity.amp_library.utils.menu.MenuUtils
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import java.io.File

object FolderMenuUtils {

    private val TAG = "FolderMenuUtils"

    interface Callbacks {

        fun showToast(message: String)

        fun showToast(@StringRes messageResId: Int)

        fun shareSong(song: Song)

        fun setRingtone(song: Song)

        fun showBiographyDialog(song: Song)

        fun onPlaylistItemsInserted()

        fun onQueueItemsInserted(message: String)

        fun showTagEditor(song: Song)

        fun onFileNameChanged(folderView: FolderView)

        fun onFileDeleted(folderView: FolderView)

        fun playNext(songsSingle: Single<List<Song>>)
    }

    private fun getSongForFile(fileObject: FileObject): Single<Song> {
        return FileHelper.getSong(File(fileObject.path))
            .observeOn(AndroidSchedulers.mainThread())
    }

    private fun getSongsForFolderObject(folderObject: FolderObject): Single<List<Song>> {
        return FileHelper.getSongList(File(folderObject.path), true, false)
    }

    private fun scanFile(fileObject: FileObject, callbacks: Callbacks) {
        CustomMediaScanner.scanFile(fileObject.path, { callbacks.showToast(it) })
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

        val builder = DialogUtils.getBuilder(context)
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
        val builder = DialogUtils.getBuilder(context)
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
                    CustomMediaScanner.scanFiles(listOf(fileObject.path), null)
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

    fun setupFolderMenu(menu: PopupMenu, fileObject: BaseFileObject) {

        menu.inflate(R.menu.menu_file)

        // Add playlist menu
        val sub = menu.menu.findItem(R.id.addToPlaylist).subMenu
        PlaylistUtils.createPlaylistMenu(sub)

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

    fun getFolderMenuClickListener(context: Context, mediaManager: MediaManager, folderView: FolderView, callbacks: Callbacks): PopupMenu.OnMenuItemClickListener? {
        when (folderView.baseFileObject.fileType) {
            FileType.FILE -> return getFileMenuClickListener(context, mediaManager, folderView, folderView.baseFileObject as FileObject, callbacks)
            FileType.FOLDER -> return getFolderMenuClickListener(context, mediaManager, folderView, folderView.baseFileObject as FolderObject, callbacks)
        }
        return null
    }

    private fun getFolderMenuClickListener(context: Context, mediaManager: MediaManager, folderView: FolderView, folderObject: FolderObject, callbacks: Callbacks): PopupMenu.OnMenuItemClickListener {

        return PopupMenu.OnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.play -> {
                    MenuUtils.play(mediaManager, getSongsForFolderObject(folderObject), { callbacks.showToast(it) })
                    return@OnMenuItemClickListener true
                }
                R.id.playNext -> {
                    callbacks.playNext(getSongsForFolderObject(folderObject))
                    return@OnMenuItemClickListener true
                }
                MediaManager.NEW_PLAYLIST -> {
                    MenuUtils.newPlaylist(context, getSongsForFolderObject(folderObject), { callbacks.onPlaylistItemsInserted() })
                    return@OnMenuItemClickListener true
                }
                MediaManager.PLAYLIST_SELECTED -> {
                    MenuUtils.addToPlaylist(
                        context,
                        menuItem.intent.getSerializableExtra(PlaylistUtils.ARG_PLAYLIST) as Playlist,
                        getSongsForFolderObject(folderObject),
                        { callbacks.onPlaylistItemsInserted() })
                    return@OnMenuItemClickListener true
                }
                R.id.addToQueue -> {
                    MenuUtils.addToQueue(mediaManager, getSongsForFolderObject(folderObject), { callbacks.onQueueItemsInserted(it) })
                    return@OnMenuItemClickListener true
                }
                R.id.scan -> {
                    scanFolder(context, folderObject)
                    return@OnMenuItemClickListener true
                }
                R.id.whitelist -> {
                    MenuUtils.whitelist(getSongsForFolderObject(folderObject))
                    return@OnMenuItemClickListener true
                }
                R.id.blacklist -> {
                    MenuUtils.blacklist(getSongsForFolderObject(folderObject))
                    return@OnMenuItemClickListener true
                }
                R.id.rename -> {
                    renameFile(context, folderView, folderObject, callbacks)
                    return@OnMenuItemClickListener true
                }
                R.id.delete -> {
                    deleteFile(context, folderView, folderObject, callbacks)
                    return@OnMenuItemClickListener true
                }
            }
            false
        }
    }

    private fun getFileMenuClickListener(context: Context, mediaManager: MediaManager, folderView: FolderView, fileObject: FileObject, callbacks: Callbacks): PopupMenu.OnMenuItemClickListener {
        return PopupMenu.OnMenuItemClickListener { menuItem ->

            val errorHandler: (Throwable) -> Unit = { e -> LogUtils.logException(TAG, "getFileMenuClickListener threw error", e) }

            when (menuItem.itemId) {
                R.id.playNext -> {
                    getSongForFile(fileObject).subscribe({ song -> MenuUtils.playNext(mediaManager, song, { callbacks.showToast(it) }) }, errorHandler)
                    return@OnMenuItemClickListener true
                }
                MediaManager.NEW_PLAYLIST -> {
                    getSongForFile(fileObject).subscribe({ song -> MenuUtils.newPlaylist(context, listOf(song), { callbacks.onPlaylistItemsInserted() }) }, errorHandler)
                    return@OnMenuItemClickListener true
                }
                MediaManager.PLAYLIST_SELECTED -> {
                    getSongForFile(fileObject).subscribe({ song ->
                        MenuUtils.addToPlaylist(
                            context,
                            menuItem.intent.getSerializableExtra(PlaylistUtils.ARG_PLAYLIST) as Playlist,
                            listOf(song),
                            { callbacks.onPlaylistItemsInserted() })
                    }, errorHandler)
                    return@OnMenuItemClickListener true
                }
                R.id.addToQueue -> {
                    getSongForFile(fileObject).subscribe({ song -> MenuUtils.addToQueue(mediaManager, listOf(song), { callbacks.onQueueItemsInserted(it) }) }, errorHandler)
                    return@OnMenuItemClickListener true
                }
                R.id.scan -> {
                    scanFile(fileObject, callbacks)
                    return@OnMenuItemClickListener true
                }
                R.id.editTags -> {
                    getSongForFile(fileObject).subscribe({ callbacks.showTagEditor(it) }, errorHandler)
                    return@OnMenuItemClickListener true
                }
                R.id.share -> {
                    getSongForFile(fileObject).subscribe({ callbacks.shareSong(it) }, errorHandler)
                    return@OnMenuItemClickListener true
                }
                R.id.ringtone -> {
                    getSongForFile(fileObject).subscribe({ callbacks.setRingtone(it) }, errorHandler)
                    return@OnMenuItemClickListener true
                }
                R.id.songInfo -> {
                    getSongForFile(fileObject).subscribe({ callbacks.showBiographyDialog(it) }, errorHandler)
                    return@OnMenuItemClickListener true
                }
                R.id.blacklist -> {
                    getSongForFile(fileObject).subscribe({ MenuUtils.blacklist(it) }, errorHandler)
                    return@OnMenuItemClickListener true
                }
                R.id.whitelist -> {
                    getSongForFile(fileObject).subscribe({ MenuUtils.whitelist(it) }, errorHandler)
                    return@OnMenuItemClickListener true
                }
                R.id.rename -> {
                    renameFile(context, folderView, fileObject, callbacks)
                    return@OnMenuItemClickListener true
                }
                R.id.delete -> {
                    deleteFile(context, folderView, fileObject, callbacks)
                    return@OnMenuItemClickListener true
                }
            }
            false
        }
    }
}
