package com.simplecity.amp_library.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.playback.constants.InternalIntents;
import com.simplecity.amp_library.ui.screens.main.MainActivity;

/**
 * @see <a href="https://code.google.com/p/dashclock/">DashClock</a>
 */
public class DashClockService extends DashClockExtension {

    /**
     * Used to display the data in on the DashClock widgetUsed to display the
     * data in on the DashClock widget
     */
    private final ExtensionData mExtensionData = new ExtensionData();

    /**
     * The {@link android.content.IntentFilter} used to monitor specific playback changes from
     * Shuttle's Service
     */
    private final IntentFilter mFilter = new IntentFilter();

    /**
     * Determines if the DashClock widget has been initialized
     */
    boolean mIsInitialized;

    /**
     * Determines if music is currently playing
     */
    boolean mIsPlaying;

    /**
     * The {@link Intent} invoked when the widget it touched
     */
    private Intent mIntent;

    @Override
    public void onCreate() {
        super.onCreate();
        mIntent = new Intent(this, MainActivity.class);
        mFilter.addAction(InternalIntents.PLAY_STATE_CHANGED);
        mFilter.addAction(InternalIntents.META_CHANGED);
        registerReceiver(mStatusListener, mFilter);
    }

    @Override
    protected void onInitialize(boolean isReconnect) {
        mIsInitialized = true;
        super.onInitialize(isReconnect);
    }

    @Override
    public void onDestroy() {
        mIsInitialized = false;
        unregisterReceiver(mStatusListener);
        super.onDestroy();
    }

    /**
     * The {@link BroadcastReceiver} used to retrieve the current track's
     * information
     */
    private final BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final Bundle extras = intent.getExtras();

            if (!mIsInitialized || extras == null) {
                return;
            }

            mIsPlaying = extras.getBoolean("playing", false);

            if (!mIsPlaying) {
                publishUpdate(null);
            } else {
                final String artist = extras.getString("artist");
                final String album = extras.getString("album");
                final String track = extras.getString("track");
                publishUpdate(artist, album, track);
            }
        }
    };

    /**
     * Notify DashClock of the changes
     */
    void publishUpdate(String artist, String album, String track) {
        if (artist == null || album == null || track == null) {
            return;
        }
        // Publish the extension data update
        publishUpdate(mExtensionData
                .visible(true)
                .icon(R.drawable.ic_headphones_white).status(track)
                .expandedTitle(track).expandedBody(artist + " - " + album)
                .clickIntent(mIntent));
    }

    @Override
    protected void onUpdateData(int reason) {
        // Nothing to do
    }
}
