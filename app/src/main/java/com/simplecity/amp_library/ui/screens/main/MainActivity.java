package com.simplecity.amp_library.ui.screens.main;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import com.afollestad.aesthetic.Aesthetic;
import com.greysonparrelli.permiso.Permiso;
import com.simplecity.amp_library.BuildConfig;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.data.Repository;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Query;
import com.simplecity.amp_library.playback.MediaManager;
import com.simplecity.amp_library.playback.constants.ShortcutCommands;
import com.simplecity.amp_library.sql.sqlbrite.SqlBriteUtils;
import com.simplecity.amp_library.ui.common.BaseActivity;
import com.simplecity.amp_library.ui.common.ToolbarListener;
import com.simplecity.amp_library.ui.dialog.ChangelogDialog;
import com.simplecity.amp_library.ui.screens.drawer.DrawerProvider;
import com.simplecity.amp_library.ui.screens.drawer.NavigationEventRelay;
import com.simplecity.amp_library.utils.AnalyticsManager;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.MusicServiceConnectionUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ThemeUtils;
import com.simplecity.amp_library.utils.playlists.PlaylistManager;
import dagger.android.AndroidInjection;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import kotlin.Unit;
import test.com.androidnavigation.fragment.BackPressHandler;
import test.com.androidnavigation.fragment.BackPressListener;

public class MainActivity extends BaseActivity implements
        ToolbarListener,
        BackPressHandler,
        DrawerProvider {

    private static final String TAG = "MainActivity";

    private List<BackPressListener> backPressListeners = new ArrayList<>();

    private DrawerLayout drawerLayout;

    private View navigationView;

    private boolean hasPendingPlaybackRequest;

    @Inject
    NavigationEventRelay navigationEventRelay;

    @Inject
    MediaManager mediaManager;

    @Inject
    Repository.SongsRepository songsRepository;

    @Inject
    AnalyticsManager analyticsManager;

    @Inject
    SettingsManager settingsManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        analyticsManager.dropBreadcrumb(TAG, "onCreate()");

        // If we haven't set any defaults, do that now
        if (Aesthetic.isFirstTime(this)) {

            ThemeUtils.Theme theme = ThemeUtils.getRandom();

            Aesthetic.get(this)
                    .activityTheme(theme.isDark ? R.style.AppTheme : R.style.AppTheme_Light)
                    .isDark(theme.isDark)
                    .colorPrimaryRes(theme.primaryColor)
                    .colorAccentRes(theme.accentColor)
                    .colorStatusBarAuto()
                    .apply();

            analyticsManager.logInitialTheme(theme);
        }

        setContentView(R.layout.activity_main);

        Permiso.getInstance().setActivity(this);

        navigationView = findViewById(R.id.navView);

        //Ensure the drawer draws a content scrim over the status bar.
        drawerLayout = findViewById(R.id.drawer_layout);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            drawerLayout.setOnApplyWindowInsetsListener((view, windowInsets) -> {
                navigationView.dispatchApplyWindowInsets(windowInsets);
                return windowInsets.replaceSystemWindowInsets(0, 0, 0, 0);
            });
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.mainContainer, MainController.newInstance())
                    .commit();
        }

        handleIntent(getIntent());
    }

    @Override
    public void onResume() {
        super.onResume();
        analyticsManager.dropBreadcrumb(TAG, "onCreate()");

        showChangelogDialog();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        analyticsManager.dropBreadcrumb(TAG, "onServiceConnected()");

        handlePendingPlaybackRequest();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        handleIntent(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();

        analyticsManager.dropBreadcrumb(TAG, "onPause()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        analyticsManager.dropBreadcrumb(TAG, "onDestroy()");
    }

    private void handleIntent(Intent intent) {
        Single.fromCallable(() -> {
            boolean handled = false;
            if (ShortcutCommands.PLAYLIST.equals(intent.getAction())) {
                Playlist playlist = (Playlist) intent.getExtras().getSerializable(PlaylistManager.ARG_PLAYLIST);
                NavigationEventRelay.NavigationEvent navigationEvent = new NavigationEventRelay.NavigationEvent(NavigationEventRelay.NavigationEvent.Type.PLAYLIST_SELECTED, playlist, true);
                navigationEventRelay.sendEvent(navigationEvent);
                handled = true;
            } else if (ShortcutCommands.FOLDERS.equals(intent.getAction())) {
                NavigationEventRelay.NavigationEvent foldersSelectedEvent = new NavigationEventRelay.NavigationEvent(NavigationEventRelay.NavigationEvent.Type.FOLDERS_SELECTED, null, true);
                navigationEventRelay.sendEvent(foldersSelectedEvent);
                handled = true;
            }

            if (!handled) {
                handlePlaybackRequest(intent);
            } else {
                setIntent(new Intent());
            }

            return true;
        })
                .delaySubscription(350, TimeUnit.MILLISECONDS)
                .subscribe(
                        aBoolean -> {
                        },
                        throwable -> LogUtils.logException(TAG, "handleIntent error", throwable)
                );
    }

    private void handlePendingPlaybackRequest() {
        if (hasPendingPlaybackRequest) {
            handlePlaybackRequest(getIntent());
        }
    }

    @SuppressLint("CheckResult")
    private void handlePlaybackRequest(Intent intent) {
        if (intent == null) {
            return;
        } else if (MusicServiceConnectionUtils.serviceBinder == null) {
            hasPendingPlaybackRequest = true;
            return;
        }

        final Uri uri = intent.getData();
        final String mimeType = intent.getType();

        if (uri != null && uri.toString().length() > 0) {
            mediaManager.playFile(uri);
            // Make sure to process intent only once
            setIntent(new Intent());
        } else if (MediaStore.Audio.Playlists.CONTENT_TYPE.equals(mimeType)) {
            long id = parseIdFromIntent(intent, "playlistId", "playlist");
            if (id >= 0) {
                Query query = Playlist.getQuery();
                query.uri = ContentUris.withAppendedId(query.uri, id);
                SqlBriteUtils.createSingle(this, (cursor) -> new Playlist(this, cursor), query, null)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                playlist -> {
                                    mediaManager.playAll(songsRepository.getSongs(playlist).first(new ArrayList<>()),
                                            () -> {
                                                // Todo: Show playback failure toast
                                                return Unit.INSTANCE;
                                            });
                                    // Make sure to process intent only once
                                    setIntent(new Intent());
                                },
                                error -> LogUtils.logException(TAG, "Error handling playback request", error)
                        );
            }
        }

        hasPendingPlaybackRequest = false;
    }

    private long parseIdFromIntent(Intent intent, String longKey, String stringKey) {
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

    private void showChangelogDialog() {
        int storedVersionCode = settingsManager.getStoredVersionCode();

        // If we've stored a version code in the past, and it's lower than the current version code,
        // we can show the changelog.
        // Don't show the changelog for first time users.
        if (storedVersionCode != -1 && storedVersionCode < BuildConfig.VERSION_CODE) {
            if (settingsManager.getShowChangelogOnLaunch()) {
                ChangelogDialog.Companion.newInstance().show(getSupportFragmentManager());
            }
        }
        settingsManager.setVersionCode();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            if (!backPressListeners.isEmpty()) {
                for (int i = backPressListeners.size() - 1; i >= 0; i--) {
                    BackPressListener backPressListener = backPressListeners.get(i);
                    if (backPressListener.consumeBackPress()) {
                        return;
                    }
                }
            }
            super.onBackPressed();
        }
    }

    @Override
    public void toolbarAttached(Toolbar toolbar) {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, 0);
            }
        };
        drawer.addDrawerListener(toggle);
        toggle.syncState();
    }

    @Override
    public void addBackPressListener(@NonNull BackPressListener listener) {
        if (!backPressListeners.contains(listener)) {
            backPressListeners.add(listener);
        }
    }

    @Override
    public void removeBackPressListener(@NonNull BackPressListener listener) {
        if (backPressListeners.contains(listener)) {
            backPressListeners.remove(listener);
        }
    }

    @Override
    protected String screenName() {
        return "MainActivity";
    }

    @Override
    public DrawerLayout getDrawerLayout() {
        return drawerLayout;
    }
}
