package com.simplecity.amp_library.utils.menu.album

import android.widget.Toast
import com.simplecity.amp_library.model.Album
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.tagger.TaggerDialog
import com.simplecity.amp_library.ui.dialog.BiographyDialog
import com.simplecity.amp_library.ui.dialog.DeleteDialog
import com.simplecity.amp_library.ui.dialog.UpgradeDialog
import com.simplecity.amp_library.ui.fragments.BaseFragment
import com.simplecity.amp_library.utils.ArtworkDialog
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable

class AlbumMenuCallbacksAdapter(val fragment: BaseFragment, val disposables: CompositeDisposable) : AlbumMenuUtils.Callbacks {

    override fun onPlaylistItemsInserted() {

    }

    override fun onQueueItemsInserted(message: String) {

    }

    override fun playNext(songsSingle: Single<List<Song>>) {
        fragment.mediaManager.playNext(songsSingle) { message -> Toast.makeText(fragment.context, message, Toast.LENGTH_LONG).show() }
    }

    override fun showTagEditor(album: Album) {
        TaggerDialog.newInstance(album).show(fragment.childFragmentManager)
    }

    override fun showDeleteDialog(album: Album) {
        DeleteDialog.newInstance(DeleteDialog.ListAlbumsRef { listOf(album) }).show(fragment.childFragmentManager)
    }

    override fun showDeleteDialog(albums: List<Album>) {
        DeleteDialog.newInstance(DeleteDialog.ListAlbumsRef { albums }).show(fragment.childFragmentManager)
    }

    override fun showDeleteDialog(albums: Single<List<Album>>) {
        disposables.add(albums.subscribe { albumsList ->
            DeleteDialog.newInstance(DeleteDialog.ListAlbumsRef { albumsList }).show(fragment.childFragmentManager)
        })
    }

    override fun showAlbumInfo(album: Album) {
        BiographyDialog.getAlbumBiographyDialog(fragment.context, album.albumArtistName, album.name).show()
    }

    override fun showArtworkChooser(album: Album) {
        ArtworkDialog.build(fragment.context, album).show()
    }

    override fun showUpgradeDialog() {
        UpgradeDialog.getUpgradeDialog(fragment.activity!!).show()
    }

    override fun showToast(message: String) {
        Toast.makeText(fragment.context, message, Toast.LENGTH_LONG).show()
    }
}
