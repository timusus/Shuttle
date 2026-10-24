package com.simplecity.amp_library.ui.activities;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.KeyEvent;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumer;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.simplecity.amp_library.ShuttleApplication;

public abstract class BaseCastActivity extends BaseActivity {

    private static final String TAG = "BaseCastActivity";

    // Null when Cast couldn't initialise (no Play services); getInstance() throws in that case.
    @Nullable
    public VideoCastManager castManager = getCastManagerOrNull();

    @Nullable
    private static VideoCastManager getCastManagerOrNull() {
        try {
            return VideoCastManager.getInstance();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    @NonNull
    private VideoCastConsumer castConsumer = new VideoCastConsumerImpl() {
        @Override
        public void onConnectionSuspended(int cause) {
            Log.d(TAG, "onConnectionSuspended() was called with cause: " + cause);
            //Todo: Show toast
        }

        @Override
        public void onConnectivityRecovered() {
            //Todo: Show toast
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (castManager != null) {
            castManager.reconnectSessionIfPossible();
        }
    }

    @Override
    protected void onPause() {

        if (castManager != null) {
            castManager.removeVideoCastConsumer(castConsumer);
        }

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        castManager = getCastManagerOrNull();
        if (castManager != null) {
            castManager.addVideoCastConsumer(castConsumer);
        }
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        return (castManager != null && castManager.onDispatchVolumeKeyEvent(event, ShuttleApplication.VOLUME_INCREMENT))
                || super.dispatchKeyEvent(event);
    }
}
