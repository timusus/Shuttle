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
 * An interface that extends {@link BaseCastConsumer} and adds callbacks for application lifecycle
 * and success or failure of message exchange with a cast device.
 */
public interface DataCastConsumer extends BaseCastConsumer {
    /**
     * Called when the application is successfully launched or joined. Upon successful connection, a
     * session ID is returned. <code>wasLaunched</code> indicates if the application was launched or
     * joined.
     */
    void onApplicationConnected(ApplicationMetadata appMetadata,
            String applicationStatus, String sessionId, boolean wasLaunched);

    /**
     * Called when the current application has stopped
     */
    void onApplicationDisconnected(int errorCode);

    /**
     * Called when an attempt to stop a receiver application has failed.
     */
    void onApplicationStopFailed(int errorCode);

    /**
     * Called when an application launch has failed. Failure reason is captured in the
     * <code>errorCode</code> argument. Here is a list of possible values:
     * <ul>
     * <li>4 : Application not found
     * <li>5 : Application not currently running
     * <li>6 : Application already running
     * </ul>
     */
    void onApplicationConnectionFailed(int errorCode);

    /**
     * Called when application status changes. The argument is built by the receiver
     */
    void onApplicationStatusChanged(String appStatus);

    /**
     * Called when the device's volume is changed. Note not to mix that with the stream's volume
     */
    void onVolumeChanged(double value, boolean isMute);

    /**
     * Called when a message is received from a given {@link CastDevice} for a given
     * <code>namespace</code>.
     */
    void onMessageReceived(CastDevice castDevice, String namespace, String message);

    /**
     * Called when there is an error sending a message.
     *
     * @param status The status of the result
     */
    void onMessageSendFailed(Status status);

    /**
     * Called when this callback is removed from the Cast object.
     *
     * @param castDevice The castDevice from where the message originated.
     * @param namespace The associated namespace of the removed listener.
     */
    void onRemoved(CastDevice castDevice, String namespace);
}
