package com.simplecity.amp_library.ui.drawer;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.widget.PopupMenu;
import android.view.View;

import com.annimon.stream.Stream;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.PlaylistsModel;
import com.simplecity.amp_library.ui.presenters.PurchasePresenter;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.MenuUtils;
import com.simplecity.amp_library.utils.PermissionUtils;
import com.simplecity.amp_library.utils.ShuttleUtils;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class DrawerPresenter extends PurchasePresenter<DrawerView> {

    private static final String TAG = "DrawerPresenter";

    @Inject
    NavigationEventRelay navigationEventRelay;

    @Inject
    PlaylistsModel playlistsModel;

    @Inject
    public DrawerPresenter(Activity activity) {
        super(activity);
    }

    @Override
    public void bindView(@NonNull DrawerView view) {
        super.bindView(view);

        loadData(view);

        addDisposable(navigationEventRelay.getEvents().subscribe(drawerEvent -> {
            DrawerView drawerView = getView();
            switch (drawerEvent.type) {
                case NavigationEventRelay.NavigationEvent.Type.LIBRARY_SELECTED:
                    if (drawerView != null) {
                        drawerView.setDrawerItemSelected(DrawerParent.Type.LIBRARY);
                    }
                    break;
                case NavigationEventRelay.NavigationEvent.Type.FOLDERS_SELECTED:
                    if (drawerView != null) {
                        if (ShuttleUtils.isUpgraded()) {
                            drawerView.setDrawerItemSelected(DrawerParent.Type.FOLDERS);
                        } else {
                            upgradeClicked();
                        }
                    }
                    break;
            }
        }));
    }

    void onDrawerItemClicked(DrawerParent drawerParent) {
        DrawerView drawerView = getView();
        if (drawerView != null && drawerParent.isSelectable()) {
            drawerView.setDrawerItemSelected(drawerParent.type);
        }

        closeDrawer();

        if (drawerParent.navigationEvent != null) {
            navigationEventRelay.sendEvent(drawerParent.navigationEvent);
        }
    }

    void onPlaylistClicked(Playlist playlist) {
        closeDrawer();
        navigationEventRelay.sendEvent(new NavigationEventRelay.NavigationEvent(NavigationEventRelay.NavigationEvent.Type.PLAYLIST_SELECTED, playlist));
    }

    private void closeDrawer() {
        DrawerView drawerView = getView();
        if (drawerView != null) {
            drawerView.closeDrawer();
        }
    }

    private void loadData(@NonNull DrawerView drawerView) {
        PermissionUtils.RequestStoragePermissions(() ->
                addDisposable(playlistsModel.getPlaylistsObservable()
                        .map(playlists -> Stream.of(playlists)
                                .map(playlist1 -> {
                                    DrawerChild drawerChild = new DrawerChild(playlist1);
                                    drawerChild.setListener(new DrawerChild.ClickListener() {
                                        @Override
                                        public void onClick(Playlist playlist) {
                                            onPlaylistClicked(playlist);
                                        }

                                        @Override
                                        public void onOverflowClick(View v, Playlist playlist) {
                                            PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                                            MenuUtils.setupPlaylistMenu(popupMenu, playlist);
                                            popupMenu.setOnMenuItemClickListener(MenuUtils.getPlaylistPopupMenuClickListener(v.getContext(), playlist, null));
                                            popupMenu.show();
                                        }
                                    });
                                    return drawerChild;
                                })
                                .toList())
                        .observeOn(AndroidSchedulers.mainThread())
                        // Delay the subscription so we're not querying data while the app is launching
                        .delaySubscription(Observable.timer(1500, TimeUnit.MILLISECONDS))
                        // after all, clear all playlist item
                        // to avoid memory leak in static var DrawerParent.playlistsParent
                        .doFinally(() -> drawerView.setPlaylistItems(Collections.emptyList()))
                        .subscribe(
                                drawerView::setPlaylistItems,
                                error -> LogUtils.logException(TAG, "Error refreshing DrawerFragment adapter items", error)
                        )));
    }

}