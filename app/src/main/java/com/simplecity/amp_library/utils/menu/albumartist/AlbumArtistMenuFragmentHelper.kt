package com.simplecity.amp_library.utils.menu.albumartist

import android.support.v4.app.Fragment
import android.widget.Toast
import com.simplecity.amp_library.model.AlbumArtist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.tagger.TaggerDialog
import com.simplecity.amp_library.ui.dialog.BiographyDialog
import com.simplecity.amp_library.ui.dialog.DeleteDialog
import com.simplecity.amp_library.ui.dialog.UpgradeDialog
import com.simplecity.amp_library.utils.ArtworkDialog
import com.simplecity.amp_library.utils.MusicUtils
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable

class AlbumArtistMenuFragmentHelper(val fragment: Fragment, val disposables: CompositeDisposable) {

    val callbacks: AlbumArtistMenuUtils.Callbacks = object : AlbumArtistMenuUtils.Callbacks {

        override fun onPlaylistItemsInserted() {

        }

        override fun onQueueItemsInserted(message: String) {

        }

        override fun playNext(songsSingle: Single<List<Song>>) {
            MusicUtils.playNext(songsSingle) { message -> Toast.makeText(fragment.context, message, Toast.LENGTH_LONG).show() }
        }

        override fun showTagEditor(albumArtist: AlbumArtist) {
            TaggerDialog.newInstance(albumArtist).show(fragment.childFragmentManager)
        }

        override fun showDeleteDialog(albumArtist: AlbumArtist) {
            DeleteDialog.newInstance(DeleteDialog.ListArtistsRef { listOf(albumArtist) }).show(fragment.childFragmentManager)
        }

        override fun showDeleteDialog(albumArtists: List<AlbumArtist>) {
            DeleteDialog.newInstance(DeleteDialog.ListArtistsRef { albumArtists }).show(fragment.childFragmentManager)
        }

        override fun showDeleteDialog(albumArtists: Single<MutableList<AlbumArtist>>) {
            disposables.add(albumArtists.subscribe { albumArtists ->
                DeleteDialog.newInstance(DeleteDialog.ListArtistsRef { albumArtists })
            })
        }

        override fun showAlbumArtistInfo(albumArtist: AlbumArtist) {
            BiographyDialog.getArtistBiographyDialog(fragment.context, albumArtist.name).show()
        }

        override fun showArtworkChooser(albumArtist: AlbumArtist) {
            ArtworkDialog.build(fragment.context, albumArtist).show()
        }

        override fun showUpgradeDialog() {
            UpgradeDialog.getUpgradeDialog(fragment.activity!!).show()
        }

        override fun showToast(message: String) {
            Toast.makeText(fragment.context, message, Toast.LENGTH_LONG).show()
        }
    }
}