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

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.common.api.Status;

/**
 * A no-op implementation of the {@link DataCastConsumer}
 */
public class DataCastConsumerImpl extends BaseCastConsumerImpl implements DataCastConsumer {

    @Override
    public void onApplicationConnected(ApplicationMetadata appMetadata, String applicationStatus,
            String sessionId, boolean wasLaunched) {
    }

    @Override
    public void onApplicationDisconnected(int errorCode) {
    }

    @Override
    public void onApplicationStopFailed(int errorCode) {
    }

    @Override
    public void onApplicationConnectionFailed(int errorCode) {
    }

    @Override
    public void onApplicationStatusChanged(String appStatus) {
    }

    @Override
    public void onVolumeChanged(double value, boolean isMute) {
    }

    @Override
    public void onMessageReceived(CastDevice castDevice, String namespace, String message) {
    }

    @Override
    public void onMessageSendFailed(Status status) {
    }

    @Override
    public void onRemoved(CastDevice castDevice, String namespace) {
    }

}
