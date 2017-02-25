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
import com.google.android.gms.cast.Cast.CastOptions.Builder;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastStatusCodes;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.DataCastConsumer;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.DataCastConsumerImpl;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.CastException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.google.android.libraries.cast.companionlibrary.utils.LogUtils;

import android.content.Context;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.text.TextUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * A concrete subclass of {@link BaseCastManager} that is suitable for data-centric applications
 * that use multiple namespaces.
 * <p>
 * This is a singleton that needs to be "initialized" (by calling <code>initialize()</code>) prior
 * to usage. Subsequent to initialization, an easier way to get access to the singleton class is to
 * call a variant of <code>getInstance()</code>. Prior to initialization, build an instance of
 * {@link CastConfiguration} object with the features that you need and use that to initialize this
 * singleton.
 *
 * <p>Beyond managing the connectivity to a cast device, this class provides easy-to-use methods to
 * send and receive messages using one or more namespaces. These namespaces can be configured when
 * you are building the instance of {@link CastConfiguration} or can be added later on.
 * Clients can subclass this class to extend the features and functionality beyond what this class
 * provides. This class manages various states of the remote cast device. Client applications,
 * however, can complement the default behavior of this class by hooking into various callbacks that
 * it provides (see
 * {@link com.google.android.libraries.cast.companionlibrary.cast.callbacks.DataCastConsumer}).
 * Since the number of these callbacks is usually much larger than what a single application might
 * be interested in, there is a no-op implementation of this interface (see
 * {@link DataCastConsumerImpl}) that applications can subclass to override only those methods that
 * they are interested in. Since this library depends on the cast functionalities provided by the
 * Google Play services, the library checks to ensure that the right version of that service is
 * installed. It also provides a simple static method {@code checkGooglePlayServices()} that clients
 * can call at an early stage of their applications to provide a dialog for users if they need to
 * update/activate their Google Play Services library. To learn more about this library, please read
 * the documentation that is distributed as part of this library.
 */
public class DataCastManager extends BaseCastManager implements Cast.MessageReceivedCallback {

    private static final String TAG = LogUtils.makeLogTag(DataCastManager.class);
    private static DataCastManager sInstance;
    private final Set<String> mNamespaceList = new HashSet<>();
    private final Set<DataCastConsumer> mDataConsumers = new CopyOnWriteArraySet<>();

    private DataCastManager() {
    }

    public static synchronized DataCastManager initialize(Context context,
            CastConfiguration castConfiguration) {
        if (sInstance == null) {
            LOGD(TAG, "New instance of DataCastManager is created");
            if (ConnectionResult.SUCCESS != GooglePlayServicesUtil
                    .isGooglePlayServicesAvailable(context)) {
                String msg = "Couldn't find the appropriate version of Google Play Services";
                LOGE(TAG, msg);
                throw new RuntimeException(msg);
            }
            sInstance = new DataCastManager(context, castConfiguration);
        }
        return sInstance;
    }

    protected DataCastManager(Context context, CastConfiguration castConfiguration) {
        super(context, castConfiguration);
        List<String> namespaces = castConfiguration.getNamespaces();
        if (namespaces != null) {
            for (String namespace : namespaces) {
                if (!TextUtils.isEmpty(namespace)) {
                    mNamespaceList.add(namespace);
                } else {
                    LOGD(TAG, "A null or empty namespace was ignored.");
                }
            }
        }
    }

    /**
     * Returns a (singleton) instance of this class. Clients should call this method in order to
     * get a hold of this singleton instance. If it is not initialized yet, a
     * {@link CastException} will be thrown.
     */
    public static DataCastManager getInstance() {
        if (sInstance == null) {
            String msg = "No DataCastManager instance was found, did you forget to initialize it?";
            LOGE(TAG, msg);
            throw new IllegalStateException(msg);
        }
        return sInstance;
    }

    /**
     * Adds a channel with the given {@code namespace} and registers {@link DataCastManager} as
     * the callback receiver. If the namespace is already registered, this returns
     * <code>false</code>, otherwise returns <code>true</code>.
     *
     * @throws NoConnectionException If no connectivity to the device exists
     * @throws TransientNetworkDisconnectionException If framework is still trying to recover from a
     * possibly transient loss of network
     */
    public boolean addNamespace(String namespace) throws
            TransientNetworkDisconnectionException, NoConnectionException {
        checkConnectivity();
        if (TextUtils.isEmpty(namespace)) {
            throw new IllegalArgumentException("namespace cannot be empty");
        }
        if (mNamespaceList.contains(namespace)) {
            LOGD(TAG, "Ignoring to add a namespace that is already added.");
            return false;
        }
        try {
            Cast.CastApi.setMessageReceivedCallbacks(mApiClient, namespace, this);
            mNamespaceList.add(namespace);
            return true;
        } catch (IOException | IllegalStateException e) {
            LOGE(TAG, String.format("addNamespace(%s)", namespace), e);
        }
        return false;
    }

    /**
     * Unregisters a namespace. If namespace is not already registered, it returns
     * <code>false</code>, otherwise a successful removal returns <code>true</code>.
     *
     * @throws NoConnectionException If no connectivity to the device exists
     * @throws TransientNetworkDisconnectionException If framework is still trying to recover from a
     * possibly transient loss of network
     */
    public boolean removeNamespace(String namespace) throws TransientNetworkDisconnectionException,
            NoConnectionException {
        checkConnectivity();
        if (TextUtils.isEmpty(namespace)) {
            throw new IllegalArgumentException("namespace cannot be empty");
        }
        if (!mNamespaceList.contains(namespace)) {
            LOGD(TAG, "Ignoring to remove a namespace that is not registered.");
            return false;
        }
        try {
            Cast.CastApi.removeMessageReceivedCallbacks(mApiClient, namespace);
            mNamespaceList.remove(namespace);
            return true;
        } catch (IOException | IllegalStateException e) {
            LOGE(TAG, String.format("removeNamespace(%s)", namespace), e);
        }
        return false;

    }

    /**
     * Sends the <code>message</code> on the data channel for the <code>namespace</code>. If fails,
     * it will call <code>onMessageSendFailed</code>
     *
     * @throws IllegalArgumentException If the the message is null, empty, or too long; or if the
     * namespace is null or too long.
     * @throws IllegalStateException If there is no active service connection.
     * @throws IOException
     */
    public void sendDataMessage(String message, String namespace)
            throws IllegalArgumentException, IllegalStateException, IOException {
        checkConnectivity();
        if (TextUtils.isEmpty(namespace)) {
            throw new IllegalArgumentException("namespace cannot be empty");
        }
        Cast.CastApi.sendMessage(mApiClient, namespace, message).
                setResultCallback(new ResultCallback<Status>() {

                    @Override
                    public void onResult(Status result) {
                        if (!result.isSuccess()) {
                            DataCastManager.this.onMessageSendFailed(result);
                        }
                    }
                });
    }

    @Override
    protected void onDeviceUnselected() {
        detachDataChannels();
    }

    @Override
    protected Builder getCastOptionBuilder(CastDevice device) {

        Builder builder = Cast.CastOptions.builder(
                mSelectedCastDevice, new CastListener());
        if (isFeatureEnabled(CastConfiguration.FEATURE_DEBUGGING)) {
            builder.setVerboseLoggingEnabled(true);
        }
        return builder;
    }

    class CastListener extends Cast.Listener {

        /*
         * (non-Javadoc)
         * @see com.google.android.gms.cast.Cast.Listener#onApplicationDisconnected (int)
         */
        @Override
        public void onApplicationDisconnected(int statusCode) {
            DataCastManager.this.onApplicationDisconnected(statusCode);
        }

        /*
         * (non-Javadoc)
         * @see com.google.android.gms.cast.Cast.Listener#onApplicationStatusChanged ()
         */
        @Override
        public void onApplicationStatusChanged() {
            DataCastManager.this.onApplicationStatusChanged();
        }
    }

    @Override
    public void onApplicationConnected(ApplicationMetadata appMetadata, String applicationStatus,
            String sessionId, boolean wasLaunched) {
        LOGD(TAG, "onApplicationConnected() reached with sessionId: " + sessionId);

        // saving session for future retrieval; we only save the last session info
        mPreferenceAccessor.saveStringToPreference(PREFS_KEY_SESSION_ID, sessionId);
        if (mReconnectionStatus == RECONNECTION_STATUS_IN_PROGRESS) {
            // we have tried to reconnect and successfully launched the app, so
            // it is time to select the route and make the cast icon happy :-)
            List<RouteInfo> routes = mMediaRouter.getRoutes();
            if (routes != null) {
                String routeId = mPreferenceAccessor.getStringFromPreference(PREFS_KEY_ROUTE_ID);
                boolean found = false;
                for (RouteInfo routeInfo : routes) {
                    if (routeId.equals(routeInfo.getId())) {
                        // found the right route
                        LOGD(TAG, "Found the correct route during reconnection attempt");
                        found = true;
                        mReconnectionStatus = RECONNECTION_STATUS_FINALIZED;
                        mMediaRouter.selectRoute(routeInfo);
                        break;
                    }
                }
                if (!found) {
                    // we were hoping to have the route that we wanted, but we
                    // didn't so we deselect the device
                    onDeviceSelected(null /* CastDevice */, null /* RouteInfo */);
                    mReconnectionStatus = RECONNECTION_STATUS_INACTIVE;
                    return;
                }
            }
        }
        // registering namespaces, if any
        try {
            attachDataChannels();
            mSessionId = sessionId;
            for (DataCastConsumer consumer : mDataConsumers) {
                consumer.onApplicationConnected(appMetadata, applicationStatus, sessionId,
                        wasLaunched);
            }
        } catch (IllegalStateException | IOException e) {
            LOGE(TAG, "Failed to attach namespaces", e);
        }

    }

    /*
     * Adds namespaces for data channel(s)
     *
     * @throws NoConnectionException If no connectivity to the device exists
     * @throws TransientNetworkDisconnectionException If framework is still trying to recover from a
     * possibly transient loss of network
     * @throws IOException If an I/O error occurs while performing the request.
     * @throws IllegalStateException Thrown when the controller is not connected to a CastDevice.
     * @throws IllegalArgumentException If namespace is null.
     */
    private void attachDataChannels() throws IllegalStateException, IOException {
        checkConnectivity();
        for (String namespace : mNamespaceList) {
            Cast.CastApi.setMessageReceivedCallbacks(mApiClient, namespace, this);
        }
    }

    /*
     * Remove namespaces
     *
     * @throws NoConnectionException If no connectivity to the device exists
     * @throws TransientNetworkDisconnectionException If framework is still trying to recover from a
     * possibly transient loss of network
     */
    private void detachDataChannels() {
        if (mApiClient == null) {
            return;
        }
        for (String namespace : mNamespaceList) {
            try {
                Cast.CastApi.removeMessageReceivedCallbacks(mApiClient, namespace);
            } catch (IOException | IllegalArgumentException e) {
                LOGE(TAG, "detachDataChannels() Failed to remove namespace: " + namespace, e);
            }
        }
    }

    @Override
    public void onApplicationConnectionFailed(int errorCode) {
        if (mReconnectionStatus == RECONNECTION_STATUS_IN_PROGRESS) {
            if (errorCode == CastStatusCodes.APPLICATION_NOT_RUNNING) {
                // while trying to re-establish session, we found out that the app is not running
                // so we need to disconnect
                mReconnectionStatus = RECONNECTION_STATUS_INACTIVE;
                onDeviceSelected(null /* CastDevice */, null /* RouteInfo */);
            }
        } else {
            for (DataCastConsumer consumer : mDataConsumers) {
                consumer.onApplicationConnectionFailed(errorCode);
            }
            onDeviceSelected(null /* CastDevice */, null /* RouteInfo */);
            if (mMediaRouter != null) {
                LOGD(TAG, "onApplicationConnectionFailed(): Setting route to default");
                mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());
            }
        }
    }

    public void onApplicationDisconnected(int errorCode) {
        for (DataCastConsumer consumer : mDataConsumers) {
            consumer.onApplicationDisconnected(errorCode);
        }
        if (mMediaRouter != null) {
            mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());
        }
        onDeviceSelected(null /* CastDevice */, null /* RouteInfo */);

    }

    public void onApplicationStatusChanged() {
        String appStatus;
        if (!isConnected()) {
            return;
        }
        try {
            appStatus = Cast.CastApi.getApplicationStatus(mApiClient);
            LOGD(TAG, "onApplicationStatusChanged() reached: " + appStatus);
            for (DataCastConsumer consumer : mDataConsumers) {
                consumer.onApplicationStatusChanged(appStatus);
            }
        } catch (IllegalStateException e) {
            LOGE(TAG, "onApplicationStatusChanged(): Failed", e);
        }

    }

    @Override
    public void onApplicationStopFailed(int errorCode) {
        for (DataCastConsumer consumer : mDataConsumers) {
            consumer.onApplicationStopFailed(errorCode);
        }
    }

    @Override
    public void onConnectivityRecovered() {
        try {
            attachDataChannels();
        } catch (IOException | IllegalStateException e) {
            LOGE(TAG, "onConnectivityRecovered(): Failed to reattach data channels", e);
        }
        super.onConnectivityRecovered();
    }

    @Override
    public void onMessageReceived(CastDevice castDevice, String namespace, String message) {
        for (DataCastConsumer consumer : mDataConsumers) {
            consumer.onMessageReceived(castDevice, namespace, message);
        }
    }

    public void onMessageSendFailed(Status result) {
        for (DataCastConsumer consumer : mDataConsumers) {
            consumer.onMessageSendFailed(result);
        }
    }

    /**
     * Registers an
     * {@link com.google.android.libraries.cast.companionlibrary.cast.callbacks.DataCastConsumer}
     * interface with this class. Registered listeners will be notified of changes to a variety of
     * lifecycle and status changes through the callbacks that the interface provides.
     */
    public void addDataCastConsumer(DataCastConsumer listener) {
        if (listener != null) {
            addBaseCastConsumer(listener);
            boolean result;
            result = mDataConsumers.add(listener);
            if (result) {
                LOGD(TAG, "Successfully added the new DataCastConsumer listener " + listener);
            } else {
                LOGD(TAG, "Adding Listener " + listener + " was already registered, "
                        + "skipping this step");
            }
        }
    }

    /**
     * Unregisters an {@link com.google.android.libraries.cast.companionlibrary.cast.callbacks.DataCastConsumer}.
     */
    public void removeDataCastConsumer(DataCastConsumer listener) {
        if (listener != null) {
            removeBaseCastConsumer(listener);
            mDataConsumers.remove(listener);
        }
    }
}
