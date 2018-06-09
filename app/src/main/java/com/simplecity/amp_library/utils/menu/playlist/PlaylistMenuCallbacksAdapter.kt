package com.simplecity.amp_library.utils.menu.playlist

import android.widget.Toast
import com.simplecity.amp_library.R
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.ui.fragments.BaseFragment
import com.simplecity.amp_library.utils.DialogUtils
import com.simplecity.amp_library.utils.PlaylistUtils
import io.reactivex.disposables.CompositeDisposable

open class PlaylistMenuCallbacksAdapter(val fragment: BaseFragment, val disposables: CompositeDisposable) : PlaylistMenuUtils.Callbacks {

    override fun showToast(message: String) {
        Toast.makeText(fragment.context, message, Toast.LENGTH_LONG).show()
    }

    override fun showToast(messageResId: Int) {
        Toast.makeText(fragment.context, messageResId, Toast.LENGTH_LONG).show()
    }

    override fun showWeekSelectorDialog() {
        DialogUtils.showWeekSelectorDialog(fragment.context)
    }

    override fun showRenamePlaylistDialog(playlist: Playlist) {
        PlaylistUtils.renamePlaylistDialog(fragment.context, playlist)
    }

    override fun showCreateM3uPlaylistDialog(playlist: Playlist) {
        PlaylistUtils.createM3uPlaylist(fragment.context, playlist)
    }

    override fun playNext(playlist: Playlist) {
        fragment.mediaManager.playNext(playlist.songsObservable.first(emptyList<Song>())) { message -> Toast.makeText(fragment.context, message, Toast.LENGTH_LONG).show() }
    }

    override fun showDeleteConfirmationDialog(playlist: Playlist, onDelete: () -> Unit) {
        DialogUtils.getBuilder(fragment.context)
            .title(R.string.dialog_title_playlist_delete)
            .content(R.string.dialog_message_playlist_delete, playlist.name)
            .positiveText(R.string.dialog_button_delete)
            .onPositive { _, _ -> onDelete() }
            .negativeText(R.string.cancel)
            .show()
    }

    override fun onPlaylistDeleted() {

    }
}