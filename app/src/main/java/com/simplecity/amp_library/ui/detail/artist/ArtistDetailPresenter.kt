package com.simplecity.amp_library.ui.detail.artist

import android.content.Context
import android.support.v4.util.Pair
import android.view.MenuItem
import com.simplecity.amp_library.model.Album
import com.simplecity.amp_library.model.AlbumArtist
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.playback.MediaManager
import com.simplecity.amp_library.rx.UnsafeAction
import com.simplecity.amp_library.ui.presenters.Presenter
import com.simplecity.amp_library.utils.Operators
import com.simplecity.amp_library.utils.PermissionUtils
import com.simplecity.amp_library.utils.PlaylistUtils
import com.simplecity.amp_library.utils.ShuttleUtils
import com.simplecity.amp_library.utils.sorting.SortManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers

class ArtistDetailPresenter constructor(private val mediaManager: MediaManager, private val albumArtist: AlbumArtist) : Presenter<ArtistDetailView>() {

    private var songs: MutableList<Song> = mutableListOf()

    private fun sortSongs(songs: MutableList<Song>) {
        @SortManager.SongSort val songSort = SortManager.getInstance().artistDetailSongsSortOrder

        val songsAscending = SortManager.getInstance().artistDetailSongsAscending

        SortManager.getInstance().sortSongs(songs, songSort)
        if (!songsAscending) {
            songs.reverse()
        }
    }

    private fun sortAlbums(albums: MutableList<Album>) {
        @SortManager.AlbumSort val albumSort = SortManager.getInstance().artistDetailAlbumsSortOrder

        val albumsAscending = SortManager.getInstance().artistDetailAlbumsAscending

        SortManager.getInstance().sortAlbums(albums, albumSort)
        if (!albumsAscending) {
            albums.reverse()
        }
    }

    fun loadData() {
        PermissionUtils.RequestStoragePermissions {
            addDisposable(
                albumArtist.songsSingle
                    .zipWith<MutableList<Album>, Pair<MutableList<Album>, MutableList<Song>>>(
                        albumArtist.songsSingle.map { songs -> Operators.songsToAlbums(songs) },
                        BiFunction { songs, albums -> Pair(albums, songs) }).subscribeOn(Schedulers.io())
                    .doOnSuccess { pair ->
                        sortAlbums(pair.first!!)
                        sortSongs(pair.second!!)
                    }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { pair ->
                        this.songs = pair.second!!

                        view?.setData(pair)
                    }
            )
        }
    }

    fun closeContextualToolbar() {
        view?.closeContextualToolbar()
    }

    fun fabClicked() {
        mediaManager.shuffleAll(songs) { message ->
            view?.showToast(message)
        }
    }

    fun playAll() {
        mediaManager.playAll(songs, 0, true) { message ->
            view?.showToast(message)
        }
    }

    fun playNext() {
        mediaManager.playNext(songs) { message ->
            view?.showToast(message)
        }
    }

    fun addToQueue() {
        mediaManager.addToQueue(songs) { message ->
            view?.showToast(message)
        }
    }

    fun editTags() {
        if (!ShuttleUtils.isUpgraded()) {
            view?.showUpgradeDialog()
        } else {
            view?.showTaggerDialog()
        }
    }

    fun editArtwork() {
        view?.showArtworkDialog()
    }

    fun showBio() {
        view?.showBioDialog()
    }

    fun newPlaylist() {
        view?.showCreatePlaylistDialog(songs.toList())
    }

    fun playlistSelected(context: Context, item: MenuItem, insertCallback: UnsafeAction) {
        val playlist = item.intent.getSerializableExtra(PlaylistUtils.ARG_PLAYLIST) as Playlist
        PlaylistUtils.addToPlaylist(context, playlist, songs, insertCallback)
    }

    fun songClicked(song: Song) {
        mediaManager.playAll(songs, songs.indexOf(song), true) { message ->
            view?.showToast(message)
        }
    }
}
