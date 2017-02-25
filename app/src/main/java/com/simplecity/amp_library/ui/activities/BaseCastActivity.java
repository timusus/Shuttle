package com.simplecity.amp_library.ui.activities;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.KeyEvent;

import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumer;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.utils.ShuttleUtils;

public abstract class BaseCastActivity extends BaseActivity {

    private static final String TAG = "BaseCastActivity";

    public VideoCastManager mCastManager;
    private VideoCastConsumer mCastConsumer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ShuttleUtils.isUpgraded()) {
            mCastManager = VideoCastManager.getInstance();
        }

        mCastConsumer = new VideoCastConsumerImpl() {
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

        if (mCastManager != null) {
            mCastManager.reconnectSessionIfPossible();
        }
    }

    @Override
    protected void onPause() {

        if (mCastManager != null) {
            mCastManager.removeVideoCastConsumer(mCastConsumer);
        }

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (ShuttleUtils.isUpgraded()) {
            mCastManager = VideoCastManager.getInstance();
            mCastManager.addVideoCastConsumer(mCastConsumer);
        }
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if (mCastManager != null) {
            return mCastManager.onDispatchVolumeKeyEvent(event, ShuttleApplication.VOLUME_INCREMENT)
                    || super.dispatchKeyEvent(event);
        }
        return super.dispatchKeyEvent(event);
    }

}
