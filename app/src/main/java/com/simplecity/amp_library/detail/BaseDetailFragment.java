package com.simplecity.amp_library.detail;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.CallSuper;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.tagger.TaggerDialog;
import com.simplecity.amp_library.ui.fragments.BaseFragment;
import com.simplecity.amp_library.ui.fragments.TransitionListenerAdapter;
import com.simplecity.amp_library.ui.modelviews.EmptyView;
import com.simplecity.amp_library.utils.ActionBarUtils;
import com.simplecity.amp_library.utils.ColorUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.ThemeUtils;
import com.simplecity.amp_library.utils.TypefaceManager;
import com.simplecityapps.recycler_adapter.adapter.ViewModelAdapter;
import com.simplecityapps.recycler_adapter.model.ViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.RecyclerListener;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import rx.subscriptions.CompositeSubscription;

public abstract class BaseDetailFragment extends BaseFragment implements
        DetailView,
        MusicUtils.Defs,
        Toolbar.OnMenuItemClickListener {

    private static final String ARG_TRANSITION_NAME = "transition_name";

    @BindView(R.id.toolbar_layout)
    CustomCollapsingToolbarLayout toolbarLayout;

    private Unbinder unbinder;

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

    public BaseDetailFragment() {
    }

    protected abstract SongsProvider getSongProvider();

    protected abstract AlbumsProvider getAlbumsProvider();

    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);

        Log.i(screenName(), "onCreate");

        adapter = createAdapter();

        detailPresenter = new DetailPresenter(getSongProvider(), getAlbumsProvider());

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

        if (transitionName != null) {
            fab.setVisibility(View.GONE);
        }

        themeUIComponents();

        detailPresenter.bindView(this);

        return rootView;
    }

    protected abstract ViewModelAdapter createAdapter();

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        unbinder.unbind();
        detailPresenter.unbindView(this);
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

        if (fab != null) {
            fab.setBackgroundTintList(ColorStateList.valueOf(ColorUtils.getAccentColor()));
            fab.setRippleColor(ColorUtils.darkerise(ColorUtils.getAccentColor(), 0.85f));
        }

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                ThemeUtils.themeRecyclerView(recyclerView);
                super.onScrollStateChanged(recyclerView, newState);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.i(screenName(), "OnResume");

        IntentFilter filter = new IntentFilter();
        filter.addAction("restartLoader");
        getActivity().registerReceiver(receiver, filter);

        subscriptions = new CompositeSubscription();

        detailPresenter.loadData();
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
    public void onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        super.onDestroy();
    }

    @NonNull
    protected abstract String getToolbarTitle();

    @Nullable
    protected String getToolbarSubtitle() {
        return null;
    }

    @OnClick(R.id.fab)
    void onFabClicked() {
        detailPresenter.fabClicked();
    }

    private void setupToolbarMenu(Toolbar toolbar) {
        toolbar.inflateMenu(R.menu.menu_detail_sort);

        setupCastToolbar(toolbar);

        toolbar.setOnMenuItemClickListener(this);

        // Create playlist menu
        final SubMenu sub = toolbar.getMenu().findItem(R.id.addToPlaylist).getSubMenu();
        PlaylistUtils.makePlaylistMenu(getActivity(), sub, SONG_FRAGMENT_GROUP_ID);

        // Inflate sorting menus
        inflateSortMenus(toolbar, toolbar.getMenu().findItem(R.id.sorting));
    }

    /**
     * Provide an opportunity to inflate custom sorting menu(s) into the R.id.sorting placeholder menu.
     * <p>
     * Call super after inflating will ensure {@link #updateMenuItems(Toolbar) is called}, which is where
     * the default menu item checked states should be set.
     */
    @CallSuper
    void inflateSortMenus(Toolbar toolbar, MenuItem item) {
        updateMenuItems(toolbar);
    }

    /**
     * Called when toolbar menu item states should be updated
     */
    protected void updateMenuItems(Toolbar toolbar) {

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

                        //Fade in the text protection scrim
                        textProtectionScrim.setAlpha(0f);
                        textProtectionScrim.setVisibility(View.VISIBLE);
                        ObjectAnimator fadeAnimator = ObjectAnimator.ofFloat(textProtectionScrim, View.ALPHA, 0f, 1f);
                        fadeAnimator.setDuration(250);
                        fadeAnimator.start();

                        textProtectionScrim2.setAlpha(0f);
                        textProtectionScrim2.setVisibility(View.VISIBLE);
                        fadeAnimator = ObjectAnimator.ofFloat(textProtectionScrim2, View.ALPHA, 0f, 1f);
                        fadeAnimator.setDuration(250);
                        fadeAnimator.start();

                        //Fade & grow the FAB
                        fab.setAlpha(0f);
                        fab.setVisibility(View.VISIBLE);

                        fadeAnimator = ObjectAnimator.ofFloat(fab, View.ALPHA, 0.5f, 1f);
                        ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(fab, View.SCALE_X, 0f, 1f);
                        ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(fab, View.SCALE_Y, 0f, 1f);

                        AnimatorSet animatorSet = new AnimatorSet();
                        animatorSet.setInterpolator(new OvershootInterpolator(2f));
                        animatorSet.playTogether(fadeAnimator, scaleXAnimator, scaleYAnimator);
                        animatorSet.setDuration(250);
                        animatorSet.start();
                    }
                }
            };
        }
        return null;
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
            case NEW_PLAYLIST:
                detailPresenter.newPlaylist(getContext());
                return true;
            case PLAYLIST_SELECTED:
                detailPresenter.playlistSelected(getContext(), item);
                return true;
        }

        handleSortMenuClicks(item);

        return super.onOptionsItemSelected(item);
    }

    protected MaterialDialog getArtworkDialog() {
        return null;
    }

    protected TaggerDialog getTaggerDialog() {
        return null;
    }

    void handleSortMenuClicks(MenuItem item) {

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
}

//                            //Start the slideshow observable.
//
//                            List<Album> albums = Stream.of(adaptableItems)
//                                    .filter(adaptableItem -> adaptableItem instanceof SongView)
//                                    .map(songView -> (Song) songView.getItem())
//                                    .map(Song::getAlbum)
//                                    .distinct()
//                                    .collect(Collectors.toList());
//
//                            if (playlist != null || genre != null && !albums.isEmpty()) {
//                                if (slideShowObservable != null && !slideShowObservable.isUnsubscribed()) {
//                                    slideShowObservable.unsubscribe();
//                                }
//                                slideShowObservable = Observable.interval(8, TimeUnit.SECONDS)
//                                        .onBackpressureDrop()
//                                        .startWith(0L)
//                                        .map(aLong -> {
//                                            if (albums.isEmpty() || aLong == 0L && currentSlideShowAlbum != null) {
//                                                //This is our first emission since onResume, but not our first since view creation.
//                                                //Skip this load.
//                                                return null;
//                                            }
//                                            return albums.get(new Random().nextInt(albums.size()));
//                                        })
//                                        .subscribeOn(Schedulers.io())
//                                        .observeOn(AndroidSchedulers.mainThread())
//                                        .subscribe(nextSlideShowAlbum -> {
//                                            if (nextSlideShowAlbum != null && nextSlideShowAlbum != currentSlideShowAlbum) {
//                                                //This crazy business is what's required to have a smooth Glide crossfade with no 'white flicker'
//                                                requestManager
//                                                        .load(nextSlideShowAlbum)
//                                                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
//                                                        .priority(Priority.HIGH)
//                                                        .error(GlideUtils.getPlaceHolderDrawable(nextSlideShowAlbum.name, true))
//                                                        .centerCrop()
//                                                        .thumbnail(Glide
//                                                                .with(this)
//                                                                .load(currentSlideShowAlbum)
//                                                                .centerCrop())
//                                                        .animate(new AlwaysCrossFade(false))
//                                                        .into(headerImageView);
//                                                currentSlideShowAlbum = nextSlideShowAlbum;
//                                            }
//                                        });
//                                subscriptions.add(slideShowObservable);
//                            }
//                        }));
//            }
//        });
//    }