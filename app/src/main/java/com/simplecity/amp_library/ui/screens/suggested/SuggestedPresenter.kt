package com.simplecity.amp_library.ui.screens.suggested

import com.simplecity.amp_library.data.Repository.PlaylistsRepository
import com.simplecity.amp_library.data.Repository.SongsRepository
import com.simplecity.amp_library.model.Album
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.ui.common.Presenter
import com.simplecity.amp_library.ui.screens.album.menu.AlbumMenuPresenter
import com.simplecity.amp_library.ui.screens.songs.menu.SongMenuPresenter
import com.simplecity.amp_library.ui.screens.suggested.SuggestedContract.View
import com.simplecity.amp_library.utils.ComparisonUtils
import com.simplecity.amp_library.utils.LogUtils
import com.simplecity.amp_library.utils.Operators
import com.simplecity.amp_library.utils.extensions.getSongsSingle
import com.simplecity.amp_library.utils.menu.album.AlbumsMenuCallbacks
import com.simplecity.amp_library.utils.menu.song.SongsMenuCallbacks
import com.simplecity.amp_library.utils.playlists.FavoritesPlaylistManager
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Function4
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class SuggestedPresenter @Inject constructor(
    private val songsRepository: SongsRepository,
    private val playlistRepository: PlaylistsRepository,
    private val favoritesPlaylistManager: FavoritesPlaylistManager,
    private val songMenuPresenter: SongMenuPresenter,
    private val albumMenuPresenter: AlbumMenuPresenter
) :
    Presenter<SuggestedContract.View>(),
    SuggestedContract.Presenter,
    SongsMenuCallbacks by songMenuPresenter,
    AlbumsMenuCallbacks by albumMenuPresenter {

    override fun bindView(view: View) {
        super.bindView(view)

        songMenuPresenter.bindView(view)
        albumMenuPresenter.bindView(view)
    }

    override fun unbindView(view: View) {
        super.unbindView(view)

        songMenuPresenter.unbindView(view)
        albumMenuPresenter.unbindView(view)
    }

    data class SuggestedData(
        val mostPlayedPlaylist: Playlist, val mostPlayedSongs: List<Song>,
        val recentlyPlayedPlaylist: Playlist, val recentlyPlayedAlbums: List<Album>,
        val favoriteSongsPlaylist: Playlist, val favoriteSongs: List<Song>,
        val recentlyAddedAlbumsPlaylist: Playlist, val recentlyAddedAlbums: List<Album>
    )

    override fun loadData() {

        val mostPlayedPlaylist = playlistRepository.getMostPlayedPlaylist()
        val recentlyPlayedPlaylist = playlistRepository.getRecentlyPlayedPlaylist()
        val recentlyAddedAlbumsPlaylist = playlistRepository.getRecentlyAddedPlaylist()
        lateinit var favoriteSongsPlaylist: Playlist

        val mostPlayedSongs = songsRepository.getSongs(mostPlayedPlaylist)
            .take(20)

        val recentlyPlayedAlbums = songsRepository.getSongs(recentlyPlayedPlaylist)
            .flatMap { songs -> Observable.just(Operators.songsToAlbums(songs)) }
            .flatMapSingle { albums ->
                Observable.fromIterable(albums)
                    .sorted { a, b -> ComparisonUtils.compareLong(b.lastPlayed, a.lastPlayed) }
                    .concatMapSingle { album ->
                        album.getSongsSingle(songsRepository)
                            .map { songs ->
                                album.numSongs = songs.size
                                album
                            }
                            .filter { a -> a.numSongs > 0 }
                            .toSingle()
                    }
                    .sorted { a, b -> ComparisonUtils.compareLong(b.lastPlayed, a.lastPlayed) }
                    .take(6)
                    .toList()
            }

        val favoriteSongs = favoritesPlaylistManager.getFavoritesPlaylist()
            .flatMapObservable { playlist ->
                favoriteSongsPlaylist = playlist
                songsRepository.getSongs(favoriteSongsPlaylist)
                    .take(20)
            }

        val recentlyAddedAlbums = songsRepository.getSongs(recentlyAddedAlbumsPlaylist)
            .flatMap { songs -> Observable.just(Operators.songsToAlbums(songs)) }
            .flatMapSingle { source ->
                Observable.fromIterable(source)
                    .sorted { a, b -> ComparisonUtils.compareLong(b.dateAdded, a.dateAdded) }
                    .take(10)
                    .toList()
            }

        addDisposable(
            Observable.combineLatest(mostPlayedSongs, recentlyPlayedAlbums, favoriteSongs, recentlyAddedAlbums,
                Function4<List<Song>, List<Album>, List<Song>, List<Album>, SuggestedData> { mostPlayedSongs, recentlyPlayedAlbums, favoriteSongs, recentlyAddedAlbums ->
                    SuggestedData(
                        mostPlayedPlaylist, mostPlayedSongs,
                        recentlyPlayedPlaylist, recentlyPlayedAlbums,
                        favoriteSongsPlaylist, favoriteSongs,
                        recentlyAddedAlbumsPlaylist, recentlyAddedAlbums
                    )
                })
                .subscribe(
                    { suggestedData -> view?.setData(suggestedData) },
                    { error -> LogUtils.logException(TAG, "Failed to load data", error) }
                )
        )
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

    companion object {
        private const val TAG = "SuggestedPresenter"
    }
}