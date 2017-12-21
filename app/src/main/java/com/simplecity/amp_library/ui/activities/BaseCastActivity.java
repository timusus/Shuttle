package com.simplecity.amp_library.ui.activities;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.KeyEvent;

import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumer;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.simplecity.amp_library.ShuttleApplication;

public abstract class BaseCastActivity extends BaseActivity {

    private static final String TAG = "BaseCastActivity";

    @NonNull
    public VideoCastManager castManager = VideoCastManager.getInstance();

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

        castManager.reconnectSessionIfPossible();
    }

    @Override
    protected void onPause() {

        castManager.removeVideoCastConsumer(castConsumer);

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        castManager = VideoCastManager.getInstance();
        castManager.addVideoCastConsumer(castConsumer);
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        return castManager.onDispatchVolumeKeyEvent(event, ShuttleApplication.VOLUME_INCREMENT)
                || super.dispatchKeyEvent(event);
    }

}
