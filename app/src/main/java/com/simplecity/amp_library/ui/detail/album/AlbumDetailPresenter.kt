package com.simplecity.amp_library.ui.detail.album

import android.content.Context
import android.view.MenuItem
import com.simplecity.amp_library.model.Album
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.rx.UnsafeAction
import com.simplecity.amp_library.ui.presenters.Presenter
import com.simplecity.amp_library.utils.*
import io.reactivex.android.schedulers.AndroidSchedulers

class AlbumDetailPresenter constructor(private val album: Album) : Presenter<AlbumDetailView>() {

    private var songs: MutableList<Song> = mutableListOf()

    private fun sortSongs(songs: MutableList<Song>) {
        @SortManager.SongSort val songSort = SortManager.getInstance().albumDetailSongsSortOrder

        val songsAscending = SortManager.getInstance().albumDetailSongsAscending

        SortManager.getInstance().sortSongs(songs, songSort)
        if (!songsAscending) {
            songs.reverse()
        }
    }

    fun loadData() {
        PermissionUtils.RequestStoragePermissions {
            addDisposable(
                album.songsSingle
                    .doOnSuccess { sortSongs(it) }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { songs ->
                        this.songs = songs
                        view?.setData(songs)
                    }
            )
        }
    }

    fun closeContextualToolbar() {
        view?.closeContextualToolbar()
    }

    fun fabClicked() {
        MusicUtils.shuffleAll(songs) { message ->
            view?.showToast(message)
        }
    }

    fun playAll() {
        MusicUtils.playAll(songs, 0, true) { message ->
            view?.showToast(message)
        }
    }

    fun playNext() {
        MusicUtils.playNext(songs) { message ->
            view?.showToast(message)
        }
    }

    fun addToQueue() {
        MusicUtils.addToQueue(songs) { message ->
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
        MusicUtils.playAll(songs, songs.indexOf(song), true) { message ->
            view?.showToast(message)
        }
    }
}
