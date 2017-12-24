package com.simplecity.amp_library.ui.detail;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CustomCollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
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

import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.materialdialogs.MaterialDialog;
import com.annimon.stream.Collectors;
import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.glide.utils.AlwaysCrossFade;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.ArtworkProvider;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.tagger.TaggerDialog;
import com.simplecity.amp_library.ui.adapters.LoggingViewModelAdapter;
import com.simplecity.amp_library.ui.dialog.UpgradeDialog;
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
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.MenuUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.Operators;
import com.simplecity.amp_library.utils.PlaceholderProvider;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.ResourceUtils;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.SortManager;
import com.simplecity.amp_library.utils.StringUtils;
import com.simplecity.amp_library.utils.TypefaceManager;
import com.simplecityapps.recycler_adapter.adapter.CompletionListUpdateCallbackAdapter;
import com.simplecityapps.recycler_adapter.adapter.ViewModelAdapter;
import com.simplecityapps.recycler_adapter.model.ViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.RecyclerListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.afollestad.aesthetic.Rx.distinctToMainThread;

public abstract class BaseDetailFragment extends BaseFragment implements
        DetailView,
        Toolbar.OnMenuItemClickListener,
        SongsProvider,
        AlbumsProvider,
        AlbumView.ClickListener,
        SongView.ClickListener,
        DrawerLockManager.DrawerLock,
        ContextualToolbarHost {

    private static final String TAG = "BaseDetailFragment";

    private static final String ARG_TRANSITION_NAME = "transition_name";

    private Unbinder unbinder;

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

    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;

    @BindView(R.id.contextualToolbar)
    ContextualToolbar contextualToolbar;

    private ColorStateList collapsingToolbarTextColor;
    private ColorStateList collapsingToolbarSubTextColor;

    protected CompositeDisposable disposables = new CompositeDisposable();

    @Nullable
    private Disposable setItemsDisposable = null;

    @Nullable
    private Disposable setHorizontalItemsDisposable = null;

    protected RequestManager requestManager;

    protected DetailPresenter detailPresenter;

    protected ViewModelAdapter adapter;

    private HorizontalRecyclerView horizontalRecyclerView;

    @Nullable
    Album currentSlideShowAlbum;

    private boolean isFirstLoad = true;

    private ContextualToolbarHelper<Single<List<Song>>> contextualToolbarHelper;

    public BaseDetailFragment() {
    }

    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);

        adapter = new LoggingViewModelAdapter("BaseDetailFragment");

        horizontalRecyclerView = new HorizontalRecyclerView("BaseDetail - horizontal");

        detailPresenter = new DetailPresenter(this, this);

        setHasOptionsMenu(true);

        setEnterSharedElementCallback(enterSharedElementCallback);

        if (requestManager == null) {
            requestManager = Glide.with(this);
        }

        isFirstLoad = true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        unbinder = ButterKnife.bind(this, rootView);

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

        toolbarLayout.setTitle(getToolbarTitle());
        toolbarLayout.setSubtitle(getToolbarSubtitle());
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

        detailPresenter.bindView(this);

        loadBackgroundImage();

        Aesthetic.get(getContext())
                .colorPrimary()
                .take(1)
                .subscribe(primaryColor -> {
                    toolbarLayout.setContentScrimColor(primaryColor);
                    toolbarLayout.setBackgroundColor(primaryColor);
                });

        disposables.add(Aesthetic.get(getContext())
                .colorPrimary()
                .compose(distinctToMainThread())
                .subscribe(primaryColor -> {
                    toolbarLayout.setContentScrimColor(primaryColor);
                    toolbarLayout.setBackgroundColor(primaryColor);
                }));

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        detailPresenter.loadData();

        if (canPlaySlideshow()) {
            startSlideShow();
        }

        DrawerLockManager.getInstance().addDrawerLock(this);
    }

    @Override
    public void onPause() {

        DrawerLockManager.getInstance().removeDrawerLock(this);

        super.onPause();
    }

    @Override
    public void onDestroyView() {

        if (setHorizontalItemsDisposable != null) {
            setHorizontalItemsDisposable.dispose();
        }
        if (setItemsDisposable != null) {
            setItemsDisposable.dispose();
        }

        disposables.clear();

        detailPresenter.unbindView(this);

        unbinder.unbind();

        isFirstLoad = false;

        super.onDestroyView();
    }

    boolean showAlbumMenu() {
        return true;
    }

    @NonNull
    @Override
    public abstract Single<List<Song>> getSongs();

    abstract void setSongSortOrder(@SortManager.SongSort int sortOrder);

    @SortManager.SongSort
    abstract int getSongSortOrder();

    abstract void setSongsAscending(boolean ascending);

    abstract boolean getSongsAscending();

    void setAlbumSortOrder(@SortManager.AlbumSort int sortOrder) {
    }

    @SortManager.AlbumSort
    int getAlbumSort() {
        return -1;
    }

    void setAlbumsAscending(boolean ascending) {

    }

    boolean getAlbumsAscending() {
        return false;
    }

    @Nullable
    ArtworkProvider getArtworkProvider() {
        return null;
    }

    @NonNull
    abstract Drawable getPlaceHolderDrawable();

    @NonNull
    protected abstract String getToolbarTitle();

    boolean canPlaySlideshow() {
        return false;
    }

    @Nullable
    protected MaterialDialog getArtworkDialog() {
        return null;
    }

    @Nullable
    protected TaggerDialog getTaggerDialog() {
        return null;
    }

    @Nullable
    MaterialDialog getInfoDialog() {
        return null;
    }

    @Nullable
    protected String getToolbarSubtitle() {
        return null;
    }

    boolean showSongOverflowRemoveButton() {
        return false;
    }

    void songRemoved(int position, Song song) {
    }

    protected void sortSongs(List<Song> songs) {
        @SortManager.SongSort int songSort = getSongSortOrder();

        boolean songsAscending = getSongsAscending();

        SortManager.getInstance().sortSongs(songs, songSort);
        if (!songsAscending) {
            Collections.reverse(songs);
        }
    }

    @NonNull
    @Override
    public List<ViewModel> getSongViewModels(List<Song> songs) {
        List<ViewModel> items = new ArrayList<>();

        items.add(new SubheaderView(StringUtils.makeSongsLabel(getContext(), songs.size())));

        items.addAll(Stream.of(songs)
                .map(song -> {
                    SongView songView = new SongView(song, requestManager);
                    songView.setClickListener(BaseDetailFragment.this);
                    return songView;
                }).toList());

        return items;
    }

    protected void sortAlbums(List<Album> albums) {
        @SortManager.AlbumSort int albumSort = getAlbumSort();

        boolean albumsAscending = getAlbumsAscending();

        SortManager.getInstance().sortAlbums(albums, albumSort);
        if (!albumsAscending) {
            Collections.reverse(albums);
        }
    }

    @NonNull
    @Override
    public Single<List<Album>> getAlbums() {
        return Single.just(Collections.emptyList());
    }

    @NonNull
    @Override
    public List<ViewModel> getAlbumViewModels(List<Album> albums) {

        if (albums.isEmpty()) {
            return Collections.emptyList();
        }

        List<ViewModel> items = new ArrayList<>();

        if (setHorizontalItemsDisposable != null) {
            setHorizontalItemsDisposable.dispose();
        }
        setHorizontalItemsDisposable = horizontalRecyclerView.setItems(Stream.of(albums)
                .map(album -> {
                    HorizontalAlbumView horizontalAlbumView = new HorizontalAlbumView(album, requestManager);
                    horizontalAlbumView.setClickListener(BaseDetailFragment.this);
                    horizontalAlbumView.showYear(true);
                    return horizontalAlbumView;
                })
                .collect(Collectors.toList()));

        items.add(new SubheaderView(StringUtils.makeAlbumsLabel(getContext(), albums.size())));
        items.add(horizontalRecyclerView);

        return items;
    }

    void loadBackgroundImage() {

        if (getArtworkProvider() == null) {
            return;
        }

        int width = ResourceUtils.getScreenSize().width + ResourceUtils.toPixels(60);
        int height = getResources().getDimensionPixelSize(R.dimen.header_view_height);

        requestManager.load(getArtworkProvider())
                // Need to override the height/width, as the shared element transition tricks Glide into thinking this ImageView has
                // the same dimensions as the ImageView that the transition starts with.
                // So we'll set it to screen width (plus a little extra, which might fix an issue on some devices..)
                .override(width, height)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .priority(Priority.HIGH)
                .placeholder(getPlaceHolderDrawable())
                .centerCrop()
                .animate(new AlwaysCrossFade(false))
                .into(headerImageView);
    }

    void startSlideShow() {
        disposables.add(
                Observable.combineLatest(
                        getAlbums().toObservable(),
                        Observable.interval(8, TimeUnit.SECONDS)
                                // Load an image straight away
                                .startWith(0L)
                                // If we have a 'current slideshowAlbum' then we're coming back from onResume. Don't load a new one immediately.
                                .delay(currentSlideShowAlbum == null ? 0 : 8, TimeUnit.SECONDS),
                        (albums, aLong) -> {
                            if (albums.isEmpty()) {
                                return Optional.ofNullable(currentSlideShowAlbum);
                            } else {
                                return Optional.of(albums.get(new Random().nextInt(albums.size())));
                            }
                        }
                ).subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(nextSlideShowAlbumOptional -> {
                            if (nextSlideShowAlbumOptional.isPresent()) {
                                Album nextSlideshowAlbum = nextSlideShowAlbumOptional.get();
                                if (nextSlideshowAlbum != currentSlideShowAlbum) {
                                    //This crazy business is what's required to have a smooth Glide crossfade with no 'white flicker'
                                    requestManager.load(nextSlideshowAlbum)
                                            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                                            .priority(Priority.HIGH)
                                            .error(PlaceholderProvider.getInstance().getPlaceHolderDrawable(nextSlideshowAlbum.name, true))
                                            .centerCrop()
                                            .thumbnail(Glide
                                                    .with(this)
                                                    .load(currentSlideShowAlbum)
                                                    .centerCrop())
                                            .crossFade(600)
                                            .into(headerImageView);
                                    currentSlideShowAlbum = nextSlideshowAlbum;
                                }
                            }
                        }, error -> LogUtils.logException(TAG, "startSlideShow threw error", error)));
    }

    @OnClick(R.id.fab)
    void onFabClicked() {
        detailPresenter.fabClicked();
    }

    protected void setupToolbarMenu(Toolbar toolbar) {
        toolbar.inflateMenu(R.menu.menu_detail_sort);

        setupCastMenu(toolbar.getMenu());

        toolbar.setOnMenuItemClickListener(this);

        // Create playlist menu
        final SubMenu sub = toolbar.getMenu().findItem(R.id.addToPlaylist).getSubMenu();
        disposables.add(PlaylistUtils.createUpdatingPlaylistMenu(sub).subscribe());

        // Inflate sorting menus
        MenuItem item = toolbar.getMenu().findItem(R.id.sorting);
        if (showAlbumMenu()) {
            getActivity().getMenuInflater().inflate(R.menu.menu_detail_sort_albums, item.getSubMenu());
        }
        getActivity().getMenuInflater().inflate(R.menu.menu_detail_sort_songs, item.getSubMenu());

        updateMenuItems(toolbar);
    }

    @Override
    public void setSharedElementEnterTransition(Object transition) {
        super.setSharedElementEnterTransition(transition);
        if (ShuttleUtils.hasLollipop()) {
            ((Transition) transition).addListener(getSharedElementEnterTransitionListenerAdapter());
        }
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

    private TransitionListenerAdapter getSharedElementEnterTransitionListenerAdapter() {
        if (ShuttleUtils.hasLollipop()) {
            return new TransitionListenerAdapter() {
                @Override
                public void onTransitionEnd(Transition transition) {
                    if (ShuttleUtils.hasLollipop()) {
                        transition.removeListener(this);
                        fadeInUi();
                    }
                }
            };
        }
        return null;
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
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.play:
                detailPresenter.playAll();
                return true;
            case R.id.addToQueue:
                detailPresenter.addToQueue();
                return true;
            case MusicUtils.Defs.NEW_PLAYLIST:
                detailPresenter.newPlaylist(getContext());
                return true;
            case MusicUtils.Defs.PLAYLIST_SELECTED:
                detailPresenter.playlistSelected(getContext(), item);
                return true;
            case R.id.editTags:
                detailPresenter.editTags(getTaggerDialog(), UpgradeDialog.getUpgradeDialog(getActivity()));
                return true;
            case R.id.info:
                detailPresenter.infoClicked(getInfoDialog());
                return true;
            case R.id.artwork:
                detailPresenter.editArtwork(getArtworkDialog());
                return true;
        }

        handleSortMenuClicks(item);

        return super.onOptionsItemSelected(item);
    }

    void handleSortMenuClicks(MenuItem item) {

        boolean sortChanged = false;

        switch (item.getItemId()) {
            //Songs
            case R.id.sort_song_default:
                setSongSortOrder(SortManager.SongSort.DETAIL_DEFAULT);
                sortChanged = true;
                break;
            case R.id.sort_song_name:
                setSongSortOrder(SortManager.SongSort.NAME);
                sortChanged = true;
                break;
            case R.id.sort_song_track_number:
                setSongSortOrder(SortManager.SongSort.TRACK_NUMBER);
                sortChanged = true;
                break;
            case R.id.sort_song_duration:
                setSongSortOrder(SortManager.SongSort.DURATION);
                sortChanged = true;
                break;
            case R.id.sort_song_year:
                setSongSortOrder(SortManager.SongSort.YEAR);
                sortChanged = true;
                break;
            case R.id.sort_song_date:
                setSongSortOrder(SortManager.SongSort.DATE);
                sortChanged = true;
                break;
            case R.id.sort_song_album_name:
                setSongSortOrder(SortManager.SongSort.ALBUM_NAME);
                sortChanged = true;
                break;
            case R.id.sort_songs_ascending:
                setSongsAscending(!item.isChecked());
                sortChanged = true;
                break;

            //Albums
            case R.id.sort_album_default:
                setAlbumSortOrder(SortManager.AlbumSort.DEFAULT);
                sortChanged = true;
                break;
            case R.id.sort_album_name:
                setAlbumSortOrder(SortManager.AlbumSort.NAME);
                sortChanged = true;
                break;
            case R.id.sort_album_year:
                setAlbumSortOrder(SortManager.AlbumSort.YEAR);
                sortChanged = true;
                break;
            case R.id.sort_albums_ascending:
                setAlbumsAscending(!item.isChecked());
                sortChanged = true;
                break;
        }

        updateMenuItems(toolbar);

        if (sortChanged) {
            detailPresenter.loadData();
        }
    }

    /**
     * Called when toolbar menu item states should be updated
     */
    protected void updateMenuItems(Toolbar toolbar) {
        // Songs
        switch (getSongSortOrder()) {
            case SortManager.SongSort.DETAIL_DEFAULT:
                toolbar.getMenu().findItem(R.id.sort_song_default).setChecked(true);
                break;
            case SortManager.SongSort.NAME:
                toolbar.getMenu().findItem(R.id.sort_song_name).setChecked(true);
                break;
            case SortManager.SongSort.TRACK_NUMBER:
                toolbar.getMenu().findItem(R.id.sort_song_track_number).setChecked(true);
                break;
            case SortManager.SongSort.DURATION:
                toolbar.getMenu().findItem(R.id.sort_song_duration).setChecked(true);
                break;
            case SortManager.SongSort.DATE:
                toolbar.getMenu().findItem(R.id.sort_song_date).setChecked(true);
                break;
            case SortManager.SongSort.YEAR:
                toolbar.getMenu().findItem(R.id.sort_song_year).setChecked(true);
                break;
            case SortManager.SongSort.ALBUM_NAME:
                toolbar.getMenu().findItem(R.id.sort_song_album_name).setChecked(true);
                break;
            case SortManager.SongSort.ARTIST_NAME:
                toolbar.getMenu().findItem(R.id.sort_song_artist_name).setChecked(true);
                break;
        }

        toolbar.getMenu().findItem(R.id.sort_songs_ascending).setChecked(getSongsAscending());

        if (showAlbumMenu()) {
            //Albums
            switch (getAlbumSort()) {
                case SortManager.AlbumSort.DEFAULT:
                    toolbar.getMenu().findItem(R.id.sort_album_default).setChecked(true);
                    break;
                case SortManager.AlbumSort.NAME:
                    toolbar.getMenu().findItem(R.id.sort_album_name).setChecked(true);
                    break;
                case SortManager.AlbumSort.YEAR:
                    toolbar.getMenu().findItem(R.id.sort_album_year).setChecked(true);
                    break;
                case SortManager.AlbumSort.ARTIST_NAME:
                    toolbar.getMenu().findItem(R.id.sort_album_artist_name).setChecked(true);
                    break;
            }

            toolbar.getMenu().findItem(R.id.sort_albums_ascending).setChecked(getAlbumsAscending());
        }
    }

    @Override
    public void itemsLoaded(List<ViewModel> items) {
        if (setItemsDisposable != null) {
            setItemsDisposable.dispose();
        }
        setItemsDisposable = adapter.setItems(items, new CompletionListUpdateCallbackAdapter() {
            @Override
            public void onComplete() {
                if (recyclerView != null) {
                    recyclerView.scheduleLayoutAnimation();
                }
            }
        });
    }

    @Override
    public void setEmpty(boolean empty) {
        if (setItemsDisposable != null) {
            setItemsDisposable.dispose();
        }
        setItemsDisposable = adapter.setItems(Collections.singletonList(new EmptyView(R.string.empty_songlist)));
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void showTaggerDialog(TaggerDialog taggerDialog) {
        taggerDialog.show(getChildFragmentManager());
    }

    @Override
    public void showArtworkDialog(MaterialDialog artworkDialog) {
        artworkDialog.show();
    }

    @Override
    public void showInfoDialog(MaterialDialog infoDialog) {
        infoDialog.show();
    }

    void pushDetailController(BaseDetailFragment detailFragment, String tag, View transitionView) {
        List<Pair<View, String>> transitions = new ArrayList<>();
        String transitionName = ViewCompat.getTransitionName(transitionView);
        transitions.add(new Pair<>(transitionView, transitionName));
        transitions.add(new Pair<>(toolbar, "toolbar"));

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Transition moveTransition = TransitionInflater.from(getContext()).inflateTransition(R.transition.image_transition);
            detailFragment.setSharedElementEnterTransition(moveTransition);
            detailFragment.setSharedElementReturnTransition(moveTransition);
        }
        getNavigationController().pushViewController(detailFragment, tag, transitions);
    }

    @Override
    public void onSongClick(int position, SongView songView) {
        if (!contextualToolbarHelper.handleClick(position, songView, Single.just(Collections.singletonList(songView.song)))) {
            disposables.add(getSongs()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(songs -> MusicUtils.playAll(songs, songs.indexOf(songView.song), true, message -> Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show())));
        }
    }

    @Override
    public boolean onSongLongClick(int position, SongView songView) {
        return contextualToolbarHelper.handleLongClick(position, songView, Single.just(Collections.singletonList(songView.song)));
    }

    @Override
    public void onSongOverflowClick(int position, View v, Song song) {
        PopupMenu popupMenu = new PopupMenu(getContext(), v);
        MenuUtils.setupSongMenu(popupMenu, showSongOverflowRemoveButton());
        popupMenu.setOnMenuItemClickListener(MenuUtils.getSongMenuClickListener(
                getContext(),
                song,
                taggerDialog -> {
                    if (!ShuttleUtils.isUpgraded()) {
                        UpgradeDialog.getUpgradeDialog(getActivity()).show();
                    } else {
                        taggerDialog.show(getFragmentManager());
                    }
                },
                () -> songRemoved(position, song), null));
        popupMenu.show();
    }

    @Override
    public void onAlbumClick(int position, AlbumView albumView, AlbumView.ViewHolder viewHolder) {
        if (!contextualToolbarHelper.handleClick(position, albumView, albumView.album.getSongsSingle())) {
            pushDetailController(AlbumDetailFragment.newInstance(albumView.album, ViewCompat.getTransitionName(viewHolder.imageOne)), "AlbumDetailFragment", viewHolder.imageOne);
        }
    }

    @Override
    public boolean onAlbumLongClick(int position, AlbumView albumView) {
        return contextualToolbarHelper.handleLongClick(position, albumView, albumView.album.getSongsSingle());
    }

    @Override
    public void onAlbumOverflowClicked(View v, Album album) {
        PopupMenu popupMenu = new PopupMenu(getContext(), v);
        MenuUtils.setupAlbumMenu(popupMenu);
        popupMenu.setOnMenuItemClickListener(
                MenuUtils.getAlbumMenuClickListener(getContext(),
                        album,
                        taggerDialog -> taggerDialog.show(getFragmentManager()),
                        () -> UpgradeDialog.getUpgradeDialog(getActivity()).show()));
        popupMenu.show();
    }

    @Override
    public void onStartDrag(SongView.ViewHolder holder) {

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

            contextualToolbar.setOnMenuItemClickListener(MenuUtils.getSongMenuClickListener(
                    getContext(), Single.defer(() -> Operators.reduceSongSingles(contextualToolbarHelper.getItems()))));

            contextualToolbarHelper = new ContextualToolbarHelper<Single<List<Song>>>(contextualToolbar, new ContextualToolbarHelper.Callback() {

                @Override
                public void notifyItemChanged(int position, SelectableViewModel viewModel) {
                    if (adapter.items.contains(viewModel)) {
                        adapter.notifyItemChanged(position, 0);
                    } else if (horizontalRecyclerView.viewModelAdapter.items.contains(viewModel)) {
                        horizontalRecyclerView.viewModelAdapter.notifyItemChanged(position);
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
                    super.finish();
                    toolbarLayout.setCollapsedTitleTextColor(collapsingToolbarTextColor);
                    toolbarLayout.setCollapsedSubTextColor(collapsingToolbarSubTextColor);

                    toolbar.setVisibility(View.VISIBLE);
                }
            };
        }
    }

    @Override
    abstract protected String screenName();
}