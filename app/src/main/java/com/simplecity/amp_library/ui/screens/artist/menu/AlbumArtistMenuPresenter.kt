package com.simplecity.amp_library.ui.screens.album.menu

import com.simplecity.amp_library.data.Repository
import com.simplecity.amp_library.model.AlbumArtist
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.playback.MediaManager
import com.simplecity.amp_library.ui.common.Presenter
import com.simplecity.amp_library.ui.screens.album.menu.AlbumArtistMenuContract.View
import com.simplecity.amp_library.ui.screens.drawer.NavigationEventRelay
import com.simplecity.amp_library.ui.screens.drawer.NavigationEventRelay.NavigationEvent
import com.simplecity.amp_library.ui.screens.drawer.NavigationEventRelay.NavigationEvent.Type
import com.simplecity.amp_library.ui.screens.songs.menu.SongMenuPresenter
import com.simplecity.amp_library.utils.LogUtils
import com.simplecity.amp_library.utils.Operators
import com.simplecity.amp_library.utils.extensions.getSongs
import com.simplecity.amp_library.utils.playlists.PlaylistManager
import com.simplecity.amp_library.utils.sorting.SortManager
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class AlbumArtistMenuPresenter @Inject constructor(
    private val playlistManager: PlaylistManager,
    private val songsRepository: Repository.SongsRepository,
    private val mediaManager: MediaManager,
    private val blacklistRepository: Repository.BlacklistRepository,
    private val navigationEventRelay: NavigationEventRelay,
    private val sortManager: SortManager

) : Presenter<View>(), AlbumArtistMenuContract.Presenter {

    override fun createArtistsPlaylist(albumArtists: List<AlbumArtist>) {
        getSongs(albumArtists) { songs ->
            view?.presentCreatePlaylistDialog(songs)
        }
    }

    override fun addArtistsToPlaylist(playlist: Playlist, albumArtists: List<AlbumArtist>) {
        getSongs(albumArtists) { songs ->
            playlistManager.addToPlaylist(playlist, songs) { numSongs ->
                view?.onSongsAddedToPlaylist(playlist, numSongs)
            }
        }
    }

    override fun addArtistsToQueue(albumArtists: List<AlbumArtist>) {
        getSongs(albumArtists) { songs ->
            mediaManager.addToQueue(songs) { numSongs ->
                view?.onSongsAddedToQueue(numSongs)
            }
        }
    }

    override fun playArtistsNext(albumArtists: List<AlbumArtist>) {
        getSongs(albumArtists) { songs ->
            mediaManager.playNext(songs) { numSongs ->
                view?.onSongsAddedToQueue(numSongs)
            }
        }
    }

    override fun play(albumArtist: AlbumArtist) {
        mediaManager.playAll(albumArtist.getSongsSingle(songsRepository)) { view?.onPlaybackFailed() }
    }

    override fun editTags(albumArtist: AlbumArtist) {
        view?.presentTagEditorDialog(albumArtist)
    }

    override fun albumArtistInfo(albumArtist: AlbumArtist) {
        view?.presentAlbumArtistInfoDialog(albumArtist)
    }

    override fun editArtwork(albumArtist: AlbumArtist) {
        view?.presentArtworkEditorDialog(albumArtist)
    }

    override fun blacklistArtists(albumArtists: List<AlbumArtist>) {
        getSongs(albumArtists) { songs -> blacklistRepository.addAllSongs(songs) }
    }

    override fun deleteArtists(albumArtists: List<AlbumArtist>) {
        view?.presentArtistDeleteDialog(albumArtists)
    }

    override fun goToArtist(albumArtist: AlbumArtist) {
        navigationEventRelay.sendEvent(NavigationEvent(Type.GO_TO_ARTIST, albumArtist, true))
    }

    override fun albumShuffle(albumArtist: AlbumArtist) {
        mediaManager.playAll(albumArtist.getSongs(songsRepository)
            .map { songs -> Operators.albumShuffleSongs(songs, sortManager) }) {
            view?.onPlaybackFailed()
            Unit
        }
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

    private fun getSongs(albumArtists: List<AlbumArtist>, onSuccess: (songs: List<Song>) -> Unit) {
        addDisposable(
            albumArtists.getSongs(songsRepository)
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