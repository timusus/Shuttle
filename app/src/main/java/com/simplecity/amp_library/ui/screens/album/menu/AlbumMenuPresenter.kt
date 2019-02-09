package com.simplecity.amp_library.ui.screens.album.menu

import com.simplecity.amp_library.data.Repository
import com.simplecity.amp_library.model.Album
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.playback.MediaManager
import com.simplecity.amp_library.ui.common.Presenter
import com.simplecity.amp_library.ui.screens.album.menu.AlbumMenuContract.View
import com.simplecity.amp_library.ui.screens.drawer.NavigationEventRelay
import com.simplecity.amp_library.ui.screens.drawer.NavigationEventRelay.NavigationEvent
import com.simplecity.amp_library.ui.screens.drawer.NavigationEventRelay.NavigationEvent.Type
import com.simplecity.amp_library.ui.screens.songs.menu.SongMenuPresenter
import com.simplecity.amp_library.utils.LogUtils
import com.simplecity.amp_library.utils.extensions.getSongs
import com.simplecity.amp_library.utils.extensions.getSongsSingle
import com.simplecity.amp_library.utils.playlists.PlaylistManager
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class AlbumMenuPresenter @Inject constructor(
    private val playlistManager: PlaylistManager,
    private val songsRepository: Repository.SongsRepository,
    private val mediaManager: MediaManager,
    private val blacklistRepository: Repository.BlacklistRepository,
    private val albumArtistsRepository: Repository.AlbumArtistsRepository,
    private val navigationEventRelay: NavigationEventRelay
) : Presenter<View>(), AlbumMenuContract.Presenter {

    override fun createPlaylistFromAlbums(albums: List<Album>) {
        getSongs(albums) { songs ->
            view?.presentCreatePlaylistDialog(songs)
        }
    }

    override fun addAlbumsToPlaylist(playlist: Playlist, albums: List<Album>) {
        getSongs(albums) { songs ->
            playlistManager.addToPlaylist(playlist, songs) { numSongs ->
                view?.onSongsAddedToPlaylist(playlist, numSongs)
            }
        }
    }

    override fun addAlbumsToQueue(albums: List<Album>) {
        getSongs(albums) { songs ->
            mediaManager.addToQueue(songs) { numSongs ->
                view?.onSongsAddedToQueue(numSongs)
            }
        }
    }

    override fun playAlbumsNext(albums: List<Album>) {
        getSongs(albums) { songs ->
            mediaManager.playNext(songs) { numSongs ->
                view?.onSongsAddedToQueue(numSongs)
            }
        }
    }

    override fun play(album: Album) {
        mediaManager.playAll(album.getSongsSingle(songsRepository)) { view?.onPlaybackFailed() }
    }

    override fun editTags(album: Album) {
        view?.presentTagEditorDialog(album)
    }

    override fun albumInfo(album: Album) {
        view?.presentAlbumInfoDialog(album)
    }

    override fun editArtwork(album: Album) {
        view?.presentArtworkEditorDialog(album)
    }

    override fun blacklistAlbums(albums: List<Album>) {
        getSongs(albums) { songs -> blacklistRepository.addAllSongs(songs) }
    }

    override fun deleteAlbums(albums: List<Album>) {
        view?.presentDeleteAlbumsDialog(albums)
    }

    override fun goToArtist(album: Album) {
        addDisposable(albumArtistsRepository.getAlbumArtists()
            .first(emptyList())
            .flatMapObservable { Observable.fromIterable(it) }
            .filter { albumArtist -> albumArtist.name == album.albumArtist.name && albumArtist.albums.contains(album) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { albumArtist -> navigationEventRelay.sendEvent(NavigationEvent(Type.GO_TO_ARTIST, albumArtist, true)) },
                { error -> LogUtils.logException(com.simplecity.amp_library.ui.screens.songs.menu.SongMenuPresenter.TAG, "Failed to retrieve album artist", error) }
            ))
    }

    override fun <T> transform(src: Single<List<T>>, dst: (List<T>) -> Unit) {
        addDisposable(
            src
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                    { items -> dst(items) },
                    { error -> LogUtils.logException(SongMenuPresenter.TAG, "Failed to transform src single", error) }
                )
        )
    }

    private fun getSongs(albums: List<Album>, onSuccess: (songs: List<Song>) -> Unit) {
        addDisposable(
            albums.getSongs(songsRepository)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    onSuccess,
                    { error -> LogUtils.logException(TAG, "Failed to retrieve songs", error) }
                )
        )
    }

    companion object {
        const val TAG = "AlbumMenuContract"
    }

}