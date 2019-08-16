package com.simplecity.amp_library.ui.screens.suggested

import android.content.Context
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.bumptech.glide.RequestManager
import com.simplecity.amp_library.R
import com.simplecity.amp_library.R.string
import com.simplecity.amp_library.data.Repository
import com.simplecity.amp_library.model.Album
import com.simplecity.amp_library.model.AlbumArtist
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.model.SuggestedHeader
import com.simplecity.amp_library.ui.screens.tagger.TaggerDialog
import com.simplecity.amp_library.ui.adapters.ViewType
import com.simplecity.amp_library.ui.common.BaseFragment
import com.simplecity.amp_library.ui.dialog.AlbumBiographyDialog
import com.simplecity.amp_library.ui.dialog.DeleteDialog
import com.simplecity.amp_library.ui.dialog.SongInfoDialog
import com.simplecity.amp_library.ui.modelviews.AlbumView
import com.simplecity.amp_library.ui.modelviews.EmptyView
import com.simplecity.amp_library.ui.modelviews.HorizontalRecyclerView
import com.simplecity.amp_library.ui.modelviews.SuggestedHeaderView
import com.simplecity.amp_library.ui.modelviews.SuggestedSongView
import com.simplecity.amp_library.ui.screens.playlist.detail.PlaylistDetailFragment
import com.simplecity.amp_library.ui.screens.playlist.dialog.CreatePlaylistDialog
import com.simplecity.amp_library.ui.screens.suggested.SuggestedPresenter.SuggestedData
import com.simplecity.amp_library.ui.views.SuggestedDividerDecoration
import com.simplecity.amp_library.utils.ArtworkDialog
import com.simplecity.amp_library.utils.RingtoneManager
import com.simplecity.amp_library.utils.SettingsManager
import com.simplecity.amp_library.utils.ShuttleUtils
import com.simplecity.amp_library.utils.extensions.share
import com.simplecity.amp_library.utils.menu.album.AlbumMenuUtils
import com.simplecity.amp_library.utils.menu.song.SongMenuUtils
import com.simplecity.amp_library.utils.playlists.FavoritesPlaylistManager
import com.simplecity.amp_library.utils.playlists.PlaylistMenuHelper
import com.simplecity.amp_library.utils.sorting.SortManager
import com.simplecity.amp_library.utils.withArgs
import com.simplecityapps.recycler_adapter.adapter.ViewModelAdapter
import com.simplecityapps.recycler_adapter.model.ViewModel
import com.simplecityapps.recycler_adapter.recyclerview.RecyclerListener
import dagger.android.support.AndroidSupportInjection
import io.reactivex.disposables.CompositeDisposable
import java.util.ArrayList
import javax.inject.Inject

class SuggestedFragment :
    BaseFragment(),
    SuggestedHeaderView.ClickListener,
    AlbumView.ClickListener,
    SuggestedContract.View {

    @Inject lateinit var presenter: SuggestedPresenter

    @Inject lateinit var songsRepository: Repository.SongsRepository

    @Inject lateinit var sortManager: SortManager

    @Inject lateinit var settingsManager: SettingsManager

    @Inject lateinit var favoritesPlaylistManager: FavoritesPlaylistManager

    @Inject lateinit var playlistMenuHelper: PlaylistMenuHelper

    @Inject lateinit var requestManager: RequestManager

    private lateinit var adapter: ViewModelAdapter

    private lateinit var favoriteRecyclerView: HorizontalRecyclerView

    private lateinit var mostPlayedRecyclerView: HorizontalRecyclerView

    private val disposables = CompositeDisposable()

    private val refreshDisposables = CompositeDisposable()

    private var suggestedClickListener: SuggestedClickListener? = null

    interface SuggestedClickListener {

        fun onAlbumArtistClicked(albumArtist: AlbumArtist, transitionView: View)

        fun onAlbumClicked(album: Album, transitionView: View)
    }


    // Lifecycle

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)

        if (parentFragment is SuggestedClickListener) {
            suggestedClickListener = parentFragment as SuggestedClickListener?
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = ViewModelAdapter()
        mostPlayedRecyclerView = HorizontalRecyclerView("SuggestedFragment - mostPlayed")
        favoriteRecyclerView = HorizontalRecyclerView("SuggestedFragment - favorite")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_suggested, container, false) as RecyclerView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view as RecyclerView

        val spanCount = if (ShuttleUtils.isTablet(context!!)) 12 else 6

        val gridLayoutManager = GridLayoutManager(context, spanCount)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                if (!adapter.items.isEmpty() && position >= 0) {
                    val item = adapter.items[position]
                    if (item is HorizontalRecyclerView
                        || item is SuggestedHeaderView
                        || item is AlbumView && item.getViewType() == ViewType.ALBUM_LIST
                        || item is AlbumView && item.getViewType() == ViewType.ALBUM_LIST_SMALL
                        || item is EmptyView
                    ) {
                        return spanCount
                    }
                    if (item is AlbumView && item.getViewType() == ViewType.ALBUM_CARD_LARGE) {
                        return 3
                    }
                }

                return 2
            }
        }

        view.addItemDecoration(SuggestedDividerDecoration(resources))
        view.setRecyclerListener(RecyclerListener())
        view.layoutManager = gridLayoutManager
        view.adapter = adapter

        presenter.bindView(this)
    }

    override fun onResume() {
        super.onResume()

        presenter.loadData()
    }

    override fun onPause() {

        disposables.clear()

        refreshDisposables.clear()

        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        presenter.unbindView(this)
    }

    override fun onDetach() {
        super.onDetach()

        suggestedClickListener = null
    }

    inner class SongClickListener(val songs: List<Song>) : SuggestedSongView.ClickListener {

        override fun onSongClick(song: Song, holder: SuggestedSongView.ViewHolder) {
            mediaManager.playAll(songs, songs.indexOf(song), true) {
                onPlaybackFailed()
            }
        }

        override fun onSongOverflowClicked(v: View, position: Int, song: Song) {
            val popupMenu = PopupMenu(context!!, v)
            SongMenuUtils.setupSongMenu(popupMenu, false, true, playlistMenuHelper)
            popupMenu.setOnMenuItemClickListener(SongMenuUtils.getSongMenuClickListener(song, presenter))
            popupMenu.show()
        }
    }


    // AlbumView.ClickListener implementation

    override fun onAlbumClick(position: Int, albumView: AlbumView, viewHolder: AlbumView.ViewHolder) {
        suggestedClickListener?.onAlbumClicked(albumView.album, viewHolder.imageOne)
    }

    override fun onAlbumLongClick(position: Int, albumView: AlbumView): Boolean {
        return false
    }

    override fun onAlbumOverflowClicked(v: View, album: Album) {
        val menu = PopupMenu(context!!, v)
        AlbumMenuUtils.setupAlbumMenu(menu, playlistMenuHelper, true)
        menu.setOnMenuItemClickListener(AlbumMenuUtils.getAlbumMenuClickListener(album, presenter))
        menu.show()
    }

    override fun onSuggestedHeaderClick(suggestedHeader: SuggestedHeader) {
        navigationController.pushViewController(PlaylistDetailFragment.newInstance(suggestedHeader.playlist), "PlaylistListFragment")
    }


    // SuggestedContract.View implementation

    override fun setData(suggestedData: SuggestedData) {

        val viewModels = ArrayList<ViewModel<*>>()

        if (suggestedData.mostPlayedSongs.isNotEmpty()) {
            val mostPlayedHeader = SuggestedHeader(getString(string.mostplayed), getString(string.suggested_most_played_songs_subtitle), suggestedData.mostPlayedPlaylist)
            val mostPlayedHeaderView = SuggestedHeaderView(mostPlayedHeader)
            mostPlayedHeaderView.setClickListener(this)
            viewModels.add(mostPlayedHeaderView)
            viewModels.add(mostPlayedRecyclerView)

            val songClickListener = SongClickListener(suggestedData.mostPlayedSongs)

            mostPlayedRecyclerView.setItems(suggestedData.mostPlayedSongs
                .map { song ->
                    val suggestedSongView = SuggestedSongView(song, requestManager, settingsManager)
                    suggestedSongView.setClickListener(songClickListener)
                    suggestedSongView
                })
        }

        if (suggestedData.recentlyPlayedAlbums.isNotEmpty()) {
            val recentlyPlayedHeader = SuggestedHeader(getString(string.suggested_recent_title), getString(string.suggested_recent_subtitle), suggestedData.recentlyPlayedPlaylist)
            val recentlyPlayedHeaderView = SuggestedHeaderView(recentlyPlayedHeader)
            recentlyPlayedHeaderView.setClickListener(this)
            viewModels.add(recentlyPlayedHeaderView)

            viewModels.addAll(
                suggestedData.recentlyPlayedAlbums
                    .map { album ->
                        val albumView = AlbumView(album, ViewType.ALBUM_LIST_SMALL, requestManager, sortManager, settingsManager)
                        albumView.setClickListener(this)
                        albumView
                    }.toList()
            )
        }

        if (suggestedData.favoriteSongs.isNotEmpty()) {
            val favoriteSongsHeader = SuggestedHeader(getString(string.fav_title), getString(string.suggested_favorite_subtitle), suggestedData.favoriteSongsPlaylist)
            val favoriteHeaderView = SuggestedHeaderView(favoriteSongsHeader)
            favoriteHeaderView.setClickListener(this)
            viewModels.add(favoriteHeaderView)

            viewModels.add(favoriteRecyclerView)

            val songClickListener = SongClickListener(suggestedData.favoriteSongs)
            analyticsManager.dropBreadcrumb(TAG, "favoriteRecyclerView.setItems()")
            favoriteRecyclerView.setItems(
                suggestedData.favoriteSongs
                    .map { song ->
                        val suggestedSongView = SuggestedSongView(song, requestManager, settingsManager)
                        suggestedSongView.setClickListener(songClickListener)
                        suggestedSongView
                    }.toList()
            )
        }

        if (suggestedData.recentlyAddedAlbums.isNotEmpty()) {
            val recentlyAddedHeader = SuggestedHeader(getString(string.recentlyadded), getString(string.suggested_recently_added_subtitle), suggestedData.recentlyAddedAlbumsPlaylist)
            val recentlyAddedHeaderView = SuggestedHeaderView(recentlyAddedHeader)
            recentlyAddedHeaderView.setClickListener(this)
            viewModels.add(recentlyAddedHeaderView)

            viewModels.addAll(
                suggestedData.recentlyAddedAlbums
                    .map { album ->
                        val albumView = AlbumView(album, ViewType.ALBUM_CARD, requestManager, sortManager, settingsManager)
                        albumView.setClickListener(this)
                        albumView
                    }.toList()
            )
        }

        if (viewModels.isEmpty()) {
            refreshDisposables.add(adapter.setItems(listOf<ViewModel<*>>(EmptyView(R.string.empty_suggested))))
        } else {
            refreshDisposables.add(adapter.setItems(viewModels))
        }
    }


    // SongMenuContract.View Implementation

    override fun presentCreatePlaylistDialog(songs: List<Song>) {
        CreatePlaylistDialog.newInstance(songs).show(childFragmentManager, "CreatePlaylistDialog")
    }

    override fun presentSongInfoDialog(song: Song) {
        SongInfoDialog.newInstance(song).show(childFragmentManager)
    }

    override fun onSongsAddedToPlaylist(playlist: Playlist, numSongs: Int) {
        Toast.makeText(context, context!!.resources.getQuantityString(R.plurals.NNNtrackstoplaylist, numSongs, numSongs), Toast.LENGTH_SHORT).show()
    }

    override fun onSongsAddedToQueue(numSongs: Int) {
        Toast.makeText(context, context!!.resources.getQuantityString(R.plurals.NNNtrackstoqueue, numSongs, numSongs), Toast.LENGTH_SHORT).show()
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


    // AlbumMenuContract.View Implementation

    override fun onPlaybackFailed() {
        // Todo: Improve error message
        Toast.makeText(context, R.string.emptyplaylist, Toast.LENGTH_SHORT).show()
    }

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


    // BaseFragment implementation

    override fun screenName(): String {
        return TAG
    }


    // Static

    companion object {

        private const val ARG_TITLE = "title"

        private const val TAG = "SuggestedFragment"

        fun newInstance(title: String) = SuggestedFragment().withArgs {
            putString(ARG_TITLE, title)
        }
    }
}
