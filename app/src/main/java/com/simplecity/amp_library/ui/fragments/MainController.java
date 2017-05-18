package com.simplecity.amp_library.ui.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.ui.detail.PlaylistDetailFragment;
import com.simplecity.amp_library.ui.drawer.DrawerEventRelay;
import com.simplecity.amp_library.ui.settings.SettingsParentFragment;
import com.simplecity.amp_library.ui.views.UpNextView;
import com.simplecity.amp_library.ui.views.multisheet.CustomMultiSheetView;
import com.simplecity.amp_library.ui.views.multisheet.MultiSheetEventRelay;

import javax.inject.Inject;

import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;
import test.com.androidnavigation.fragment.BaseNavigationController;
import test.com.androidnavigation.fragment.FragmentInfo;
import test.com.multisheetview.ui.view.MultiSheetView;

public class MainController extends BaseNavigationController {

    private static final String TAG = "MainController";

    @Inject DrawerEventRelay drawerEventRelay;

    @Inject MultiSheetEventRelay multiSheetEventRelay;

    private CompositeSubscription subscriptions = new CompositeSubscription();

    private Handler delayHandler;

    public static MainController newInstance() {
        Bundle args = new Bundle();
        MainController fragment = new MainController();
        fragment.setArguments(args);
        return fragment;
    }

    public MainController() {

    }

    private MultiSheetView multiSheetView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        ShuttleApplication.getInstance().getAppComponent().inject(this);

        multiSheetView = (CustomMultiSheetView) rootView.findViewById(R.id.multiSheetView);

        if (savedInstanceState == null) {

            getChildFragmentManager()
                    .beginTransaction()
                    .add(multiSheetView.getSheetContainerViewResId(MultiSheetView.Sheet.FIRST), PlayerFragment.newInstance())
                    .add(multiSheetView.getSheetPeekViewResId(MultiSheetView.Sheet.FIRST), MiniPlayerFragment.newInstance())
                    .add(multiSheetView.getSheetContainerViewResId(MultiSheetView.Sheet.SECOND), QueueFragment.newInstance())
                    .commit();

            ((ViewGroup) multiSheetView.findViewById(multiSheetView.getSheetPeekViewResId(MultiSheetView.Sheet.SECOND))).addView(new UpNextView(getContext()));
        }

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (delayHandler != null) {
            delayHandler.removeCallbacksAndMessages(null);
        }
        delayHandler = new Handler();

        subscriptions.add(drawerEventRelay.getEvents()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(drawerEvent -> {
                    switch (drawerEvent.type) {
                        case DrawerEventRelay.DrawerEvent.Type.LIBRARY_SELECTED:
                            popToRootViewController();
                            break;
                        case DrawerEventRelay.DrawerEvent.Type.FOLDERS_SELECTED:
                            pushViewController(FolderFragment.newInstance("PageTitle"), "FolderFragment");
                            break;
                        case DrawerEventRelay.DrawerEvent.Type.SETTINGS_SELECTED:
                            delayHandler.postDelayed(() -> multiSheetEventRelay.sendEvent(new MultiSheetEventRelay.MultiSheetEvent(MultiSheetEventRelay.MultiSheetEvent.Action.HIDE, MultiSheetView.Sheet.FIRST)), 100);
                            delayHandler.postDelayed(() -> pushViewController(SettingsParentFragment.newInstance(R.xml.settings_headers, R.string.settings), "Settings Fragment"), 250);
                            break;
                        case DrawerEventRelay.DrawerEvent.Type.SUPPORT_SELECTED:
                            delayHandler.postDelayed(() -> multiSheetEventRelay.sendEvent(new MultiSheetEventRelay.MultiSheetEvent(MultiSheetEventRelay.MultiSheetEvent.Action.HIDE, MultiSheetView.Sheet.FIRST)), 100);
                            delayHandler.postDelayed(() -> pushViewController(SettingsParentFragment.newInstance(R.xml.settings_support, R.string.pref_title_support), "Support Fragment"), 250);
                            break;
                        case DrawerEventRelay.DrawerEvent.Type.PLAYLIST_SELECTED:
                            pushViewController(PlaylistDetailFragment.newInstance((Playlist) drawerEvent.data), "PlaylistDetailFragment");
                            break;
                    }
                }));
    }

    @Override
    public void onPause() {
        super.onPause();

        delayHandler.removeCallbacksAndMessages(null);
        delayHandler = null;

        subscriptions.clear();
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
}