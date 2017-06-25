package com.simplecity.amp_library.ui.drawer;

import android.support.annotation.NonNull;
import android.support.v7.widget.PopupMenu;
import android.view.View;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.crashlytics.android.core.CrashlyticsCore;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.PlaylistsModel;
import com.simplecity.amp_library.ui.presenters.Presenter;
import com.simplecity.amp_library.utils.MenuUtils;
import com.simplecity.amp_library.utils.PermissionUtils;

import javax.inject.Inject;

import rx.android.schedulers.AndroidSchedulers;

public class DrawerPresenter extends Presenter<DrawerView> {

    @Inject NavigationEventRelay navigationEventRelay;

    @Inject PlaylistsModel playlistsModel;

    @Inject
    public DrawerPresenter() {
    }

    @Override
    public void bindView(@NonNull DrawerView view) {
        super.bindView(view);

        loadData();

        addSubcscription(navigationEventRelay.getEvents().subscribe(drawerEvent -> {
            DrawerView drawerView = getView();
            switch (drawerEvent.type) {
                case NavigationEventRelay.NavigationEvent.Type.LIBRARY_SELECTED:
                    if (drawerView != null) {
                        drawerView.setDrawerItemSelected(DrawerParent.Type.LIBRARY);
                    }
                    break;
                case NavigationEventRelay.NavigationEvent.Type.FOLDERS_SELECTED:
                    if (drawerView != null) {
                        drawerView.setDrawerItemSelected(DrawerParent.Type.FOLDERS);
                    }
                    break;
            }
        }));
    }

    void onDrawerItemClicked(DrawerParent drawerParent) {
        DrawerView drawerView = getView();
        if (drawerView != null && drawerParent.selectable) {
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

    private void loadData() {
        PermissionUtils.RequestStoragePermissions(() ->
                playlistsModel.getPlaylists()
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
                                            popupMenu.setOnMenuItemClickListener(MenuUtils.getPlaylistClickListener(v.getContext(), playlist));
                                            popupMenu.show();
                                        }
                                    });
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
                        ));
    }

}