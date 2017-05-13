package com.simplecity.amp_library.ui.drawer;

import java.util.List;

public interface DrawerView {

    void setItems(List<DrawerChild> drawerChildren);

    void closeDrawer();

    void setDrawerItemSelected(@DrawerParent.Type int type);

}
