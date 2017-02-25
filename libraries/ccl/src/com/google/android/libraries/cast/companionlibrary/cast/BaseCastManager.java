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

package com.google.android.libraries.cast.companionlibrary.cast;

import static com.google.android.libraries.cast.companionlibrary.utils.LogUtils.LOGD;
import static com.google.android.libraries.cast.companionlibrary.utils.LogUtils.LOGE;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.Cast.ApplicationConnectionResult;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.CastStatusCodes;
import com.google.android.gms.cast.LaunchOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.libraries.cast.companionlibrary.R;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.BaseCastConsumer;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.CastException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.OnFailedListener;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.reconnection.ReconnectionService;
import com.google.android.libraries.cast.companionlibrary.utils.LogUtils;
import com.google.android.libraries.cast.companionlibrary.utils.PreferenceAccessor;
import com.google.android.libraries.cast.companionlibrary.utils.Utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.IntDef;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.app.MediaRouteButton;
import android.support.v7.app.MediaRouteDialogFactory;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.view.Menu;
import android.view.MenuItem;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * An abstract class that manages connectivity to a cast device. Subclasses are expected to extend
 * the functionality of this class based on their purpose.
 */
public abstract class BaseCastManager
        implements ConnectionCallbacks, OnConnectionFailedListener, OnFailedListener {

    private static final String TAG = LogUtils.makeLogTag(BaseCastManager.class);

    public static final int RECONNECTION_STATUS_STARTED = 1;
    public static final int RECONNECTION_STATUS_IN_PROGRESS = 2;
    public static final int RECONNECTION_STATUS_FINALIZED = 3;
    public static final int RECONNECTION_STATUS_INACTIVE = 4;

    public static final String PREFS_KEY_SESSION_ID = "session-id";
    public static final String PREFS_KEY_SSID = "ssid";
    public static final String PREFS_KEY_MEDIA_END = "media-end";
    public static final String PREFS_KEY_APPLICATION_ID = "application-id";
    public static final String PREFS_KEY_CAST_ACTIVITY_NAME = "cast-activity-name";
    public static final String PREFS_KEY_CAST_CUSTOM_DATA_NAMESPACE = "cast-custom-data-namespace";
    public static final String PREFS_KEY_ROUTE_ID = "route-id";

    public static final int CLEAR_ALL = 0;
    public static final int CLEAR_ROUTE = 1;
    public static final int CLEAR_WIFI = 1 << 1;
    public static final int CLEAR_SESSION = 1 << 2;
    public static final int CLEAR_MEDIA_END = 1 << 3;

    public static final int DISCONNECT_REASON_OTHER = 0;
    public static final int DISCONNECT_REASON_CONNECTIVITY = 1;
    public static final int DISCONNECT_REASON_APP_NOT_RUNNING = 2;
    public static final int DISCONNECT_REASON_EXPLICIT = 3;
    protected CastConfiguration mCastConfiguration;

    /**
     * Enumerates the reasons behind a disconnect
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({DISCONNECT_REASON_OTHER, DISCONNECT_REASON_CONNECTIVITY,
            DISCONNECT_REASON_APP_NOT_RUNNING, DISCONNECT_REASON_EXPLICIT})
    public @interface DisconnectReason {}

    public static final int NO_APPLICATION_ERROR = 0;

    public static final int NO_STATUS_CODE = -1;
    private static final int SESSION_RECOVERY_TIMEOUT_S = 10;
    private static final int WHAT_UI_VISIBLE = 0;
    private static final int WHAT_UI_HIDDEN = 1;
    private static final int UI_VISIBILITY_DELAY_MS = 300;

    private static String sCclVersion;

    protected Context mContext;
    protected MediaRouter mMediaRouter;
    protected MediaRouteSelector mMediaRouteSelector;
    protected CastMediaRouterCallback mMediaRouterCallback;
    protected CastDevice mSelectedCastDevice;
    protected String mDeviceName;
    protected PreferenceAccessor mPreferenceAccessor;

    private final Set<BaseCastConsumer> mBaseCastConsumers = new CopyOnWriteArraySet<>();
    private boolean mDestroyOnDisconnect = false;
    protected String mApplicationId;
    protected int mReconnectionStatus = RECONNECTION_STATUS_INACTIVE;
    protected int mVisibilityCounter;
    protected boolean mUiVisible;
    protected GoogleApiClient mApiClient;
    protected AsyncTask<Void, Integer, Boolean> mReconnectionTask;
    protected int mCapabilities;
    protected boolean mConnectionSuspended;
    protected String mSessionId;
    private Handler mUiVisibilityHandler;
    private RouteInfo mRouteInfo;
    protected int mApplicationErrorCode = NO_APPLICATION_ERROR;

    protected BaseCastManager() {
    }

    /**
     * Since application lifecycle callbacks are managed by subclasses, this abstract method needs
     * to be implemented by each subclass independently.
     *
     * @param device The Cast receiver device returned from the MediaRouteProvider. Should not be
     * {@code null}.
     */
    protected abstract Cast.CastOptions.Builder getCastOptionBuilder(CastDevice device);

    /**
     * Subclasses should implement this to react appropriately to the successful launch of their
     * application. This is called when the application is successfully launched.
     */
    protected abstract void onApplicationConnected(ApplicationMetadata applicationMetadata,
            String applicationStatus, String sessionId, boolean wasLaunched);

    /**
     * Called when the launch of application has failed. Subclasses need to handle this by doing
     * appropriate clean up.
     */
    protected abstract void onApplicationConnectionFailed(int statusCode);

    /**
     * Called when the attempt to stop application has failed.
     */
    protected abstract void onApplicationStopFailed(int statusCode);

    /**
     * Called when a Cast device is unselected (i.e. disconnected). Most of the logic is handled by
     * the {@link BaseCastManager} but each subclass may have some additional logic that can be
     * done, e.g. detaching data or media channels that they may have set up.
     */
    protected void onDeviceUnselected() {
        // no-op implementation
    }

    protected BaseCastManager(Context context, CastConfiguration castConfiguration) {
        mCastConfiguration = castConfiguration;
        mCapabilities = castConfiguration.getCapabilities();
        LogUtils.setDebug(isFeatureEnabled(CastConfiguration.FEATURE_DEBUGGING));

        sCclVersion = context.getString(R.string.ccl_version);
        mApplicationId = castConfiguration.getApplicationId();
        LOGD(TAG, "BaseCastManager is instantiated\nVersion: " + sCclVersion
                + "\nApplication ID: " + mApplicationId);
        mContext = context.getApplicationContext();
        mPreferenceAccessor = new PreferenceAccessor(mContext);
        mUiVisibilityHandler = new Handler(new UpdateUiVisibilityHandlerCallback());
        mPreferenceAccessor.saveStringToPreference(PREFS_KEY_APPLICATION_ID, mApplicationId);

        mMediaRouter = MediaRouter.getInstance(mContext);
        mMediaRouteSelector = new MediaRouteSelector.Builder().addControlCategory(
                CastMediaControlIntent.categoryForCast(mApplicationId)).build();

        mMediaRouterCallback = new CastMediaRouterCallback(this);
        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
    }

    /**
     * Returns the {@link MediaRouteDialogFactory} that defines the chooser and controller dialogs
     * for selecting a route and controlling the route when connected. The default factory will be
     * used unless a different one is configured in {@link CastConfiguration}.
     */
    private MediaRouteDialogFactory getMediaRouteDialogFactory() {
        return mCastConfiguration.getMediaRouteDialogFactory();
    }

    /**
     * Called when a {@link CastDevice} is extracted from the {@link RouteInfo}. This is where all
     * the fun starts!
     */
    public final void onDeviceSelected(CastDevice device, RouteInfo routeInfo) {
        for (BaseCastConsumer consumer : mBaseCastConsumers) {
            consumer.onDeviceSelected(device, routeInfo);
        }
        if (device == null) {
            disconnectDevice(mDestroyOnDisconnect, true, false);
        } else {
            setDevice(device);
        }
    }

    /**
     * This is called from
     * {@link com.google.android.libraries.cast.companionlibrary.cast.CastMediaRouterCallback} to
     * signal the change in presence of cast devices on network.
     *
     * @param castDevicePresent Indicates where a cast device is present, <code>true</code>, or not,
     * <code>false</code>.
     */
    public final void onCastAvailabilityChanged(boolean castDevicePresent) {
        for (BaseCastConsumer consumer : mBaseCastConsumers) {
            consumer.onCastAvailabilityChanged(castDevicePresent);
        }
    }

    /**
     * Disconnects from the connected device.
     *
     * @param stopAppOnExit If {@code true}, the application running on the cast device will be
     * stopped when disconnected.
     * @param clearPersistedConnectionData If {@code true}, the persisted connection information
     * will be cleared as part of this call.
     * @param setDefaultRoute If {@code true}, after disconnection, the selected route will be set
     * to the Default Route.
     */
    public final void disconnectDevice(boolean stopAppOnExit, boolean clearPersistedConnectionData,
            boolean setDefaultRoute) {
        LOGD(TAG, "disconnectDevice(" + clearPersistedConnectionData + "," + setDefaultRoute + ")");
        if (mSelectedCastDevice == null) {
            return;
        }
        mSelectedCastDevice = null;
        mDeviceName = null;

        String message = "disconnectDevice() Disconnect Reason: ";
        int reason;
        if (mConnectionSuspended) {
            message += "Connectivity lost";
            reason = DISCONNECT_REASON_CONNECTIVITY;
        } else {
            switch (mApplicationErrorCode) {
                case CastStatusCodes.APPLICATION_NOT_RUNNING:
                    message += "App was taken over or not available anymore";
                    reason = DISCONNECT_REASON_APP_NOT_RUNNING;
                    break;
                case NO_APPLICATION_ERROR:
                    message += "Intentional disconnect";
                    reason = DISCONNECT_REASON_EXPLICIT;
                    break;
                default:
                    message += "Other";
                    reason = DISCONNECT_REASON_OTHER;
            }
        }
        LOGD(TAG, message);
        for (BaseCastConsumer consumer : mBaseCastConsumers) {
            consumer.onDisconnectionReason(reason);
        }

        LOGD(TAG, "mConnectionSuspended: " + mConnectionSuspended);
        if (!mConnectionSuspended && clearPersistedConnectionData) {
            clearPersistedConnectionInfo(CLEAR_ALL);
            stopReconnectionService();
        }
        try {
            if ((isConnected() || isConnecting()) && stopAppOnExit) {
                LOGD(TAG, "Calling stopApplication");
                stopApplication();
            }
        } catch (NoConnectionException | TransientNetworkDisconnectionException e) {
            LOGE(TAG, "Failed to stop the application after disconnecting route", e);
        }
        onDeviceUnselected();
        if (mApiClient != null) {
            // the following check is currently required, without including a check for
            // isConnecting() due to a bug in the current play services library and will be removed
            // when that bug is addressed; calling disconnect() while we are in "connecting" state
            // will throw an exception
            if (mApiClient.isConnected()) {
                LOGD(TAG, "Trying to disconnect");
                mApiClient.disconnect();
            }
            if ((mMediaRouter != null) && setDefaultRoute) {
                LOGD(TAG, "disconnectDevice(): Setting route to default");
                mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());
            }
            mApiClient = null;
        }
        mSessionId = null;
        onDisconnected(stopAppOnExit, clearPersistedConnectionData, setDefaultRoute);
    }

    /**
     * Returns {@code true} if and only if the selected cast device is on the local network.
     *
     * @throws CastException if no cast device has been selected.
     */
    public final boolean isDeviceOnLocalNetwork() throws CastException {
        if (mSelectedCastDevice == null) {
            throw new CastException("No cast device has yet been selected");
        }
        return mSelectedCastDevice.isOnLocalNetwork();
    }

    private void setDevice(CastDevice device) {
        mSelectedCastDevice = device;
        mDeviceName = mSelectedCastDevice.getFriendlyName();

        if (mApiClient == null) {
            LOGD(TAG, "acquiring a connection to Google Play services for " + mSelectedCastDevice);
            Cast.CastOptions.Builder apiOptionsBuilder = getCastOptionBuilder(mSelectedCastDevice);
            mApiClient = new GoogleApiClient.Builder(mContext)
                    .addApi(Cast.API, apiOptionsBuilder.build())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            mApiClient.connect();
        } else if (!mApiClient.isConnected() && !mApiClient.isConnecting()) {
            mApiClient.connect();
        }
    }

    /**
     * Called as soon as a non-default {@link RouteInfo} is discovered. The main usage for this is
     * to provide a hint to clients that the cast button is going to become visible/available soon.
     * A client, for example, can use this to show a quick help screen to educate the user on the
     * cast concept and the usage of the cast button.
     */
    public final void onCastDeviceDetected(RouteInfo info) {
        for (BaseCastConsumer consumer : mBaseCastConsumers) {
            consumer.onCastDeviceDetected(info);
        }
    }

    /**
     * Called when a route is removed.
     */
    public final void onRouteRemoved(RouteInfo info) {
        for (BaseCastConsumer consumer : mBaseCastConsumers) {
            consumer.onRouteRemoved(info);
        }
    }

    /**
     * Adds and wires up the Media Router cast button. It returns a reference to the Media Router
     * menu item if the caller needs such reference. It is assumed that the enclosing
     * {@link android.app.Activity} inherits (directly or indirectly) from
     * {@link android.support.v7.app.AppCompatActivity}.
     *
     * @param menu Menu reference
     * @param menuResourceId The resource id of the cast button in the xml menu descriptor file
     */
    public final MenuItem addMediaRouterButton(Menu menu, int menuResourceId) {
        MenuItem mediaRouteMenuItem = menu.findItem(menuResourceId);
        MediaRouteActionProvider mediaRouteActionProvider = (MediaRouteActionProvider)
                MenuItemCompat.getActionProvider(mediaRouteMenuItem);
        mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);
        if (getMediaRouteDialogFactory() != null) {
            mediaRouteActionProvider.setDialogFactory(getMediaRouteDialogFactory());
        }
        return mediaRouteMenuItem;
    }

    /**
     * Adds and wires up the {@link android.support.v7.app.MediaRouteButton} instance that is passed
     * as an argument. This requires that
     * <ul>
     * <li>The enclosing {@link android.app.Activity} inherits (directly or indirectly) from
     * {@link android.support.v4.app.FragmentActivity}</li>
     * <li>User adds the {@link android.support.v7.app.MediaRouteButton} to the layout and passes a
     * reference to that instance to this method</li>
     * <li>User is in charge of controlling the visibility of this button. However, this library
     * makes it easier to do so: use the callback <code>onCastAvailabilityChanged(boolean)</code>
     * to change the visibility of the button in your client. For example, extend
     * {@link com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl}
     * and override that method:
     *
     * <pre>
       public void onCastAvailabilityChanged(boolean castPresent) {
           mMediaRouteButton.setVisibility(castPresent ? View.VISIBLE : View.INVISIBLE);
       }
     * </pre>
     * </li>
     * </ul>
     */
    public final void addMediaRouterButton(MediaRouteButton button) {
        button.setRouteSelector(mMediaRouteSelector);
        if (getMediaRouteDialogFactory() != null) {
            button.setDialogFactory(getMediaRouteDialogFactory());
        }
    }

    /**
     * Calling this method signals the library that an activity page is made visible. In common
     * cases, this should be called in the "onResume()" method of each activity of the application.
     * The library keeps a counter and when at least one page of the application becomes visible,
     * the {@link #onUiVisibilityChanged(boolean)} method is called.
     */
    public final synchronized void incrementUiCounter() {
        mVisibilityCounter++;
        if (!mUiVisible) {
            mUiVisible = true;
            mUiVisibilityHandler.removeMessages(WHAT_UI_HIDDEN);
            mUiVisibilityHandler.sendEmptyMessageDelayed(WHAT_UI_VISIBLE, UI_VISIBILITY_DELAY_MS);
        }
        if (mVisibilityCounter == 0) {
            LOGD(TAG, "UI is no longer visible");
        } else {
            LOGD(TAG, "UI is visible");
        }
    }

    /**
     * Calling this method signals the library that an activity page is made invisible. In common
     * cases, this should be called in the "onPause()" method of each activity of the application.
     * The library keeps a counter and when all pages of the application become invisible, the
     * {@link #onUiVisibilityChanged(boolean)} method is called.
     */
    public final synchronized void decrementUiCounter() {
        if (--mVisibilityCounter == 0) {
            LOGD(TAG, "UI is no longer visible");
            if (mUiVisible) {
                mUiVisible = false;
                mUiVisibilityHandler.removeMessages(WHAT_UI_VISIBLE);
                mUiVisibilityHandler.sendEmptyMessageDelayed(WHAT_UI_HIDDEN,
                        UI_VISIBILITY_DELAY_MS);
            }
        } else {
            LOGD(TAG, "UI is visible");
        }
    }

    /**
     * This is called when UI visibility of the client has changed
     *
     * @param visible The updated visibility status
     */
    protected void onUiVisibilityChanged(boolean visible) {
        if (visible) {
            if (mMediaRouter != null && mMediaRouterCallback != null) {
                LOGD(TAG, "onUiVisibilityChanged() addCallback called");
                startCastDiscovery();
                if (isFeatureEnabled(CastConfiguration.FEATURE_AUTO_RECONNECT)) {
                    reconnectSessionIfPossible();
                }
            }
        } else {
            if (mMediaRouter != null) {
                LOGD(TAG, "onUiVisibilityChanged() removeCallback called");
                stopCastDiscovery();
            }
        }
        for (BaseCastConsumer consumer : mBaseCastConsumers) {
            consumer.onUiVisibilityChanged(visible);
        }
    }

    /**
     * Starts the discovery of cast devices by registering a {@link android.support.v7.media
     * .MediaRouter.Callback}
     */
    public final void startCastDiscovery() {
        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
    }

    /**
     * Stops the process of cast discovery by removing the registered
     * {@link android.support.v7.media.MediaRouter.Callback}
     */
    public final void stopCastDiscovery() {
        mMediaRouter.removeCallback(mMediaRouterCallback);
    }

    /**
     * A utility method to validate that the appropriate version of the Google Play Services is
     * available on the device. If not, it will open a dialog to address the issue. The dialog
     * displays a localized message about the error and upon user confirmation (by tapping on
     * dialog) will direct them to the Play Store if Google Play services is out of date or missing,
     * or to system settings if Google Play services is disabled on the device.
     */
    public static boolean checkGooglePlayServices(final Activity activity) {
        return Utils.checkGooglePlayServices(activity);
    }

    /**
     * can be used to find out if the application is connected to the service or not.
     *
     * @return <code>true</code> if connected, <code>false</code> otherwise.
     */
    public final boolean isConnected() {
        return (mApiClient != null) && mApiClient.isConnected();
    }

    /**
     * Returns <code>true</code> only if application is connecting to the Cast service.
     */
    public final boolean isConnecting() {
        return (mApiClient != null) && mApiClient.isConnecting();
    }

    /**
     * Disconnects from the cast device.
     */
    public final void disconnect() {
        if (isConnected() || isConnecting()) {
            disconnectDevice(mDestroyOnDisconnect, true, true);
        }
    }

    /**
     * Returns the assigned human-readable name of the device, or <code>null</code> if no device is
     * connected.
     */
    public final String getDeviceName() {
        return mDeviceName;
    }

    /**
     * Sets a flag to control whether disconnection form a cast device should result in stopping
     * the running application or not. If <code>true</code> is passed, then application will be
     * stopped. Default behavior is not to stop the app.
     */
    public final void setStopOnDisconnect(boolean stopOnExit) {
        mDestroyOnDisconnect = stopOnExit;
    }

    /**
     * Returns the {@link MediaRouteSelector} object.
     */
    public final MediaRouteSelector getMediaRouteSelector() {
        return mMediaRouteSelector;
    }

    /**
     * Returns the {@link android.support.v7.media.MediaRouter.RouteInfo} corresponding to the
     * selected route.
     */
    public final RouteInfo getRouteInfo() {
        return mRouteInfo;
    }

    /**
     * Sets the {@link android.support.v7.media.MediaRouter.RouteInfo} corresponding to the
     * selected route.
     */
    public final void setRouteInfo(RouteInfo routeInfo) {
        mRouteInfo = routeInfo;
    }

    /*
     * Returns true if and only if the feature is turned on
     */
    public final boolean isFeatureEnabled(int feature) {
        return (feature & mCapabilities) == feature;
    }

    /**
     * Sets the device (system) volume.
     *
     * @param volume Should be a value between 0 and 1, inclusive.
     * @throws CastException
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public final void setDeviceVolume(double volume) throws CastException,
            TransientNetworkDisconnectionException, NoConnectionException {
        checkConnectivity();
        try {
            Cast.CastApi.setVolume(mApiClient, volume);
        } catch (IOException e) {
            throw new CastException("Failed to set volume", e);
        } catch (IllegalStateException e) {
            throw new NoConnectionException("setDeviceVolume()", e);
        }
    }

    /**
     * Gets the remote's system volume, a number between 0 and 1, inclusive.
     *
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public final double getDeviceVolume() throws TransientNetworkDisconnectionException,
            NoConnectionException {
        checkConnectivity();
        try {
            return Cast.CastApi.getVolume(mApiClient);
        } catch (IllegalStateException e) {
            throw new NoConnectionException("getDeviceVolume()", e);
        }
    }

    /**
     * Increments (or decrements) the device volume by the given amount.
     *
     * @throws CastException
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public final void adjustDeviceVolume(double delta) throws CastException,
            TransientNetworkDisconnectionException, NoConnectionException {
        checkConnectivity();
        double vol = getDeviceVolume();
        if (vol >= 0) {
            setDeviceVolume(vol + delta);
        }
    }

    /**
     * Returns <code>true</code> if remote device is muted. It internally determines if this should
     * be done for <code>stream</code> or <code>device</code> volume.
     *
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public final boolean isDeviceMute() throws TransientNetworkDisconnectionException,
            NoConnectionException {
        checkConnectivity();
        try {
            return Cast.CastApi.isMute(mApiClient);
        } catch (IllegalStateException e) {
            throw new NoConnectionException("isDeviceMute()", e);
        }
    }

    /**
     * Mutes or un-mutes the device volume.
     *
     * @throws CastException
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public final void setDeviceMute(boolean mute) throws CastException,
            TransientNetworkDisconnectionException, NoConnectionException {
        checkConnectivity();
        try {
            Cast.CastApi.setMute(mApiClient, mute);
        } catch (IOException e) {
            throw new CastException("setDeviceMute", e);
        } catch (IllegalStateException e) {
            throw new NoConnectionException("setDeviceMute()", e);
        }
    }

    /**
     * Returns the current reconnection status
     */
    public final int getReconnectionStatus() {
        return mReconnectionStatus;
    }

    /**
     * Sets the reconnection status
     */
    public final void setReconnectionStatus(int status) {
        if (mReconnectionStatus != status) {
            mReconnectionStatus = status;
            onReconnectionStatusChanged(mReconnectionStatus);
        }
    }

    private void onReconnectionStatusChanged(int status) {
        for (BaseCastConsumer consumer : mBaseCastConsumers) {
            consumer.onReconnectionStatusChanged(status);
        }
    }

    /**
     * Returns <code>true</code> if there is enough persisted information to attempt a session
     * recovery. For this to return <code>true</code>, there needs to be a persisted session ID and
     * a route ID from the last successful launch.
     */
    protected final boolean canConsiderSessionRecovery() {
        return canConsiderSessionRecovery(null);
    }

    /**
     * Returns <code>true</code> if there is enough persisted information to attempt a session
     * recovery. For this to return <code>true</code>, there needs to be persisted session ID and
     * route ID from the last successful launch. In addition, if <code>ssidName</code> is non-null,
     * then an additional check is also performed to make sure the persisted wifi name is the same
     * as the <code>ssidName</code>
     */
    public final boolean canConsiderSessionRecovery(String ssidName) {
        String sessionId = mPreferenceAccessor.getStringFromPreference(PREFS_KEY_SESSION_ID);
        String routeId = mPreferenceAccessor.getStringFromPreference(PREFS_KEY_ROUTE_ID);
        String ssid = mPreferenceAccessor.getStringFromPreference(PREFS_KEY_SSID);
        if (sessionId == null || routeId == null) {
            return false;
        }
        if (ssidName != null && (ssid == null || (!ssid.equals(ssidName)))) {
            return false;
        }
        LOGD(TAG, "Found session info in the preferences, so proceed with an "
                + "attempt to reconnect if possible");
        return true;
    }

    private void reconnectSessionIfPossibleInternal(RouteInfo theRoute) {
        if (isConnected()) {
            return;
        }
        String sessionId = mPreferenceAccessor.getStringFromPreference(PREFS_KEY_SESSION_ID);
        String routeId = mPreferenceAccessor.getStringFromPreference(PREFS_KEY_ROUTE_ID);
        LOGD(TAG, "reconnectSessionIfPossible() Retrieved from preferences: " + "sessionId="
                + sessionId + ", routeId=" + routeId);
        if (sessionId == null || routeId == null) {
            return;
        }
        setReconnectionStatus(RECONNECTION_STATUS_IN_PROGRESS);
        CastDevice device = CastDevice.getFromBundle(theRoute.getExtras());

        if (device != null) {
            LOGD(TAG, "trying to acquire Cast Client for " + device);
            onDeviceSelected(device, theRoute);
        }
    }

    /*
     * Cancels the task responsible for recovery of prior sessions, is used internally.
     */
    public final void cancelReconnectionTask() {
        LOGD(TAG, "cancelling reconnection task");
        if (mReconnectionTask != null && !mReconnectionTask.isCancelled()) {
            mReconnectionTask.cancel(true);
        }
    }

    /**
     * This method tries to automatically re-establish re-establish connection to a session if
     * <ul>
     * <li>User had not done a manual disconnect in the last session
     * <li>Device that user had connected to previously is still running the same session
     * </ul>
     * Under these conditions, a best-effort attempt will be made to continue with the same
     * session. This attempt will go on for {@code SESSION_RECOVERY_TIMEOUT} seconds.
     */
    public final void reconnectSessionIfPossible() {
        reconnectSessionIfPossible(SESSION_RECOVERY_TIMEOUT_S);
    }

    /**
     * This method tries to automatically re-establish connection to a session if
     * <ul>
     * <li>User had not done a manual disconnect in the last session
     * <li>The Cast Device that user had connected to previously is still running the same session
     * </ul>
     * Under these conditions, a best-effort attempt will be made to continue with the same
     * session. This attempt will go on for <code>timeoutInSeconds</code> seconds.
     */
    public final void reconnectSessionIfPossible(final int timeoutInSeconds) {
        reconnectSessionIfPossible(timeoutInSeconds, null);
    }

    /**
     * This method tries to automatically re-establish connection to a session if
     * <ul>
     * <li>User had not done a manual disconnect in the last session
     * <li>The Cast Device that user had connected to previously is still running the same session
     * </ul>
     * Under these conditions, a best-effort attempt will be made to continue with the same
     * session.
     * This attempt will go on for <code>timeoutInSeconds</code> seconds.
     *
     * @param timeoutInSeconds the length of time, in seconds, to attempt reconnection before giving
     * up
     * @param ssidName The name of Wifi SSID
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void reconnectSessionIfPossible(final int timeoutInSeconds, String ssidName) {
        LOGD(TAG, String.format("reconnectSessionIfPossible(%d, %s)", timeoutInSeconds, ssidName));
        if (isConnected()) {
            return;
        }
        String routeId = mPreferenceAccessor.getStringFromPreference(PREFS_KEY_ROUTE_ID);
        if (canConsiderSessionRecovery(ssidName)) {
            List<RouteInfo> routes = mMediaRouter.getRoutes();
            RouteInfo theRoute = null;
            if (routes != null) {
                for (RouteInfo route : routes) {
                    if (route.getId().equals(routeId)) {
                        theRoute = route;
                        break;
                    }
                }
            }
            if (theRoute != null) {
                // route has already been discovered, so lets just get the device
                reconnectSessionIfPossibleInternal(theRoute);
            } else {
                // we set a flag so if the route is discovered within a short period, we let
                // onRouteAdded callback of CastMediaRouterCallback take care of that
                setReconnectionStatus(RECONNECTION_STATUS_STARTED);
            }

            // cancel any prior reconnection task
            if (mReconnectionTask != null && !mReconnectionTask.isCancelled()) {
                mReconnectionTask.cancel(true);
            }

            // we may need to reconnect to an existing session
            mReconnectionTask = new AsyncTask<Void, Integer, Boolean>() {

                @Override
                protected Boolean doInBackground(Void... params) {
                    for (int i = 0; i < timeoutInSeconds; i++) {
                        LOGD(TAG, "Reconnection: Attempt " + (i + 1));
                        if (isCancelled()) {
                            return true;
                        }
                        try {
                            if (isConnected()) {
                                cancel(true);
                            }
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            // ignore
                        }
                    }
                    return false;
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    if (result == null || !result) {
                        LOGD(TAG, "Couldn't reconnect, dropping connection");
                        setReconnectionStatus(RECONNECTION_STATUS_INACTIVE);
                        onDeviceSelected(null /* CastDevice */, null /* RouteInfo */);
                    }
                }

            };
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                mReconnectionTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                mReconnectionTask.execute();
            }
        }
    }

    /**
     * This is called by the library when a connection is re-established after a transient
     * disconnect. Note: this is not called by SDK.
     */
    public void onConnectivityRecovered() {
        for (BaseCastConsumer consumer : mBaseCastConsumers) {
            consumer.onConnectivityRecovered();
        }
    }

    /*
     * (non-Javadoc)
     * @see com.google.android.gms.GoogleApiClient.ConnectionCallbacks#onConnected
     * (android.os.Bundle)
     */
    @Override
    public final void onConnected(Bundle hint) {
        LOGD(TAG, "onConnected() reached with prior suspension: " + mConnectionSuspended);
        if (mConnectionSuspended) {
            mConnectionSuspended = false;
            if (hint != null && hint.getBoolean(Cast.EXTRA_APP_NO_LONGER_RUNNING)) {
                // the same app is not running any more
                LOGD(TAG, "onConnected(): App no longer running, so disconnecting");
                disconnect();
            } else {
                onConnectivityRecovered();
            }
            return;
        }
        if (!isConnected()) {
            if (mReconnectionStatus == RECONNECTION_STATUS_IN_PROGRESS) {
                setReconnectionStatus(RECONNECTION_STATUS_INACTIVE);
            }
            return;
        }
        try {
            if (isFeatureEnabled(CastConfiguration.FEATURE_WIFI_RECONNECT)) {
                String ssid = Utils.getWifiSsid(mContext);
                mPreferenceAccessor.saveStringToPreference(PREFS_KEY_SSID, ssid);
            }
            Cast.CastApi.requestStatus(mApiClient);
            if (!mCastConfiguration.isDisableLaunchOnConnect()) {
                launchApp();
            }

            for (BaseCastConsumer consumer : mBaseCastConsumers) {
                consumer.onConnected();
            }

        } catch (IOException | IllegalStateException e) {
            LOGE(TAG, "requestStatus()", e);
        }

    }

    /*
     * Note: this is not called by the SDK anymore but this library calls this in the appropriate
     * time.
     */
    protected void onDisconnected(boolean stopAppOnExit, boolean clearPersistedConnectionData,
            boolean setDefaultRoute) {
        LOGD(TAG, "onDisconnected() reached");
        mDeviceName = null;
        for (BaseCastConsumer consumer : mBaseCastConsumers) {
            consumer.onDisconnected();
        }
    }

    /*
     * (non-Javadoc)
     * @see com.google.android.gms.GoogleApiClient.OnConnectionFailedListener#
     * onConnectionFailed(com.google.android.gms.common.ConnectionResult)
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        LOGD(TAG, "onConnectionFailed() reached, error code: " + result.getErrorCode()
                + ", reason: " + result.toString());
        disconnectDevice(mDestroyOnDisconnect, false /* clearPersistentConnectionData */,
                false /* setDefaultRoute */);
        mConnectionSuspended = false;
        if (mMediaRouter != null) {
            mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());
        }

        for (BaseCastConsumer consumer : mBaseCastConsumers) {
            consumer.onConnectionFailed(result);
        }

        PendingIntent pendingIntent = result.getResolution();
        if (pendingIntent != null) {
            try {
                pendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                LOGE(TAG, "Failed to show recovery from the recoverable error", e);
            }
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        mConnectionSuspended = true;
        LOGD(TAG, "onConnectionSuspended() was called with cause: " + cause);
        for (BaseCastConsumer consumer : mBaseCastConsumers) {
            consumer.onConnectionSuspended(cause);
        }
    }

    /**
     * Launches application with the given {@code applicationId} and {@link LaunchOptions}.
     *
     * @throws TransientNetworkDisconnectionException
     * @throws NoConnectionException
     */
    public final void launchApp(String applicationId, LaunchOptions launchOptions)
            throws TransientNetworkDisconnectionException, NoConnectionException {
        LOGD(TAG, "launchApp(applicationId, launchOptions) is called");
        if (!isConnected()) {
            if (mReconnectionStatus == RECONNECTION_STATUS_IN_PROGRESS) {
                setReconnectionStatus(RECONNECTION_STATUS_INACTIVE);
                return;
            }
            checkConnectivity();
        }

        if (mReconnectionStatus == RECONNECTION_STATUS_IN_PROGRESS) {
            LOGD(TAG, "Attempting to join a previously interrupted session...");
            String sessionId = mPreferenceAccessor.getStringFromPreference(PREFS_KEY_SESSION_ID);
            LOGD(TAG, "joinApplication() -> start");
            Cast.CastApi.joinApplication(mApiClient, applicationId, sessionId).setResultCallback(
                    new ResultCallback<Cast.ApplicationConnectionResult>() {

                        @Override
                        public void onResult(ApplicationConnectionResult result) {
                            if (result.getStatus().isSuccess()) {
                                LOGD(TAG, "joinApplication() -> success");
                                onApplicationConnected(result.getApplicationMetadata(),
                                        result.getApplicationStatus(), result.getSessionId(),
                                        result.getWasLaunched());
                            } else {
                                LOGD(TAG, "joinApplication() -> failure");
                                clearPersistedConnectionInfo(CLEAR_SESSION | CLEAR_MEDIA_END);
                                cancelReconnectionTask();
                                onApplicationConnectionFailed(result.getStatus().getStatusCode());
                            }
                        }
                    }
            );
        } else {
            LOGD(TAG, "Launching app");
            Cast.CastApi.launchApplication(mApiClient, applicationId, launchOptions)
                    .setResultCallback(
                            new ResultCallback<Cast.ApplicationConnectionResult>() {

                                @Override
                                public void onResult(ApplicationConnectionResult result) {
                                    if (result.getStatus().isSuccess()) {
                                        LOGD(TAG, "launchApplication() -> success result");
                                        onApplicationConnected(result.getApplicationMetadata(),
                                                result.getApplicationStatus(),
                                                result.getSessionId(),
                                                result.getWasLaunched());
                                    } else {
                                        LOGD(TAG, "launchApplication() -> failure result");
                                        onApplicationConnectionFailed(
                                                result.getStatus().getStatusCode());
                                    }
                                }
                            }
                    );
        }
    }

    /*
     * Launches application. For this to succeed, a connection should be already established by the
     * CastClient.
     */
    private void launchApp() throws TransientNetworkDisconnectionException, NoConnectionException {
        LOGD(TAG, "launchApp() is called");
        launchApp(mCastConfiguration.getApplicationId(), mCastConfiguration.getLaunchOptions());
    }

    /**
     * Stops the application on the receiver device.
     *
     * @throws NoConnectionException
     * @throws TransientNetworkDisconnectionException
     */
    public final void stopApplication()
            throws TransientNetworkDisconnectionException, NoConnectionException {
        checkConnectivity();
        Cast.CastApi.stopApplication(mApiClient, mSessionId).setResultCallback(
                new ResultCallback<Status>() {

                    @Override
                    public void onResult(Status result) {
                        if (!result.isSuccess()) {
                            LOGD(TAG, "stopApplication -> onResult: stopping "
                                    + "application failed");
                            onApplicationStopFailed(result.getStatusCode());
                        } else {
                            LOGD(TAG, "stopApplication -> onResult Stopped application "
                                    + "successfully");
                        }
                    }
                });
    }

    /**
     * Registers a {@link BaseCastConsumer} interface with this class. Registered listeners will be
     * notified of changes to a variety of lifecycle callbacks that the interface provides.
     *
     * @see {@code BaseCastConsumerImpl}
     */
    public final void addBaseCastConsumer(BaseCastConsumer listener) {
        if (listener != null) {
            if (mBaseCastConsumers.add(listener)) {
                LOGD(TAG, "Successfully added the new BaseCastConsumer listener " + listener);
            }
        }
    }

    /**
     * Unregisters a {@link BaseCastConsumer}.
     */
    public final void removeBaseCastConsumer(BaseCastConsumer listener) {
        if (listener != null) {
            if (mBaseCastConsumers.remove(listener)) {
                LOGD(TAG, "Successfully removed the existing BaseCastConsumer listener "
                        + listener);
            }
        }
    }

    /**
     * A simple method that throws an exception if there is no connectivity to the cast device.
     *
     * @throws TransientNetworkDisconnectionException If framework is still trying to recover
     * @throws NoConnectionException If no connectivity to the device exists
     */
    public final void checkConnectivity() throws TransientNetworkDisconnectionException,
            NoConnectionException {
        if (!isConnected()) {
            if (mConnectionSuspended) {
                throw new TransientNetworkDisconnectionException();
            } else {
                throw new NoConnectionException();
            }
        }
    }

    @Override
    public void onFailed(int resourceId, int statusCode) {
        LOGD(TAG, "onFailed() was called with statusCode: " + statusCode);
        for (BaseCastConsumer consumer : mBaseCastConsumers) {
            consumer.onFailed(resourceId, statusCode);
        }
    }

    /**
     * Returns the version of this library.
     */
    public static String getCclVersion() {
        return sCclVersion;
    }

    public PreferenceAccessor getPreferenceAccessor() {
        return mPreferenceAccessor;
    }

    /**
     * Clears the persisted connection information. Bitwise OR combination of the following options
     * should be passed as the argument:
     * <ul>
     *     <li>CLEAR_SESSION</li>
     *     <li>CLEAR_ROUTE</li>
     *     <li>CLEAR_WIFI</li>
     *     <li>CLEAR_MEDIA_END</li>
     *     <li>CLEAR_ALL</li>
     * </ul>
     * Clients can form an or
     */
    public final void clearPersistedConnectionInfo(int what) {
        LOGD(TAG, "clearPersistedConnectionInfo(): Clearing persisted data for " + what);
        if (isFlagSet(what, CLEAR_SESSION)) {
            mPreferenceAccessor.saveStringToPreference(PREFS_KEY_SESSION_ID, null);
        }
        if (isFlagSet(what, CLEAR_ROUTE)) {
            mPreferenceAccessor.saveStringToPreference(PREFS_KEY_ROUTE_ID, null);
        }
        if (isFlagSet(what, CLEAR_WIFI)) {
            mPreferenceAccessor.saveStringToPreference(PREFS_KEY_SSID, null);
        }
        if (isFlagSet(what, CLEAR_MEDIA_END)) {
            mPreferenceAccessor.saveLongToPreference(PREFS_KEY_MEDIA_END, null);
        }
    }

    private static boolean isFlagSet(int mask, int flag) {
        return (mask == CLEAR_ALL) || ((mask & flag) == flag);
    }

    protected void startReconnectionService(long mediaDurationLeft) {
        if (!isFeatureEnabled(CastConfiguration.FEATURE_WIFI_RECONNECT)) {
            return;
        }
        LOGD(TAG, "startReconnectionService() for media length lef = " + mediaDurationLeft);
        long endTime = SystemClock.elapsedRealtime() + mediaDurationLeft;
        mPreferenceAccessor.saveLongToPreference(PREFS_KEY_MEDIA_END, endTime);
        Context applicationContext = mContext.getApplicationContext();
        Intent service = new Intent(applicationContext, ReconnectionService.class);
        service.setPackage(applicationContext.getPackageName());
        applicationContext.startService(service);
    }

    protected void stopReconnectionService() {
        if (!isFeatureEnabled(CastConfiguration.FEATURE_WIFI_RECONNECT)) {
            return;
        }
        LOGD(TAG, "stopReconnectionService()");
        Context applicationContext = mContext.getApplicationContext();
        Intent service = new Intent(applicationContext, ReconnectionService.class);
        service.setPackage(applicationContext.getPackageName());
        applicationContext.stopService(service);
    }

    /**
     * A Handler.Callback to receive individual messages when UI goes hidden or becomes visible.
     */
    private class UpdateUiVisibilityHandlerCallback implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {
            onUiVisibilityChanged(msg.what == WHAT_UI_VISIBLE);
            return true;
        }
    }

    /**
     * Returns {@code true} if and only if there is at least one route matching the
     * {@link #getMediaRouteSelector()}.
     */
    public boolean isAnyRouteAvailable() {
        return mMediaRouterCallback.isRouteAvailable();
    }

    public CastConfiguration getCastConfiguration() {
        return mCastConfiguration;
    }
}
