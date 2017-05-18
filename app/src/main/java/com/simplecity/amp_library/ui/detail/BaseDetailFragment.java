package com.simplecity.amp_library.ui.detail;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CustomCollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.glide.utils.AlwaysCrossFade;
import com.simplecity.amp_library.glide.utils.GlideUtils;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.ArtworkProvider;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.tagger.TaggerDialog;
import com.simplecity.amp_library.ui.fragments.BaseFragment;
import com.simplecity.amp_library.ui.fragments.TransitionListenerAdapter;
import com.simplecity.amp_library.ui.modelviews.AlbumView;
import com.simplecity.amp_library.ui.modelviews.EmptyView;
import com.simplecity.amp_library.ui.modelviews.HorizontalAlbumView;
import com.simplecity.amp_library.ui.modelviews.HorizontalRecyclerView;
import com.simplecity.amp_library.ui.modelviews.SongView;
import com.simplecity.amp_library.ui.modelviews.SubheaderView;
import com.simplecity.amp_library.utils.ActionBarUtils;
import com.simplecity.amp_library.utils.ColorUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.ResourceUtils;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.SortManager;
import com.simplecity.amp_library.utils.StringUtils;
import com.simplecity.amp_library.utils.ThemeUtils;
import com.simplecity.amp_library.utils.TypefaceManager;
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
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public abstract class BaseDetailFragment extends BaseFragment implements
        DetailView,
        Toolbar.OnMenuItemClickListener,
        SongsProvider,
        AlbumsProvider,
        AlbumView.ClickListener,
        SongView.ClickListener {

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

    private View rootView;

    private BroadcastReceiver receiver;

    private SharedPreferences prefs;

    private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener;

    protected CompositeSubscription subscriptions;

    protected RequestManager requestManager;

    protected DetailPresenter detailPresenter;

    protected ViewModelAdapter adapter;

    private HorizontalRecyclerView horizontalRecyclerView;

    @Nullable Album currentSlideShowAlbum;

    public BaseDetailFragment() {
    }

    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);

        adapter = new ViewModelAdapter();

        horizontalRecyclerView = new HorizontalRecyclerView();

        detailPresenter = new DetailPresenter(this, this);

        setHasOptionsMenu(true);

        setEnterSharedElementCallback(enterSharedElementCallback);

        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() != null && intent.getAction().equals("restartLoader")) {
                    detailPresenter.loadData();
                }
            }
        };

        sharedPreferenceChangeListener = (sharedPreferences, key) -> {
            if (key.equals("pref_theme_highlight_color") || key.equals("pref_theme_accent_color") || key.equals("pref_theme_white_accent")) {
                themeUIComponents();
            } else if (key.equals("songWhitelist")) {
                detailPresenter.loadData();
            }
        };

        prefs.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

        if (requestManager == null) {
            requestManager = Glide.with(this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        unbinder = ButterKnife.bind(this, rootView);

        toolbar.setNavigationOnClickListener(v -> getNavigationController().popViewController());
        toolbar.getLayoutParams().height = (int) (ActionBarUtils.getActionBarHeight(getContext()) + ActionBarUtils.getStatusBarHeight(getContext()));
        toolbar.setPadding(toolbar.getPaddingLeft(), (int) (toolbar.getPaddingTop() + ActionBarUtils.getStatusBarHeight(getContext())), toolbar.getPaddingRight(), toolbar.getPaddingBottom());
        setupToolbarMenu(toolbar);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setRecyclerListener(new RecyclerListener());
        recyclerView.setAdapter(adapter);

        toolbarLayout.setTitle(getToolbarTitle());
        toolbarLayout.setSubtitle(getToolbarSubtitle());
        toolbarLayout.setExpandedTitleTypeface(TypefaceManager.getInstance().getTypeface(TypefaceManager.SANS_SERIF_LIGHT));
        toolbarLayout.setCollapsedTitleTypeface(TypefaceManager.getInstance().getTypeface(TypefaceManager.SANS_SERIF));

        String transitionName = getArguments().getString(ARG_TRANSITION_NAME);
        ViewCompat.setTransitionName(headerImageView, transitionName);

        fab.setVisibility(View.GONE);

        if (transitionName == null) {
            fadeInUi();
        }

        themeUIComponents();

        detailPresenter.bindView(this);

        loadBackgroundImage();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction("restartLoader");
        getActivity().registerReceiver(receiver, filter);

        subscriptions = new CompositeSubscription();

        detailPresenter.loadData();

        if (canPlaySlideshow()) {
            startSlideShow();
        }
    }

    @Override
    public void onPause() {

        if (receiver != null) {
            getActivity().unregisterReceiver(receiver);
        }
        subscriptions.unsubscribe();

        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        unbinder.unbind();
        detailPresenter.unbindView(this);
    }

    @Override
    public void onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        super.onDestroy();
    }

    private void themeUIComponents() {

        if (rootView != null) {
            int themeType = ThemeUtils.getThemeType(getActivity());
            if (themeType == ThemeUtils.ThemeType.TYPE_DARK
                    || themeType == ThemeUtils.ThemeType.TYPE_SOLID_DARK) {
                rootView.setBackgroundColor(getResources().getColor(R.color.bg_dark));
            } else if (themeType == ThemeUtils.ThemeType.TYPE_BLACK
                    || themeType == ThemeUtils.ThemeType.TYPE_SOLID_BLACK) {
                rootView.setBackgroundColor(getResources().getColor(R.color.bg_black));
            } else {
                rootView.setBackgroundColor(getResources().getColor(R.color.bg_light));
            }
        }

        ThemeUtils.themeRecyclerView(recyclerView);

        fab.setBackgroundTintList(ColorStateList.valueOf(ColorUtils.getAccentColor()));
        fab.setRippleColor(ColorUtils.darkerise(ColorUtils.getAccentColor(), 0.85f));

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                ThemeUtils.themeRecyclerView(recyclerView);
                super.onScrollStateChanged(recyclerView, newState);
            }
        });
    }

    boolean showAlbumMenu() {
        return true;
    }

    @NonNull
    @Override
    public abstract Observable<List<Song>> getSongs();

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
    protected String getToolbarSubtitle() {
        return null;
    }

    @NonNull
    @Override
    public List<ViewModel> getSongViewModels(List<Song> songs) {
        List<ViewModel> items = new ArrayList<>();

        @SortManager.SongSort int songSort = getSongSortOrder();

        boolean songsAscending = getSongsAscending();

        if (songSort != SortManager.SongSort.DETAIL_DEFAULT) {
            SortManager.getInstance().sortSongs(songs, songSort);
            if (!songsAscending) {
                Collections.reverse(songs);
            }
        }

        items.add(new SubheaderView(StringUtils.makeSongsLabel(getContext(), songs.size())));

        items.addAll(Stream.of(songs)
                .map(song -> {
                    SongView songView = new SongView(song, requestManager);
                    songView.setClickListener(BaseDetailFragment.this);
                    return songView;
                }).collect(Collectors.toList()));

        return items;
    }

    @NonNull
    @Override
    public Observable<List<Album>> getAlbums() {
        return Observable.just(Collections.emptyList());
    }

    @NonNull
    @Override
    public List<ViewModel> getAlbumViewModels(List<Album> albums) {

        if (albums.isEmpty()) {
            return Collections.emptyList();
        }

        List<ViewModel> items = new ArrayList<>();

        boolean albumsAscending = getAlbumsAscending();
        @SortManager.AlbumSort int albumSort = getAlbumSort();

        SortManager.getInstance().sortAlbums(albums, albumSort);
        if (!albumsAscending) {
            Collections.reverse(albums);
        }

        horizontalRecyclerView.setItems(Stream.of(albums)
                .map(album -> {
                    HorizontalAlbumView horizontalAlbumView = new HorizontalAlbumView(album, requestManager);
                    horizontalAlbumView.setClickListener(BaseDetailFragment.this);
                    horizontalAlbumView.setShowYear(true);
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
        subscriptions.add(
                Observable.combineLatest(
                        getAlbums(),
                        Observable.interval(8, TimeUnit.SECONDS)
                                // Load an image straight away
                                .startWith(0L)
                                // If we have a 'current slideshowAlbum' then we're coming back from onResume. Don't load a new one immediately.
                                .delay(currentSlideShowAlbum == null ? 0 : 8, TimeUnit.SECONDS),
                        (albums, aLong) -> albums.get(new Random().nextInt(albums.size()))
                ).subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(nextSlideShowAlbum -> {
                            if (nextSlideShowAlbum != null && nextSlideShowAlbum != currentSlideShowAlbum) {
                                //This crazy business is what's required to have a smooth Glide crossfade with no 'white flicker'
                                requestManager.load(nextSlideShowAlbum)
                                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                                        .priority(Priority.HIGH)
                                        .error(GlideUtils.getPlaceHolderDrawable(nextSlideShowAlbum.name, true))
                                        .centerCrop()
                                        .thumbnail(Glide
                                                .with(this)
                                                .load(currentSlideShowAlbum)
                                                .centerCrop())
                                        .animate(new AlwaysCrossFade(false))
                                        .into(headerImageView);
                                currentSlideShowAlbum = nextSlideShowAlbum;
                            }
                        }));
    }

    @OnClick(R.id.fab)
    void onFabClicked() {
        detailPresenter.fabClicked();
    }

    private void setupToolbarMenu(Toolbar toolbar) {
        toolbar.inflateMenu(R.menu.menu_detail_sort);

        setupCastMenu(toolbar.getMenu());

        toolbar.setOnMenuItemClickListener(this);

        // Create playlist menu
        final SubMenu sub = toolbar.getMenu().findItem(R.id.addToPlaylist).getSubMenu();
        PlaylistUtils.makePlaylistMenu(getActivity(), sub, MusicUtils.Defs.SONG_FRAGMENT_GROUP_ID);

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
            case R.id.editTags:
                detailPresenter.editTags(getTaggerDialog());
                return true;
            case R.id.editArtwork:
                detailPresenter.editArtwork(getArtworkDialog());
                return true;
            case MusicUtils.Defs.NEW_PLAYLIST:
                detailPresenter.newPlaylist(getContext());
                return true;
            case MusicUtils.Defs.PLAYLIST_SELECTED:
                detailPresenter.playlistSelected(getContext(), item);
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
        switch (SortManager.getInstance().getArtistDetailSongsSortOrder()) {
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

        toolbar.getMenu().findItem(R.id.sort_songs_ascending).setChecked(SortManager.getInstance().getArtistDetailSongsAscending());

        if (showAlbumMenu()) {
            //Albums
            switch (SortManager.getInstance().getArtistDetailAlbumsSortOrder()) {
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

            toolbar.getMenu().findItem(R.id.sort_albums_ascending).setChecked(SortManager.getInstance().getArtistDetailAlbumsAscending());
        }
    }

    @Override
    public void itemsLoaded(List<ViewModel> items) {
        adapter.setItems(items);
    }

    @Override
    public void setEmpty(boolean empty) {
        adapter.setEmpty(new EmptyView(R.string.empty_songlist));
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
    public void onSongClick(Song song, SongView.ViewHolder holder) {
        subscriptions.add(getSongs()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> {
                    int position = songs.indexOf(song);
                    MusicUtils.playAll(songs, position, message -> Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show());
                }));
    }

    @Override
    public boolean onSongLongClick(Song song) {
        return false;
    }

    @Override
    public void onSongOverflowClick(View v, Song song) {

    }

    @Override
    public void onAlbumClick(Album album, AlbumView.ViewHolder holder) {
        pushDetailController(AlbumDetailFragment.newInstance(album, ViewCompat.getTransitionName(holder.imageOne)), "AlbumDetailFragment", holder.imageOne);
    }

    @Override
    public boolean onAlbumLongClick(Album album) {
        return false;
    }

    @Override
    public void onAlbumOverflowClicked(View v, Album album) {

    }

    @Override
    public void onStartDrag(SongView.ViewHolder holder) {

    }

    @Override
    abstract protected String screenName();
}