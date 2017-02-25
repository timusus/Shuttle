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

import android.support.v7.media.MediaRouter.RouteInfo;

/**
 * A no-op implementation of the {@link BaseCastConsumer}
 */
public class BaseCastConsumerImpl implements BaseCastConsumer {

    @Override
    public void onConnected() {
        // no-op
    }

    @Override
    public void onDisconnected() {
        // no-op
    }

    @Override
    public void onDisconnectionReason(@BaseCastManager.DisconnectReason int reason) {
        // no-op
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // no-op
    }

    @Override
    public void onCastDeviceDetected(RouteInfo info) {
        // no-op
    }

    @Override
    public void onCastAvailabilityChanged(boolean castPresent) {
        // no-op
    }

    @Override
    public void onRouteRemoved(RouteInfo info) {
        // no-op
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // no-op
    }

    @Override
    public void onConnectivityRecovered() {
        // no-op
    }

    @Override
    public void onUiVisibilityChanged(boolean visible) {
        // no-op
    }

    @Override
    public void onReconnectionStatusChanged(int status) {
        // no-op
    }

    @Override
    public void onDeviceSelected(CastDevice device, RouteInfo routeInfo) {
        // no-op
    }

    @Override
    public void onFailed(int resourceId, int statusCode) {
        // no-op
    }

}
