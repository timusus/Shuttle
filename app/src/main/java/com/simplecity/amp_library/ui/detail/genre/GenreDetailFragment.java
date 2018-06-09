package com.simplecity.amp_library.ui.detail.genre;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CustomCollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import com.afollestad.aesthetic.Aesthetic;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.glide.utils.AlwaysCrossFade;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.ArtworkProvider;
import com.simplecity.amp_library.model.Genre;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.MediaManager;
import com.simplecity.amp_library.ui.detail.album.AlbumDetailFragment;
import com.simplecity.amp_library.ui.drawer.DrawerLockManager;
import com.simplecity.amp_library.ui.fragments.BaseFragment;
import com.simplecity.amp_library.ui.fragments.TransitionListenerAdapter;
import com.simplecity.amp_library.ui.modelviews.AlbumView;
import com.simplecity.amp_library.ui.modelviews.EmptyView;
import com.simplecity.amp_library.ui.modelviews.HorizontalAlbumView;
import com.simplecity.amp_library.ui.modelviews.HorizontalRecyclerView;
import com.simplecity.amp_library.ui.modelviews.SelectableViewModel;
import com.simplecity.amp_library.ui.modelviews.SongView;
import com.simplecity.amp_library.ui.modelviews.SubheaderView;
import com.simplecity.amp_library.ui.views.ContextualToolbar;
import com.simplecity.amp_library.ui.views.ContextualToolbarHost;
import com.simplecity.amp_library.utils.ActionBarUtils;
import com.simplecity.amp_library.utils.ContextualToolbarHelper;
import com.simplecity.amp_library.utils.Operators;
import com.simplecity.amp_library.utils.PlaceholderProvider;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.ResourceUtils;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.StringUtils;
import com.simplecity.amp_library.utils.TypefaceManager;
import com.simplecity.amp_library.utils.menu.album.AlbumMenuCallbacksAdapter;
import com.simplecity.amp_library.utils.menu.album.AlbumMenuUtils;
import com.simplecity.amp_library.utils.menu.song.SongMenuCallbacksAdapter;
import com.simplecity.amp_library.utils.menu.song.SongMenuUtils;
import com.simplecity.amp_library.utils.sorting.AlbumSortHelper;
import com.simplecity.amp_library.utils.sorting.SongSortHelper;
import com.simplecity.amp_library.utils.sorting.SortManager;
import com.simplecityapps.recycler_adapter.adapter.CompletionListUpdateCallbackAdapter;
import com.simplecityapps.recycler_adapter.adapter.ViewModelAdapter;
import com.simplecityapps.recycler_adapter.model.ViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.RecyclerListener;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;

import static com.afollestad.aesthetic.Rx.distinctToMainThread;

public class GenreDetailFragment extends BaseFragment implements
        GenreDetailView,
        Toolbar.OnMenuItemClickListener,
        DrawerLockManager.DrawerLock,
        ContextualToolbarHost {

    private static final String TAG = "BaseDetailFragment";

    private static final String ARG_TRANSITION_NAME = "transition_name";

    public static String ARG_GENRE = "genre";

    private Genre genre;

    private GenreDetailPresenter presenter;

    private ViewModelAdapter adapter = new ViewModelAdapter();

    private RequestManager requestManager;

    private CompositeDisposable disposables = new CompositeDisposable();

    private AlbumMenuCallbacksAdapter albumMenuCallbacksAdapter = new AlbumMenuCallbacksAdapter(this, disposables);

    private SongMenuCallbacksAdapter songMenuCallbacksAdapter = new SongMenuCallbacksAdapter(this, disposables);

    private ColorStateList collapsingToolbarTextColor;

    private ColorStateList collapsingToolbarSubTextColor;

    private EmptyView emptyView = new EmptyView(R.string.empty_songlist);

    private HorizontalRecyclerView horizontalRecyclerView = new HorizontalRecyclerView("BaseDetail - horizontal");

    @Nullable
    private Disposable setHorizontalItemsDisposable = null;

    @Nullable
    private Disposable setItemsDisposable = null;

    private ContextualToolbarHelper<Single<List<Song>>> contextualToolbarHelper;

    private Unbinder unbinder;

    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;

    @BindView(R.id.toolbar_layout)
    CustomCollapsingToolbarLayout toolbarLayout;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.textProtectionScrim)
    View textProtectionScrim;

    @BindView(R.id.textProtectionScrim2)
    View textProtectionScrim2;

    @BindView(R.id.fab)
    FloatingActionButton fab;

    @BindView(R.id.background)
    ImageView headerImageView;

    @BindView(R.id.contextualToolbar)
    ContextualToolbar contextualToolbar;

    private boolean isFirstLoad = true;

    public static GenreDetailFragment newInstance(Genre genre) {
        Bundle args = new Bundle();
        GenreDetailFragment fragment = new GenreDetailFragment();
        args.putSerializable(ARG_GENRE, genre);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        //noinspection ConstantConditions
        genre = (Genre) getArguments().getSerializable(ARG_GENRE);
    }

    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);

        presenter = new GenreDetailPresenter(mediaManager, genre);

        requestManager = Glide.with(this);

        setHasOptionsMenu(true);

        setEnterSharedElementCallback(enterSharedElementCallback);

        isFirstLoad = true;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        unbinder = ButterKnife.bind(this, view);

        toolbar.setNavigationOnClickListener(v -> getNavigationController().popViewController());

        if (ShuttleUtils.canDrawBehindStatusBar()) {
            toolbar.getLayoutParams().height = (int) (ActionBarUtils.getActionBarHeight(getContext()) + ActionBarUtils.getStatusBarHeight(getContext()));
            toolbar.setPadding(toolbar.getPaddingLeft(), (int) (toolbar.getPaddingTop() + ActionBarUtils.getStatusBarHeight(getContext())), toolbar.getPaddingRight(), toolbar.getPaddingBottom());
        }

        setupToolbarMenu(toolbar);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setRecyclerListener(new RecyclerListener());
        recyclerView.setAdapter(adapter);

        if (isFirstLoad) {
            recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_animation_from_bottom));
        }

        toolbarLayout.setTitle(genre.name);
        toolbarLayout.setSubtitle(null);
        toolbarLayout.setExpandedTitleTypeface(TypefaceManager.getInstance().getTypeface(TypefaceManager.SANS_SERIF_LIGHT));
        toolbarLayout.setCollapsedTitleTypeface(TypefaceManager.getInstance().getTypeface(TypefaceManager.SANS_SERIF));

        setupContextualToolbar();

        String transitionName = getArguments().getString(ARG_TRANSITION_NAME);
        ViewCompat.setTransitionName(headerImageView, transitionName);

        if (isFirstLoad) {
            fab.setVisibility(View.GONE);
        }

        if (transitionName == null) {
            fadeInUi();
        }

        loadBackgroundImage();

        disposables.add(Aesthetic.get(getContext())
                .colorPrimary()
                .compose(distinctToMainThread())
                .subscribe(primaryColor -> {
                    toolbarLayout.setContentScrimColor(primaryColor);
                    toolbarLayout.setBackgroundColor(primaryColor);
                }));

        presenter.bindView(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        presenter.loadData();

        DrawerLockManager.getInstance().addDrawerLock(this);
    }

    @Override
    public void onPause() {

        DrawerLockManager.getInstance().removeDrawerLock(this);

        super.onPause();
    }

    @Override
    public void onDestroyView() {

        if (setItemsDisposable != null) {
            setItemsDisposable.dispose();
        }

        if (setHorizontalItemsDisposable != null) {
            setHorizontalItemsDisposable.dispose();
        }

        disposables.clear();

        presenter.unbindView(this);

        unbinder.unbind();

        isFirstLoad = false;

        super.onDestroyView();
    }

    private void setupToolbarMenu(Toolbar toolbar) {
        toolbar.inflateMenu(R.menu.menu_detail_sort);

        setupCastMenu(toolbar.getMenu());

        toolbar.setOnMenuItemClickListener(this);

        // Create playlist menu
        SubMenu sub = toolbar.getMenu().findItem(R.id.addToPlaylist).getSubMenu();
        disposables.add(PlaylistUtils.createUpdatingPlaylistMenu(sub).subscribe());

        // Inflate sorting menus
        MenuItem item = toolbar.getMenu().findItem(R.id.sorting);

        getActivity().getMenuInflater().inflate(R.menu.menu_detail_sort_albums, item.getSubMenu());
        getActivity().getMenuInflater().inflate(R.menu.menu_detail_sort_songs, item.getSubMenu());

        AlbumSortHelper.updateAlbumSortMenuItems(toolbar.getMenu(), SortManager.getInstance().getGenreDetailAlbumsSortOrder(), SortManager.getInstance().getGenreDetailAlbumsAscending());
        SongSortHelper.updateSongSortMenuItems(toolbar.getMenu(), SortManager.getInstance().getGenreDetailSongsSortOrder(), SortManager.getInstance().getGenreDetailSongsAscending());
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.play:
                presenter.playAll();
                return true;
            case R.id.playNext:
                presenter.playNext();
                return true;
            case R.id.addToQueue:
                presenter.addToQueue();
                return true;
            case MediaManager.NEW_PLAYLIST:
                presenter.newPlaylist();
                return true;
            case MediaManager.PLAYLIST_SELECTED:
                presenter.playlistSelected(getContext(), item, () -> presenter.closeContextualToolbar());
                return true;
        }

        Integer albumSortOrder = AlbumSortHelper.handleAlbumDetailMenuSortOrderClicks(item);
        if (albumSortOrder != null) {
            SortManager.getInstance().setGenreDetailAlbumsSortOrder(albumSortOrder);
            presenter.loadData();
        }
        Boolean albumsAsc = AlbumSortHelper.handleAlbumDetailMenuSortOrderAscClicks(item);
        if (albumsAsc != null) {
            SortManager.getInstance().setGenreDetailAlbumsAscending(albumsAsc);
            presenter.loadData();
        }
        Integer songSortOrder = SongSortHelper.handleSongMenuSortOrderClicks(item);
        if (songSortOrder != null) {
            SortManager.getInstance().setGenreDetailSongsSortOrder(songSortOrder);
            presenter.loadData();
        }
        Boolean songsAsc = SongSortHelper.handleSongDetailMenuSortOrderAscClicks(item);
        if (songsAsc != null) {
            SortManager.getInstance().setGenreDetailSongsAscending(songsAsc);
            presenter.loadData();
        }

        AlbumSortHelper.updateAlbumSortMenuItems(toolbar.getMenu(), SortManager.getInstance().getGenreDetailAlbumsSortOrder(), SortManager.getInstance().getGenreDetailAlbumsAscending());
        SongSortHelper.updateSongSortMenuItems(toolbar.getMenu(), SortManager.getInstance().getGenreDetailSongsSortOrder(), SortManager.getInstance().getGenreDetailSongsAscending());

        return super.onOptionsItemSelected(item);
    }

    void loadBackgroundImage() {

        int width = ResourceUtils.getScreenSize().width + ResourceUtils.toPixels(60);
        int height = getResources().getDimensionPixelSize(R.dimen.header_view_height);

        requestManager.load((ArtworkProvider) null)
                // Need to override the height/width, as the shared element transition tricks Glide into thinking this ImageView has
                // the same dimensions as the ImageView that the transition starts with.
                // So we'll set it to screen width (plus a little extra, which might fix an issue on some devices..)
                .override(width, height)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .priority(Priority.HIGH)
                .placeholder(PlaceholderProvider.getInstance().getPlaceHolderDrawable(genre.name, true))
                .centerCrop()
                .animate(new AlwaysCrossFade(false))
                .into(headerImageView);
    }

    @Override
    public void fadeInSlideShowAlbum(@Nullable Album previousAlbum, @NotNull Album newAlbum) {
        //This crazy business is what's required to have a smooth Glide crossfade with no 'white flicker'
        requestManager.load(newAlbum)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .priority(Priority.HIGH)
                .error(PlaceholderProvider.getInstance().getPlaceHolderDrawable(newAlbum.name, true))
                .centerCrop()
                .thumbnail(Glide
                        .with(this)
                        .load(previousAlbum)
                        .centerCrop())
                .crossFade(600)
                .into(headerImageView);
    }

    @OnClick(R.id.fab)
    void onFabClicked() {
        presenter.fabClicked();
    }

    @Override
    public void setSharedElementEnterTransition(Object transition) {
        super.setSharedElementEnterTransition(transition);
        ((Transition) transition).addListener(getSharedElementEnterTransitionListenerAdapter());
    }

    private TransitionListenerAdapter getSharedElementEnterTransitionListenerAdapter() {
        return new TransitionListenerAdapter() {
            @Override
            public void onTransitionEnd(Transition transition) {
                transition.removeListener(this);
                fadeInUi();
            }
        };
    }

    void fadeInUi() {

        if (textProtectionScrim == null || textProtectionScrim2 == null || fab == null) {
            return;
        }

        //Fade in the text protection scrim
        textProtectionScrim.setAlpha(0f);
        textProtectionScrim.setVisibility(View.VISIBLE);
        ObjectAnimator fadeAnimator = ObjectAnimator.ofFloat(textProtectionScrim, View.ALPHA, 0f, 1f);
        fadeAnimator.setDuration(600);
        fadeAnimator.start();

        textProtectionScrim2.setAlpha(0f);
        textProtectionScrim2.setVisibility(View.VISIBLE);
        fadeAnimator = ObjectAnimator.ofFloat(textProtectionScrim2, View.ALPHA, 0f, 1f);
        fadeAnimator.setDuration(600);
        fadeAnimator.start();

        //Fade & grow the FAB
        fab.setAlpha(0f);
        fab.setVisibility(View.VISIBLE);

        fadeAnimator = ObjectAnimator.ofFloat(fab, View.ALPHA, 0.5f, 1f);
        ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(fab, View.SCALE_X, 0f, 1f);
        ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(fab, View.SCALE_Y, 0f, 1f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(fadeAnimator, scaleXAnimator, scaleYAnimator);
        animatorSet.setDuration(250);
        animatorSet.start();
    }

    @Override
    public void setData(Pair<List<Album>, List<Song>> data) {
        if (setItemsDisposable != null) {
            setItemsDisposable.dispose();
        }

        List<ViewModel> viewModels = new ArrayList<>();

        if (!data.first.isEmpty()) {

            List<ViewModel> items = new ArrayList<>();

            if (setHorizontalItemsDisposable != null) {
                setHorizontalItemsDisposable.dispose();
            }
            setHorizontalItemsDisposable = horizontalRecyclerView.setItems(Stream.of(data.first)
                    .map(album -> {
                        HorizontalAlbumView horizontalAlbumView = new HorizontalAlbumView(album, requestManager);
                        horizontalAlbumView.setClickListener(albumClickListener);
                        horizontalAlbumView.showYear(true);
                        return horizontalAlbumView;
                    })
                    .collect(Collectors.toList()));

            items.add(new SubheaderView(StringUtils.makeAlbumsLabel(ShuttleApplication.getInstance(), data.first.size())));
            items.add(horizontalRecyclerView);

            viewModels.addAll(items);
        }

        if (!data.second.isEmpty()) {
            List<ViewModel> items = new ArrayList<>();

            items.add(new SubheaderView(StringUtils.makeSongsAndTimeLabel(getContext(), data.second.size(),  Stream.of(data.second).mapToLong(song -> song.duration / 1000).sum())));

            items.addAll(Stream.of(data.second)
                    .map(song -> {
                        SongView songView = new SongView(song, requestManager);
                        songView.setClickListener(songClickListener);
                        return songView;
                    }).toList());

            viewModels.addAll(items);
        }
        if (viewModels.isEmpty()) {
            viewModels.add(emptyView);
        }

        setItemsDisposable = adapter.setItems(viewModels, new CompletionListUpdateCallbackAdapter() {
            @Override
            public void onComplete() {
                if (recyclerView != null) {
                    recyclerView.scheduleLayoutAnimation();
                }
            }
        });
    }

    @Override
    public ContextualToolbar getContextualToolbar() {
        return contextualToolbar;
    }

    private void setupContextualToolbar() {

        ContextualToolbar contextualToolbar = ContextualToolbar.findContextualToolbar(this);
        if (contextualToolbar != null) {

            contextualToolbar.setTransparentBackground(true);

            contextualToolbar.getMenu().clear();
            contextualToolbar.inflateMenu(R.menu.context_menu_general);
            SubMenu sub = contextualToolbar.getMenu().findItem(R.id.addToPlaylist).getSubMenu();
            disposables.add(PlaylistUtils.createUpdatingPlaylistMenu(sub).subscribe());

            contextualToolbar.setOnMenuItemClickListener(
                    SongMenuUtils.INSTANCE.getSongMenuClickListener(Single.defer(() -> Operators.reduceSongSingles(contextualToolbarHelper.getItems())), songMenuCallbacksAdapter)
            );

            contextualToolbarHelper = new ContextualToolbarHelper<Single<List<Song>>>(contextualToolbar, new ContextualToolbarHelper.Callback() {

                @Override
                public void notifyItemChanged(SelectableViewModel viewModel) {
                    if (adapter.items.contains(viewModel)) {
                        int index = adapter.items.indexOf(viewModel);
                        if (index >= 0) {
                            adapter.notifyItemChanged(index, 0);
                        }
                    } else if (horizontalRecyclerView.viewModelAdapter.items.contains(viewModel)) {
                        int index = horizontalRecyclerView.viewModelAdapter.items.indexOf(viewModel);
                        if (index >= 0) {
                            horizontalRecyclerView.viewModelAdapter.notifyItemChanged(index, 0);
                        }
                    }
                }

                @Override
                public void notifyDatasetChanged() {
                    adapter.notifyItemRangeChanged(0, adapter.items.size(), 0);
                    horizontalRecyclerView.viewModelAdapter.notifyItemRangeChanged(0, horizontalRecyclerView.viewModelAdapter.items.size(), 0);
                }
            }) {
                @Override
                public void start() {
                    super.start();
                    // Need to hide the collapsed text, as it overlaps the contextual toolbar
                    collapsingToolbarTextColor = toolbarLayout.getCollapsedTitleTextColor();
                    collapsingToolbarSubTextColor = toolbarLayout.getCollapsedSubTextColor();
                    toolbarLayout.setCollapsedTitleTextColor(0x01FFFFFF);
                    toolbarLayout.setCollapsedSubTextColor(0x01FFFFFF);

                    toolbar.setVisibility(View.GONE);
                }

                @Override
                public void finish() {
                    if (toolbarLayout != null) {
                        toolbarLayout.setCollapsedTitleTextColor(collapsingToolbarTextColor);
                        toolbarLayout.setCollapsedSubTextColor(collapsingToolbarSubTextColor);
                    }
                    if (toolbar != null) {
                        toolbar.setVisibility(View.VISIBLE);
                    }
                    super.finish();
                }
            };
        }
    }

    @Override
    public String screenName() {
        return "GenreDetailFragment";
    }

    private SharedElementCallback enterSharedElementCallback = new SharedElementCallback() {
        @Override
        public void onSharedElementStart(List<String> sharedElementNames, List<View> sharedElements, List<View> sharedElementSnapshots) {
            super.onSharedElementStart(sharedElementNames, sharedElements, sharedElementSnapshots);

            if (fab != null) {
                fab.setVisibility(View.GONE);
            }
        }
    };

    public SongView.ClickListener songClickListener = new SongView.ClickListener() {
        @Override
        public void onSongClick(int position, SongView songView) {
            if (!contextualToolbarHelper.handleClick(songView, Single.just(Collections.singletonList(songView.song)))) {
                presenter.songClicked(songView.song);
            }
        }

        @Override
        public boolean onSongLongClick(int position, SongView songView) {
            return contextualToolbarHelper.handleLongClick(songView, Single.just(Collections.singletonList(songView.song)));
        }

        @Override
        public void onSongOverflowClick(int position, View v, Song song) {
            PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
            SongMenuUtils.INSTANCE.setupSongMenu(popupMenu, false);
            popupMenu.setOnMenuItemClickListener(SongMenuUtils.INSTANCE.getSongMenuClickListener(song, songMenuCallbacksAdapter));
            popupMenu.show();
        }

        @Override
        public void onStartDrag(SongView.ViewHolder holder) {

        }
    };

    public AlbumView.ClickListener albumClickListener = new AlbumView.ClickListener() {

        @Override
        public void onAlbumClick(int position, AlbumView albumView, AlbumView.ViewHolder viewHolder) {
            if (!contextualToolbarHelper.handleClick(albumView, albumView.album.getSongsSingle())) {
                pushDetailFragment(AlbumDetailFragment.newInstance(albumView.album, ViewCompat.getTransitionName(viewHolder.imageOne)), viewHolder.imageOne);
            }
        }

        @Override
        public boolean onAlbumLongClick(int position, AlbumView albumView) {
            return contextualToolbarHelper.handleLongClick(albumView, albumView.album.getSongsSingle());
        }

        @Override
        public void onAlbumOverflowClicked(View v, Album album) {
            PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
            AlbumMenuUtils.INSTANCE.setupAlbumMenu(popupMenu);
            popupMenu.setOnMenuItemClickListener(AlbumMenuUtils.INSTANCE.getAlbumMenuClickListener(v.getContext(), mediaManager, album, albumMenuCallbacksAdapter));
            popupMenu.show();
        }
    };

    void pushDetailFragment(Fragment fragment, @Nullable View transitionView) {

        List<Pair<View, String>> transitions = new ArrayList<>();

        if (transitionView != null) {
            String transitionName = ViewCompat.getTransitionName(transitionView);
            transitions.add(new Pair<>(transitionView, transitionName));
            //            transitions.add(new Pair<>(toolbar, "toolbar"));

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                Transition moveTransition = TransitionInflater.from(getContext()).inflateTransition(R.transition.image_transition);
                fragment.setSharedElementEnterTransition(moveTransition);
                fragment.setSharedElementReturnTransition(moveTransition);
            }
        }

        getNavigationController().pushViewController(fragment, "DetailFragment", transitions);
    }

    // GenreDetailView implementation

    @Override
    public void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void showCreatePlaylistDialog(@NonNull List<Song> songs) {
        PlaylistUtils.createPlaylistDialog(getContext(), songs, () -> presenter.closeContextualToolbar());
    }

    @Override
    public void closeContextualToolbar() {
        if (contextualToolbarHelper != null) {
            contextualToolbarHelper.finish();
        }
    }
}



