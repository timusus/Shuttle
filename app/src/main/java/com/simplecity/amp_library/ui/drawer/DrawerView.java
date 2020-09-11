package com.simplecity.amp_library.ui.drawer;

import com.simplecity.amp_library.model.Playlist;
import java.util.List;

public interface DrawerView {

    void setPlaylistItems(List<Playlist> playlists);

    void closeDrawer();

    void setDrawerItemSelected(@DrawerParent.Type int type);
}
