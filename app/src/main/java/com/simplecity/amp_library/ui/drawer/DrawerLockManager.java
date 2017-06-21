package com.simplecity.amp_library.ui.drawer;

import android.support.annotation.Nullable;

public class DrawerLockManager {

    private static final String TAG = "DrawerLockManager";

    private static DrawerLockManager instance;

    private int drawerLocks = 0;

    private boolean isLocked = false;

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

    public boolean isLocked() {
        return isLocked;
    }

    public void addDrawerLock() {
        drawerLocks++;
        if (drawerLockController != null) {
            drawerLockController.lockDrawer();
        }
        isLocked = true;
    }

    public void removeDrawerLock() {
        drawerLocks = Math.max(0, drawerLocks - 1);
        if (drawerLocks == 0) {
            if (drawerLockController != null) {
                drawerLockController.unlockDrawer();
                isLocked = false;
            }
        }
    }
}