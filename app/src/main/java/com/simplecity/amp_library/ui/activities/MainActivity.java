package com.simplecity.amp_library.ui.activities;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.vending.billing.utils.IabHelper;
import com.android.vending.billing.utils.IabResult;
import com.android.vending.billing.utils.Purchase;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.simplecity.amp_library.IabManager;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.constants.Config;
import com.simplecity.amp_library.interfaces.BackPressListener;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.DrawerGroupItem;
import com.simplecity.amp_library.model.Genre;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Query;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.MusicService;
import com.simplecity.amp_library.sql.sqlbrite.SqlBriteUtils;
import com.simplecity.amp_library.tagger.TaggerDialog;
import com.simplecity.amp_library.ui.fragments.AlbumArtistFragment;
import com.simplecity.amp_library.ui.fragments.AlbumFragment;
import com.simplecity.amp_library.ui.fragments.DetailFragment;
import com.simplecity.amp_library.ui.fragments.DrawerHeaderFragment;
import com.simplecity.amp_library.ui.fragments.FolderFragment;
import com.simplecity.amp_library.ui.fragments.GenreFragment;
import com.simplecity.amp_library.ui.fragments.MainFragment;
import com.simplecity.amp_library.ui.fragments.MiniPlayerFragment;
import com.simplecity.amp_library.ui.fragments.NavigationDrawerFragment;
import com.simplecity.amp_library.ui.fragments.PlayerFragment;
import com.simplecity.amp_library.ui.fragments.PlaylistFragment;
import com.simplecity.amp_library.ui.fragments.QueueFragment;
import com.simplecity.amp_library.ui.fragments.QueuePagerFragment;
import com.simplecity.amp_library.ui.fragments.SuggestedFragment;
import com.simplecity.amp_library.ui.views.CustomDrawerLayout;
import com.simplecity.amp_library.utils.ActionBarUtils;
import com.simplecity.amp_library.utils.AnalyticsManager;
import com.simplecity.amp_library.utils.ColorUtils;
import com.simplecity.amp_library.utils.DataManager;
import com.simplecity.amp_library.utils.DialogUtils;
import com.simplecity.amp_library.utils.MusicServiceConnectionUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.ResourceUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.SleepTimer;
import com.simplecity.amp_library.utils.ThemeUtils;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;

@SuppressWarnings("ResourceAsColor")
public class MainActivity extends BaseCastActivity implements
        AlbumArtistFragment.AlbumArtistClickListener,
        AlbumFragment.AlbumClickListener,
        GenreFragment.GenreClickListener,
        PlaylistFragment.PlaylistClickListener,
        NavigationDrawerFragment.DrawerClickListener,
        SuggestedFragment.SuggestedClickListener,
        MusicUtils.Defs {

    private static final String TAG = "MainActivity";

    public static final int REQUEST_SEARCH = 100;

    public static final String ARG_MODEL = "model";

    private static final int REQUEST_EXPAND = 200;

    private static final String ARG_EXPANDED = "is_expanded";

    SharedPreferences mPrefs;
    private WeakReference<BackPressListener> mBackPressListenerReference;
    private SlidingUpPanelLayout mSlidingUpPanelLayout;

    private CustomDrawerLayout mDrawerLayout;

    //Request code for the purchase flow
    static final int RC_REQUEST = 300;

    /**
     * Fragment managing the behaviors, interactions and presentation of the
     * navigation drawer.
     */
    NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in
     * {@link #restoreActionBar()}.
     */
    CharSequence mTitle;

    private Toolbar mToolbar;

    private FrameLayout mDummyStatusBar;

    private boolean mIsSlidingEnabled;

    private Drawable mActionBarBackButton;

    private SystemBarTintManager mTintManager;

    private float mAlpha;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(MusicService.InternalIntents.FAVORITE_CHANGED)) {
                supportInvalidateOptionsMenu();
            }
        }
    };

    private boolean mHasPendingPlaybackRequest;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        handleIntent(intent);
    }

    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {

        if (!ShuttleUtils.isUpgraded() && !ShuttleUtils.isAmazonBuild()) {
            IabManager.getInstance();
        }

        ThemeUtils.setTheme(this);

        if (!ShuttleUtils.hasLollipop() && ShuttleUtils.hasKitKat()) {
            getWindow().setFlags(FLAG_TRANSLUCENT_STATUS, FLAG_TRANSLUCENT_STATUS);
            mTintManager = new SystemBarTintManager(this);
        }
        if (ShuttleUtils.hasLollipop()) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        if (SettingsManager.getInstance().canTintNavBar()) {
            getWindow().setNavigationBarColor(ColorUtils.getPrimaryColorDark(this));
        }

        supportRequestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Now call super to ensure the theme was properly set
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mIsSlidingEnabled = getResources().getBoolean(R.bool.isSlidingEnabled);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        mDummyStatusBar = (FrameLayout) findViewById(R.id.dummyStatusBar);

        if (ShuttleUtils.hasKitKat()) {
            mDummyStatusBar.setVisibility(View.VISIBLE);
            mDummyStatusBar.setBackgroundColor(ColorUtils.getPrimaryColorDark(this));
            LinearLayout.LayoutParams statusBarParams = new LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, (int) ActionBarUtils.getStatusBarHeight(this));
            mDummyStatusBar.setLayoutParams(statusBarParams);
        }

        setSupportActionBar(mToolbar);

        ThemeUtils.themeStatusBar(this, mTintManager);

        mNavigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        mTitle = getString(R.string.library_title);

        mDrawerLayout = (CustomDrawerLayout) findViewById(R.id.drawer_layout);
        if (ShuttleUtils.hasLollipop() && ShuttleUtils.hasKitKat()) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        mDrawerLayout.setStatusBarBackgroundColor(ShuttleUtils.hasLollipop() ? ColorUtils.getPrimaryColorDark(this) : ColorUtils.getPrimaryColor());

        mNavigationDrawerFragment.setup((DrawerLayout) findViewById(R.id.drawer_layout));

        if (mIsSlidingEnabled) {
            mSlidingUpPanelLayout = (SlidingUpPanelLayout) findViewById(R.id.container);

            setDragView(null, false);

            //The second panel slide offset is mini player height + toolbar height + status bar height.
            //This gets our 'up next' sitting snugly underneath the toolbar
            int offset = (int) (ActionBarUtils.getActionBarHeight(this)
                    + (ShuttleUtils.hasKitKat() ? ActionBarUtils.getStatusBarHeight(this) : 0)
                    - getResources().getDimension(R.dimen.mini_player_height));
            mSlidingUpPanelLayout.setSlidePanelOffset(-offset);

            mSlidingUpPanelLayout.hidePanel();

            mSlidingUpPanelLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {

                @Override
                public void onPanelSlide(View panel, float slideOffset) {

                    setActionBarAlpha(slideOffset, false);

                    boolean canChangeElevation = true;
                    Fragment playingFragment = getSupportFragmentManager().findFragmentById(R.id.player_container);
                    if (playingFragment != null) {
                        Fragment childFragment = playingFragment.getChildFragmentManager().findFragmentById(R.id.queue_container);
                        if (childFragment != null && childFragment instanceof QueueFragment) {
                            canChangeElevation = false;
                        }
                    }
                    if (canChangeElevation) {
                        getSupportActionBar().setElevation(ResourceUtils.toPixels(4) * slideOffset);
                    }

                    mNavigationDrawerFragment.animateDrawerToggle(slideOffset);
                }

                @Override
                public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                    switch (newState) {
                        case COLLAPSED: {

                            setDragView(null, false);

                            mTitle = getString(R.string.library_title);

                            supportInvalidateOptionsMenu();

                            toggleQueue(false);

                            mNavigationDrawerFragment.toggleDrawerLock(false);
                            break;
                        }
                        case EXPANDED: {

                            Fragment playerFragment = getSupportFragmentManager().findFragmentById(R.id.player_container);
                            if (playerFragment != null && playerFragment instanceof PlayerFragment) {
                                setDragView(((PlayerFragment) playerFragment).getDragView(), true);

                                if (((PlayerFragment) playerFragment).isQueueShowing()) {
                                    toggleQueue(true);
                                }
                            }

                            mTitle = getString(R.string.nowplaying_title);

                            supportInvalidateOptionsMenu();

                            mNavigationDrawerFragment.toggleDrawerLock(true);
                            break;
                        }
                    }
                }
            });
        }

        if (savedInstanceState != null && mIsSlidingEnabled) {
            if (savedInstanceState.getBoolean(ARG_EXPANDED, false)) {

                final ActionBar actionBar = getSupportActionBar();

                //If the sliding panel was previously expanded, expand it again.
                mSlidingUpPanelLayout.post(() -> {
                    mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED, false);
                    setActionBarAlpha(1f, false);
                });

                mTitle = getString(R.string.nowplaying_title);
                if (actionBar != null) {
                    actionBar.setTitle(mTitle);
                }

                Fragment playingFragment = getSupportFragmentManager().findFragmentById(R.id.player_container);
                if (playingFragment != null) {
                    Fragment childFragment = playingFragment.getChildFragmentManager().findFragmentById(R.id.queue_container);
                    if (childFragment != null && childFragment instanceof QueueFragment) {
                        toggleQueue(true);
                    }
                }
            }
        }

        if (savedInstanceState == null) {

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.main_container, MainFragment.newInstance())
                    .commit();

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.mini_player_container, MiniPlayerFragment.newInstance())
                    .commit();

            if (mIsSlidingEnabled) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .add(R.id.player_container, PlayerFragment.newInstance())
                        .commit();
            }
        }

        themeTaskDescription();

        handleIntent(getIntent());
    }

    public void setDragView(View dragView, boolean fromExpandedView) {

        if (mSlidingUpPanelLayout == null) {
            return;
        }

        if (fromExpandedView) {
            if (mSlidingUpPanelLayout.getPanelState() != SlidingUpPanelLayout.PanelState.EXPANDED) {
                dragView = null;
            }
        }

        if (dragView == null) {
            dragView = findViewById(R.id.mini_player_container);
        }

        mSlidingUpPanelLayout.setDragView(dragView);
    }

    public void setScrollableView(View scrollableView) {
        if (mSlidingUpPanelLayout == null) {
            return;
        }
        mSlidingUpPanelLayout.setScrollableView(scrollableView);
    }

    public void togglePane() {

        if (!mIsSlidingEnabled) {
            return;
        }

        if (mSlidingUpPanelLayout == null) {
            return;
        }
        if (mSlidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
            mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED, true);
        } else if (mSlidingUpPanelLayout.getPanelState() != SlidingUpPanelLayout.PanelState.HIDDEN) {
            mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED, true);
        }
    }

    public void onSectionAttached(String title) {
        mTitle = title;
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    @Override
    public void onResume() {

        super.onResume();

        sendBroadcast(new Intent(QueueFragment.UPDATE_QUEUE_FRAGMENT));
        sendBroadcast(new Intent(MiniPlayerFragment.UPDATE_MINI_PLAYER));
        sendBroadcast(new Intent(DrawerHeaderFragment.UPDATE_DRAWER_HEADER));
        sendBroadcast(new Intent(PlayerFragment.UPDATE_PLAYING_FRAGMENT));

        DialogUtils.showUpgradeNagDialog(this, (materialDialog, dialogAction) -> {
            if (ShuttleUtils.isAmazonBuild()) {
                ShuttleUtils.openShuttleLink(MainActivity.this, "com.simplecity.amp_pro");
            } else {
                AnalyticsManager.logUpgrade(AnalyticsManager.UpgradeType.NAG);
                purchasePremiumUpgrade();
            }
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(MusicService.InternalIntents.FAVORITE_CHANGED));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        //The user has chosen an item from the search activity, so expand the sliding panel
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_EXPAND:
                    if (mSlidingUpPanelLayout != null) {
                        mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED, true);
                    }
                    return;
                case REQUEST_SEARCH:
                    swapFragments(DetailFragment.newInstance(data.getSerializableExtra(ARG_MODEL)), true);
                    return;
                case TaggerDialog.DOCUMENT_TREE_REQUEST_CODE:
                    if (getSupportFragmentManager().findFragmentByTag(TaggerDialog.TAG) != null) {
                        getSupportFragmentManager().findFragmentByTag(TaggerDialog.TAG).onActivityResult(requestCode, resultCode, data);
                    }
                    return;
            }
        }

        if (!ShuttleUtils.isUpgraded() && !ShuttleUtils.isAmazonBuild()) {
            if (IabManager.getInstance().iabHelper == null) return;

            if (!IabManager.getInstance().iabHelper.handleActivityResult(requestCode, resultCode, data)) {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        if (mIsSlidingEnabled) {
            savedInstanceState.putBoolean(ARG_EXPANDED, mSlidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED);
        }
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {

            getMenuInflater().inflate(R.menu.menu_main_activity, menu);

            restoreActionBar();

            if (!ShuttleUtils.isUpgraded()) {
                menu.findItem(R.id.media_route_menu_item).setVisible(false);
            } else {
                if (mCastManager != null) {
                    mCastManager.addMediaRouterButton(menu, R.id.media_route_menu_item);
                }
            }

            menu.add(0, EQUALIZER, 2, R.string.equalizer);

            SubMenu gotoMenu = menu.addSubMenu(0, GO_TO, 3, R.string.go_to);
            gotoMenu.add(0, GO_TO_ARTIST, 0, R.string.artist_title);
            gotoMenu.add(0, GO_TO_ALBUM, 1, R.string.album_title);

            menu.add(0, TIMER, TIMER, R.string.timer);

            if (mSlidingUpPanelLayout == null || mSlidingUpPanelLayout.getPanelState() != SlidingUpPanelLayout.PanelState.EXPANDED) {

                menu.findItem(R.id.menu_favorite).setVisible(false);
                menu.findItem(R.id.menu_share).setVisible(false);
                menu.findItem(R.id.menu_queue).setVisible(false);
                if (menu.findItem(GO_TO) != null) {
                    menu.findItem(GO_TO).setVisible(false);
                }
                menu.findItem(R.id.action_search).setVisible(true);

            } else {
                menu.findItem(R.id.action_search).setVisible(false);
                menu.findItem(R.id.menu_favorite).setVisible(true);
                menu.findItem(R.id.menu_share).setVisible(true);
                if (!ShuttleUtils.isTablet() && ShuttleUtils.isLandscape()) {
                    menu.findItem(R.id.menu_queue).setVisible(true);
                } else {
                    menu.findItem(R.id.menu_queue).setVisible(false);
                }

                if (menu.findItem(GO_TO) != null) {
                    menu.findItem(GO_TO).setVisible(true);
                }

                if (ShuttleUtils.isUpgraded()) {
                    menu.add(0, TAGGER, 3, R.string.edit_tags);
                }

                SubMenu sub = menu.addSubMenu(0, ADD_TO_PLAYLIST, 4, R.string.save_as_playlist);
                PlaylistUtils.makePlaylistMenu(this, sub, 0);

                menu.add(0, CLEAR_QUEUE, 5, R.string.clear_queue);

                menu.add(0, DELETE_ITEM, 6, R.string.delete_item);

                menu.add(0, VIEW_INFO, 7, R.string.song_info);
            }

            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        PlaylistUtils.isFavorite(this, MusicUtils.getSong())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(isFavorite -> {
                    if (isFavorite) {
                        final MenuItem favItem = menu.findItem(R.id.menu_favorite);
                        int[] attrs = new int[]{R.attr.btn_fav_pressed};
                        TypedArray ta = obtainStyledAttributes(attrs);
                        if (ta != null) {
                            Drawable drawableFromTheme = ta.getDrawable(0);
                            ta.recycle();
                            if (favItem != null) {
                                favItem.setIcon(drawableFromTheme);
                            }
                        }
                    }
                });

        MenuItem whiteListItem = menu.findItem(R.id.whitelist);
        MenuItem sortingItem = menu.findItem(R.id.sort);
        MenuItem viewAsItem = menu.findItem(R.id.view_as);

        if (mSlidingUpPanelLayout == null || !mIsSlidingEnabled || mSlidingUpPanelLayout.getPanelState() != SlidingUpPanelLayout.PanelState.EXPANDED) {
            if (whiteListItem != null) {
                whiteListItem.setVisible(true);
            }
            if (sortingItem != null) {
                sortingItem.setVisible(true);
            }
            if (viewAsItem != null) {
                viewAsItem.setVisible(true);
            }
        } else {
            if (whiteListItem != null) {
                whiteListItem.setVisible(false);
            }
            if (sortingItem != null) {
                sortingItem.setVisible(false);
            }
            if (viewAsItem != null) {
                viewAsItem.setVisible(false);
            }
        }

        if (getCurrentFragment() instanceof DetailFragment) {
            if (sortingItem != null) {
                sortingItem.setVisible(false);
            }
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                intent.putExtra(SearchManager.QUERY, "");
                startActivityForResult(intent, REQUEST_SEARCH);
                return true;
            case EQUALIZER:
                final Intent equalizerIntent = new Intent(this, EqualizerActivity.class);
                startActivity(equalizerIntent);
                return true;
            case GO_TO_ARTIST:
                long time = System.currentTimeMillis();
                Album currentAlbum = MusicUtils.getAlbum();
                if (currentAlbum != null) {
                    DataManager.getInstance().getAlbumArtistsRelay()
                            .first()
                            .flatMap(Observable::from)
                            .filter(albumArtist -> albumArtist.name.equals(currentAlbum.albumArtistName)
                                    && com.annimon.stream.Stream.of(albumArtist.albums).anyMatch(album -> album.id == currentAlbum.id))
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(albumArtist -> {
                                swapFragments(DetailFragment.newInstance(albumArtist), true);
                                new Handler().postDelayed(() ->
                                        mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED, true), time - System.currentTimeMillis() + 250);
                            });
                }
                return true;
            case GO_TO_ALBUM:
                time = System.currentTimeMillis();
                currentAlbum = MusicUtils.getAlbum();
                if (currentAlbum != null) {
                    DataManager.getInstance().getAlbumsRelay()
                            .first()
                            .flatMap(Observable::from)
                            .filter(album -> album.id == currentAlbum.id)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(album -> {
                                swapFragments(DetailFragment.newInstance(album), true);
                                new Handler().postDelayed(() ->
                                        mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED, true), time - System.currentTimeMillis() + 250);
                            });
                }
                return true;
            case TIMER:
                SleepTimer.createTimer(this, MusicUtils.getTimerActive(), MusicUtils.getTimeRemaining());
                return true;
            case DELETE_ITEM:
                new DialogUtils.DeleteDialogBuilder()
                        .context(this)
                        .singleMessageId(R.string.delete_song_desc)
                        .multipleMessage(R.string.delete_song_desc_multiple)
                        .itemNames(Collections.singletonList(MusicUtils.getSongName()))
                        .songsToDelete(Observable.just(Collections.singletonList(MusicUtils.getSong())))
                        .build()
                        .show();
                return true;
            case NEW_PLAYLIST:
                PlaylistUtils.createPlaylistDialog(this, MusicUtils.getQueue());
                return true;
            case PLAYLIST_SELECTED:
                List<Song> songs = MusicUtils.getQueue();
                Playlist playlist = (Playlist) item.getIntent().getSerializableExtra(ShuttleUtils.ARG_PLAYLIST);
                PlaylistUtils.addToPlaylist(this, playlist, songs);
                return true;
            case CLEAR_QUEUE:
                MusicUtils.clearQueue();
                intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                return true;
            case TAGGER:
                TaggerDialog.newInstance(MusicUtils.getSong())
                        .show(getSupportFragmentManager());
                return true;
            case VIEW_INFO:
                DialogUtils.showSongInfoDialog(this, MusicUtils.getSong());
                return true;
            case R.id.menu_favorite:
                PlaylistUtils.toggleFavorite(this);
                return true;
            case R.id.menu_share:
                DialogUtils.showShareDialog(MainActivity.this, MusicUtils.getSong());
                return true;
            case R.id.menu_queue:
                Fragment playingFragment = getSupportFragmentManager().findFragmentById(R.id.player_container);
                if (playingFragment != null) {
                    ((PlayerFragment) playingFragment).toggleQueue();
                }
                return true;
            case android.R.id.home:
                playingFragment = getSupportFragmentManager().findFragmentById(R.id.player_container);
                if (playingFragment != null) {
                    Fragment childFragment = playingFragment.getChildFragmentManager().findFragmentById(R.id.queue_container);
                    if (childFragment != null && childFragment instanceof QueueFragment) {
                        ((PlayerFragment) playingFragment).toggleQueue();
                        toggleQueue(false);
                        return true;
                    }
                }

                if (mSlidingUpPanelLayout != null) {
                    if (mSlidingUpPanelLayout.getPanelState() != SlidingUpPanelLayout.PanelState.COLLAPSED && mSlidingUpPanelLayout.getPanelState() != SlidingUpPanelLayout.PanelState.HIDDEN) {
                        mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED, true);
                        return true;
                    }
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder obj) {
        super.onServiceConnected(name, obj);
        supportInvalidateOptionsMenu();

        sendBroadcast(new Intent(QueueFragment.UPDATE_QUEUE_FRAGMENT));
        sendBroadcast(new Intent(MiniPlayerFragment.UPDATE_MINI_PLAYER));
        sendBroadcast(new Intent(DrawerHeaderFragment.UPDATE_DRAWER_HEADER));
        sendBroadcast(new Intent(PlayerFragment.UPDATE_PLAYING_FRAGMENT));

        if (mIsSlidingEnabled) {
            PlayerFragment playerFragment = (PlayerFragment) getSupportFragmentManager().findFragmentById(R.id.player_container);

            if (playerFragment != null) {

                playerFragment.updateTrackInfo();
                playerFragment.setRepeatButtonImage();
                playerFragment.setShuffleButtonImage();
                playerFragment.setPauseButtonImage();

                // If the QueuePagerFragment's adapter is empty, it's because it was created before the service
                // was connected. We need to recreate it now that we know the service is connected.
                Fragment fragment = playerFragment.getChildFragmentManager().findFragmentById(R.id.main_container);
                if (fragment instanceof QueueFragment) {
                    ((QueueFragment) fragment).scrollToCurrentItem();
                } else if (fragment instanceof QueuePagerFragment) {
                    ((QueuePagerFragment) fragment).resetAdapter();
                    ((QueuePagerFragment) fragment).updateQueuePosition();
                }
            }
        }

        handlePendingPlaybackRequest();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        super.onServiceDisconnected(name);
        if (mIsSlidingEnabled) {
            PlayerFragment playerFragment = (PlayerFragment) getSupportFragmentManager().findFragmentById(R.id.player_container);
            if (playerFragment != null) {
                playerFragment.setPauseButtonImage();
            }
        }
    }

    @Override
    public void onBackPressed() {

        Fragment playingFragment = getSupportFragmentManager().findFragmentById(R.id.player_container);
        if (playingFragment != null) {
            Fragment childFragment = playingFragment.getChildFragmentManager().findFragmentById(R.id.queue_container);
            if (childFragment != null && childFragment instanceof QueueFragment) {
                ((PlayerFragment) playingFragment).toggleQueue();
                toggleQueue(false);
                return;
            }
        }

        if (mIsSlidingEnabled) {
            if (mSlidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
                mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED, true);
                return;
            }
        }

        boolean isShowingFolders = false;
        Fragment containerFragment = getSupportFragmentManager().findFragmentById(R.id.main_container);
        if (containerFragment != null && containerFragment instanceof FolderFragment) {
            isShowingFolders = true;
        } else if (containerFragment instanceof MainFragment) {
            Fragment currentPagerFragment = ((MainFragment) containerFragment).getCurrentFragment();
            if (currentPagerFragment != null && currentPagerFragment instanceof FolderFragment) {
                isShowingFolders = true;
            }
        }
        if (isShowingFolders) {
            if (mBackPressListenerReference != null && mBackPressListenerReference.get() != null) {
                if (mBackPressListenerReference.get().onBackPressed()) {
                    return;
                }
            }
        }

        super.onBackPressed();

        if (isShowingFolders || containerFragment instanceof MainFragment) {
            if (mNavigationDrawerFragment != null) {
                mNavigationDrawerFragment.setDrawerItem(0);
            }
            mTitle = getString(R.string.library_title);
            supportInvalidateOptionsMenu();
        }
    }

    /**
     * Perform a fragment transaction for the passed in fragment
     *
     * @param fragment the fragment to transition to
     */
    public void swapFragments(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out);
        fragmentTransaction.replace(R.id.main_container, fragment);
        if (addToBackStack) {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.commitAllowingStateLoss();
    }

    public void swapFragments(Serializable item, View transitionView) {

        String transitionName = ViewCompat.getTransitionName(transitionView);

        DetailFragment detailFragment = DetailFragment.newInstance(item, transitionName);

        if (ShuttleUtils.hasLollipop()) {
            Transition moveTransition = TransitionInflater.from(this).inflateTransition(R.transition.image_transition);
            detailFragment.setSharedElementEnterTransition(moveTransition);
            detailFragment.setEnterTransition(new Fade());
            getCurrentFragment().setExitTransition(new Fade());
        }

        getSupportFragmentManager().beginTransaction()
                .addSharedElement(transitionView, transitionName)
                .replace(R.id.main_container, detailFragment)
                .addToBackStack(null)
                .commit();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void themeColorChanged() {
        if (mDummyStatusBar != null) {
            mDummyStatusBar.setBackgroundColor(ShuttleUtils.hasLollipop() ? ColorUtils.getPrimaryColorDark(this) : ColorUtils.getPrimaryColor());
        }
        if (mDrawerLayout != null && ShuttleUtils.hasKitKat()) {
            mDrawerLayout.setStatusBarBackgroundColor(ShuttleUtils.hasLollipop() ? ColorUtils.getPrimaryColorDark(MainActivity.this) : ColorUtils.getPrimaryColor());
        }
        if (SettingsManager.getInstance().canTintNavBar()) {
            getWindow().setNavigationBarColor(ColorUtils.getPrimaryColorDark(MainActivity.this));
        }
        themeTaskDescription();
    }

    @Override
    public void onItemClicked(AlbumArtist albumArtist, View transitionView) {
        mTitle = getString(R.string.library_title);
        swapFragments(albumArtist, transitionView);
    }

    @Override
    public void onItemClicked(Album album, View transitionView) {
        mTitle = getString(R.string.library_title);
        swapFragments(album, transitionView);
    }

    @Override
    public void onItemClicked(Genre genre) {
        mTitle = getString(R.string.library_title);
        swapFragments(DetailFragment.newInstance(genre), true);
    }

    @Override
    public void onItemClicked(Playlist playlist) {
        mTitle = getString(R.string.library_title);
        swapFragments(DetailFragment.newInstance(playlist), true);
    }

    @Override
    public void onItemClicked(Serializable item, View transitionView) {
        mTitle = getString(R.string.library_title);
        if (transitionView != null) {
            swapFragments(item, transitionView);
        } else {
            swapFragments(DetailFragment.newInstance(item), true);
        }
    }

    @Override
    public void onItemClicked(DrawerGroupItem drawerGroupItem) {
        switch (drawerGroupItem.type) {
            case DrawerGroupItem.Type.LIBRARY:
                if (getCurrentFragment() instanceof MainFragment) {
                    return;
                } else {
                    for (int i = 0, count = getSupportFragmentManager().getBackStackEntryCount(); i < count; i++) {
                        try {
                            getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                        } catch (IllegalStateException e) {
                            Log.e(TAG, "Error popping backstack: " + e);
                            CrashlyticsCore.getInstance().logException(e);
                        }
                    }
                    mTitle = getString(R.string.library_title);
                }
                break;
            case DrawerGroupItem.Type.FOLDERS:
                if (getCurrentFragment() instanceof FolderFragment) {
                    return;
                }
                if (ShuttleUtils.isUpgraded()) {
                    //Folder
                    swapFragments(FolderFragment.newInstance(null), true);
                } else {
                    DialogUtils.showUpgradeDialog(this, (materialDialog, dialogAction) -> {
                        if (ShuttleUtils.isAmazonBuild()) {
                            ShuttleUtils.openShuttleLink(MainActivity.this, "com.simplecity.amp_pro");
                        } else {
                            AnalyticsManager.logUpgrade(AnalyticsManager.UpgradeType.FOLDER);
                            purchasePremiumUpgrade();
                        }
                    });
                }
                break;

            case DrawerGroupItem.Type.SETTINGS:
                startActivity(new Intent(this, SettingsActivity.class));
                break;

            case DrawerGroupItem.Type.SUPPORT:
                Intent intent = new Intent(this, SettingsActivity.class);
                intent.putExtra(SettingsActivity.EXTRA_SUPPORT, true);
                startActivity(intent);
                break;
        }
    }

    private Fragment getCurrentFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.main_container);
    }

    private void handleIntent(Intent intent) {
        boolean handled = false;
        if (MusicService.ShortcutCommands.PLAYLIST.equals(intent.getAction())) {
            new Handler().postDelayed(() -> swapFragments(DetailFragment.newInstance(intent.getExtras().getSerializable(ShuttleUtils.ARG_PLAYLIST)), true), 50);
            handled = true;
        } else if (MusicService.ShortcutCommands.FOLDERS.equals(intent.getAction())) {
            new Handler().postDelayed(() -> swapFragments(FolderFragment.newInstance(null), true), 50);
            handled = true;
        }

        if (!handled) {
            handlePlaybackRequest(intent);
        } else {
            setIntent(new Intent());
        }
    }

    private void handlePendingPlaybackRequest() {
        if (mHasPendingPlaybackRequest) {
            handlePlaybackRequest(getIntent());
        }
    }

    private void handlePlaybackRequest(Intent intent) {

        if (intent == null) {
            return;
        } else if (MusicServiceConnectionUtils.sServiceBinder == null) {
            mHasPendingPlaybackRequest = true;
            return;
        }

        final Uri uri = intent.getData();
        final String mimeType = intent.getType();

        if (uri != null && uri.toString().length() > 0) {
            MusicUtils.playFile(uri);
            // Make sure to process intent only once
            setIntent(new Intent());
        } else if (MediaStore.Audio.Playlists.CONTENT_TYPE.equals(mimeType)) {
            long id = parseIdFromIntent(intent, "playlistId", "playlist", -1);
            if (id >= 0) {

                Query query = Playlist.getQuery();
                query.uri = ContentUris.withAppendedId(query.uri, id);

                SqlBriteUtils.createSingleQuery(this, Playlist::new, query)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(playlist -> {
                            MusicUtils.playAll(this, playlist.getSongsObservable(this));
                            // Make sure to process intent only once
                            setIntent(new Intent());
                        });
            }
        }

        mHasPendingPlaybackRequest = false;
    }

    private long parseIdFromIntent(Intent intent, String longKey,
                                   String stringKey, long defaultId) {
        long id = intent.getLongExtra(longKey, -1);
        if (id < 0) {
            String idString = intent.getStringExtra(stringKey);
            if (idString != null) {
                try {
                    id = Long.parseLong(idString);
                } catch (NumberFormatException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }
        return id;
    }

    private IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {

            // if we were disposed of in the meantime, quit.
            if (IabManager.getInstance().iabHelper == null) return;

            if (result.isFailure()) {
                Log.e(TAG, "Error purchasing: " + result);
                return;
            }

            String sku = purchase.getSku();

            if (sku.equals(Config.SKU_PREMIUM)) {
                mPrefs.edit().putBoolean("pref_theme_gold", true).apply();
                ShuttleApplication.getInstance().setIsUpgraded(true);
                DialogUtils.showUpgradeThankyouDialog(MainActivity.this);
            }
        }
    };

    public void purchasePremiumUpgrade() {
        try {
            IabManager.getInstance().iabHelper.launchPurchaseFlow(this, Config.SKU_PREMIUM, RC_REQUEST,
                    mPurchaseFinishedListener, "");
        } catch (IllegalStateException | NullPointerException e) {
            Toast.makeText(this, R.string.toast_purchase_failed, Toast.LENGTH_SHORT).show();
            Crashlytics.log("purchasePremiumUpgrade failed.. " + e.getMessage());
        }
    }

    public void togglePanelVisibility(boolean show) {
        if (!mIsSlidingEnabled) {
            findViewById(R.id.mini_player_container).setVisibility(show ? View.VISIBLE : View.GONE);
        } else {
            if (mSlidingUpPanelLayout != null) {
                if (show) {
                    mSlidingUpPanelLayout.showPanel();
                } else {
                    mSlidingUpPanelLayout.hidePanel();
                }
            }
        }
    }

    public void toggleQueue(boolean show) {
        if (mToolbar == null) {
            return;
        }

        if (mActionBarBackButton == null) {
            mActionBarBackButton = mToolbar.getNavigationIcon();
        }
        if (show) {
            mToolbar.setNavigationIcon(R.drawable.ic_action_navigation_close);
            mTitle = getString(R.string.up_next_title);
            mToolbar.setTitle(mTitle);
            animateElevationChange(4, 0);
        } else {
            mToolbar.setNavigationIcon(mActionBarBackButton);
            if (mSlidingUpPanelLayout != null) {
                if (mSlidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    mTitle = getResources().getString(R.string.nowplaying_title);
                } else {
                    mTitle = getResources().getString(R.string.library_title);
                }
            }
            mToolbar.setTitle(mTitle);
            animateElevationChange(0, 4);
        }
    }

    private void animateElevationChange(int currentElevationDips, int targetElevationDips) {

        currentElevationDips = ResourceUtils.toPixels(currentElevationDips);
        targetElevationDips = ResourceUtils.toPixels(targetElevationDips);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            return;
        }
        ValueAnimator valueAnimator = ValueAnimator.ofInt(currentElevationDips, targetElevationDips);
        valueAnimator.setIntValues(currentElevationDips, targetElevationDips);
        valueAnimator.addUpdateListener(animation -> {
            int value = (Integer) animation.getAnimatedValue();
            actionBar.setElevation(value);
        });
        valueAnimator.setDuration(250);
        valueAnimator.start();
    }

    public void setOnBackPressedListener(BackPressListener listener) {
        if (listener == null) {
            mBackPressListenerReference.clear();
        } else {
            mBackPressListenerReference = new WeakReference<>(listener);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void setActionBarAlpha(float alpha, boolean store) {
        Fragment mainFragment = getSupportFragmentManager().findFragmentById(R.id.main_container);
        if (mainFragment != null && !(mainFragment instanceof DetailFragment)) {
            if (alpha > 0f) {
                alpha = 1f;
            } else {
                alpha = 0f;
            }
        } else {
            if (store) {
                mAlpha = alpha;
            } else {
                alpha = Math.max(0f, Math.min(1f, mAlpha + alpha));
            }
        }

        if (mToolbar != null) {
            mToolbar.setBackgroundColor(ColorUtils.adjustAlpha(ColorUtils.getPrimaryColor(), alpha));
        }

        if (mDummyStatusBar != null) {
            mDummyStatusBar.setBackgroundColor(ColorUtils.adjustAlpha(ShuttleUtils.hasLollipop() ? ColorUtils.getPrimaryColorDark(this) : ColorUtils.getPrimaryColor(), alpha));
        }

        if (ShuttleUtils.hasKitKat()) {
            if (mTintManager != null) {
                mTintManager.setStatusBarTintColor(ColorUtils.adjustAlpha(ShuttleUtils.hasLollipop() ? ColorUtils.getPrimaryColorDark(this) : ColorUtils.getPrimaryColor(), alpha));
            }
        }

        if (ShuttleUtils.hasLollipop()) {
            getWindow().setStatusBarColor(ColorUtils.adjustAlpha(ColorUtils.getPrimaryColorDark(this), alpha));
        }
    }

    private void themeTaskDescription() {
        if (ShuttleUtils.hasLollipop()) {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
            if (bitmap != null) {
                ActivityManager.TaskDescription td = new ActivityManager.TaskDescription(null, bitmap, ColorUtils.getPrimaryColor());
                setTaskDescription(td);
                bitmap.recycle();
            }
        }
    }

    public boolean canSetAlpha() {

        boolean canSetAlpha = true;

        if (mIsSlidingEnabled
                && mSlidingUpPanelLayout != null
                && (mSlidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.DRAGGING
                || mSlidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED)) {
            canSetAlpha = false;
        }

        return canSetAlpha;
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}