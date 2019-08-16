package com.simplecity.amp_library.ui.screens.playlist.detail

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.support.v4.app.SharedElementCallback
import android.support.v4.view.ViewCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.support.v7.widget.helper.ItemTouchHelper
import android.transition.Transition
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import com.afollestad.aesthetic.Aesthetic
import com.afollestad.aesthetic.Rx.distinctToMainThread
import com.annimon.stream.IntStream
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.gms.cast.framework.CastButtonFactory
import com.simplecity.amp_library.R
import com.simplecity.amp_library.cast.CastManager
import com.simplecity.amp_library.data.Repository
import com.simplecity.amp_library.glide.utils.AlwaysCrossFade
import com.simplecity.amp_library.model.Album
import com.simplecity.amp_library.model.ArtworkProvider
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.ui.screens.tagger.TaggerDialog
import com.simplecity.amp_library.ui.common.BaseFragment
import com.simplecity.amp_library.ui.common.TransitionListenerAdapter
import com.simplecity.amp_library.ui.dialog.DeleteDialog
import com.simplecity.amp_library.ui.dialog.SongInfoDialog
import com.simplecity.amp_library.ui.dialog.WeekSelectorDialog
import com.simplecity.amp_library.ui.modelviews.EmptyView
import com.simplecity.amp_library.ui.modelviews.SelectableViewModel
import com.simplecity.amp_library.ui.modelviews.SongView
import com.simplecity.amp_library.ui.modelviews.SubheaderView
import com.simplecity.amp_library.ui.screens.drawer.DrawerLockManager
import com.simplecity.amp_library.ui.screens.playlist.dialog.CreatePlaylistDialog
import com.simplecity.amp_library.ui.screens.playlist.dialog.DeletePlaylistConfirmationDialog
import com.simplecity.amp_library.ui.screens.playlist.dialog.M3uPlaylistDialog
import com.simplecity.amp_library.ui.screens.playlist.dialog.RenamePlaylistDialog
import com.simplecity.amp_library.ui.views.ContextualToolbar
import com.simplecity.amp_library.ui.views.ContextualToolbarHost
import com.simplecity.amp_library.ui.views.recyclerview.ItemTouchHelperCallback
import com.simplecity.amp_library.utils.ActionBarUtils
import com.simplecity.amp_library.utils.ContextualToolbarHelper
import com.simplecity.amp_library.utils.Operators
import com.simplecity.amp_library.utils.PlaceholderProvider
import com.simplecity.amp_library.utils.ResourceUtils
import com.simplecity.amp_library.utils.RingtoneManager
import com.simplecity.amp_library.utils.SettingsManager
import com.simplecity.amp_library.utils.ShuttleUtils
import com.simplecity.amp_library.utils.StringUtils
import com.simplecity.amp_library.utils.TypefaceManager
import com.simplecity.amp_library.utils.extensions.share
import com.simplecity.amp_library.utils.menu.playlist.PlaylistMenuUtils
import com.simplecity.amp_library.utils.menu.song.SongMenuUtils
import com.simplecity.amp_library.utils.playlists.PlaylistMenuHelper
import com.simplecity.amp_library.utils.sorting.AlbumSortHelper
import com.simplecity.amp_library.utils.sorting.SongSortHelper
import com.simplecity.amp_library.utils.sorting.SortManager
import com.simplecityapps.recycler_adapter.adapter.CompletionListUpdateCallbackAdapter
import com.simplecityapps.recycler_adapter.adapter.ViewModelAdapter
import com.simplecityapps.recycler_adapter.model.ViewModel
import com.simplecityapps.recycler_adapter.recyclerview.RecyclerListener
import dagger.android.support.AndroidSupportInjection
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_detail.background
import kotlinx.android.synthetic.main.fragment_detail.fab
import kotlinx.android.synthetic.main.fragment_detail.recyclerView
import kotlinx.android.synthetic.main.fragment_detail.textProtectionScrim
import kotlinx.android.synthetic.main.fragment_detail.textProtectionScrim2
import kotlinx.android.synthetic.main.fragment_detail.toolbar
import kotlinx.android.synthetic.main.fragment_detail.toolbar_layout
import java.util.ArrayList
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_detail.contextualToolbar as ctxToolbar

@SuppressLint("RestrictedApi")
class PlaylistDetailFragment :
    BaseFragment(),
    PlaylistDetailView,
    Toolbar.OnMenuItemClickListener,
    DrawerLockManager.DrawerLock,
    ContextualToolbarHost {

    private lateinit var playlist: Playlist

    private lateinit var adapter: ViewModelAdapter

    private var disposables = CompositeDisposable()

    private var collapsingToolbarTextColor: ColorStateList? = null
    private var collapsingToolbarSubTextColor: ColorStateList? = null

    private val emptyView = EmptyView(R.string.empty_songlist)

    private var setItemsDisposable: Disposable? = null

    private var contextualToolbarHelper: ContextualToolbarHelper<Single<List<Song>>>? = null

    private var isFirstLoad = true

    private lateinit var presenter: PlaylistDetailPresenter

    @Inject lateinit var presenterFactory: PlaylistDetailPresenter.Factory

    @Inject lateinit var requestManager: RequestManager

    @Inject lateinit var songsRepository: Repository.SongsRepository

    @Inject lateinit var sortManager: SortManager

    @Inject lateinit var settingsManager: SettingsManager

    @Inject lateinit var playlistMenuHelper: PlaylistMenuHelper

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)

        playlist = arguments!!.getSerializable(ARG_PLAYLIST) as Playlist

        presenter = presenterFactory.create(playlist)
    }

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        setHasOptionsMenu(true)

        setEnterSharedElementCallback(enterSharedElementCallback)

        isFirstLoad = true

        adapter = ViewModelAdapter()

        // Todo: On playlist deleted
        //Toast.makeText(getContext(), R.string.playlist_deleted_message, Toast.LENGTH_SHORT).show();
        //getNavigationController().popViewController();
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar!!.setNavigationOnClickListener { v -> navigationController.popViewController() }

        if (ShuttleUtils.canDrawBehindStatusBar()) {
            toolbar!!.layoutParams.height = (ActionBarUtils.getActionBarHeight(context!!) + ActionBarUtils.getStatusBarHeight(context!!)).toInt()
            toolbar!!.setPadding(toolbar!!.paddingLeft, (toolbar!!.paddingTop + ActionBarUtils.getStatusBarHeight(context!!)).toInt(), toolbar!!.paddingRight, toolbar!!.paddingBottom)
        }

        setupToolbarMenu(toolbar!!)

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setRecyclerListener(RecyclerListener())
        recyclerView.adapter = adapter

        if (isFirstLoad) {
            recyclerView.layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_from_bottom)
        }

        toolbar_layout.title = playlist.name
        toolbar_layout.setSubtitle(null)
        toolbar_layout.setExpandedTitleTypeface(TypefaceManager.getInstance().getTypeface(context, TypefaceManager.SANS_SERIF_LIGHT))
        toolbar_layout.setCollapsedTitleTypeface(TypefaceManager.getInstance().getTypeface(context, TypefaceManager.SANS_SERIF))

        setupContextualToolbar()

        val transitionName = arguments!!.getString(ARG_TRANSITION_NAME)
        ViewCompat.setTransitionName(background, transitionName)

        if (isFirstLoad) {
            fab!!.visibility = View.GONE
        }

        fab.setOnClickListener {
            presenter.shuffleAll()
        }

        if (transitionName == null) {
            fadeInUi()
        }

        loadBackgroundImage()

        disposables.add(Aesthetic.get(context)
            .colorPrimary()
            .compose(distinctToMainThread())
            .subscribe { primaryColor ->
                toolbar_layout.setContentScrimColor(primaryColor!!)
                toolbar_layout.setBackgroundColor(primaryColor)
            })

        itemTouchHelper.attachToRecyclerView(recyclerView)

        presenter.bindView(this)
    }

    override fun onPause() {

        DrawerLockManager.getInstance().removeDrawerLock(this)

        super.onPause()
    }

    override fun onResume() {
        super.onResume()

        presenter.loadData()

        DrawerLockManager.getInstance().addDrawerLock(this)
    }

    override fun onDestroyView() {

        if (setItemsDisposable != null) {
            setItemsDisposable!!.dispose()
        }

        disposables.clear()

        presenter.unbindView(this)

        isFirstLoad = false

        super.onDestroyView()
    }

    private fun setupToolbarMenu(toolbar: Toolbar) {
        toolbar.inflateMenu(R.menu.menu_detail_sort)

        if (CastManager.isCastAvailable(context!!, settingsManager)) {
            val menuItem = CastButtonFactory.setUpMediaRouteButton(context, toolbar.menu, R.id.media_route_menu_item)
            menuItem.isVisible = true
        }

        toolbar.setOnMenuItemClickListener(this)

        // Inflate sorting menus
        val item = toolbar.menu.findItem(R.id.sorting)
        activity!!.menuInflater.inflate(R.menu.menu_detail_sort_albums, item.subMenu)
        activity!!.menuInflater.inflate(R.menu.menu_detail_sort_songs, item.subMenu)

        PlaylistMenuUtils.setupPlaylistMenu(toolbar, playlist)

        toolbar.menu.findItem(R.id.editTags).isVisible = true
        toolbar.menu.findItem(R.id.info).isVisible = true
        toolbar.menu.findItem(R.id.artwork).isVisible = true
        toolbar.menu.findItem(R.id.playPlaylist).isVisible = false

        AlbumSortHelper.updateAlbumSortMenuItems(
            toolbar.menu, sortManager.getPlaylistDetailAlbumsSortOrder(playlist),
            sortManager.getPlaylistDetailAlbumsAscending(playlist)
        )
        SongSortHelper.updateSongSortMenuItems(
            toolbar.menu, sortManager.getPlaylistDetailSongsSortOrder(playlist),
            sortManager.getPlaylistDetailSongsAscending(playlist)
        )
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        if (PlaylistMenuUtils.getPlaylistPopupMenuClickListener(playlist, presenter).onMenuItemClick(item)) {
            val albumSortOrder = AlbumSortHelper.handleAlbumDetailMenuSortOrderClicks(item)
            if (albumSortOrder != null) {
                sortManager.setPlaylistDetailAlbumsSortOrder(playlist, albumSortOrder)
                presenter.loadData()
            }
            val albumsAsc = AlbumSortHelper.handleAlbumDetailMenuSortOrderAscClicks(item)
            if (albumsAsc != null) {
                sortManager.setPlaylistDetailAlbumsAscending(playlist, albumsAsc)
                presenter.loadData()
            }
            val songSortOrder = SongSortHelper.handleSongMenuSortOrderClicks(item)
            if (songSortOrder != null) {
                sortManager.setPlaylistDetailSongsSortOrder(playlist, songSortOrder)
                presenter.loadData()
            }
            val songsAsc = SongSortHelper.handleSongDetailMenuSortOrderAscClicks(item)
            if (songsAsc != null) {
                sortManager.setPlaylistDetailSongsAscending(playlist, songsAsc)
                presenter.loadData()
            }

            AlbumSortHelper.updateAlbumSortMenuItems(
                toolbar!!.menu, sortManager.getPlaylistDetailAlbumsSortOrder(playlist),
                sortManager.getPlaylistDetailAlbumsAscending(playlist)
            )
            SongSortHelper.updateSongSortMenuItems(
                toolbar!!.menu, sortManager.getPlaylistDetailSongsSortOrder(playlist),
                sortManager.getPlaylistDetailSongsAscending(playlist)
            )
        }

        return super.onOptionsItemSelected(item)
    }

    private fun loadBackgroundImage() {

        val width = ResourceUtils.getScreenSize().width + ResourceUtils.toPixels(60f)
        val height = resources.getDimensionPixelSize(R.dimen.header_view_height)

        requestManager.load<ArtworkProvider>(null as ArtworkProvider?)
            // Need to override the height/width, as the shared element transition tricks Glide into thinking this ImageView has
            // the same dimensions as the ImageView that the transition starts with.
            // So we'll set it to screen width (plus a little extra, which might fix an issue on some devices..)
            .override(width, height)
            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
            .priority(Priority.HIGH)
            .placeholder(PlaceholderProvider.getInstance(context).getPlaceHolderDrawable(playlist.name, true, settingsManager))
            .centerCrop()
            .animate(AlwaysCrossFade(false))
            .into(background)
    }

    override fun fadeInSlideShowAlbum(previousAlbum: Album?, newAlbum: Album) {
        //This crazy business is what's required to have a smooth Glide crossfade with no 'white flicker'
        requestManager.load(newAlbum)
            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
            .priority(Priority.HIGH)
            .error(PlaceholderProvider.getInstance(context).getPlaceHolderDrawable(newAlbum.name, true, settingsManager))
            .centerCrop()
            .thumbnail(
                Glide
                    .with(this)
                    .load<Album>(previousAlbum)
                    .centerCrop()
            )
            .crossFade(600)
            .into(background)
    }

    override fun setSharedElementEnterTransition(transition: Any?) {
        super.setSharedElementEnterTransition(transition)
        (transition as Transition).addListener(sharedElementEnterTransitionListenerAdapter)
    }

    internal fun fadeInUi() {

        if (textProtectionScrim == null || textProtectionScrim2 == null || fab == null) {
            return
        }

        //Fade in the text protection scrim
        textProtectionScrim!!.alpha = 0f
        textProtectionScrim!!.visibility = View.VISIBLE
        var fadeAnimator = ObjectAnimator.ofFloat(textProtectionScrim, View.ALPHA, 0f, 1f)
        fadeAnimator.duration = 600
        fadeAnimator.start()

        textProtectionScrim2!!.alpha = 0f
        textProtectionScrim2!!.visibility = View.VISIBLE
        fadeAnimator = ObjectAnimator.ofFloat(textProtectionScrim2, View.ALPHA, 0f, 1f)
        fadeAnimator.duration = 600
        fadeAnimator.start()

        //Fade & grow the FAB
        fab!!.alpha = 0f
        fab!!.visibility = View.VISIBLE

        fadeAnimator = ObjectAnimator.ofFloat(fab, View.ALPHA, 0.5f, 1f)
        val scaleXAnimator = ObjectAnimator.ofFloat(fab, View.SCALE_X, 0f, 1f)
        val scaleYAnimator = ObjectAnimator.ofFloat(fab, View.SCALE_Y, 0f, 1f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(fadeAnimator, scaleXAnimator, scaleYAnimator)
        animatorSet.duration = 250
        animatorSet.start()
    }

    override fun setData(data: MutableList<Song>) {
        val viewModels = ArrayList<ViewModel<*>>()

        if (!data.isEmpty()) {
            val items = ArrayList<ViewModel<*>>()

            items.add(SubheaderView(StringUtils.makeSongsAndTimeLabel(context!!, data.size, data.map { song -> song.duration / 1000 }.sum())))

            items.addAll(
                data.map { song ->
                    val songView = SongView(song, requestManager, sortManager, settingsManager)
                    songView.showPlayCount(playlist.type == Playlist.Type.MOST_PLAYED)
                    if (playlist.canEdit && sortManager.getPlaylistDetailSongsSortOrder(playlist) == SortManager.SongSort.DETAIL_DEFAULT) {
                        songView.setEditable(true)
                    }
                    songView.setClickListener(songClickListener)
                    songView
                }.toList()
            )

            viewModels.addAll(items)
        }
        if (viewModels.isEmpty()) {
            viewModels.add(emptyView)
        }

        setItemsDisposable = adapter.setItems(viewModels, object : CompletionListUpdateCallbackAdapter() {
            override fun onComplete() {
                recyclerView?.scheduleLayoutAnimation()
            }
        })
    }

    override fun getContextualToolbar(): ContextualToolbar? {
        return ctxToolbar as ContextualToolbar
    }

    private fun setupContextualToolbar() {

        val contextualToolbar = ContextualToolbar.findContextualToolbar(this)
        if (contextualToolbar != null) {

            contextualToolbar.setTransparentBackground(true)

            contextualToolbar.menu.clear()
            contextualToolbar.inflateMenu(R.menu.context_menu_general)
            val sub = contextualToolbar.menu.findItem(R.id.addToPlaylist).subMenu
            disposables.add(playlistMenuHelper.createUpdatingPlaylistMenu(sub).subscribe())

            contextualToolbar.setOnMenuItemClickListener(
                SongMenuUtils.getSongMenuClickListener(Single.defer { Operators.reduceSongSingles(contextualToolbarHelper!!.items) }, presenter)
            )

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
                    // Need to hide the collapsed text, as it overlaps the contextual toolbar
                    collapsingToolbarTextColor = toolbar_layout.collapsedTitleTextColor
                    collapsingToolbarSubTextColor = toolbar_layout.collapsedSubTextColor
                    toolbar_layout.setCollapsedTitleTextColor(0x01FFFFFF)
                    toolbar_layout.setCollapsedSubTextColor(0x01FFFFFF)

                    toolbar!!.visibility = View.GONE
                }

                override fun finish() {
                    if (toolbar_layout != null && collapsingToolbarTextColor != null && collapsingToolbarSubTextColor != null) {
                        toolbar_layout.collapsedTitleTextColor = collapsingToolbarTextColor!!
                        toolbar_layout.collapsedSubTextColor = collapsingToolbarSubTextColor!!
                    }
                    if (toolbar != null) {
                        toolbar!!.visibility = View.VISIBLE
                    }
                    super.finish()
                }
            }
        }
    }

    private val sharedElementEnterTransitionListenerAdapter: TransitionListenerAdapter
        get() = object : TransitionListenerAdapter() {
            override fun onTransitionEnd(transition: Transition) {
                transition.removeListener(this)
                fadeInUi()
            }
        }

    private val enterSharedElementCallback = object : SharedElementCallback() {
        override fun onSharedElementStart(sharedElementNames: List<String>?, sharedElements: List<View>?, sharedElementSnapshots: List<View>?) {
            super.onSharedElementStart(sharedElementNames, sharedElements, sharedElementSnapshots)

            if (fab != null) {
                fab!!.visibility = View.GONE
            }
        }
    }

    private val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelperCallback(
        { fromPosition, toPosition -> adapter.moveItem(fromPosition, toPosition) },
        { fromPosition, toPosition ->
            val from = adapter.items[fromPosition] as SongView
            val to = adapter.items[toPosition] as SongView

            val songViews = adapter.items
                .filter { itemView -> itemView is SongView }
                .map { itemView -> itemView as SongView }
                .toList()

            val adjustedFrom = IntStream.range(0, songViews.size)
                .filter { i -> from == songViews[i] }
                .findFirst()
                .orElse(-1)

            val adjustedTo = IntStream.range(0, songViews.size)
                .filter { i -> to == songViews[i] }
                .findFirst()
                .orElse(-1)

            if (adjustedFrom != -1 && adjustedTo != -1) {
                playlist.moveSong(context, adjustedFrom, adjustedTo)
            }
        }, {
            // Nothing to do
        },
        { pos ->

        }) {
        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            return if (viewHolder.itemViewType == target.itemViewType) {
                super.onMove(recyclerView, viewHolder, target)
            } else false
        }
    })

    private val songClickListener: SongView.ClickListener = object : SongView.ClickListener {
        override fun onSongClick(position: Int, songView: SongView) {
            if (!contextualToolbarHelper!!.handleClick(songView, Single.just(listOf(songView.song)))) {
                presenter.play(songView.song)
            }
        }

        override fun onSongLongClick(position: Int, songView: SongView): Boolean {
            return contextualToolbarHelper!!.handleLongClick(songView, Single.just(listOf(songView.song)))
        }

        override fun onSongOverflowClick(position: Int, v: View, song: Song) {
            val popupMenu = PopupMenu(v.context, v)
            SongMenuUtils.setupSongMenu(popupMenu, playlist.canEdit, true, playlistMenuHelper)
            popupMenu.setOnMenuItemClickListener(SongMenuUtils.getSongMenuClickListener(song, presenter))
            popupMenu.show()
        }

        override fun onStartDrag(holder: SongView.ViewHolder) {
            itemTouchHelper.startDrag(holder)
        }
    }


    // BaseDetailFragment Implementation

    override fun screenName(): String {
        return "PlaylistDetailFragment"
    }


    // PlaylistDetailView implementation

    override fun closeContextualToolbar() {
        if (contextualToolbarHelper != null) {
            contextualToolbarHelper!!.finish()
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


    // PlaylistMenuContract.View Implementation

    override fun onPlaybackFailed() {
        // Todo: Improve error message
        Toast.makeText(context, R.string.empty_playlist, Toast.LENGTH_SHORT).show()
    }

    override fun presentEditDialog(playlist: Playlist) {
        WeekSelectorDialog().show(childFragmentManager)
    }

    override fun presentRenameDialog(playlist: Playlist) {
        RenamePlaylistDialog.newInstance(playlist).show(childFragmentManager)
    }

    override fun presentM3uDialog(playlist: Playlist) {
        M3uPlaylistDialog.newInstance(playlist).show(childFragmentManager)
    }

    override fun presentDeletePlaylistDialog(playlist: Playlist) {
        DeletePlaylistConfirmationDialog.newInstance(playlist).show(childFragmentManager)
    }


    // Static

    companion object {

        private const val TAG = "BaseDetailFragment"

        private const val ARG_TRANSITION_NAME = "transition_name"

        private const val ARG_PLAYLIST = "playlist"

        fun newInstance(playlist: Playlist): PlaylistDetailFragment {
            val args = Bundle()
            args.putSerializable(ARG_PLAYLIST, playlist)
            val fragment = PlaylistDetailFragment()
            fragment.arguments = args
            return fragment
        }
    }
}