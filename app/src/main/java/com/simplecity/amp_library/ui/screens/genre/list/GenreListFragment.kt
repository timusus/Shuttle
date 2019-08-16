package com.simplecity.amp_library.ui.screens.genre.list

import android.content.Context
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.simplecity.amp_library.R
import com.simplecity.amp_library.model.Genre
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.ui.adapters.SectionedAdapter
import com.simplecity.amp_library.ui.common.BaseFragment
import com.simplecity.amp_library.ui.modelviews.EmptyView
import com.simplecity.amp_library.ui.modelviews.GenreView
import com.simplecity.amp_library.ui.screens.playlist.dialog.CreatePlaylistDialog
import com.simplecity.amp_library.ui.settings.SettingsParentFragment.ARG_TITLE
import com.simplecity.amp_library.utils.menu.genre.GenreMenuUtils
import com.simplecity.amp_library.utils.playlists.PlaylistMenuHelper
import com.simplecity.amp_library.utils.withArgs
import com.simplecityapps.recycler_adapter.recyclerview.RecyclerListener
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import dagger.android.support.AndroidSupportInjection
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import javax.inject.Inject

class GenreListFragment :
    BaseFragment(),
    GenreView.ClickListener,
    GenreListContract.View {

    private var genreClickListener: GenreClickListener? = null

    private lateinit var recyclerView: FastScrollRecyclerView

    private lateinit var adapter: SectionedAdapter

    private var refreshDisposable: Disposable? = null

    private val disposables = CompositeDisposable()

    @Inject lateinit var presenter: GenreListPresenter

    @Inject lateinit var playlistMenuHelper: PlaylistMenuHelper

    interface GenreClickListener {
        fun onGenreClicked(genre: Genre)
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)

        if (parentFragment is GenreClickListener) {
            genreClickListener = parentFragment as GenreClickListener?
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = SectionedAdapter()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        recyclerView = inflater.inflate(R.layout.fragment_recycler, container, false) as FastScrollRecyclerView
        return recyclerView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setRecyclerListener(RecyclerListener())
        recyclerView.adapter = adapter

        presenter.bindView(this)
    }

    override fun onResume() {
        super.onResume()

        presenter.loadGenres()
    }

    override fun onPause() {

        refreshDisposable?.dispose()

        disposables.clear()

        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        presenter.unbindView(this)
    }

    override fun onItemClick(genre: Genre) {
        genreClickListener?.onGenreClicked(genre)
    }

    override fun onOverflowClick(v: View, genre: Genre) {
        val popupMenu = PopupMenu(context!!, v)
        popupMenu.inflate(R.menu.menu_genre)

        // Add playlist menu
        val subMenu = popupMenu.menu.findItem(R.id.addToPlaylist).subMenu
        playlistMenuHelper.createPlaylistMenu(subMenu)

        popupMenu.setOnMenuItemClickListener(GenreMenuUtils.getGenreClickListener(genre, presenter))
        popupMenu.show()
    }

    // GenreListContract.View Implementation

    override fun setData(genres: List<Genre>) {
        if (genres.isEmpty()) {
            adapter.setItems(listOf(EmptyView(R.string.empty_genres)))
        } else {
            adapter.setItems(genres.map {
                val genreView = GenreView(it)
                genreView.setClickListener(this)
                genreView
            })
        }
    }

    // GenreMenuContract.View Implementation

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

    // BaseFragment Implementation

    override fun screenName(): String {
        return TAG
    }

    // Static

    companion object {

        private const val TAG = "GenreListFragment"

        fun newInstance(title: String) = GenreListFragment().withArgs {
            putString(ARG_TITLE, title)
        }
    }
}
