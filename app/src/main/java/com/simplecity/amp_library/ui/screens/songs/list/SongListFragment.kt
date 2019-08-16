package com.simplecity.amp_library.ui.screens.songs.list

import android.content.Context
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
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
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.ui.adapters.SectionedAdapter
import com.simplecity.amp_library.ui.common.BaseFragment
import com.simplecity.amp_library.ui.dialog.DeleteDialog
import com.simplecity.amp_library.ui.dialog.SongInfoDialog
import com.simplecity.amp_library.ui.modelviews.EmptyView
import com.simplecity.amp_library.ui.modelviews.SelectableViewModel
import com.simplecity.amp_library.ui.modelviews.ShuffleView
import com.simplecity.amp_library.ui.modelviews.SongView
import com.simplecity.amp_library.ui.screens.playlist.dialog.CreatePlaylistDialog
import com.simplecity.amp_library.ui.screens.songs.menu.SongMenuContract
import com.simplecity.amp_library.ui.screens.tagger.TaggerDialog
import com.simplecity.amp_library.ui.views.ContextualToolbar
import com.simplecity.amp_library.utils.ContextualToolbarHelper
import com.simplecity.amp_library.utils.LogUtils
import com.simplecity.amp_library.utils.RingtoneManager
import com.simplecity.amp_library.utils.SettingsManager
import com.simplecity.amp_library.utils.extensions.share
import com.simplecity.amp_library.utils.menu.song.SongMenuUtils
import com.simplecity.amp_library.utils.playlists.PlaylistMenuHelper
import com.simplecity.amp_library.utils.sorting.SongSortHelper
import com.simplecity.amp_library.utils.sorting.SortManager
import com.simplecity.amp_library.utils.withArgs
import com.simplecityapps.recycler_adapter.adapter.CompletionListUpdateCallbackAdapter
import com.simplecityapps.recycler_adapter.model.ViewModel
import com.simplecityapps.recycler_adapter.recyclerview.RecyclerListener
import dagger.android.support.AndroidSupportInjection
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_recycler.recyclerView
import javax.inject.Inject

class SongListFragment :
    BaseFragment(),
    SongView.ClickListener,
    ShuffleView.ShuffleClickListener,
    SongListContract.View,
    SongMenuContract.View {

    private val adapter = SectionedAdapter()

    private val shuffleView = ShuffleView()

    private var contextualToolbarHelper: ContextualToolbarHelper<Song>? = null

    private var setDataDisposable: Disposable? = null

    private var playlistMenuDisposable: Disposable? = null

    private val menuDisposables = CompositeDisposable()

    @Inject lateinit var requestManager: RequestManager

    @Inject lateinit var songsPresenter: SongListPresenter

    @Inject lateinit var sortManager: SortManager

    @Inject lateinit var settingsManager: SettingsManager

    @Inject lateinit var playlistMenuHelper: PlaylistMenuHelper

    // Lifecycle

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        setHasOptionsMenu(true)

        shuffleView.setClickListener(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_recycler, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setRecyclerListener(RecyclerListener())
        recyclerView.adapter = adapter

        songsPresenter.bindView(this)
    }

    override fun onResume() {
        super.onResume()

        songsPresenter.loadSongs()

        if (userVisibleHint) {
            setupContextualToolbar()
        }
    }

    override fun onPause() {

        setDataDisposable?.dispose()

        playlistMenuDisposable?.dispose()

        menuDisposables.clear()

        super.onPause()
    }

    override fun onDestroyView() {
        songsPresenter.unbindView(this)
        super.onDestroyView()
    }


    // Options Menu

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater!!.inflate(R.menu.menu_sort_songs, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        super.onPrepareOptionsMenu(menu)
        SongSortHelper.updateSongSortMenuItems(menu!!, sortManager.songsSortOrder, sortManager.songsAscending)
        menu.findItem(R.id.showArtwork).isChecked = settingsManager.showArtworkInSongList()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val songSortOder = SongSortHelper.handleSongMenuSortOrderClicks(item!!)
        if (songSortOder != null) {
            songsPresenter.setSongsSortOrder(songSortOder)
            return true
        }
        val songsAsc = SongSortHelper.handleSongDetailMenuSortOrderAscClicks(item)
        if (songsAsc != null) {
            songsPresenter.setSongsAscending(songsAsc)
            return true
        }

        if (item.itemId == R.id.showArtwork) {
            songsPresenter.setShowArtwork(!item.isChecked)
        }

        return super.onOptionsItemSelected(item)
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
            playlistMenuDisposable = playlistMenuHelper.createUpdatingPlaylistMenu(sub)
                .doOnError { throwable -> LogUtils.logException(TAG, "setupContextualToolbar error", throwable) }
                .subscribe()

            contextualToolbarHelper = ContextualToolbarHelper(context, contextualToolbar, object : ContextualToolbarHelper.Callback {
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

            contextualToolbar.setOnMenuItemClickListener(
                SongMenuUtils.getSongMenuClickListener(
                    Single.defer { Single.just(contextualToolbarHelper!!.items) },
                    songsPresenter
                )
            )
        }
    }


    // SongListContract.View Implementation

    override fun setData(songs: List<Song>, scrollToTop: Boolean) {
        setDataDisposable?.dispose()

        val showArtwork = settingsManager.showArtworkInSongList()

        if (songs.isEmpty()) {
            setDataDisposable = adapter.setItems(listOf(EmptyView(R.string.empty_songlist)))
        } else {

            val viewModels = mutableListOf<ViewModel<*>>(shuffleView)
            viewModels.addAll(
                songs
                    .map { song ->
                        val songView = SongView(song, requestManager, sortManager, settingsManager)
                        songView.setClickListener(this)
                        songView.showAlbumArt(showArtwork)
                        songView as ViewModel<*>
                    }
                    .toList())

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

    override fun showPlaybackError() {
        Toast.makeText(context, R.string.empty_playlist, Toast.LENGTH_SHORT).show()
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
        val string = context!!.resources.getQuantityString(R.plurals.NNNtrackstoqueue, numSongs, numSongs)
        Toast.makeText(context, string, Toast.LENGTH_SHORT).show()
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

    // SongView.ClickListener Implementation

    override fun onSongClick(position: Int, songView: SongView) {
        if (!contextualToolbarHelper!!.handleClick(songView, songView.song)) {
            songsPresenter.play(songView.song)
        }
    }

    override fun onSongOverflowClick(position: Int, view: View, song: Song) {
        val menu = PopupMenu(context!!, view)
        SongMenuUtils.setupSongMenu(menu, false, true, playlistMenuHelper)
        menu.setOnMenuItemClickListener(SongMenuUtils.getSongMenuClickListener(song, songsPresenter))
        menu.show()
    }

    override fun onSongLongClick(position: Int, songView: SongView): Boolean {
        return contextualToolbarHelper!!.handleLongClick(songView, songView.song)
    }

    override fun onStartDrag(viewHolder: SongView.ViewHolder) {
        // Nothing to do
    }


    // ShuffleView.OnClickListener Implementation

    override fun onShuffleItemClick() {
        songsPresenter.shuffleAll()
    }


    // BaseFragment Implementation

    override fun screenName(): String {
        return TAG
    }


    // Static

    companion object {

        private const val TAG = "SongFragment"

        private const val ARG_TITLE = "title"

        fun newInstance(title: String) = SongListFragment().withArgs {
            putString(ARG_TITLE, title)
        }
    }
}
