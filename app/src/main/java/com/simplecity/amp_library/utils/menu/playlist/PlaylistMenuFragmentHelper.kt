package com.simplecity.amp_library.utils.menu.playlist

import android.widget.Toast
import com.simplecity.amp_library.R
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.rx.UnsafeAction
import com.simplecity.amp_library.ui.fragments.BaseFragment
import com.simplecity.amp_library.utils.DialogUtils
import com.simplecity.amp_library.utils.PlaylistUtils
import io.reactivex.disposables.CompositeDisposable

class PlaylistMenuFragmentHelper(val fragment: BaseFragment, val disposables: CompositeDisposable, callbacks: PlaylistMenuUtils.Callbacks? = null) {

    val callbacks: PlaylistMenuUtils.Callbacks = object : PlaylistMenuUtils.Callbacks {
        override fun showToast(message: String) {
            Toast.makeText(fragment.context, message, Toast.LENGTH_LONG).show()
            callbacks?.showToast(message)
        }

        override fun showToast(messageResId: Int) {
            Toast.makeText(fragment.context, messageResId, Toast.LENGTH_LONG).show()
            callbacks?.showToast(messageResId)
        }

        override fun showWeekSelectorDialog() {
            DialogUtils.showWeekSelectorDialog(fragment.context)
            callbacks?.showWeekSelectorDialog()
        }

        override fun showRenamePlaylistDialog(playlist: Playlist) {
            PlaylistUtils.renamePlaylistDialog(fragment.context, playlist)
            callbacks?.showRenamePlaylistDialog(playlist)
        }

        override fun showCreateM3uPlaylistDialog(playlist: Playlist) {
            PlaylistUtils.createM3uPlaylist(fragment.context, playlist)
            callbacks?.showCreateM3uPlaylistDialog(playlist)
        }

        override fun playNext(playlist: Playlist) {
            fragment.mediaManager.playNext(playlist.songsObservable.first(emptyList<Song>())) { message -> Toast.makeText(fragment.context, message, Toast.LENGTH_LONG).show() }
            callbacks?.playNext(playlist)
        }

        override fun showDeleteConfirmationDialog(playlist: Playlist, onDelete: UnsafeAction) {
            DialogUtils.getBuilder(fragment.context)
                .title(R.string.dialog_title_playlist_delete)
                .content(R.string.dialog_message_playlist_delete, playlist.name)
                .positiveText(R.string.dialog_button_delete)
                .onPositive { _, _ -> onDelete.run() }
                .negativeText(R.string.cancel)
                .show()
            callbacks?.showDeleteConfirmationDialog(playlist, onDelete)
        }

        override fun onPlaylistDeleted() {
            callbacks?.onPlaylistDeleted()
        }
    }
}