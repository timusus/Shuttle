package com.simplecity.amp_library.utils.menu.song

import android.widget.Toast
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.tagger.TaggerDialog
import com.simplecity.amp_library.ui.dialog.BiographyDialog
import com.simplecity.amp_library.ui.dialog.DeleteDialog
import com.simplecity.amp_library.ui.fragments.BaseFragment
import com.simplecity.amp_library.ui.queue.QueueItem
import com.simplecity.amp_library.utils.ShuttleUtils
import com.simplecity.amp_library.utils.extensions.share
import com.simplecity.amp_library.utils.menu.MenuUtils
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

open class SongMenuCallbacksAdapter(val fragment: BaseFragment, val disposables: CompositeDisposable) : SongMenuUtils.SongCallbacks, SongMenuUtils.SongListCallbacks {

    override fun playNext(song: Song) {
        MenuUtils.playNext(fragment.mediaManager, song, { showToast(it) })
    }

    override fun moveToNext(queueItem: QueueItem) {
        fragment.mediaManager.moveToNext(queueItem)
    }

    override fun newPlaylist(song: Song) {
        MenuUtils.newPlaylist(fragment.context, listOf(song), { onPlaylistItemsInserted(listOf(song)) })
    }

    override fun playlistSelected(playlist: Playlist, song: Song) {
        MenuUtils.addToPlaylist(fragment.context, playlist, listOf(song), { onPlaylistItemsInserted(listOf(song)) })
    }

    override fun addToQueue(song: Song) {
        MenuUtils.addToQueue(fragment.mediaManager, listOf(song), { onQueueItemInserted(it) })
    }

    override fun onQueueItemInserted(message: String) {
        Toast.makeText(fragment.context, message, Toast.LENGTH_LONG).show()
    }

    override fun showBiographyDialog(song: Song) {
        BiographyDialog.getSongInfoDialog(fragment.context!!, song).show()
    }

    override fun showTagEditor(song: Song) {
        TaggerDialog.newInstance(song).show(fragment.childFragmentManager)
    }

    override fun showDeleteDialog(song: Song) {
        DeleteDialog.newInstance(DeleteDialog.ListSongsRef { listOf(song) }).show(fragment.childFragmentManager)
    }

    override fun showToast(message: String) {
        Toast.makeText(fragment.context, message, Toast.LENGTH_LONG).show()
    }

    override fun shareSong(song: Song) {
        song.share(fragment.context!!)
    }

    override fun setRingtone(song: Song) {
        ShuttleUtils.setRingtone(fragment.context, song)
    }

    override fun removeQueueItem(queueItem: QueueItem) {

    }

    override fun removeSong(song: Song) {

    }

    override fun onPlaylistItemsInserted(songs: Single<List<Song>>) {

    }

    override fun onPlaylistItemsInserted(songs: List<Song>) {

    }

    override fun addToQueue(songs: Single<List<Song>>) {
        MenuUtils.addToQueue(fragment.mediaManager, songs, { onQueueItemInserted(it) })
    }

    override fun blacklist(songs: Single<List<Song>>) {
        MenuUtils.blacklist(songs)
    }

    override fun removeQueueItems(queueItems: Single<List<QueueItem>>) {

    }

    override fun playNext(songs: Single<List<Song>>) {
        fragment.mediaManager.playNext(songs) { message -> Toast.makeText(fragment.context, message, Toast.LENGTH_LONG).show() }
    }

    override fun showDeleteDialog(songs: Single<List<Song>>) {
        disposables.add(songs
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { songs -> DeleteDialog.newInstance(DeleteDialog.ListSongsRef { songs }).show(fragment.childFragmentManager) })
    }

    override fun newPlaylist(songs: Single<List<Song>>) {
        MenuUtils.newPlaylist(fragment.context, songs, { onPlaylistItemsInserted(songs) })
    }

    override fun playlistSelected(playlist: Playlist, songs: Single<List<Song>>) {
        MenuUtils.addToPlaylist(fragment.context, playlist, songs, { onPlaylistItemsInserted(songs) })
    }

}