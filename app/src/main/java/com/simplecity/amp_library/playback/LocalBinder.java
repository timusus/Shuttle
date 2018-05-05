package com.simplecity.amp_library.playback;

import android.os.Binder;
import java.lang.ref.WeakReference;

/**
 * Class used for the client Binder.  Because we know this service always
 * runs in the same process as its clients, we don't need to deal with IPC.
 */
public class LocalBinder extends Binder {

    private WeakReference<MusicService> weakReference;

    public LocalBinder(MusicService musicService) {
        weakReference = new WeakReference<>(musicService);
    }

    public MusicService getService() {
        // Return this instance of MusicService so clients can call public methods
        return weakReference.get();
    }
}
