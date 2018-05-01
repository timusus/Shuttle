package com.simplecity.amp_library.utils.menu.song

import android.widget.Toast
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.tagger.TaggerDialog
import com.simplecity.amp_library.ui.dialog.BiographyDialog
import com.simplecity.amp_library.ui.dialog.DeleteDialog
import com.simplecity.amp_library.ui.fragments.BaseFragment
import com.simplecity.amp_library.utils.ShuttleUtils
import com.simplecity.amp_library.utils.extensions.share
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class SongMenuFragmentHelper(val fragment: BaseFragment, val disposables: CompositeDisposable, callbacks: SongMenuUtils.Callbacks? = null) {

    val songMenuCallbacks: SongMenuUtils.Callbacks = object : SongMenuUtils.Callbacks {

        override fun onPlaylistItemsInserted() {
            callbacks?.onPlaylistItemsInserted()
        }

        override fun onQueueItemInserted(message: String) {
            Toast.makeText(fragment.context, message, Toast.LENGTH_LONG).show()
            callbacks?.onQueueItemInserted(message)
        }

        override fun onSongsRemoved(songsSingle: Single<List<Song>>) {
            callbacks?.onSongsRemoved(songsSingle)
        }

        override fun onSongRemoved(position: Int, song: Song) {
            callbacks?.onSongRemoved(position, song)
        }

        override fun playNext(songsSingle: Single<List<Song>>) {
            fragment.mediaManager.playNext(songsSingle) { message -> Toast.makeText(fragment.context, message, Toast.LENGTH_LONG).show() }
            callbacks?.playNext(songsSingle)
        }

        override fun showBiographyDialog(song: Song) {
            BiographyDialog.getSongInfoDialog(fragment.context!!, song).show()
            callbacks?.showBiographyDialog(song)
        }

        override fun showDeleteDialog(songsSingle: Single<List<Song>>) {
            disposables.add(songsSingle
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { songs -> DeleteDialog.newInstance(DeleteDialog.ListSongsRef { songs }).show(fragment.childFragmentManager) })
            callbacks?.showDeleteDialog(songsSingle)
        }

        override fun showDeleteDialog(song: Song) {
            DeleteDialog.newInstance(DeleteDialog.ListSongsRef { listOf(song) }).show(fragment.childFragmentManager)
            callbacks?.showDeleteDialog(song)
        }

        override fun showTagEditor(song: Song) {
            TaggerDialog.newInstance(song).show(fragment.childFragmentManager)
            callbacks?.showTagEditor(song)
        }

        override fun showToast(message: String) {
            Toast.makeText(fragment.context, message, Toast.LENGTH_LONG).show()
            callbacks?.showToast(message)
        }

        override fun shareSong(song: Song) {
            song.share(fragment.context!!)
            callbacks?.shareSong(song)
        }

        override fun setRingtone(song: Song) {
            ShuttleUtils.setRingtone(fragment.context, song)
            callbacks?.setRingtone(song)
        }
    }
}
