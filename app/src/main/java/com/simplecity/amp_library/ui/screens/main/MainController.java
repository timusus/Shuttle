package com.simplecity.amp_library.ui.screens.main;

import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.util.Pair;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.cantrowitz.rxbroadcast.RxBroadcast;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.Genre;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.playback.MediaManager;
import com.simplecity.amp_library.playback.constants.InternalIntents;
import com.simplecity.amp_library.rx.UnsafeAction;
import com.simplecity.amp_library.ui.screens.album.detail.AlbumDetailFragment;
import com.simplecity.amp_library.ui.screens.artist.detail.ArtistDetailFragment;
import com.simplecity.amp_library.ui.screens.drawer.DrawerLockController;
import com.simplecity.amp_library.ui.screens.drawer.DrawerLockManager;
import com.simplecity.amp_library.ui.screens.drawer.DrawerProvider;
import com.simplecity.amp_library.ui.screens.drawer.MiniPlayerLockManager;
import com.simplecity.amp_library.ui.screens.drawer.NavigationEventRelay;
import com.simplecity.amp_library.ui.screens.equalizer.EqualizerFragment;
import com.simplecity.amp_library.ui.screens.folders.FolderFragment;
import com.simplecity.amp_library.ui.screens.genre.detail.GenreDetailFragment;
import com.simplecity.amp_library.ui.screens.miniplayer.MiniPlayerFragment;
import com.simplecity.amp_library.ui.screens.nowplaying.PlayerFragment;
import com.simplecity.amp_library.ui.screens.nowplaying.PlayerPresenter;
import com.simplecity.amp_library.ui.screens.playlist.detail.PlaylistDetailFragment;
import com.simplecity.amp_library.ui.screens.queue.QueueFragment;
import com.simplecity.amp_library.ui.screens.upnext.UpNextView;
import com.simplecity.amp_library.ui.settings.SettingsParentFragment;
import com.simplecity.amp_library.ui.views.multisheet.CustomMultiSheetView;
import com.simplecity.amp_library.ui.views.multisheet.MultiSheetEventRelay;
import com.simplecity.amp_library.ui.views.multisheet.MultiSheetSlideEventRelay;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.SleepTimer;
import com.simplecity.multisheetview.ui.view.MultiSheetView;
import dagger.android.support.AndroidSupportInjection;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.util.List;
import javax.inject.Inject;
import test.com.androidnavigation.fragment.BackPressHandler;
import test.com.androidnavigation.fragment.BaseNavigationController;
import test.com.androidnavigation.fragment.FragmentInfo;

public class MainController extends BaseNavigationController implements BackPressHandler, DrawerLockController {

    private static final String TAG = "MainController";

    public static final String STATE_CURRENT_SHEET = "current_sheet";

    @Inject
    NavigationEventRelay navigationEventRelay;

    @Inject
    MultiSheetEventRelay multiSheetEventRelay;

    @Inject
    MultiSheetSlideEventRelay multiSheetSlideEventRelay;

    @Inject
    MediaManager mediaManager;

    @Inject
    PlayerPresenter playerPresenter;

    @Inject
    SettingsManager settingsManager;

    private Handler delayHandler;

    @BindView(R.id.multiSheetView)
    CustomMultiSheetView multiSheetView;

    private CompositeDisposable disposables = new CompositeDisposable();

    public static MainController newInstance() {
        Bundle args = new Bundle();
        MainController fragment = new MainController();
        fragment.setArguments(args);
        return fragment;
    }

    public MainController() {

    }

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        ButterKnife.bind(this, rootView);


        multiSheetView.setMultiSheetEventRelay(multiSheetEventRelay);
        multiSheetView.setMultiSheetSlideEventRelay(multiSheetSlideEventRelay);

        if (savedInstanceState == null) {
            getChildFragmentManager()
                    .beginTransaction()
                    .add(multiSheetView.getSheetContainerViewResId(MultiSheetView.Sheet.FIRST), PlayerFragment.newInstance())
                    .add(multiSheetView.getSheetPeekViewResId(MultiSheetView.Sheet.FIRST), MiniPlayerFragment.newInstance())
                    .add(multiSheetView.getSheetContainerViewResId(MultiSheetView.Sheet.SECOND), QueueFragment.Companion.newInstance())
                    .commit();
        } else {
            multiSheetView.restoreSheet(savedInstanceState.getInt(STATE_CURRENT_SHEET));
        }

        ((ViewGroup) multiSheetView.findViewById(multiSheetView.getSheetPeekViewResId(MultiSheetView.Sheet.SECOND))).addView(
                UpNextView.Companion.newInstance(getContext(), playerPresenter, settingsManager));

        toggleBottomSheetVisibility(false, false);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (delayHandler != null) {
            delayHandler.removeCallbacksAndMessages(null);
        }
        delayHandler = new Handler();

        disposables.add(navigationEventRelay.getEvents()
                .observeOn(AndroidSchedulers.mainThread())
                .filter(NavigationEventRelay.NavigationEvent::isActionable)
                .subscribe(navigationEvent -> {
                    switch (navigationEvent.type) {
                        case NavigationEventRelay.NavigationEvent.Type.LIBRARY_SELECTED:
                            popToRootViewController();
                            break;
                        case NavigationEventRelay.NavigationEvent.Type.FOLDERS_SELECTED:
                            delayHandler.postDelayed(() -> pushViewController(FolderFragment.newInstance(getString(R.string.folders_title), false), "FolderFragment"), 250);
                            break;
                        case NavigationEventRelay.NavigationEvent.Type.SLEEP_TIMER_SELECTED:
                            UnsafeAction showToast = () -> Toast.makeText(getContext(), R.string.sleep_timer_started, Toast.LENGTH_SHORT).show();
                            SleepTimer.getInstance().getDialog(
                                    getContext(),
                                    () -> SleepTimer.getInstance().showMinutesDialog(getContext(), showToast),
                                    showToast
                            ).show();
                            break;
                        case NavigationEventRelay.NavigationEvent.Type.EQUALIZER_SELECTED:
                            delayHandler.postDelayed(
                                    () -> multiSheetEventRelay.sendEvent(new MultiSheetEventRelay.MultiSheetEvent(MultiSheetEventRelay.MultiSheetEvent.Action.HIDE, MultiSheetView.Sheet.FIRST)), 100);
                            delayHandler.postDelayed(() -> pushViewController(EqualizerFragment.newInstance(), "EqualizerFragment"), 250);
                            break;
                        case NavigationEventRelay.NavigationEvent.Type.SETTINGS_SELECTED:
                            delayHandler.postDelayed(
                                    () -> multiSheetEventRelay.sendEvent(new MultiSheetEventRelay.MultiSheetEvent(MultiSheetEventRelay.MultiSheetEvent.Action.HIDE, MultiSheetView.Sheet.FIRST)), 100);
                            delayHandler.postDelayed(() -> pushViewController(SettingsParentFragment.newInstance(R.xml.settings_headers, R.string.settings), "Settings Fragment"), 250);
                            break;
                        case NavigationEventRelay.NavigationEvent.Type.SUPPORT_SELECTED:
                            delayHandler.postDelayed(
                                    () -> multiSheetEventRelay.sendEvent(new MultiSheetEventRelay.MultiSheetEvent(MultiSheetEventRelay.MultiSheetEvent.Action.HIDE, MultiSheetView.Sheet.FIRST)), 100);
                            delayHandler.postDelayed(() -> pushViewController(SettingsParentFragment.newInstance(R.xml.settings_support, R.string.pref_title_support), "Support Fragment"), 250);
                            break;
                        case NavigationEventRelay.NavigationEvent.Type.PLAYLIST_SELECTED:
                            delayHandler.postDelayed(() -> pushViewController(PlaylistDetailFragment.Companion.newInstance((Playlist) navigationEvent.data), "PlaylistDetailFragment"), 250);
                            break;
                        case NavigationEventRelay.NavigationEvent.Type.GO_TO_ARTIST:
                            multiSheetView.goToSheet(MultiSheetView.Sheet.NONE);
                            AlbumArtist albumArtist = (AlbumArtist) navigationEvent.data;
                            delayHandler.postDelayed(() -> {
                                popToRootViewController();
                                pushViewController(ArtistDetailFragment.Companion.newInstance(albumArtist, null), "ArtistDetailFragment");
                            }, 250);
                            break;
                        case NavigationEventRelay.NavigationEvent.Type.GO_TO_ALBUM:
                            multiSheetView.goToSheet(MultiSheetView.Sheet.NONE);
                            Album album = (Album) navigationEvent.data;
                            delayHandler.postDelayed(() -> {
                                popToRootViewController();
                                pushViewController(AlbumDetailFragment.Companion.newInstance(album, null), "AlbumDetailFragment");
                            }, 250);
                            break;
                        case NavigationEventRelay.NavigationEvent.Type.GO_TO_GENRE:
                            multiSheetView.goToSheet(MultiSheetView.Sheet.NONE);
                            Genre genre = (Genre) navigationEvent.data;
                            delayHandler.postDelayed(() -> {
                                popToRootViewController();
                                pushViewController(GenreDetailFragment.Companion.newInstance(genre), "GenreDetailFragment");
                            }, 250);
                            break;
                    }
                }));

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(InternalIntents.SERVICE_CONNECTED);
        intentFilter.addAction(InternalIntents.QUEUE_CHANGED);
        disposables.add(
                RxBroadcast.fromBroadcast(getContext(), intentFilter)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(intent -> {
                            toggleBottomSheetVisibility(true, true);
                        })
        );

        DrawerLockManager.getInstance().setDrawerLockController(this);
    }

    @Override
    public void onPause() {
        delayHandler.removeCallbacksAndMessages(null);
        delayHandler = null;

        disposables.clear();

        DrawerLockManager.getInstance().setDrawerLockController(null);

        super.onPause();
    }

    /**
     * Hide/show the bottom sheet, depending on whether the queue is empty.
     */
    private void toggleBottomSheetVisibility(boolean collapse, boolean animate) {
        if (!mediaManager.getQueueReloading() && mediaManager.getQueue().isEmpty()) {
            multiSheetView.hide(collapse, false);
        } else if (MiniPlayerLockManager.getInstance().canShowMiniPlayer()) {
            multiSheetView.unhide(animate);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_CURRENT_SHEET, multiSheetView.getCurrentSheet());
        super.onSaveInstanceState(outState);
    }

    @Override
    public FragmentInfo getRootViewControllerInfo() {
        return LibraryController.fragmentInfo();
    }

    @Override
    public boolean consumeBackPress() {
        if (multiSheetView.consumeBackPress()) {
            return true;
        }

        return super.consumeBackPress();
    }

    @Override
    public void lockDrawer() {
        ((DrawerProvider) getActivity()).getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    @Override
    public void unlockDrawer() {
        // Don't unlock the drawer if one of the sheets is expanded
        if (multiSheetView.getCurrentSheet() == MultiSheetView.Sheet.FIRST || multiSheetView.getCurrentSheet() == MultiSheetView.Sheet.SECOND) {
            return;
        }

        ((DrawerProvider) getActivity()).getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    // Todo:  Remove once cause of shared element crash is understood.
    // This is a copy of the superclass method of the same name/signature, with some additional logging
    // to help ascertain the cause of a crash.
    @Override
    public void pushViewController(@NonNull Fragment fragment, @Nullable String tag, @Nullable List<Pair<View, String>> sharedElements) {
        FragmentTransaction fragmentTransaction = getChildFragmentManager()
                .beginTransaction();

        if (sharedElements != null) {
            for (Pair<View, String> pair : sharedElements) {
                try {
                    fragmentTransaction.addSharedElement(pair.first, pair.second);
                } catch (IllegalArgumentException e) {
                    LogUtils.logException(TAG, String.format("Error adding shared element transition.. key: %s, value: %s", pair.first, pair.second), e);
                }
            }
        }

        fragmentTransaction.addToBackStack(null)
                .replace(test.com.androidnavigation.R.id.mainContainer, fragment, tag)
                .commit();
    }
}