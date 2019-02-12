package com.simplecity.amp_library.ui.screens.main;

import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.aesthetic.ViewBackgroundAction;
import com.annimon.stream.Stream;
import com.cantrowitz.rxbroadcast.RxBroadcast;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.cast.CastManager;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.CategoryItem;
import com.simplecity.amp_library.model.Genre;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.ui.adapters.PagerAdapter;
import com.simplecity.amp_library.ui.common.BaseFragment;
import com.simplecity.amp_library.ui.common.ToolbarListener;
import com.simplecity.amp_library.ui.screens.album.detail.AlbumDetailFragment;
import com.simplecity.amp_library.ui.screens.album.list.AlbumListFragment;
import com.simplecity.amp_library.ui.screens.artist.detail.ArtistDetailFragment;
import com.simplecity.amp_library.ui.screens.artist.list.AlbumArtistListFragment;
import com.simplecity.amp_library.ui.screens.drawer.NavigationEventRelay;
import com.simplecity.amp_library.ui.screens.genre.detail.GenreDetailFragment;
import com.simplecity.amp_library.ui.screens.genre.list.GenreListFragment;
import com.simplecity.amp_library.ui.screens.playlist.detail.PlaylistDetailFragment;
import com.simplecity.amp_library.ui.screens.playlist.list.PlaylistListFragment;
import com.simplecity.amp_library.ui.screens.search.SearchFragment;
import com.simplecity.amp_library.ui.screens.suggested.SuggestedFragment;
import com.simplecity.amp_library.ui.views.ContextualToolbar;
import com.simplecity.amp_library.ui.views.ContextualToolbarHost;
import com.simplecity.amp_library.ui.views.RatingSnackbar;
import com.simplecity.amp_library.ui.views.multisheet.MultiSheetEventRelay;
import com.simplecity.amp_library.utils.AnalyticsManager;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.multisheetview.ui.view.MultiSheetView;
import dagger.android.support.AndroidSupportInjection;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import kotlin.Unit;
import test.com.androidnavigation.fragment.FragmentInfo;

import static com.afollestad.aesthetic.Rx.distinctToMainThread;
import static com.afollestad.aesthetic.Rx.onErrorLogAndRethrow;

public class LibraryController extends BaseFragment implements
        AlbumArtistListFragment.AlbumArtistClickListener,
        AlbumListFragment.AlbumClickListener,
        SuggestedFragment.SuggestedClickListener,
        PlaylistListFragment.PlaylistClickListener,
        GenreListFragment.GenreClickListener,
        ContextualToolbarHost {

    private static final String TAG = "LibraryController";

    public static final String EVENT_TABS_CHANGED = "tabs_changed";

    @BindView(R.id.tabs)
    TabLayout slidingTabLayout;

    @BindView(R.id.pager)
    ViewPager pager;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.contextualToolbar)
    ContextualToolbar contextualToolbar;

    @BindView(R.id.app_bar)
    AppBarLayout appBarLayout;

    @Inject
    NavigationEventRelay navigationEventRelay;

    @Inject
    SettingsManager settingsManager;

    @Inject
    MultiSheetEventRelay multiSheetEventRelay;

    @Inject
    AnalyticsManager analyticsManager;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private Disposable tabChangedDisposable;

    private Unbinder unbinder;

    private boolean refreshPagerAdapter = false;

    private PagerAdapter pagerAdapter;

    public static FragmentInfo fragmentInfo() {
        return new FragmentInfo(LibraryController.class, null, "LibraryController");
    }

    public LibraryController() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidSupportInjection.inject(this);
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        tabChangedDisposable = RxBroadcast.fromLocalBroadcast(getContext(), new IntentFilter(EVENT_TABS_CHANGED)).subscribe(onNext -> refreshPagerAdapter = true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_library, container, false);

        unbinder = ButterKnife.bind(this, rootView);

        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        setupViewPager();

        compositeDisposable.add(Aesthetic.get(getContext())
                .colorPrimary()
                .compose(distinctToMainThread())
                .subscribe(color -> ViewBackgroundAction.create(appBarLayout)
                        .accept(color), onErrorLogAndRethrow()));

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() instanceof ToolbarListener) {
            ((ToolbarListener) getActivity()).toolbarAttached(view.findViewById(R.id.toolbar));
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!mediaManager.getQueue().isEmpty()) {
            multiSheetEventRelay.sendEvent(new MultiSheetEventRelay.MultiSheetEvent(MultiSheetEventRelay.MultiSheetEvent.Action.SHOW_IF_HIDDEN, MultiSheetView.Sheet.NONE));
        }

        navigationEventRelay.sendEvent(new NavigationEventRelay.NavigationEvent(NavigationEventRelay.NavigationEvent.Type.LIBRARY_SELECTED, null, false));
    }

    @Override
    public void onDestroyView() {
        pager.setAdapter(null);
        compositeDisposable.clear();
        unbinder.unbind();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        tabChangedDisposable.dispose();
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.menu_library, menu);

        if (CastManager.isCastAvailable(getContext(), settingsManager)) {
            MenuItem menuItem = CastButtonFactory.setUpMediaRouteButton(getContext(), menu, R.id.media_route_menu_item);
            menuItem.setVisible(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                openSearch();
                return true;
        }
        return false;
    }

    private void setupViewPager() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        CategoryItem.getCategoryItems(sharedPreferences);

        if (pagerAdapter != null && refreshPagerAdapter) {
            pagerAdapter.removeAllChildFragments();
            refreshPagerAdapter = false;
            pager.setAdapter(null);
        }

        int defaultPage = 1;

        pagerAdapter = new PagerAdapter(getChildFragmentManager());
        List<CategoryItem> categoryItems = Stream.of(CategoryItem.getCategoryItems(sharedPreferences))
                .filter(categoryItem -> categoryItem.isChecked)
                .toList();

        int defaultPageType = settingsManager.getDefaultPageType();
        for (int i = 0; i < categoryItems.size(); i++) {
            CategoryItem categoryItem = categoryItems.get(i);
            pagerAdapter.addFragment(categoryItem.getFragment(getContext()));
            if (categoryItem.type == defaultPageType) {
                defaultPage = i;
            }
        }

        int currentPage = Math.min(defaultPage, pagerAdapter.getCount());
        pager.setAdapter(pagerAdapter);
        pager.setOffscreenPageLimit(pagerAdapter.getCount() - 1);
        pager.setCurrentItem(currentPage);

        slidingTabLayout.setupWithViewPager(pager);

        pager.postDelayed(() -> {
            if (pager != null) {
                new RatingSnackbar(settingsManager, analyticsManager).show(pager, () -> {
                    ShuttleUtils.openShuttleLink(getActivity(), getActivity().getPackageName(), getActivity().getPackageManager());
                    return Unit.INSTANCE;
                });
            }
        }, 1000);
    }

    private void openSearch() {
        getNavigationController().pushViewController(SearchFragment.Companion.newInstance(null), "SearchFragment");
    }

    @Override
    public void onAlbumArtistClicked(AlbumArtist albumArtist, View transitionView) {
        String transitionName = ViewCompat.getTransitionName(transitionView);
        ArtistDetailFragment detailFragment = ArtistDetailFragment.Companion.newInstance(albumArtist, transitionName);
        pushDetailFragment(detailFragment, transitionView);
    }

    @Override
    public void onAlbumClicked(Album album, View transitionView) {
        String transitionName = ViewCompat.getTransitionName(transitionView);
        AlbumDetailFragment detailFragment = AlbumDetailFragment.Companion.newInstance(album, transitionName);
        pushDetailFragment(detailFragment, transitionView);
    }

    @Override
    public void onGenreClicked(Genre genre) {
        pushDetailFragment(GenreDetailFragment.Companion.newInstance(genre), null);
    }

    @Override
    public void onPlaylistClicked(Playlist playlist) {
        pushDetailFragment(PlaylistDetailFragment.Companion.newInstance(playlist), null);
    }

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

    @Override
    protected String screenName() {
        return "LibraryController";
    }

    @Override
    public ContextualToolbar getContextualToolbar() {
        return contextualToolbar;
    }
}
