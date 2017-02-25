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

import com.google.android.gms.cast.CastDevice;
import com.google.android.libraries.cast.companionlibrary.utils.LogUtils;

import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;

/**
 * Provides a handy implementation of {@link MediaRouter.Callback}. When a {@link RouteInfo} is
 * selected by user from the list of available routes, this class will call the
 * {@link BaseCastManager#setDevice(CastDevice))} of the listener that was passed to it in
 * the constructor. In addition, as soon as a non-default route is discovered, the
 * {@link BaseCastManager#onCastDeviceDetected(RouteInfo))} is called.
 * <p>
 * There is also some logic in this class to help with the process of previous session recovery.
 */
public class CastMediaRouterCallback extends MediaRouter.Callback {
    private static final String TAG = LogUtils.makeLogTag(CastMediaRouterCallback.class);
    private final BaseCastManager mCastManager;
    private boolean mRouteAvailable = false;

    public CastMediaRouterCallback(BaseCastManager castManager) {
        mCastManager = castManager;
    }

    @Override
    public void onRouteSelected(MediaRouter router, RouteInfo info) {
        LOGD(TAG, "onRouteSelected: info=" + info);
        if (mCastManager.getReconnectionStatus()
                == BaseCastManager.RECONNECTION_STATUS_FINALIZED) {
            mCastManager.setReconnectionStatus(BaseCastManager.RECONNECTION_STATUS_INACTIVE);
            mCastManager.cancelReconnectionTask();
            return;
        }
        mCastManager.getPreferenceAccessor().saveStringToPreference(
                BaseCastManager.PREFS_KEY_ROUTE_ID, info.getId());
        CastDevice device = CastDevice.getFromBundle(info.getExtras());
        mCastManager.onDeviceSelected(device, info);
        LOGD(TAG, "onRouteSelected: mSelectedDevice=" + (device != null ? device.getFriendlyName()
                : "Null"));
    }

    @Override
    public void onRouteUnselected(MediaRouter router, RouteInfo routeInfo) {
        LOGD(TAG, "onRouteUnselected: route=" + routeInfo);
        mCastManager.onDeviceSelected(null, routeInfo);
    }

    @Override
    public void onRouteAdded(MediaRouter router, RouteInfo routeInfo) {
        if (!router.getDefaultRoute().equals(routeInfo)) {
            notifyRouteAvailabilityChangedIfNeeded(router);
            mCastManager.onCastDeviceDetected(routeInfo);
        }
        if (mCastManager.getReconnectionStatus()
                == BaseCastManager.RECONNECTION_STATUS_STARTED) {
            String routeId = mCastManager.getPreferenceAccessor().getStringFromPreference(
                    BaseCastManager.PREFS_KEY_ROUTE_ID);
            if (routeInfo.getId().equals(routeId)) {
                // we found the route, so lets go with that
                LOGD(TAG, "onRouteAdded: Attempting to recover a session with info=" + routeInfo);
                mCastManager.setReconnectionStatus(BaseCastManager.RECONNECTION_STATUS_IN_PROGRESS);

                CastDevice device = CastDevice.getFromBundle(routeInfo.getExtras());
                LOGD(TAG, "onRouteAdded: Attempting to recover a session with device: "
                        + (device != null ? device.getFriendlyName() : "Null"));
                mCastManager.onDeviceSelected(device, routeInfo);
            }
        }
    }

    @Override
    public void onRouteRemoved(MediaRouter router, RouteInfo route) {
        notifyRouteAvailabilityChangedIfNeeded(router);
        mCastManager.onRouteRemoved(route);
    }

    @Override
    public void onRouteChanged(MediaRouter router, RouteInfo route) {
        notifyRouteAvailabilityChangedIfNeeded(router);
    }

    private void notifyRouteAvailabilityChangedIfNeeded(MediaRouter router) {
        boolean routeAvailable = isRouteAvailable(router);
        if (routeAvailable != mRouteAvailable) {
            // availability of routes have changed
            mRouteAvailable = routeAvailable;
            mCastManager.onCastAvailabilityChanged(mRouteAvailable);
        }
    }

    private boolean isRouteAvailable(MediaRouter router) {
        return router.isRouteAvailable(mCastManager.getMediaRouteSelector(),
            MediaRouter.AVAILABILITY_FLAG_IGNORE_DEFAULT_ROUTE
                | MediaRouter.AVAILABILITY_FLAG_REQUIRE_MATCH);
    }

    /**
     * Returns {@code true} if and only if there is at least one route matching the
     * {@link BaseCastManager#getMediaRouteSelector()}.
     */
    public boolean isRouteAvailable() {
        return mRouteAvailable;
    }
}
