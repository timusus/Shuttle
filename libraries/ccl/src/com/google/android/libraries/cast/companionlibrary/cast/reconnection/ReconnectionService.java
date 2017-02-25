/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.libraries.cast.companionlibrary.cast.reconnection;

import static com.google.android.libraries.cast.companionlibrary.utils.LogUtils.LOGD;
import static com.google.android.libraries.cast.companionlibrary.utils.LogUtils.LOGE;

import com.google.android.libraries.cast.companionlibrary.cast.BaseCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.CastConfiguration;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.google.android.libraries.cast.companionlibrary.utils.LogUtils;
import com.google.android.libraries.cast.companionlibrary.utils.Utils;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.SystemClock;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A service to run in the background when the playback of a media starts, to help with reconnection
 * if needed. Due to various reasons, connectivity to the cast device can be lost; for example wifi
 * radio may turn off when device goes to sleep or user may step outside of the wifi range, etc.
 * This service helps with recovering the connectivity when circumstances are right, for example
 * when user steps back within the wifi range, etc. In order to avoid ending up with a background
 * service that lingers around longer than it is needed, this implementation uses certain heuristics
 * to stop itself when needed.
 */
public class ReconnectionService extends Service {

    private static final String TAG = LogUtils.makeLogTag(ReconnectionService.class);
    // the tolerance for considering a time value (in millis) to be zero
    private static final long EPSILON_MS = 500;
    private static final int RECONNECTION_ATTEMPT_PERIOD_S = 15;
    private BroadcastReceiver mScreenOnOffBroadcastReceiver;
    private VideoCastManager mCastManager;
    private BroadcastReceiver mWifiBroadcastReceiver;
    private boolean mWifiConnectivity = true;
    private ScheduledFuture<?> mTerminationHandler;
    private final ScheduledExecutorService mScheduler = Executors.newScheduledThreadPool(1);

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LOGD(TAG, "onStartCommand() is called");
        setUpEndTimer();
        return Service.START_STICKY;
    }

    @Override
    public void onCreate() {
        LOGD(TAG, "onCreate() is called");
        mCastManager = VideoCastManager.getInstance();
        if (!mCastManager.isConnected() && !mCastManager.isConnecting()) {
            mCastManager.reconnectSessionIfPossible();
        }

        // register a broadcast receiver to be notified when screen goes on or off
        IntentFilter screenOnOffIntentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        screenOnOffIntentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        mScreenOnOffBroadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                LOGD(TAG, "ScreenOnOffBroadcastReceiver: onReceive(): " + intent.getAction());
                long timeLeft = getMediaRemainingTime();
                if (timeLeft < EPSILON_MS) {
                    handleTermination();
                }
            }
        };
        registerReceiver(mScreenOnOffBroadcastReceiver, screenOnOffIntentFilter);

        // register a wifi receiver that would be notified when the network state changes
        IntentFilter networkIntentFilter = new IntentFilter();
        networkIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mWifiBroadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                    NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    boolean connected = info.isConnected();
                    String networkSsid = connected ? Utils.getWifiSsid(context) : null;
                    ReconnectionService.this.onWifiConnectivityChanged(connected, networkSsid);
                }
            }
        };
        registerReceiver(mWifiBroadcastReceiver, networkIntentFilter);

        super.onCreate();
    }

    /**
     * Since framework calls this method twice when a change happens, we are guarding against that
     * by caching the state the first time and avoiding the second call if it is the same status.
     */
    public void onWifiConnectivityChanged(boolean connected, final String networkSsid) {
        LOGD(TAG, "WIFI connectivity changed to " + (connected ? "enabled" : "disabled"));
        if (connected && !mWifiConnectivity) {
            mWifiConnectivity = true;
            if (mCastManager.isFeatureEnabled(CastConfiguration.FEATURE_WIFI_RECONNECT)) {
                mCastManager.startCastDiscovery();
                mCastManager.reconnectSessionIfPossible(RECONNECTION_ATTEMPT_PERIOD_S, networkSsid);
            }

        } else {
            mWifiConnectivity = connected;
        }
    }


    @Override
    public void onDestroy() {
        LOGD(TAG, "onDestroy()");
        if (mScreenOnOffBroadcastReceiver != null) {
            unregisterReceiver(mScreenOnOffBroadcastReceiver);
            mScreenOnOffBroadcastReceiver = null;
        }

        if (mWifiBroadcastReceiver != null) {
            unregisterReceiver(mWifiBroadcastReceiver);
            mWifiBroadcastReceiver = null;
        }

        clearEndTimer();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    final private Runnable mTerminationRunnable = new Runnable() {

        @Override
        public void run() {
            LOGD(TAG, "setUpEndTimer(): stopping ReconnectionService since reached the end of"
                        + " allotted time");
            handleTermination();
        }
    };


    private void setUpEndTimer() {
        LOGD(TAG, "setUpEndTimer(): setting up a timer for the end of current media");
        long timeLeft = getMediaRemainingTime();
        if (timeLeft <= 0) {
            stopSelf();
            return;
        }
        clearEndTimer();
        mTerminationHandler = mScheduler
                .schedule(mTerminationRunnable, timeLeft, TimeUnit.MILLISECONDS);
    }

    private void clearEndTimer() {
        if (mTerminationHandler != null && !mTerminationHandler.isCancelled()) {
            mTerminationHandler.cancel(true);
        }
    }

    private long getMediaRemainingTime() {
        long endTime = mCastManager.getPreferenceAccessor().getLongFromPreference(
                BaseCastManager.PREFS_KEY_MEDIA_END, 0);
        return endTime - SystemClock.elapsedRealtime();
    }

    private void handleTermination() {
        if (!mCastManager.isConnected()) {
            mCastManager.clearMediaSession();
            mCastManager.clearPersistedConnectionInfo(BaseCastManager.CLEAR_ALL);
            stopSelf();
        } else {
            // since we are connected and our timer has gone off, lets update the time remaining
            // on the media (since media may have been paused) and reset teh time left
            long timeLeft = 0;
            try {
                timeLeft = mCastManager.isRemoteStreamLive() ? 0
                        : mCastManager.getMediaTimeRemaining();

            } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                LOGE(TAG, "Failed to calculate the time left for media due to lack of connectivity",
                        e);
            }
            if (timeLeft < EPSILON_MS) {
                // no time left
                stopSelf();
            } else {
                // lets reset the counter
                mCastManager.getPreferenceAccessor().saveLongToPreference(
                        BaseCastManager.PREFS_KEY_MEDIA_END,
                        timeLeft + SystemClock.elapsedRealtime());
                LOGD(TAG, "handleTermination(): resetting the timer");
                setUpEndTimer();
            }

        }
    }
}

