package com.simplecity.amp_library.ui.screens.search

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.util.Pair
import android.support.v4.view.ViewCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.SearchView
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.annimon.stream.Stream
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.jakewharton.rxbinding2.support.v7.widget.RxSearchView
import com.simplecity.amp_library.R
import com.simplecity.amp_library.data.Repository
import com.simplecity.amp_library.format.PrefixHighlighter
import com.simplecity.amp_library.model.*
import com.simplecity.amp_library.ui.adapters.ViewType
import com.simplecity.amp_library.ui.common.BaseFragment
import com.simplecity.amp_library.ui.dialog.*
import com.simplecity.amp_library.ui.modelviews.*
import com.simplecity.amp_library.ui.screens.album.detail.AlbumDetailFragment
import com.simplecity.amp_library.ui.screens.artist.detail.ArtistDetailFragment
import com.simplecity.amp_library.ui.screens.playlist.dialog.CreatePlaylistDialog
import com.simplecity.amp_library.ui.screens.tagger.TaggerDialog
import com.simplecity.amp_library.ui.views.ContextualToolbar
import com.simplecity.amp_library.ui.views.ContextualToolbarHost
import com.simplecity.amp_library.utils.*
import com.simplecity.amp_library.utils.extensions.getSongsSingle
import com.simplecity.amp_library.utils.extensions.share
import com.simplecity.amp_library.utils.menu.album.AlbumMenuUtils
import com.simplecity.amp_library.utils.menu.albumartist.AlbumArtistMenuUtils
import com.simplecity.amp_library.utils.menu.song.SongMenuUtils
import com.simplecity.amp_library.utils.playlists.PlaylistMenuHelper
import com.simplecity.amp_library.utils.sorting.SortManager
import com.simplecityapps.recycler_adapter.adapter.CompletionListUpdateCallbackAdapter
import com.simplecityapps.recycler_adapter.adapter.ViewModelAdapter
import com.simplecityapps.recycler_adapter.model.ViewModel
import io.reactivex.BackpressureStrategy
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_search.*
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_search.contextualToolbar as ctxToolbar

class SearchFragment :
        BaseFragment(),
        com.simplecity.amp_library.ui.screens.search.SearchView,
        ContextualToolbarHost {

    private var query = ""

    private val adapter = ViewModelAdapter()

    private val loadingView = LoadingView()

    private val disposables = CompositeDisposable()

    private var contextualToolbarHelper: ContextualToolbarHelper<Single<List<Song>>>? = null

    private val emptyView = EmptyView(R.string.empty_search)

    private lateinit var artistsHeader: SearchHeaderView
    private lateinit var albumsHeader: SearchHeaderView
    private lateinit var songsHeader: SearchHeaderView

    private var prefixHighlighter: PrefixHighlighter? = null

    @Inject
    lateinit var requestManager: RequestManager

    @Inject
    lateinit var songsRepository: Repository.SongsRepository

    @Inject
    lateinit var sortManager: SortManager

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var playlistMenuHelper: PlaylistMenuHelper

    @Inject
    lateinit var presenter: SearchPresenter

    private var setDataDisposable: Disposable? = null

    private lateinit var searchView: SearchView

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefixHighlighter = PrefixHighlighter(context)

        requestManager = Glide.with(this)

        query = arguments!!.getString(ARG_QUERY, "")

        emptyView.setHeight(ResourceUtils.toPixels(96f))

        artistsHeader = SearchHeaderView(Header(context!!.getString(R.string.artists_title)))
        albumsHeader = SearchHeaderView(Header(context!!.getString(R.string.albums_title)))
        songsHeader = SearchHeaderView(Header(context!!.getString(R.string.tracks_title)))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar!!.inflateMenu(R.menu.menu_search)
        toolbar!!.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.search_fuzzy -> {
                    item.isChecked = !item.isChecked
                    presenter.setSearchFuzzy(item.isChecked)
                }
                R.id.search_artist -> {
                    item.isChecked = !item.isChecked
                    presenter.setSearchArtists(item.isChecked)
                }
                R.id.search_album -> {
                    item.isChecked = !item.isChecked
                    presenter.setSearchAlbums(item.isChecked)
                }
            }
            false
        }

        setupContextualToolbar()

        val searchItem = toolbar!!.menu.findItem(R.id.search)
        searchItem.expandActionView()
        searchView = searchItem.actionView as SearchView

        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                return false
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                val inputMethodManager = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
                searchItem.actionView!!.handler.postDelayed({ navigationController.popViewController() }, 150)
                return false
            }
        })

        recyclerView!!.layoutManager = LinearLayoutManager(context)
        recyclerView!!.adapter = adapter
    }

    override fun onResume() {
        super.onResume()

        presenter.bindView(this)

        disposables.add(RxSearchView.queryTextChangeEvents(searchView)
                .skip(1)
                .debounce(200, TimeUnit.MILLISECONDS)
                .toFlowable(BackpressureStrategy.LATEST)
                .subscribe { searchViewQueryTextEvent ->
                    query = searchViewQueryTextEvent.queryText().toString()
                    presenter.queryChanged(query)
                })

        presenter.queryChanged(query)
    }

    override fun onPause() {
        disposables.clear()
        presenter.unbindView(this)

        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        if (setDataDisposable != null) {
            setDataDisposable!!.dispose()
        }
    }

    override fun screenName(): String {
        return TAG
    }

    override fun setLoading(loading: Boolean) {
        analyticsManager.dropBreadcrumb(TAG, "setLoading..")
        adapter.setItems(listOf(loadingView))
    }

    override fun setData(searchResult: SearchResult) {
        val prefix = query.toUpperCase().toCharArray()

        val viewModels = ArrayList<ViewModel<*>>()

        if (!searchResult.albumArtists.isEmpty()) {
            viewModels.add(artistsHeader)
            viewModels.addAll(Stream.of(searchResult.albumArtists)
                    .map { albumArtist ->
                        val albumArtistView = AlbumArtistView(albumArtist, ViewType.ARTIST_LIST, requestManager, sortManager, settingsManager)
                        albumArtistView.setClickListener(albumArtistClickListener)
                        albumArtistView.setPrefix(prefixHighlighter, prefix)
                        albumArtistView as ViewModel<*>
                    }
                    .toList())
        }

        if (!searchResult.albums.isEmpty()) {
            viewModels.add(albumsHeader)
            viewModels.addAll(Stream.of(searchResult.albums).map { album ->
                val albumView = AlbumView(album, ViewType.ALBUM_LIST, requestManager, sortManager, settingsManager)
                albumView.setClickListener(albumViewClickListener)
                albumView.setPrefix(prefixHighlighter, prefix)
                albumView
            }.toList())
        }

        if (!searchResult.songs.isEmpty()) {
            viewModels.add(songsHeader)
            viewModels.addAll(Stream.of(searchResult.songs).map { song ->
                val songView = SongView(song, requestManager, sortManager, settingsManager)
                songView.setClickListener(songViewClickListener)
                songView.setPrefix(prefixHighlighter, prefix)
                songView
            }.toList())
        }

        if (viewModels.isEmpty()) {
            viewModels.add(emptyView)
        }

        analyticsManager!!.dropBreadcrumb(TAG, "setData..")
        setDataDisposable = adapter.setItems(viewModels, object : CompletionListUpdateCallbackAdapter() {
            override fun onComplete() {
                super.onComplete()

                recyclerView!!.scrollToPosition(0)
            }
        })
    }

    override fun setFilterFuzzyChecked(checked: Boolean) {
        toolbar!!.menu.findItem(R.id.search_fuzzy).isChecked = checked
    }

    override fun setFilterArtistsChecked(checked: Boolean) {
        toolbar!!.menu.findItem(R.id.search_artist).isChecked = checked
    }

    override fun setFilterAlbumsChecked(checked: Boolean) {
        toolbar!!.menu.findItem(R.id.search_album).isChecked = checked
    }

    override fun showPlaybackError() {
        // Todo: Implement
    }


    // AlbumArtistMenuContract.View Implementation

    override fun presentCreatePlaylistDialog(songs: List<Song>) {
        CreatePlaylistDialog.newInstance(songs).show(childFragmentManager)
    }

    override fun onSongsAddedToPlaylist(playlist: Playlist, numSongs: Int) {
        Toast.makeText(context, context!!.resources.getQuantityString(R.plurals.NNNtrackstoplaylist, numSongs, numSongs), Toast.LENGTH_SHORT).show()
    }

    override fun onSongsAddedToQueue(numSongs: Int) {
        Toast.makeText(context, context!!.resources.getQuantityString(R.plurals.NNNtrackstoqueue, numSongs, numSongs), Toast.LENGTH_SHORT).show()
    }

    override fun onPlaybackFailed() {
        // Todo: Improve error message
        Toast.makeText(context, R.string.emptyplaylist, Toast.LENGTH_SHORT).show()
    }

    override fun presentTagEditorDialog(albumArtist: AlbumArtist) {
        TaggerDialog.newInstance(albumArtist).show(childFragmentManager)
    }

    override fun presentArtistDeleteDialog(albumArtists: List<AlbumArtist>) {
        DeleteDialog.newInstance(DeleteDialog.ListArtistsRef { albumArtists }).show(childFragmentManager)
    }

    override fun presentAlbumArtistInfoDialog(albumArtist: AlbumArtist) {
        ArtistBiographyDialog.newInstance(albumArtist).show(childFragmentManager)
    }

    override fun presentArtworkEditorDialog(albumArtist: AlbumArtist) {
        ArtworkDialog.build(context, albumArtist).show()
    }


    // AlbumMenuContract.View Implementation

    override fun presentTagEditorDialog(album: Album) {
        TaggerDialog.newInstance(album).show(childFragmentManager)
    }

    override fun presentDeleteAlbumsDialog(albums: List<Album>) {
        DeleteDialog.newInstance(DeleteDialog.ListAlbumsRef { albums }).show(childFragmentManager)
    }

    override fun presentAlbumInfoDialog(album: Album) {
        AlbumBiographyDialog.newInstance(album).show(childFragmentManager)
    }

    override fun presentArtworkEditorDialog(album: Album) {
        ArtworkDialog.build(context, album).show()
    }


    // SongMenuContract.View Implementation

    override fun presentSongInfoDialog(song: Song) {
        SongInfoDialog.newInstance(song).show(childFragmentManager)
    }

    override fun presentTagEditorDialog(song: Song) {
        TaggerDialog.newInstance(song).show(childFragmentManager)
    }

    override fun presentDeleteDialog(songs: List<Song>) {
        DeleteDialog.newInstance(DeleteDialog.ListSongsRef { songs }).show(childFragmentManager)
    }

    override fun shareSong(song: Song) {
        song.share(context!!)
    }

    override fun presentRingtonePermissionDialog() {
        RingtoneManager.getDialog(context!!).show()
    }

    override fun showRingtoneSetMessage() {
        Toast.makeText(context, R.string.ringtone_set_new, Toast.LENGTH_SHORT).show()
    }


    override fun goToArtist(albumArtist: AlbumArtist, transitionView: View) {
        val inputMethodManager = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(this.view!!.windowToken, 0)
        val transitionName = ViewCompat.getTransitionName(transitionView)
        searchView.handler.postDelayed({ pushDetailFragment(ArtistDetailFragment.newInstance(albumArtist, transitionName!!), transitionView) }, 50)
    }

    override fun goToAlbum(album: Album, transitionView: View) {
        val inputMethodManager = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(this.view!!.windowToken, 0)
        val transitionName = ViewCompat.getTransitionName(transitionView)
        searchView.handler.postDelayed({ pushDetailFragment(AlbumDetailFragment.newInstance(album, transitionName!!), transitionView) }, 50)
    }

    override fun showUpgradeDialog() {
        UpgradeDialog().show(childFragmentManager)
    }

    private fun pushDetailFragment(fragment: Fragment, transitionView: View?) {

        val transitions = ArrayList<Pair<View, String>>()

        if (transitionView != null) {
            val transitionName = ViewCompat.getTransitionName(transitionView)
            transitions.add(Pair(transitionView, transitionName))

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                val moveTransition = TransitionInflater.from(context).inflateTransition(R.transition.image_transition)
                fragment.sharedElementEnterTransition = moveTransition
                fragment.sharedElementReturnTransition = moveTransition
            }
        }

        navigationController.pushViewController(fragment, "DetailFragment", transitions)
    }

    override fun getContextualToolbar(): ContextualToolbar? {
        return ctxToolbar as ContextualToolbar?
    }

    private fun setupContextualToolbar() {

        val contextualToolbar = ContextualToolbar.findContextualToolbar(this)
        if (contextualToolbar != null) {

            contextualToolbar.menu.clear()
            contextualToolbar.inflateMenu(R.menu.context_menu_general)
            val sub = contextualToolbar.menu.findItem(R.id.addToPlaylist).subMenu
            disposables.add(playlistMenuHelper.createUpdatingPlaylistMenu(sub).subscribe())

            contextualToolbar.setOnMenuItemClickListener(SongMenuUtils.getSongMenuClickListener(
                    Single.defer { Operators.reduceSongSingles(contextualToolbarHelper!!.items) },
                    presenter
            ))

            contextualToolbarHelper = object : ContextualToolbarHelper<Single<List<Song>>>(context!!, contextualToolbar, object : ContextualToolbarHelper.Callback {

                override fun notifyItemChanged(viewModel: SelectableViewModel) {
                    val index = adapter.items.indexOf(viewModel as ViewModel<*>)
                    if (index >= 0) {
                        adapter.notifyItemChanged(index, 0)
                    }
                }

                override fun notifyDatasetChanged() {
                    adapter.notifyItemRangeChanged(0, adapter.items.size, 0)
                }
            }) {
                override fun start() {
                    super.start()

                    toolbar!!.visibility = View.GONE
                }

                override fun finish() {
                    if (toolbar != null) {
                        toolbar!!.visibility = View.VISIBLE
                    }
                    super.finish()
                }
            }
        }
    }

    private val songViewClickListener = object : SongView.ClickListener {

        override fun onSongClick(position: Int, songView: SongView) {
            if (!contextualToolbarHelper!!.handleClick(songView, Single.just(listOf(songView.song)))) {
                presenter.onSongClick(
                        adapter.items
                                .filter { item -> item is SongView }
                                .map { item -> (item as SongView).song }.toList(),
                        songView.song
                )
            }
        }

        override fun onSongLongClick(position: Int, songView: SongView): Boolean {
            return contextualToolbarHelper!!.handleLongClick(songView, Single.just(listOf(songView.song)))
        }

        override fun onSongOverflowClick(position: Int, v: View, song: Song) {
            val menu = PopupMenu(v.context, v)
            SongMenuUtils.setupSongMenu(menu, false, true, playlistMenuHelper)
            menu.setOnMenuItemClickListener(SongMenuUtils.getSongMenuClickListener(song, presenter))
            menu.show()
        }

        override fun onStartDrag(holder: SongView.ViewHolder) {

        }
    }

    private val albumViewClickListener = object : AlbumView.ClickListener {
        override fun onAlbumClick(position: Int, albumView: AlbumView, viewHolder: AlbumView.ViewHolder) {
            if (!contextualToolbarHelper!!.handleClick(albumView, albumView.album.getSongsSingle(songsRepository))) {
                presenter.onAlbumClick(albumView, viewHolder)
            }
        }

        override fun onAlbumLongClick(position: Int, albumView: AlbumView): Boolean {
            return contextualToolbarHelper!!.handleLongClick(albumView, albumView.album.getSongsSingle(songsRepository))
        }

        override fun onAlbumOverflowClicked(v: View, album: Album) {
            val menu = PopupMenu(v.context, v)
            AlbumMenuUtils.setupAlbumMenu(menu, playlistMenuHelper, true)
            menu.setOnMenuItemClickListener(AlbumMenuUtils.getAlbumMenuClickListener(album, presenter))
            menu.show()
        }
    }

    private val albumArtistClickListener = object : AlbumArtistView.ClickListener {
        override fun onAlbumArtistClick(position: Int, albumArtistView: AlbumArtistView, viewholder: AlbumArtistView.ViewHolder) {
            if (!contextualToolbarHelper!!.handleClick(albumArtistView, albumArtistView.albumArtist.getSongsSingle(songsRepository))) {
                presenter.onArtistClicked(albumArtistView, viewholder)
            }
        }

        override fun onAlbumArtistLongClick(position: Int, albumArtistView: AlbumArtistView): Boolean {
            return contextualToolbarHelper!!.handleLongClick(albumArtistView, albumArtistView.albumArtist.getSongsSingle(songsRepository))
        }

        override fun onAlbumArtistOverflowClicked(v: View, albumArtist: AlbumArtist) {
            val menu = PopupMenu(v.context, v)
            menu.inflate(R.menu.menu_artist)
            menu.setOnMenuItemClickListener(
                    AlbumArtistMenuUtils.getAlbumArtistClickListener(albumArtist, presenter))
            menu.show()
        }
    }

    companion object {

        private const val TAG = "SearchFragment"

        const val ARG_QUERY = "query"

        fun newInstance(query: String?) = SearchFragment().withArgs {
            putString(ARG_QUERY, query)
        }
    }
}
