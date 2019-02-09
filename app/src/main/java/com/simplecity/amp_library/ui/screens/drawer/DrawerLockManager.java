package com.simplecity.amp_library.ui.screens.drawer;

import android.support.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class DrawerLockManager {

    public interface DrawerLock {
        String getTag();
    }

    private static DrawerLockManager instance;

    private List<DrawerLock> drawerLocks = new ArrayList<>();

    @Nullable
    private DrawerLockController drawerLockController;

    private DrawerLockManager() {

    }

    public static DrawerLockManager getInstance() {
        if (instance == null) {
            instance = new DrawerLockManager();
        }
        return instance;
    }

    public void setDrawerLockController(@Nullable DrawerLockController drawerLockController) {
        this.drawerLockController = drawerLockController;
    }

    public void addDrawerLock(DrawerLock drawerLock) {
        if (!drawerLocks.contains(drawerLock)) {
            drawerLocks.add(drawerLock);
        }
        if (drawerLockController != null) {
            drawerLockController.lockDrawer();
        }
    }

    public void removeDrawerLock(DrawerLock drawerLock) {
        if (drawerLocks.contains(drawerLock)) {
            drawerLocks.remove(drawerLock);
        }
        if (drawerLocks.isEmpty()) {
            if (drawerLockController != null) {
                drawerLockController.unlockDrawer();
            }
        }
    }
}