package com.simplecity.amp_library.ui.screens.artist.detail

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.SharedElementCallback
import android.support.v4.util.Pair
import android.support.v4.view.ViewCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.Toolbar
import android.transition.Transition
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import butterknife.ButterKnife
import butterknife.Unbinder
import com.afollestad.aesthetic.Aesthetic
import com.afollestad.aesthetic.Rx.distinctToMainThread
import com.bumptech.glide.Priority
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.gms.cast.framework.CastButtonFactory
import com.simplecity.amp_library.R
import com.simplecity.amp_library.cast.CastManager
import com.simplecity.amp_library.data.Repository
import com.simplecity.amp_library.glide.utils.AlwaysCrossFade
import com.simplecity.amp_library.model.Album
import com.simplecity.amp_library.model.AlbumArtist
import com.simplecity.amp_library.model.ArtworkProvider
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.ui.common.BaseFragment
import com.simplecity.amp_library.ui.common.TransitionListenerAdapter
import com.simplecity.amp_library.ui.dialog.AlbumBiographyDialog
import com.simplecity.amp_library.ui.dialog.ArtistBiographyDialog
import com.simplecity.amp_library.ui.dialog.DeleteDialog
import com.simplecity.amp_library.ui.dialog.SongInfoDialog
import com.simplecity.amp_library.ui.modelviews.AlbumView
import com.simplecity.amp_library.ui.modelviews.EmptyView
import com.simplecity.amp_library.ui.modelviews.HorizontalAlbumView
import com.simplecity.amp_library.ui.modelviews.HorizontalRecyclerView
import com.simplecity.amp_library.ui.modelviews.SelectableViewModel
import com.simplecity.amp_library.ui.modelviews.SongView
import com.simplecity.amp_library.ui.modelviews.SubheaderView
import com.simplecity.amp_library.ui.screens.album.detail.AlbumDetailFragment
import com.simplecity.amp_library.ui.screens.drawer.DrawerLockManager
import com.simplecity.amp_library.ui.screens.playlist.dialog.CreatePlaylistDialog
import com.simplecity.amp_library.ui.screens.tagger.TaggerDialog
import com.simplecity.amp_library.ui.views.ContextualToolbar
import com.simplecity.amp_library.ui.views.ContextualToolbarHost
import com.simplecity.amp_library.utils.ActionBarUtils
import com.simplecity.amp_library.utils.ArtworkDialog
import com.simplecity.amp_library.utils.ContextualToolbarHelper
import com.simplecity.amp_library.utils.Operators
import com.simplecity.amp_library.utils.PlaceholderProvider
import com.simplecity.amp_library.utils.ResourceUtils
import com.simplecity.amp_library.utils.RingtoneManager
import com.simplecity.amp_library.utils.SettingsManager
import com.simplecity.amp_library.utils.ShuttleUtils
import com.simplecity.amp_library.utils.StringUtils
import com.simplecity.amp_library.utils.TypefaceManager
import com.simplecity.amp_library.utils.extensions.getSongsSingle
import com.simplecity.amp_library.utils.extensions.share
import com.simplecity.amp_library.utils.menu.album.AlbumMenuUtils
import com.simplecity.amp_library.utils.menu.albumartist.AlbumArtistMenuUtils
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
class ArtistDetailFragment :
    BaseFragment(),
    ArtistDetailView,
    Toolbar.OnMenuItemClickListener,
    DrawerLockManager.DrawerLock,
    ContextualToolbarHost {

    private lateinit var albumArtist: AlbumArtist

    private lateinit var adapter: ViewModelAdapter

    private lateinit var presenter: ArtistDetailPresenter

    @Inject lateinit var presenterFactory: ArtistDetailPresenter.Factory

    @Inject lateinit var requestManager: RequestManager

    @Inject lateinit var songsRepository: Repository.SongsRepository

    @Inject lateinit var sortManager: SortManager

    @Inject lateinit var settingsManager: SettingsManager

    @Inject lateinit var playlistMenuHelper: PlaylistMenuHelper

    private val disposables = CompositeDisposable()

    private var collapsingToolbarTextColor: ColorStateList? = null
    private var collapsingToolbarSubTextColor: ColorStateList? = null

    private val emptyView = EmptyView(R.string.empty_songlist)

    private val horizontalRecyclerView = HorizontalRecyclerView("BaseDetail - horizontal")

    private var setHorizontalItemsDisposable: Disposable? = null

    private var setItemsDisposable: Disposable? = null

    private var contextualToolbarHelper: ContextualToolbarHelper<Single<List<Song>>>? = null

    private var unbinder: Unbinder? = null

    private var isFirstLoad = true

    private val sharedElementEnterTransitionListenerAdapter: TransitionListenerAdapter
        get() = object : TransitionListenerAdapter() {
            override fun onTransitionEnd(transition: Transition) {
                transition.removeListener(this)
                fadeInUi()
            }
        }

    private val songClickListener = object : SongView.ClickListener {
        override fun onSongClick(position: Int, songView: SongView) {
            if (!contextualToolbarHelper!!.handleClick(songView, Single.just(listOf(songView.song)))) {
                presenter.songClicked(songView.song)
            }
        }

        override fun onSongLongClick(position: Int, songView: SongView): Boolean {
            return contextualToolbarHelper!!.handleLongClick(songView, Single.just(listOf(songView.song)))
        }

        override fun onSongOverflowClick(position: Int, v: View, song: Song) {
            val popupMenu = PopupMenu(v.context, v)
            SongMenuUtils.setupSongMenu(popupMenu, false, true, playlistMenuHelper)
            popupMenu.setOnMenuItemClickListener(SongMenuUtils.getSongMenuClickListener(song, presenter))
            popupMenu.show()
        }

        override fun onStartDrag(holder: SongView.ViewHolder) {

        }
    }

    private val albumClickListener = object : AlbumView.ClickListener {

        override fun onAlbumClick(position: Int, albumView: AlbumView, viewHolder: AlbumView.ViewHolder) {
            if (!contextualToolbarHelper!!.handleClick(albumView, albumView.album.getSongsSingle(songsRepository))) {
                pushDetailFragment(AlbumDetailFragment.newInstance(albumView.album, ViewCompat.getTransitionName(viewHolder.imageOne)!!), viewHolder.imageOne)
            }
        }

        override fun onAlbumLongClick(position: Int, albumView: AlbumView): Boolean {
            return contextualToolbarHelper!!.handleLongClick(albumView, albumView.album.getSongsSingle(songsRepository))
        }

        override fun onAlbumOverflowClicked(v: View, album: Album) {
            val popupMenu = PopupMenu(v.context, v)
            AlbumMenuUtils.setupAlbumMenu(popupMenu, playlistMenuHelper, false)
            popupMenu.setOnMenuItemClickListener(AlbumMenuUtils.getAlbumMenuClickListener(album, presenter))
            popupMenu.show()
        }
    }

    private val enterSharedElementCallback = object : SharedElementCallback() {
        override fun onSharedElementStart(sharedElementNames: List<String>?, sharedElements: List<View>?, sharedElementSnapshots: List<View>?) {
            super.onSharedElementStart(sharedElementNames, sharedElements, sharedElementSnapshots)

            fab?.visibility = View.GONE
        }
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)

        albumArtist = arguments!!.getSerializable(ARG_ALBUM_ARTIST) as AlbumArtist
    }

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        presenter = presenterFactory.create(albumArtist)

        adapter = ViewModelAdapter()

        setHasOptionsMenu(true)

        setEnterSharedElementCallback(enterSharedElementCallback)

        isFirstLoad = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        unbinder = ButterKnife.bind(this, view)

        toolbar.setNavigationOnClickListener { navigationController.popViewController() }

        if (ShuttleUtils.canDrawBehindStatusBar()) {
            toolbar.layoutParams.height = (ActionBarUtils.getActionBarHeight(context!!) + ActionBarUtils.getStatusBarHeight(context!!)).toInt()
            toolbar.setPadding(toolbar.paddingLeft, (toolbar.paddingTop + ActionBarUtils.getStatusBarHeight(context!!)).toInt(), toolbar.paddingRight, toolbar.paddingBottom)
        }

        setupToolbarMenu(toolbar)

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setRecyclerListener(RecyclerListener())
        recyclerView.adapter = adapter

        if (isFirstLoad) {
            recyclerView.layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_from_bottom)
        }

        toolbar_layout.title = albumArtist.name
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

        presenter.bindView(this)
    }

    override fun onResume() {
        super.onResume()

        presenter.loadData()

        DrawerLockManager.getInstance().addDrawerLock(this)
    }

    override fun onPause() {

        DrawerLockManager.getInstance().removeDrawerLock(this)

        super.onPause()
    }

    override fun onDestroyView() {

        if (setItemsDisposable != null) {
            setItemsDisposable!!.dispose()
        }

        if (setHorizontalItemsDisposable != null) {
            setHorizontalItemsDisposable!!.dispose()
        }

        disposables.clear()

        presenter.unbindView(this)

        unbinder!!.unbind()

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

        // Create playlist menu
        val sub = toolbar.menu.findItem(R.id.addToPlaylist).subMenu
        disposables.add(playlistMenuHelper.createUpdatingPlaylistMenu(sub).subscribe())

        // Inflate sorting menus
        val item = toolbar.menu.findItem(R.id.sorting)
        activity!!.menuInflater.inflate(R.menu.menu_detail_sort_albums, item.subMenu)
        activity!!.menuInflater.inflate(R.menu.menu_detail_sort_songs, item.subMenu)

        toolbar.menu.findItem(R.id.editTags).isVisible = true
        toolbar.menu.findItem(R.id.info).isVisible = true
        toolbar.menu.findItem(R.id.artwork).isVisible = true

        AlbumSortHelper.updateAlbumSortMenuItems(toolbar.menu, sortManager.artistDetailAlbumsSortOrder, sortManager.artistDetailAlbumsAscending)
        SongSortHelper.updateSongSortMenuItems(toolbar.menu, sortManager.artistDetailSongsSortOrder, sortManager.artistDetailSongsAscending)
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        if (!AlbumArtistMenuUtils.getAlbumArtistClickListener(albumArtist, presenter).onMenuItemClick(item)) {
            val albumSortOrder = AlbumSortHelper.handleAlbumDetailMenuSortOrderClicks(item)
            if (albumSortOrder != null) {
                sortManager.artistDetailAlbumsSortOrder = albumSortOrder
                presenter.loadData()
            }
            val albumsAsc = AlbumSortHelper.handleAlbumDetailMenuSortOrderAscClicks(item)
            if (albumsAsc != null) {
                sortManager.artistDetailAlbumsAscending = albumsAsc
                presenter.loadData()
            }
            val songSortOrder = SongSortHelper.handleSongMenuSortOrderClicks(item)
            if (songSortOrder != null) {
                sortManager.artistDetailSongsSortOrder = songSortOrder
                presenter.loadData()
            }
            val songsAsc = SongSortHelper.handleSongDetailMenuSortOrderAscClicks(item)
            if (songsAsc != null) {
                sortManager.artistDetailSongsAscending = songsAsc
                presenter.loadData()
            }

            AlbumSortHelper.updateAlbumSortMenuItems(toolbar.menu, sortManager.artistDetailAlbumsSortOrder, sortManager.artistDetailAlbumsAscending)
            SongSortHelper.updateSongSortMenuItems(toolbar.menu, sortManager.artistDetailSongsSortOrder, sortManager.artistDetailSongsAscending)
        }

        return super.onOptionsItemSelected(item)
    }

    private fun loadBackgroundImage() {
        val width = ResourceUtils.getScreenSize().width + ResourceUtils.toPixels(60f)
        val height = resources.getDimensionPixelSize(R.dimen.header_view_height)

        requestManager.load<ArtworkProvider>(albumArtist as ArtworkProvider?)
            // Need to override the height/width, as the shared element transition tricks Glide into thinking this ImageView has
            // the same dimensions as the ImageView that the transition starts with.
            // So we'll set it to screen width (plus a little extra, which might fix an issue on some devices..)
            .override(width, height)
            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
            .priority(Priority.HIGH)
            .placeholder(PlaceholderProvider.getInstance(context).getPlaceHolderDrawable(albumArtist.name, true, settingsManager))
            .centerCrop()
            .animate(AlwaysCrossFade(false))
            .into(background!!)
    }

    override fun setSharedElementEnterTransition(transition: Any?) {
        super.setSharedElementEnterTransition(transition)
        (transition as Transition).addListener(sharedElementEnterTransitionListenerAdapter)
    }

    private fun fadeInUi() {

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

    override fun getContextualToolbar(): ContextualToolbar? {
        return ctxToolbar as ContextualToolbar
    }

    private fun setupContextualToolbar() {

        val contextualToolbar = ContextualToolbar.findContextualToolbar(this)
        if (contextualToolbar != null) {

            contextualToolbar.setTransparentBackground(true)

            contextualToolbar.menu.clear()
            contextualToolbar.inflateMenu(R.menu.context_menu_general)
            val subMenu = contextualToolbar.menu.findItem(R.id.addToPlaylist).subMenu
            disposables.add(playlistMenuHelper.createUpdatingPlaylistMenu(subMenu).subscribe())

            contextualToolbar.setOnMenuItemClickListener(SongMenuUtils.getSongMenuClickListener(Single.defer { Operators.reduceSongSingles(contextualToolbarHelper!!.items) }, presenter))

            contextualToolbarHelper = object : ContextualToolbarHelper<Single<List<Song>>>(context!!, contextualToolbar, object : ContextualToolbarHelper.Callback {

                override fun notifyItemChanged(viewModel: SelectableViewModel) {
                    if (adapter.items.contains(viewModel as ViewModel<*>)) {
                        val index = adapter.items.indexOf(viewModel as ViewModel<*>)
                        if (index >= 0) {
                            adapter.notifyItemChanged(index, 0)
                        }
                    } else if (horizontalRecyclerView.viewModelAdapter.items.contains(viewModel as ViewModel<*>)) {
                        val index = horizontalRecyclerView.viewModelAdapter.items.indexOf(viewModel as ViewModel<*>)
                        if (index >= 0) {
                            horizontalRecyclerView.viewModelAdapter.notifyItemChanged(index, 0)
                        }
                    }
                }

                override fun notifyDatasetChanged() {
                    adapter.notifyItemRangeChanged(0, adapter.items.size, 0)
                    horizontalRecyclerView.viewModelAdapter.notifyItemRangeChanged(0, horizontalRecyclerView.viewModelAdapter.items.size, 0)
                }
            }) {
                override fun start() {
                    super.start()
                    // Need to hide the collapsed text, as it overlaps the contextual toolbar
                    collapsingToolbarTextColor = toolbar_layout.collapsedTitleTextColor
                    collapsingToolbarSubTextColor = toolbar_layout.collapsedSubTextColor
                    toolbar_layout.setCollapsedTitleTextColor(0x01FFFFFF)
                    toolbar_layout.setCollapsedSubTextColor(0x01FFFFFF)

                    toolbar.visibility = View.GONE
                }

                override fun finish() {
                    if (toolbar_layout != null && collapsingToolbarTextColor != null && collapsingToolbarSubTextColor != null) {
                        toolbar_layout.collapsedTitleTextColor = collapsingToolbarTextColor!!
                        toolbar_layout.collapsedSubTextColor = collapsingToolbarSubTextColor!!
                    }
                    if (toolbar != null) {
                        toolbar.visibility = View.VISIBLE
                    }
                    super.finish()
                }
            }
        }
    }

    public override fun screenName(): String {
        return "ArtistDetailFragment"
    }

    internal fun pushDetailFragment(fragment: Fragment, transitionView: View?) {

        val transitions = ArrayList<Pair<View, String>>()

        if (transitionView != null) {
            val transitionName = ViewCompat.getTransitionName(transitionView)
            transitions.add(Pair(transitionView, transitionName))
            //            transitions.add(new Pair<>(toolbar, "toolbar"));

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                val moveTransition = TransitionInflater.from(context).inflateTransition(R.transition.image_transition)
                fragment.sharedElementEnterTransition = moveTransition
                fragment.sharedElementReturnTransition = moveTransition
            }
        }

        navigationController.pushViewController(fragment, "DetailFragment", transitions)
    }

    // ArtistDetailView implementation

    override fun setData(albums: List<Album>, songs: List<Song>) {
        val viewModels = ArrayList<ViewModel<*>>()

        if (!albums.isEmpty()) {

            val items = ArrayList<ViewModel<*>>()

            if (setHorizontalItemsDisposable != null) {
                setHorizontalItemsDisposable!!.dispose()
            }

            setHorizontalItemsDisposable = horizontalRecyclerView.setItems(albums
                .map { album ->
                    val horizontalAlbumView = HorizontalAlbumView(album, requestManager, sortManager, settingsManager)
                    horizontalAlbumView.setClickListener(albumClickListener)
                    horizontalAlbumView.showYear(true)
                    horizontalAlbumView
                })

            items.add(SubheaderView(StringUtils.makeAlbumsLabel(context!!, albums.size)))
            items.add(horizontalRecyclerView)

            viewModels.addAll(items)
        }

        if (!songs.isEmpty()) {
            val items = ArrayList<ViewModel<*>>()

            items.add(SubheaderView(StringUtils.makeSongsAndTimeLabel(context!!, songs.size, songs.map { song -> song.duration / 1000 }.sum())))

            items.addAll(
                songs
                    .map { song ->
                        val songView = SongView(song, requestManager, sortManager, settingsManager)
                        songView.showArtistName(false)
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

    override fun closeContextualToolbar() {
        if (contextualToolbarHelper != null) {
            contextualToolbarHelper!!.finish()
        }
    }

    // AlbumArtistMenuContract.View Implementation

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

    override fun onPlaybackFailed() {
        // Todo: Improve error message
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

    companion object {

        private const val TAG = "ArtistDetailFragment"

        private const val ARG_TRANSITION_NAME = "transition_name"

        private const val ARG_ALBUM_ARTIST = "album_artist"

        fun newInstance(albumArtist: AlbumArtist, transitionName: String?): ArtistDetailFragment {
            val args = Bundle()
            val fragment = ArtistDetailFragment()
            args.putSerializable(ARG_ALBUM_ARTIST, albumArtist)
            args.putString(ARG_TRANSITION_NAME, transitionName)
            fragment.arguments = args
            return fragment
        }
    }
}