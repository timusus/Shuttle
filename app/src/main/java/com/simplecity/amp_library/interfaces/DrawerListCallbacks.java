package com.simplecity.amp_library.interfaces;

import android.view.View;

import com.simplecity.amp_library.model.DrawerGroupItem;
import com.simplecity.amp_library.model.Playlist;

public interface DrawerListCallbacks {

    void onDrawerItemClick(DrawerGroupItem drawerGroupItem);

    void onPlaylistItemClick(DrawerGroupItem drawerGroupItem, Playlist playlist);

    void onOverflowButtonClick(View v, Playlist playlist);
}
