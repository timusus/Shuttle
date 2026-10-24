package com.simplecity.amp_library.utils.menu.playlist

import android.app.Activity
import android.content.ContentUris
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.simplecity.amp_library.R
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.ui.fragments.BaseFragment
import com.simplecity.amp_library.utils.DialogUtils
import com.simplecity.amp_library.utils.LogUtils
import com.simplecity.amp_library.utils.PlaylistUtils
import io.reactivex.disposables.CompositeDisposable

open class PlaylistMenuCallbacksAdapter(val fragment: BaseFragment, val disposables: CompositeDisposable) : PlaylistMenuUtils.Callbacks {

    private val deleteRequestLauncher = fragment.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onPlaylistDeleted()
        } else {
            showToast(R.string.playlist_save_failed)
        }
    }

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
        fragment.mediaManager.playNext(playlist.songsObservable.first(emptyList<Song>())) {
                message -> Toast.makeText(fragment.context, message, Toast.LENGTH_LONG).show()
        }
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

    override fun requestPlaylistDeletion(playlist: Playlist) {
        // From API 30 only the system can delete a playlist another app owns, after the user consents.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            showToast(R.string.playlist_save_failed)
            return
        }
        try {
            val uri = ContentUris.withAppendedId(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, playlist.id)
            val pendingIntent = MediaStore.createDeleteRequest(fragment.requireContext().contentResolver, listOf(uri))
            deleteRequestLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
        } catch (e: IllegalArgumentException) {
            LogUtils.logException("PlaylistMenuCallbacks", "Failed to create playlist delete request", e)
            showToast(R.string.playlist_save_failed)
        } catch (e: SecurityException) {
            LogUtils.logException("PlaylistMenuCallbacks", "Failed to create playlist delete request", e)
            showToast(R.string.playlist_save_failed)
        }
    }

    override fun onPlaylistDeleted() {

    }
}