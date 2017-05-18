package com.simplecity.amp_library.ui.drawer;

import android.support.annotation.NonNull;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.crashlytics.android.core.CrashlyticsCore;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.PlaylistsModel;
import com.simplecity.amp_library.ui.presenters.Presenter;
import com.simplecity.amp_library.utils.PermissionUtils;

import javax.inject.Inject;

import rx.android.schedulers.AndroidSchedulers;

public class DrawerPresenter extends Presenter<DrawerView> {

    @Inject DrawerEventRelay drawerEventRelay;

    @Inject PlaylistsModel playlistsModel;

    @Inject
    public DrawerPresenter() {
        ShuttleApplication.getInstance().getAppComponent().inject(this);
    }

    @Override
    public void bindView(@NonNull DrawerView view) {
        super.bindView(view);

        loadData();
    }

    void onDrawerItemClicked(DrawerParent drawerParent) {
        DrawerView drawerView = getView();
        if (drawerView != null && drawerParent.selectable) {
            drawerView.setDrawerItemSelected(drawerParent.type);
        }

        closeDrawer();

        if (drawerParent.drawerEvent != null) {
            drawerEventRelay.sendEvent(drawerParent.drawerEvent);
        }
    }

    void onPlaylistClicked(Playlist playlist) {
        closeDrawer();
        drawerEventRelay.sendEvent(new DrawerEventRelay.DrawerEvent(DrawerEventRelay.DrawerEvent.Type.PLAYLIST_SELECTED));
    }

    private void closeDrawer() {
        DrawerView drawerView = getView();
        if (drawerView != null) {
            drawerView.closeDrawer();
        }
    }

    private void loadData() {
        PermissionUtils.RequestStoragePermissions(() -> {
            playlistsModel.getPlaylists().map(playlists -> Stream.of(playlists)
                    .map(playlist1 -> {
                        DrawerChild drawerChild = new DrawerChild(playlist1);
                        drawerChild.setListener(this::onPlaylistClicked);
                        return drawerChild;
                    })
                    .collect(Collectors.toList()))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            drawerChildren -> {
                                DrawerView drawerView = getView();
                                if (drawerView != null) {
                                    drawerView.setItems(drawerChildren);
                                }
                            },
                            error -> CrashlyticsCore.getInstance().log("Error refreshing DrawerFragment adapter items: " + error.toString())
                    );
        });
    }

}