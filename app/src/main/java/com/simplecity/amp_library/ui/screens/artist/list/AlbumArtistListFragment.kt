package com.simplecity.amp_library.ui.screens.artist.list

import android.content.Context
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.PopupMenu
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.bumptech.glide.RequestManager
import com.simplecity.amp_library.R
import com.simplecity.amp_library.R.string
import com.simplecity.amp_library.model.AlbumArtist
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.ui.adapters.SectionedAdapter
import com.simplecity.amp_library.ui.adapters.ViewType
import com.simplecity.amp_library.ui.common.BaseFragment
import com.simplecity.amp_library.ui.dialog.ArtistBiographyDialog
import com.simplecity.amp_library.ui.dialog.DeleteDialog
import com.simplecity.amp_library.ui.modelviews.AlbumArtistView
import com.simplecity.amp_library.ui.modelviews.EmptyView
import com.simplecity.amp_library.ui.modelviews.SelectableViewModel
import com.simplecity.amp_library.ui.screens.playlist.dialog.CreatePlaylistDialog
import com.simplecity.amp_library.ui.screens.tagger.TaggerDialog
import com.simplecity.amp_library.ui.views.ContextualToolbar
import com.simplecity.amp_library.ui.views.recyclerview.GridDividerDecoration
import com.simplecity.amp_library.utils.ArtworkDialog
import com.simplecity.amp_library.utils.ContextualToolbarHelper
import com.simplecity.amp_library.utils.LogUtils
import com.simplecity.amp_library.utils.SettingsManager
import com.simplecity.amp_library.utils.menu.albumartist.AlbumArtistMenuUtils
import com.simplecity.amp_library.utils.playlists.PlaylistMenuHelper
import com.simplecity.amp_library.utils.sorting.SortManager
import com.simplecity.amp_library.utils.withArgs
import com.simplecityapps.recycler_adapter.adapter.CompletionListUpdateCallbackAdapter
import com.simplecityapps.recycler_adapter.model.ViewModel
import com.simplecityapps.recycler_adapter.recyclerview.RecyclerListener
import com.simplecityapps.recycler_adapter.recyclerview.SpanSizeLookup
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import dagger.android.support.AndroidSupportInjection
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import javax.inject.Inject

class AlbumArtistListFragment :
    BaseFragment(),
    AlbumArtistView.ClickListener,
    AlbumArtistListContract.View {

    private lateinit var albumArtistClickListener: AlbumArtistClickListener

    private lateinit var recyclerView: FastScrollRecyclerView

    private lateinit var layoutManager: GridLayoutManager

    private lateinit var adapter: SectionedAdapter

    private lateinit var spanSizeLookup: SpanSizeLookup

    private var contextualToolbarHelper: ContextualToolbarHelper<AlbumArtist>? = null

    private var playlistMenuDisposable: Disposable? = null

    private var setDataDisposable: Disposable? = null

    @Inject lateinit var requestManager: RequestManager

    @Inject lateinit var presenter: AlbumArtistListPresenter

    @Inject lateinit var sortManager: SortManager

    @Inject lateinit var settingsManager: SettingsManager

    @Inject lateinit var playlistMenuHelper: PlaylistMenuHelper

    interface AlbumArtistClickListener {
        fun onAlbumArtistClicked(albumArtist: AlbumArtist, transitionView: View)
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)

        val parentFragment = parentFragment
        if (parentFragment is AlbumArtistClickListener) {
            albumArtistClickListener = parentFragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        adapter = SectionedAdapter()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        recyclerView = inflater.inflate(R.layout.fragment_recycler, container, false) as FastScrollRecyclerView
        return recyclerView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val spanCount = settingsManager.getArtistColumnCount(context)
        layoutManager = GridLayoutManager(context, spanCount)
        spanSizeLookup = SpanSizeLookup(adapter, spanCount)
        spanSizeLookup.isSpanIndexCacheEnabled = true
        layoutManager.spanSizeLookup = spanSizeLookup

        recyclerView.adapter = adapter
        recyclerView.layoutManager = layoutManager
        recyclerView.addItemDecoration(GridDividerDecoration(resources, 4, true))
        recyclerView.setRecyclerListener(RecyclerListener())

        presenter.bindView(this)
    }

    override fun onResume() {
        super.onResume()

        presenter.loadAlbumArtists(false)

        if (userVisibleHint) {
            setupContextualToolbar()
        }
    }

    override fun onPause() {
        setDataDisposable?.dispose()

        playlistMenuDisposable?.dispose()

        super.onPause()
    }

    override fun onDestroyView() {
        presenter.unbindView(this)
        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater!!.inflate(R.menu.menu_sort_artists, menu)
        inflater.inflate(R.menu.menu_view_as, menu)

        menu!!.addSubMenu(0, MENU_GRID_SIZE, 0, R.string.menu_grid_size)
        val subMenu = menu.findItem(MENU_GRID_SIZE).subMenu

        val spanCountArray = resources.getIntArray(R.array.span_count)
        for (i in spanCountArray.indices) {
            subMenu.add(MENU_GROUP_GRID, spanCountArray[i], i, spanCountArray[i].toString())
        }
        subMenu.setGroupCheckable(MENU_GROUP_GRID, true, true)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        //Strip the 'asc' or 'desc' flag, we just want to know the sort type
        val sortOrder = sortManager.artistsSortOrder

        when (sortOrder) {
            SortManager.ArtistSort.DEFAULT -> menu.findItem(R.id.sort_artist_default)?.isChecked = true
            SortManager.ArtistSort.NAME -> menu.findItem(R.id.sort_artist_name)?.isChecked = true
        }

        menu.findItem(R.id.sort_artist_ascending)?.isChecked = sortManager.artistsAscending

        val displayType = settingsManager.artistDisplayType
        when (displayType) {
            ViewType.ARTIST_LIST -> menu.findItem(R.id.view_as_list)?.isChecked = true
            ViewType.ARTIST_GRID -> menu.findItem(R.id.view_as_grid)?.isChecked = true
            ViewType.ARTIST_CARD -> menu.findItem(R.id.view_as_grid_card)?.isChecked = true
            ViewType.ARTIST_PALETTE -> menu.findItem(R.id.view_as_grid_palette)?.isChecked = true
        }

        val gridMenuItem = menu.findItem(MENU_GRID_SIZE)
        if (displayType == ViewType.ARTIST_LIST) {
            gridMenuItem.isVisible = false
        } else {
            gridMenuItem.isVisible = true
            gridMenuItem.subMenu?.findItem(settingsManager.getArtistColumnCount(context))?.isChecked = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sort_artist_default -> presenter.setAlbumArtistsSortOrder(SortManager.ArtistSort.DEFAULT)
            R.id.sort_artist_name -> presenter.setAlbumArtistsSortOrder(SortManager.ArtistSort.NAME)
            R.id.sort_artist_ascending -> sortManager.artistsAscending = !item.isChecked
            R.id.view_as_list -> {
                val viewType = ViewType.ARTIST_LIST
                settingsManager.artistDisplayType = viewType
                setupListSpan()
                updateViewType(viewType)
            }
            R.id.view_as_grid -> {
                val viewType = ViewType.ARTIST_GRID
                settingsManager.artistDisplayType = viewType
                setupGridSpan()
                updateViewType(viewType)
            }
            R.id.view_as_grid_card -> {
                val viewType = ViewType.ARTIST_CARD
                settingsManager.artistDisplayType = viewType
                setupGridSpan()
                updateViewType(viewType)
            }
            R.id.view_as_grid_palette -> {
                val viewType = ViewType.ARTIST_PALETTE
                settingsManager.artistDisplayType = viewType
                setupGridSpan()
                updateViewType(viewType)
            }
        }

        if (item.groupId == MENU_GROUP_GRID) {
            settingsManager.setArtistColumnCount(context, item.itemId)
            spanSizeLookup.setSpanCount(item.itemId)
            (recyclerView.layoutManager as GridLayoutManager).spanCount = settingsManager.getArtistColumnCount(context)
            adapter.notifyItemRangeChanged(0, adapter.itemCount)
        }

        activity!!.invalidateOptionsMenu()

        return super.onOptionsItemSelected(item)
    }

    private fun setupGridSpan() {
        val spanCount = settingsManager.getArtistColumnCount(context)
        spanSizeLookup.setSpanCount(spanCount)
        layoutManager.spanCount = spanCount
    }

    private fun setupListSpan() {
        val spanCount = resources.getInteger(R.integer.list_num_columns)
        spanSizeLookup.setSpanCount(spanCount)
        layoutManager.spanCount = spanCount
    }

    private fun updateViewType(@ViewType viewType: Int) {
        adapter.items
            .filter { viewModel -> viewModel is AlbumArtistView }
            .forEach { viewModel -> (viewModel as AlbumArtistView).viewType = viewType }
        adapter.notifyItemRangeChanged(0, adapter.itemCount)
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser) {
            setupContextualToolbar()
        } else {
            contextualToolbarHelper?.finish()
        }
    }

    private fun setupContextualToolbar() {
        val contextualToolbar = ContextualToolbar.findContextualToolbar(this)
        if (contextualToolbar != null) {
            contextualToolbar.menu.clear()
            contextualToolbar.inflateMenu(R.menu.context_menu_general)
            val sub = contextualToolbar.menu.findItem(R.id.addToPlaylist).subMenu
            playlistMenuDisposable?.dispose()

            playlistMenuDisposable = playlistMenuHelper.createUpdatingPlaylistMenu(sub).subscribe(
                { },
                { throwable -> LogUtils.logException(TAG, "setupContextualToolbar", throwable) }
            )

            contextualToolbar.setOnMenuItemClickListener(
                AlbumArtistMenuUtils.getAlbumArtistMenuClickListener(Single.defer { Single.just(contextualToolbarHelper!!.items) }, presenter)
            )

            contextualToolbarHelper = ContextualToolbarHelper(context!!, contextualToolbar, object : ContextualToolbarHelper.Callback {
                override fun notifyItemChanged(viewModel: SelectableViewModel) {
                    val index = adapter.items.indexOf(viewModel as ViewModel<*>)
                    if (index >= 0) {
                        adapter.notifyItemChanged(index, 0)
                    }
                }

                override fun notifyDatasetChanged() {
                    adapter.notifyItemRangeChanged(0, adapter.items.size, 0)
                }
            })
        }
    }

    // AlbumArtistContract.View Implementation

    override fun setData(albumArtists: List<AlbumArtist>, scrollToTop: Boolean) {
        setDataDisposable?.dispose()

        if (albumArtists.isEmpty()) {
            setDataDisposable = adapter.setItems(listOf(EmptyView(string.empty_artists)))
        } else {
            val viewModels = albumArtists
                .map { albumArtist ->
                    val albumArtistView = AlbumArtistView(albumArtist, settingsManager.artistDisplayType, requestManager, sortManager, settingsManager)
                    albumArtistView.setClickListener(this)
                    albumArtistView
                }
                .toList()

            setDataDisposable = adapter.setItems(viewModels, object : CompletionListUpdateCallbackAdapter() {
                override fun onComplete() {
                    super.onComplete()
                    if (scrollToTop) {
                        recyclerView.smoothScrollToPosition(0)
                    }
                }
            })
        }
    }

    override fun invalidateOptionsMenu() {
        activity?.invalidateOptionsMenu()
    }

    // AlbumArtistView.ClickListener Implementation

    override fun onAlbumArtistClick(position: Int, albumArtistView: AlbumArtistView, viewholder: AlbumArtistView.ViewHolder) {
        if (!contextualToolbarHelper!!.handleClick(albumArtistView, albumArtistView.albumArtist)) {
            albumArtistClickListener.onAlbumArtistClicked(albumArtistView.albumArtist, viewholder.imageOne)
        }
    }

    override fun onAlbumArtistLongClick(position: Int, albumArtistView: AlbumArtistView): Boolean {
        return contextualToolbarHelper!!.handleLongClick(albumArtistView, albumArtistView.albumArtist)
    }

    override fun onAlbumArtistOverflowClicked(v: View, albumArtist: AlbumArtist) {
        val menu = PopupMenu(context!!, v)
        menu.inflate(R.menu.menu_artist)
        val subMenu = menu.menu.findItem(R.id.addToPlaylist).subMenu
        playlistMenuHelper.createPlaylistMenu(subMenu)
        menu.setOnMenuItemClickListener(AlbumArtistMenuUtils.getAlbumArtistClickListener(albumArtist, presenter))
        menu.show()
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

    // BaseFragment Implementation

    override fun screenName(): String {
        return TAG
    }

    // Static

    companion object {

        private const val ARG_TITLE = "title"

        private const val TAG = "AlbumArtistListFragment"

        private const val MENU_GRID_SIZE = 100
        private const val MENU_GROUP_GRID = 1

        fun newInstance(title: String) = AlbumArtistListFragment().withArgs {
            putString(ARG_TITLE, title)
        }
    }
}
