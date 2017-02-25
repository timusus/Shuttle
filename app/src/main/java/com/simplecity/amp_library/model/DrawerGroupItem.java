package com.simplecity.amp_library.model;

import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.support.annotation.StringRes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DrawerGroupItem implements Serializable {

    @IntDef({Type.LIBRARY, Type.FOLDERS, Type.PLAYLISTS, Type.SETTINGS, Type.SUPPORT, Type.DIVIDER})
    public @interface Type {
        int LIBRARY = 0;
        int FOLDERS = 1;
        int PLAYLISTS = 2;
        int SETTINGS = 3;
        int SUPPORT = 4;
        int DIVIDER = 5;
    }

    @Type
    public int type;

    @StringRes
    public int titleResId;

    @DrawableRes
    public int iconResId;

    public List<Playlist> children = new ArrayList<>();

    public DrawerGroupItem(@Type int type, @StringRes int titleResId, @DrawableRes int iconResId) {
        this.type = type;
        this.titleResId = titleResId;
        this.iconResId = iconResId;
    }

    public void addChildren(List<Playlist> playlists) {
        children.clear();
        children.addAll(playlists);
    }

    public int getChildCount() {
        return children.size();
    }

}
