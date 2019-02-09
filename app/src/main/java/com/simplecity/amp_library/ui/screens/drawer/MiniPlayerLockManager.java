package com.simplecity.amp_library.ui.screens.drawer;

import java.util.ArrayList;
import java.util.List;

public class MiniPlayerLockManager {

    public interface MiniPlayerLock {
        String getTag();
    }

    private static MiniPlayerLockManager instance;

    private List<MiniPlayerLock> miniPlayerLocks = new ArrayList<>();

    private MiniPlayerLockManager() {

    }

    public boolean canShowMiniPlayer() {
        return miniPlayerLocks.isEmpty();
    }

    public static MiniPlayerLockManager getInstance() {
        if (instance == null) {
            instance = new MiniPlayerLockManager();
        }
        return instance;
    }

    public void addMiniPlayerLock(MiniPlayerLock miniPlayerLock) {
        if (!miniPlayerLocks.contains(miniPlayerLock)) {
            miniPlayerLocks.add(miniPlayerLock);
        }
    }

    public void removeMiniPlayerLock(MiniPlayerLock miniPlayerLock) {
        if (miniPlayerLocks.contains(miniPlayerLock)) {
            miniPlayerLocks.remove(miniPlayerLock);
        }
    }
}