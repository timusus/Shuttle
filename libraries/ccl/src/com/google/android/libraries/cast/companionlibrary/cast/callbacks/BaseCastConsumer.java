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

package com.google.android.libraries.cast.companionlibrary.cast.callbacks;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.libraries.cast.companionlibrary.cast.BaseCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.OnFailedListener;

import android.support.v7.media.MediaRouter.RouteInfo;

/**
 * An interface for receiving callbacks around the connectivity status to a Cast device.
 */
public interface BaseCastConsumer extends OnFailedListener {

    /**
     * Called when connection is established
     */
    void onConnected();

    /**
     * Called when the client is temporarily in a disconnected state. This can happen if there is a
     * problem with the remote service (e.g. a crash or resource problem causes it to be killed by
     * the system). When called, all requests have been canceled and no outstanding listeners will
     * be executed. Applications could disable UI components that require the service, and wait for
     * a call to onConnectivityRecovered() to re-enable them.
     *
     * @param cause The reason for the disconnection. Defined by constants CAUSE_*.
     */
    void onConnectionSuspended(int cause);

    /**
     * Called when a device is disconnected
     */
    void onDisconnected();

    /**
     * Called when a device is disconnected or fails to reconnect and provides a reason for the
     * disconnect or failure.
     *
     * @param reason The failure/disconnect reason; can be one of the following:
     * <ul>
     *     <li>{@link BaseCastManager#DISCONNECT_REASON_APP_NOT_RUNNING}</li>
     *     <li>{@link BaseCastManager#DISCONNECT_REASON_EXPLICIT}</li>
     *     <li>{@link BaseCastManager#DISCONNECT_REASON_CONNECTIVITY}</li>
     *     <li>{@link BaseCastManager#DISCONNECT_REASON_OTHER}</li>
     * </ul>
     */
    void onDisconnectionReason(@BaseCastManager.DisconnectReason int reason);

    /**
     * Called when an error happens while connecting to a device.
     */
    void onConnectionFailed(ConnectionResult result);

    /**
     * Called when the MediaRouterCallback detects a non-default route.
     */
    void onCastDeviceDetected(RouteInfo info);

    /**
     * Called when the number of cast devices present on the network changes from 0 to a positive
     * number or vice versa. Can be used, for example, to control the visibility of {@link
     * android.support.v7.app.MediaRouteButton}
     *
     * @param castPresent set to {@code true} if at least one device becomes available,
     * {@code false} otherwise
     */
    void onCastAvailabilityChanged(boolean castPresent);

    /**
     * Called when a route is removed.
     */
    void onRouteRemoved(RouteInfo info);

    /**
     * Called after reconnection is established following a temporary disconnection, say, due to
     * network issues.
     */
    void onConnectivityRecovered();

    /**
     * Called when visibility of the application has changed.
     */
    void onUiVisibilityChanged(boolean visible);

    /**
     * Called when the status of reconnection changes.
     */
    void onReconnectionStatusChanged(int status);

    /**
     * Called when a device is selected/unselected.
     */
    void onDeviceSelected(CastDevice device, RouteInfo routeInfo);
}
