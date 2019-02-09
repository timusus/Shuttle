package com.simplecity.amp_library.ui.screens.search

import android.text.TextUtils
import com.simplecity.amp_library.data.Repository
import com.simplecity.amp_library.model.Album
import com.simplecity.amp_library.model.AlbumArtist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.playback.MediaManager
import com.simplecity.amp_library.ui.common.Presenter
import com.simplecity.amp_library.ui.modelviews.AlbumArtistView
import com.simplecity.amp_library.ui.modelviews.AlbumView
import com.simplecity.amp_library.ui.screens.album.menu.AlbumArtistMenuContract
import com.simplecity.amp_library.ui.screens.album.menu.AlbumArtistMenuPresenter
import com.simplecity.amp_library.ui.screens.album.menu.AlbumMenuContract
import com.simplecity.amp_library.ui.screens.album.menu.AlbumMenuPresenter
import com.simplecity.amp_library.ui.screens.songs.menu.SongMenuContract
import com.simplecity.amp_library.ui.screens.songs.menu.SongMenuPresenter
import com.simplecity.amp_library.utils.LogUtils
import com.simplecity.amp_library.utils.SettingsManager
import com.simplecity.amp_library.utils.StringUtils
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.SingleOperator
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Function3
import io.reactivex.schedulers.Schedulers
import java.util.*
import javax.inject.Inject

class SearchPresenter @Inject
constructor(
        private val mediaManager: MediaManager,
        private val songsRepository: Repository.SongsRepository,
        private val albumsRepository: Repository.AlbumsRepository,
        private val albumArtistsRepository: Repository.AlbumArtistsRepository,
        private val settingsManager: SettingsManager,
        private val songMenuPresenter: SongMenuPresenter,
        private val albumMenuPresenter: AlbumMenuPresenter,
        private val albumArtistsMenuPresenter: AlbumArtistMenuPresenter

) : Presenter<SearchView>(),
        SongMenuContract.Presenter by songMenuPresenter,
        AlbumMenuContract.Presenter by albumMenuPresenter,
        AlbumArtistMenuContract.Presenter by albumArtistsMenuPresenter {

    private var performSearchSubscription: Disposable? = null

    private var query: String? = null

    override fun bindView(view: SearchView) {
        super.bindView(view)

        songMenuPresenter.bindView(view)
        albumMenuPresenter.bindView(view)
        albumArtistsMenuPresenter.bindView(view)

        view.setFilterFuzzyChecked(settingsManager.searchFuzzy)
        view.setFilterArtistsChecked(settingsManager.searchArtists)
        view.setFilterAlbumsChecked(settingsManager.searchAlbums)
    }

    override fun unbindView(view: SearchView) {
        super.unbindView(view)
        songMenuPresenter.unbindView(view)
        albumMenuPresenter.unbindView(view)
        albumArtistsMenuPresenter.unbindView(view)
    }

    fun queryChanged(query: String?) {
        var query = query

        if (TextUtils.isEmpty(query)) {
            query = ""
        }

        if (query == this.query) {
            return
        }

        loadData(query!!)

        this.query = query
    }

    private fun loadData(query: String) {

        val searchView = view

        if (searchView != null) {

            searchView.setLoading(true)

            //We've received a new refresh call. Unsubscribe the in-flight subscription if it exists.
            if (performSearchSubscription != null) {
                performSearchSubscription!!.dispose()
            }

            val albumArtistsObservable = if (settingsManager.searchArtists)
                albumArtistsRepository.getAlbumArtists()
                        .first(emptyList())
                        .lift(AlbumArtistFilterOperator(query))
            else
                Single.just(emptyList())

            val albumsObservable = if (settingsManager.searchAlbums)
                albumsRepository.getAlbums()
                        .first(emptyList())
                        .lift(AlbumFilterOperator(query))
            else
                Single.just(emptyList())

            val songsObservable = songsRepository.getSongs(null as Function1<Song, Boolean>?)
                    .first(emptyList())
                    .lift(SongFilterOperator(query))

            performSearchSubscription = Single.zip<List<AlbumArtist>, List<Album>, List<Song>, SearchResult>(albumArtistsObservable, albumsObservable, songsObservable, Function3 { albumArtists: List<AlbumArtist>, albums: List<Album>, songs: List<Song> -> SearchResult(albumArtists, albums, songs) })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { searchView.setData(it) },
                            { error -> LogUtils.logException(TAG, "Error refreshing adapter", error) }
                    )

            addDisposable(performSearchSubscription!!)
        }
    }

    fun setSearchFuzzy(searchFuzzy: Boolean) {
        settingsManager.searchFuzzy = searchFuzzy
        loadData(query!!)
    }

    fun setSearchArtists(searchArtists: Boolean) {
        settingsManager.searchArtists = searchArtists
        loadData(query!!)
    }

    fun setSearchAlbums(searchAlbums: Boolean) {
        settingsManager.searchAlbums = searchAlbums
        loadData(query!!)
    }

    fun onSongClick(songs: List<Song>, song: Song) {
        val view = view

        mediaManager.playAll(songs, songs.indexOf(song), true) {
            view?.showPlaybackError()
            Unit
        }
    }

    fun onArtistClicked(albumArtistView: AlbumArtistView, viewholder: AlbumArtistView.ViewHolder) {
        val view = view
        view?.goToArtist(albumArtistView.albumArtist, viewholder.imageOne)
    }

    fun onAlbumClick(albumView: AlbumView, viewHolder: AlbumView.ViewHolder) {
        val view = view
        view?.goToAlbum(albumView.album, viewHolder.imageOne)
    }

    private inner class SongFilterOperator internal constructor(internal var filterString: String) : SingleOperator<List<Song>, List<Song>> {

        override fun apply(observer: SingleObserver<in List<Song>>): SingleObserver<in List<Song>> {
            return object : SingleObserver<List<Song>> {
                override fun onSubscribe(d: Disposable) {
                    observer.onSubscribe(d)
                }

                override fun onSuccess(songs: List<Song>) {
                    var songs = songs
                    val songList = songs.filter { song -> song.name != null }
                    songs = (if (settingsManager.searchFuzzy) applyJaroWinklerFilter(songList) else applySongFilter(songList)).toList()
                    observer.onSuccess(songs)
                }

                override fun onError(e: Throwable) {
                    observer.onError(e)
                }
            }
        }

        internal fun applyJaroWinklerFilter(songList: List<Song>): List<Song> {
            return songList.map { song -> JaroWinklerObject(song, filterString, song.name) }
                    .filter { jaroWinklerObject -> jaroWinklerObject.score > SCORE_THRESHOLD || TextUtils.isEmpty(filterString) }
                    .sortedWith(Comparator { a, b -> a.`object`.compareTo(b.`object`) })
                    .sortedWith(Comparator { a, b -> java.lang.Double.compare(b.score, a.score) })
                    .map { jaroWinklerObject -> jaroWinklerObject.`object` }
        }

        internal fun applySongFilter(songStream: List<Song>): List<Song> {
            return songStream.filter { song -> StringUtils.containsIgnoreCase(song.name, filterString) }
        }
    }

    private inner class AlbumFilterOperator internal constructor(internal var filterString: String) : SingleOperator<List<Album>, List<Album>> {

        override fun apply(observer: SingleObserver<in List<Album>>): SingleObserver<in List<Album>> {
            return object : SingleObserver<List<Album>> {
                override fun onSubscribe(d: Disposable) {
                    observer.onSubscribe(d)
                }

                override fun onSuccess(albums: List<Album>) {
                    albums.sortedWith(Comparator { a, b -> a.compareTo(b) })
                    val albumStream = albums.filter { album -> album.name != null }
                    val filteredStream = if (settingsManager.searchFuzzy) applyJaroWinklerAlbumFilter(albumStream) else applyAlbumFilter(albumStream)
                    observer.onSuccess(filteredStream.toList())
                }

                override fun onError(e: Throwable) {
                    observer.onError(e)
                }
            }
        }

        internal fun applyJaroWinklerAlbumFilter(albums: List<Album>): List<Album> {
            return albums.map { album -> JaroWinklerObject(album, filterString, album.name) }
                    .filter { jaroWinklerObject -> jaroWinklerObject.score > SCORE_THRESHOLD || TextUtils.isEmpty(filterString) }
                    .sortedWith(Comparator { a, b -> a.`object`.compareTo(b.`object`) })
                    .sortedWith(Comparator { a, b -> java.lang.Double.compare(b.score, a.score) })
                    .map { jaroWinklerObject -> jaroWinklerObject.`object` }
        }

        internal fun applyAlbumFilter(stream: List<Album>): List<Album> {
            return stream.filter { album -> StringUtils.containsIgnoreCase(album.name, filterString) }
        }
    }

    private inner class AlbumArtistFilterOperator internal constructor(internal var filterString: String) : SingleOperator<List<AlbumArtist>, List<AlbumArtist>> {

        override fun apply(observer: SingleObserver<in List<AlbumArtist>>): SingleObserver<in List<AlbumArtist>> {
            return object : SingleObserver<List<AlbumArtist>> {
                override fun onSubscribe(d: Disposable) {
                    observer.onSubscribe(d)
                }

                override fun onSuccess(albumArtists: List<AlbumArtist>) {
                    Collections.sort(albumArtists) { obj, albumArtist -> obj.compareTo(albumArtist) }
                    val albumArtistList = albumArtists.filter { albumArtist -> albumArtist.name != null }
                    val filteredList = if (settingsManager.searchFuzzy) applyJaroWinklerAlbumArtistFilter(albumArtistList) else applyAlbumArtistFilter(albumArtistList)
                    observer.onSuccess(filteredList.toList())
                }

                override fun onError(e: Throwable) {
                    observer.onError(e)
                }
            }
        }

        internal fun applyJaroWinklerAlbumArtistFilter(stream: List<AlbumArtist>): List<AlbumArtist> {
            return stream.map { albumArtist -> JaroWinklerObject(albumArtist, filterString, albumArtist.name) }
                    .filter { jaroWinklerObject -> jaroWinklerObject.score > SCORE_THRESHOLD || TextUtils.isEmpty(filterString) }
                    .sortedWith(Comparator { a, b -> a.`object`.compareTo(b.`object`) })
                    .sortedWith(Comparator { a, b -> java.lang.Double.compare(b.score, a.score) })
                    .map { jaroWinklerObject -> jaroWinklerObject.`object` }
        }

        internal fun applyAlbumArtistFilter(stream: List<AlbumArtist>): List<AlbumArtist> {
            return stream.filter { albumArtist -> StringUtils.containsIgnoreCase(albumArtist.name, filterString) }
        }
    }

    override fun <T> transform(src: Single<List<T>>, dst: (List<T>) -> Unit) {
        addDisposable(
                src
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                                { items -> dst(items) },
                                { error -> LogUtils.logException(SearchPresenter.TAG, "Failed to transform src single", error) }
                        )
        )
    }

    companion object {

        private const val TAG = "SearchPresenter"

        private const val SCORE_THRESHOLD = 0.80
    }
}
