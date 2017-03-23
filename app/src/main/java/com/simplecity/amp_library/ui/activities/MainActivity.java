package com.simplecity.amp_library.ui.activities;

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
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.graphics.drawable.DrawableCompat;
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
import android.widget.ImageView;
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

    private static final String KEY_CURRENT_PANEL = "current_panel";

    public @interface Panel {
        int NONE = 0;
        int MAIN = 1;
        int PLAYER = 2;
        int QUEUE = 3;
    }

    SharedPreferences prefs;

    private WeakReference<BackPressListener> backPressListenerReference;
    private SlidingUpPanelLayout panelOne;
    private SlidingUpPanelLayout panelTwo;

    private CustomDrawerLayout drawerLayout;

    //Request code for the purchase flow
    static final int RC_REQUEST = 300;

    /**
     * Fragment managing the behaviors, interactions and presentation of the
     * navigation drawer.
     */
    NavigationDrawerFragment navigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in
     * {@link #restoreActionBar()}.
     */
    CharSequence title;

    @Panel
    private int targetNavigationPanel;

    private Toolbar toolbar;

    private FrameLayout dummyStatusBar;

    private boolean isSlidingEnabled;

    private SystemBarTintManager tintManager;

    private float alpha;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(MusicService.InternalIntents.FAVORITE_CHANGED)) {
                supportInvalidateOptionsMenu();
            }
        }
    };

    private boolean hasPendingPlaybackRequest;

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
            tintManager = new SystemBarTintManager(this);
        }
        if (ShuttleUtils.hasLollipop()) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        if (SettingsManager.getInstance().canTintNavBar()) {
            getWindow().setNavigationBarColor(ColorUtils.getPrimaryColorDark(this));
        }

        supportRequestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Now call super to ensure the theme was properly set
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        isSlidingEnabled = getResources().getBoolean(R.bool.isSlidingEnabled);

        toolbar = (Toolbar) findViewById(R.id.toolbar);

        dummyStatusBar = (FrameLayout) findViewById(R.id.dummyStatusBar);

        if (ShuttleUtils.hasKitKat()) {
            dummyStatusBar.setVisibility(View.VISIBLE);
            dummyStatusBar.setBackgroundColor(ColorUtils.getPrimaryColorDark(this));
            LinearLayout.LayoutParams statusBarParams = new LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, (int) ActionBarUtils.getStatusBarHeight(this));
            dummyStatusBar.setLayoutParams(statusBarParams);
        }

        setSupportActionBar(toolbar);

        ThemeUtils.themeStatusBar(this, tintManager);

        navigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        title = getString(R.string.library_title);

        drawerLayout = (CustomDrawerLayout) findViewById(R.id.drawer_layout);
        if (ShuttleUtils.hasLollipop() && ShuttleUtils.hasKitKat()) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        drawerLayout.setStatusBarBackgroundColor(ShuttleUtils.hasLollipop() ? ColorUtils.getPrimaryColorDark(this) : ColorUtils.getPrimaryColor());

        ImageView arrow = (ImageView) findViewById(R.id.arrow);
        Drawable arrowDrawable = DrawableCompat.wrap(arrow.getDrawable());
        DrawableCompat.setTint(arrowDrawable, ColorUtils.getTextColorPrimary());
        arrow.setImageDrawable(arrowDrawable);

        navigationDrawerFragment.setup((DrawerLayout) findViewById(R.id.drawer_layout));

        targetNavigationPanel = Panel.NONE;

        setupFirstPanel();
        setupSecondPanel();

        if (savedInstanceState != null && isSlidingEnabled) {

            int panelIndex = savedInstanceState.getInt(KEY_CURRENT_PANEL, Panel.MAIN);
            showPanel(panelIndex);
            targetNavigationPanel = Panel.NONE;

            if (panelIndex == Panel.QUEUE) {
                setActionBarAlpha(1f, false);
                panelOne.setSlidingEnabled(false);

                panelOne.post(() -> navigationDrawerFragment.animateDrawerToggle(1));

            } else if (panelIndex == Panel.PLAYER) {

                panelOne.post(() -> navigationDrawerFragment.animateDrawerToggle(1));

                setActionBarAlpha(0f, false);
            } else {
                setActionBarAlpha(0f, false);
            }
        }

        if (savedInstanceState == null) {

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.main_container, MainFragment.newInstance())
                    .commit();
        }

        themeTaskDescription();

        handleIntent(getIntent());
    }

    private void setupFirstPanel() {
        panelOne = (SlidingUpPanelLayout) findViewById(R.id.panel1);
        panelOne.setTag("Panel One");
        panelOne.addPanelSlideListener(new SlidingUpPanelLayout.SimplePanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {

                navigationDrawerFragment.animateDrawerToggle(slideOffset);

                dummyStatusBar.setAlpha(1 - slideOffset);
            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                super.onPanelStateChanged(panel, previousState, newState);

                switch (newState) {
                    case COLLAPSED:

                        //When collapsed, the drag view is the 'mini player'
                        if (getMiniPlayerFragment() != null) {
                            panelOne.setDragView(getMiniPlayerFragment().getView());
                        }

                        checkTargetNavigation();
                        navigationDrawerFragment.toggleDrawerLock(false);

                        title = getString(R.string.library_title);
                        supportInvalidateOptionsMenu();
                        break;
                    case EXPANDED:

                        //When expanded, the drag view is the 'player fragment'
                        if (getPlayerFragment() != null) {
                            panelOne.setDragView(getPlayerFragment().getView());
                        }

                        checkTargetNavigation();
                        navigationDrawerFragment.toggleDrawerLock(true);

                        title = getString(R.string.nowplaying_title);
                        supportInvalidateOptionsMenu();
                        break;
                }
            }
        });
    }

    private void setupSecondPanel() {
        panelTwo = (SlidingUpPanelLayout) findViewById(R.id.panel2);
        panelTwo.setTag("Panel Two");

        //The second panel slide offset is mini player height + toolbar height + status bar height.
        //This gets our 'up next' sitting snugly underneath the toolbar
        int offset = (int) (ActionBarUtils.getActionBarHeight(this)
                + (ShuttleUtils.hasKitKat() ? ActionBarUtils.getStatusBarHeight(this) : 0)
                - getResources().getDimension(R.dimen.mini_player_height));
        panelTwo.setSlidePanelOffset(-offset);

        View upNextView = findViewById(R.id.upNextView);
        panelTwo.setDragView(upNextView);

        panelTwo.addPanelSlideListener(new SlidingUpPanelLayout.SimplePanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                // if we are not going to a specific panel, then disable sliding to prevent
                // the two sliding panels from fighting for touch input
                if (targetNavigationPanel == Panel.NONE) {
                    panelOne.setSlidingEnabled(false);
                }

                //Rotate the little arrow thing
                ImageView arrow = (ImageView) findViewById(R.id.arrow);
                if (panelTwo.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    arrow.setRotation(-slideOffset * 180);
                } else {
                    arrow.setRotation(slideOffset * 180);
                }

                setActionBarAlpha(slideOffset, false);
            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                super.onPanelStateChanged(panel, previousState, newState);

                switch (newState) {
                    case COLLAPSED:

                        //When collapsed, the drag view is the 'up next' view
                        View upNextView = findViewById(R.id.upNextView);
                        panelTwo.setDragView(upNextView);

                        panelOne.setSlidingEnabled(true);
                        checkTargetNavigation();
                        break;
                    case EXPANDED:

                        //When expanded, the drag view is the 'queue'
                        if (getQueueFragment() != null) {
                            panelTwo.setDragView(getQueueFragment().getView());
                        }

                        checkTargetNavigation();
                        break;
                }
            }
        });
    }

    @Panel
    public int getCurrentPanel() {
        if (panelTwo.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
            return Panel.QUEUE;
        } else if (panelOne.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
            return Panel.PLAYER;
        } else {
            return Panel.MAIN;
        }
    }

    protected void checkTargetNavigation() {
        if (targetNavigationPanel == getCurrentPanel()) {
            targetNavigationPanel = Panel.NONE;
        }
    }

    public void showPanel(@Panel int panel) {

        Log.i(TAG, "Show panel called..");

        // if we are already at our target panel, then don't do anything
        if (panel == getCurrentPanel()) {

            Log.i(TAG, "Panel = current panel. Returning");

            return;
        }

        switch (panel) {
            case Panel.MAIN:
                targetNavigationPanel = panel;
                panelTwo.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED, true);
                // Re-enable sliding on first panel so we can collapse it
                panelOne.setSlidingEnabled(true);
                panelOne.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED, true);
                break;
            case Panel.PLAYER:
                panelTwo.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED, true);
                panelOne.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED, true);
                break;
            case Panel.QUEUE:
                targetNavigationPanel = panel;
                panelTwo.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED, true);
                panelOne.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED, true);
                break;
        }
    }

    public void onSectionAttached(String title) {
        this.title = title;
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(title);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    @Override
    public void onResume() {

        super.onResume();

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
                    if (panelOne != null) {
                        panelOne.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED, true);
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
        if (isSlidingEnabled) {
            savedInstanceState.putInt(KEY_CURRENT_PANEL, getCurrentPanel());
        }
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!navigationDrawerFragment.isDrawerOpen()) {

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

            if (panelOne == null || panelOne.getPanelState() != SlidingUpPanelLayout.PanelState.EXPANDED) {

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

        if (panelOne == null || !isSlidingEnabled || panelOne.getPanelState() != SlidingUpPanelLayout.PanelState.EXPANDED) {
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
                                        panelOne.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED, true), time - System.currentTimeMillis() + 250);
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
                                        panelOne.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED, true), time - System.currentTimeMillis() + 250);
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
            case android.R.id.home:
                if (isSlidingEnabled) {
                    switch (getCurrentPanel()) {
                        case Panel.QUEUE:
                            showPanel(Panel.PLAYER);
                            break;
                        case Panel.PLAYER:
                            showPanel(Panel.MAIN);
                            break;
                        case Panel.MAIN:
                            break;
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

        if (isSlidingEnabled) {
            PlayerFragment playerFragment = (PlayerFragment) getSupportFragmentManager().findFragmentById(R.id.playerFragment);

            if (playerFragment != null) {

                playerFragment.update();

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

        togglePanelVisibility(!(MusicServiceConnectionUtils.sServiceBinder == null || MusicUtils.getSongId() == -1));
    }

    @Override
    public void onBackPressed() {

        if (isSlidingEnabled) {
            switch (getCurrentPanel()) {
                case Panel.QUEUE:
                    showPanel(Panel.PLAYER);
                    return;
                case Panel.PLAYER:
                    showPanel(Panel.MAIN);
                    return;
                case Panel.MAIN:
                    break;
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
            if (backPressListenerReference != null && backPressListenerReference.get() != null) {
                if (backPressListenerReference.get().onBackPressed()) {
                    return;
                }
            }
        }

        super.onBackPressed();

        if (isShowingFolders || containerFragment instanceof MainFragment) {
            if (navigationDrawerFragment != null) {
                navigationDrawerFragment.setDrawerItem(0);
            }
            title = getString(R.string.library_title);
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
        if (dummyStatusBar != null) {
            dummyStatusBar.setBackgroundColor(ShuttleUtils.hasLollipop() ? ColorUtils.getPrimaryColorDark(this) : ColorUtils.getPrimaryColor());
        }
        if (drawerLayout != null && ShuttleUtils.hasKitKat()) {
            drawerLayout.setStatusBarBackgroundColor(ShuttleUtils.hasLollipop() ? ColorUtils.getPrimaryColorDark(MainActivity.this) : ColorUtils.getPrimaryColor());
        }
        if (SettingsManager.getInstance().canTintNavBar()) {
            getWindow().setNavigationBarColor(ColorUtils.getPrimaryColorDark(MainActivity.this));
        }
        themeTaskDescription();
    }

    @Override
    public void onItemClicked(AlbumArtist albumArtist, View transitionView) {
        title = getString(R.string.library_title);
        swapFragments(albumArtist, transitionView);
    }

    @Override
    public void onItemClicked(Album album, View transitionView) {
        title = getString(R.string.library_title);
        swapFragments(album, transitionView);
    }

    @Override
    public void onItemClicked(Genre genre) {
        title = getString(R.string.library_title);
        swapFragments(DetailFragment.newInstance(genre), true);
    }

    @Override
    public void onItemClicked(Playlist playlist) {
        title = getString(R.string.library_title);
        swapFragments(DetailFragment.newInstance(playlist), true);
    }

    @Override
    public void onItemClicked(Serializable item, View transitionView) {
        title = getString(R.string.library_title);
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
                    title = getString(R.string.library_title);
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
        if (hasPendingPlaybackRequest) {
            handlePlaybackRequest(getIntent());
        }
    }

    private void handlePlaybackRequest(Intent intent) {

        if (intent == null) {
            return;
        } else if (MusicServiceConnectionUtils.sServiceBinder == null) {
            hasPendingPlaybackRequest = true;
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

        hasPendingPlaybackRequest = false;
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
                prefs.edit().putBoolean("pref_theme_gold", true).apply();
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
//        if (!mIsSlidingEnabled) {
////            findViewById(R.id.mini_player_container).setVisibility(show ? View.VISIBLE : View.GONE);
//        } else {
//            if (panelOne != null) {
//                if (show) {
//                    panelOne.showPanel();
//                } else {
//                    panelOne.hidePanel();
//                }
//            }
//        }
    }

    public void setOnBackPressedListener(BackPressListener listener) {
        if (listener == null) {
            backPressListenerReference.clear();
        } else {
            backPressListenerReference = new WeakReference<>(listener);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void setActionBarAlpha(float alpha, boolean store) {

        Fragment mainFragment = getSupportFragmentManager().findFragmentById(R.id.main_container);
//        if (mainFragment != null && !(mainFragment instanceof DetailFragment)) {
//            if (alpha > 0f) {
//                alpha = 1f;
//            } else {
//                alpha = 0f;
//            }
//        } else {
        if (store) {
            this.alpha = alpha;
        } else {
            alpha = Math.max(0f, Math.min(1f, this.alpha + alpha));
        }
//        }

        if (toolbar != null) {
            toolbar.setBackgroundColor(ColorUtils.adjustAlpha(ColorUtils.getPrimaryColor(), alpha));
        }

        if (dummyStatusBar != null) {
            dummyStatusBar.setBackgroundColor(ColorUtils.adjustAlpha(ShuttleUtils.hasLollipop() ? ColorUtils.getPrimaryColorDark(this) : ColorUtils.getPrimaryColor(), alpha));
        }

        if (ShuttleUtils.hasKitKat()) {
            if (tintManager != null) {
                tintManager.setStatusBarTintColor(ColorUtils.adjustAlpha(ShuttleUtils.hasLollipop() ? ColorUtils.getPrimaryColorDark(this) : ColorUtils.getPrimaryColor(), alpha));
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

        if (isSlidingEnabled
                && panelOne != null
                && (panelOne.getPanelState() == SlidingUpPanelLayout.PanelState.DRAGGING
                || panelOne.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED)) {
            canSetAlpha = false;
        }

        Log.i(TAG, "Can set alpha: " + canSetAlpha);

        return canSetAlpha;
    }

    @Override
    protected String screenName() {
        return TAG;
    }

    @Nullable
    private PlayerFragment getPlayerFragment() {
        return (PlayerFragment) getSupportFragmentManager().findFragmentById(R.id.playerFragment);
    }

    @Nullable
    private QueueFragment getQueueFragment() {
        return (QueueFragment) getSupportFragmentManager().findFragmentById(R.id.queueFragment);
    }

    @Nullable
    private MiniPlayerFragment getMiniPlayerFragment() {
        return (MiniPlayerFragment) getSupportFragmentManager().findFragmentById(R.id.miniPlayerFragment);
    }

    public void setScrollableView(View scrollableView) {
        if (panelTwo == null) {
            return;
        }
        panelTwo.setScrollableView(scrollableView);
    }
}