package com.simplecity.amp_library.ui.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.crashlytics.android.core.CrashlyticsCore;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.interfaces.DrawerListCallbacks;
import com.simplecity.amp_library.model.DrawerGroupItem;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.ui.adapters.NavigationDrawerAdapter;
import com.simplecity.amp_library.ui.views.AnimatedExpandableListView;
import com.simplecity.amp_library.utils.ComparisonUtils;
import com.simplecity.amp_library.utils.DataManager;
import com.simplecity.amp_library.utils.MenuUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.PermissionUtils;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class NavigationDrawerFragment extends BaseFragment implements
        MusicUtils.Defs,
        View.OnCreateContextMenuListener,
        DrawerListCallbacks {

    public interface DrawerClickListener {

        void onItemClicked(DrawerGroupItem drawerGroupItem);

        void onItemClicked(Playlist playlist);
    }

    private static final String TAG = "DrawerFragment";

    private View mRootView;

    private NavigationDrawerAdapter mAdapter;

    private static final String STATE_SELECTED_GROUP_ITEM = "selected_navigation_drawer_group_item";

    private static final String STATE_SELECTED_PLAYLIST = "selected_navigation_drawer_playlist";

    /**
     * Per the design guidelines, you should show the drawer on launch until the user manually
     * expands it. This shared preference tracks this.
     */
    private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";

    /**
     * Helper component that ties the action bar to the navigation drawer.
     */
    private ActionBarDrawerToggle mDrawerToggle;

    private DrawerLayout mDrawerLayout;
    private AnimatedExpandableListView mListView;

    private DrawerGroupItem mCurrentSelectedGroupItem = null;
    private Playlist mCurrentSelectedPlaylist = null;
    private boolean mFromSavedInstanceState;
    boolean mUserLearnedDrawer;

    private DrawerClickListener drawerClickListener;

    private SharedPreferences mPrefs;
    private SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferenceChangeListener;

    private Subscription subscription;

    /**
     * Empty constructor as per the {@link android.app.Fragment} docs
     */
    public NavigationDrawerFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        drawerClickListener = (DrawerClickListener) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
        mPrefs.registerOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);

        mUserLearnedDrawer = mPrefs.getBoolean(PREF_USER_LEARNED_DRAWER, false);

        if (savedInstanceState != null) {
            mCurrentSelectedGroupItem = (DrawerGroupItem) savedInstanceState.get(STATE_SELECTED_GROUP_ITEM);
            mCurrentSelectedPlaylist = (Playlist) savedInstanceState.get(STATE_SELECTED_PLAYLIST);
            mFromSavedInstanceState = true;
        }

        mAdapter = new NavigationDrawerAdapter();
        mAdapter.setListCallbacks(this);
        mCurrentSelectedGroupItem = mAdapter.getGroup(0);

        if (mFromSavedInstanceState) {
            mAdapter.setSelectedItem(mCurrentSelectedGroupItem, mCurrentSelectedPlaylist);
        }

        mSharedPreferenceChangeListener = (sharedPreferences, key) -> {
            if (key.equals("pref_theme_highlight_color") || key.equals("pref_theme_accent_color") || key.equals("pref_theme_white_accent")) {
                themeUIComponents();
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (mRootView == null) {

            mRootView = inflater.inflate(R.layout.fragment_drawer, container, false);

            mListView = (AnimatedExpandableListView) mRootView.findViewById(R.id.list);
            mListView.setGroupIndicator(null);
            mListView.setOnGroupClickListener((parent, v, groupPosition, id) -> {
                // We call collapseGroupWithAnimation(int) and
                // expandGroupWithAnimation(int) to animate group
                // expansion/collapse.
                if (mListView.isGroupExpanded(groupPosition)) {
                    mListView.collapseGroupWithAnimation(groupPosition);
                } else {
                    mListView.expandGroupWithAnimation(groupPosition);
                }
                return true;
            });
            mListView.setAdapter(mAdapter);

            getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.drawer_header_container, DrawerHeaderFragment.newInstance())
                    .commit();

            themeUIComponents();
        }
        return mRootView;
    }

    @Override
    public void onPause() {

        if (subscription != null) {
            subscription.unsubscribe();
        }

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshAdapterItems();
    }

    private void refreshAdapterItems() {
        PermissionUtils.RequestStoragePermissions(() -> {
            if (getActivity() != null && isAdded()) {

                Observable<List<Playlist>> defaultPlaylistsObservable =
                        Observable.fromCallable(() -> {
                                    List<Playlist> playlists = new ArrayList<>();

                                    Playlist podcastPlaylist = Playlist.podcastPlaylist();
                                    if (podcastPlaylist != null) {
                                        playlists.add(podcastPlaylist);
                                    }

                                    playlists.add(Playlist.recentlyAddedPlaylist());
                                    playlists.add(Playlist.mostPlayedPlaylist());

                                    return playlists;
                                }
                        ).subscribeOn(Schedulers.io());

                Observable<List<Playlist>> playlistsObservable = DataManager.getInstance().getPlaylistsRelay();

                subscription = Observable.combineLatest(
                        defaultPlaylistsObservable, playlistsObservable, (defaultPlaylists, playlists) -> {
                            List<Playlist> list = new ArrayList<>();
                            list.addAll(defaultPlaylists);
                            list.addAll(playlists);
                            return list;
                        })
                        .flatMap(playlists -> Observable.from(playlists)
                                .flatMap(playlist -> playlist.getSongsObservable(getContext())
                                        .flatMap(songs -> {
                                            if (playlist.type == Playlist.Type.USER_CREATED
                                                    || playlist.type == Playlist.Type.RECENTLY_ADDED
                                                    || !songs.isEmpty()) {
                                                return Observable.just(playlist);
                                            } else {
                                                return Observable.empty();
                                            }
                                        }))
                                .sorted((a, b) -> ComparisonUtils.compare(a.name, b.name))
                                .sorted((a, b) -> ComparisonUtils.compareInt(a.type, b.type))
                                .toList())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                items -> mAdapter.setPlaylistData(items),
                                error -> CrashlyticsCore.getInstance().log("Error refreshing NavigationDrawerFragment adapter items: " + error.toString())
                        );
            }
        });
    }

    public void themeUIComponents() {
        mAdapter.notifyDataSetChanged();

        if (mListView != null) {
            ThemeUtils.themeListView(mListView);
        }
    }

    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START);
    }

    /**
     * Users of this fragment must call this method to set up the navigation drawer interactions.
     *
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     */
    public void setup(DrawerLayout drawerLayout) {

        mDrawerLayout = drawerLayout;

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        mDrawerToggle = new ActionBarDrawerToggle(
                getActivity(),                    /* host Activity */
                mDrawerLayout,                    /* DrawerLayout object */
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (!isAdded()) {
                    return;
                }

                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);

                if (!isAdded()) {
                    return;
                }

                if (!mUserLearnedDrawer) {
                    // The user manually opened the drawer; store this flag to prevent auto-showing
                    // the navigation drawer automatically in the future.
                    mUserLearnedDrawer = true;
                    SharedPreferences sp = PreferenceManager
                            .getDefaultSharedPreferences(getActivity());
                    sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).apply();
                }

                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                // Prevent the animation from occurring unless the passed in drawer view is null. (We pass in null from our sliding up panel,
                // which is the only view which should cause animation)
                if (drawerView != null) {
                    slideOffset = 0;
                }
                super.onDrawerSlide(drawerView, slideOffset); // this disables the animation
            }
        };

        // If the user hasn't 'learned' about the drawer, open it to introduce them to the drawer,
        // per the navigation drawer design guidelines.
        if (!mUserLearnedDrawer && !mFromSavedInstanceState) {
            mDrawerLayout.openDrawer(GravityCompat.START);
        }

        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(() -> mDrawerToggle.syncState());

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // If the drawer is open, show the global app actions in the action bar. See also
        // showGlobalContextActionBar, which controls the top-left area of the action bar.
        if (mDrawerLayout != null && isDrawerOpen()) {
            inflater.inflate(R.menu.menu_global, menu);
            showGlobalContextActionBar();
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        mPrefs.unregisterOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(STATE_SELECTED_GROUP_ITEM, mCurrentSelectedGroupItem);
        outState.putSerializable(STATE_SELECTED_PLAYLIST, mCurrentSelectedPlaylist);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    /**
     * Per the navigation drawer design guidelines, updates the action bar to show the global app
     * 'context', rather than just what's in the current screen.
     */
    private void showGlobalContextActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        }
    }

    private ActionBar getActionBar() {
        if (getActivity() == null) {
            return null;
        }
        return ((AppCompatActivity) getActivity()).getSupportActionBar();
    }

    public void toggleDrawerLock(boolean lock) {
        if (lock) {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        } else {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
    }

    public void animateDrawerToggle(float slideOffset) {
        mDrawerToggle.onDrawerSlide(null, slideOffset);
    }

    @Override
    public void onDrawerItemClick(final DrawerGroupItem drawerGroupItem) {
        mDrawerLayout.closeDrawer(GravityCompat.START);

        //Settings and support should not be selected, since we'll return from them back to whatever was previously selected anyway.
        if (drawerGroupItem.type != DrawerGroupItem.Type.SETTINGS
                && drawerGroupItem.type != DrawerGroupItem.Type.SUPPORT
                && !(!ShuttleUtils.isUpgraded() && drawerGroupItem.type == DrawerGroupItem.Type.FOLDERS)) {
            mCurrentSelectedGroupItem = drawerGroupItem;
            mAdapter.setSelectedItem(mCurrentSelectedGroupItem, null);
        }
        Handler handler = new Handler();
        handler.postDelayed(() -> drawerClickListener.onItemClicked(drawerGroupItem), 200);
    }

    @Override
    public void onPlaylistItemClick(final DrawerGroupItem drawerGroupItem, final Playlist playlist) {
        mDrawerLayout.closeDrawer(GravityCompat.START);
        Handler handler = new Handler();
        handler.postDelayed(() -> drawerClickListener.onItemClicked(playlist), 200);
    }

    @Override
    public void onOverflowButtonClick(View v, final Playlist playlist) {
        PopupMenu popupMenu = new PopupMenu(getActivity(), v);
        MenuUtils.addPlaylistMenuOptions(popupMenu, playlist);
        MenuUtils.addClickHandler(getActivity(), popupMenu, playlist, null, null);
        popupMenu.show();
    }

    public void setDrawerItem(int position) {
        if (mAdapter != null) {
            mCurrentSelectedGroupItem = mAdapter.getGroup(position);
            mAdapter.setSelectedItem(mCurrentSelectedGroupItem, null);
        }
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}
