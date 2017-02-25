
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

package com.google.android.libraries.cast.companionlibrary.cast.player;

import com.google.android.gms.cast.MediaInfo;

/**
 * A public interface if an application requires a pre-authorization of a media, prior to its
 * playback. Applications should implement this interface when they want to obtain a
 * pre-authorization prior to calling the {@link VideoCastControllerActivity}. The implementation
 * should prepare the stage for its own out-of-bound process but should not start that till the
 * {@link MediaAuthService#startAuthorization()} is called by the CCL library. Applications should
 * provide a timeout limit to make sure that this out-of-bound process is completed within a
 * reasonable period of time.
 * <p/>
 * Framework passes an {@link MediaAuthListener} to the implementation of this interface to provide
 * a way for the implementation to callback to the framework with its results. When the
 * authorization process ends, the implementation has to call
 * {@link com.google.android.libraries.cast.companionlibrary.cast.player.MediaAuthListener#onAuthResult(MediaAuthStatus, MediaInfo, String, int, org.json.JSONObject)} // NOLINT
 * with the relevant results; whether the authorization was granted or rejected. If, however, the
 * process encounters an unrecoverable error, it has to call the
 * {@link com.google.android.libraries.cast.companionlibrary.cast.player.MediaAuthListener#onAuthFailure(String)} // NOLINT
 * callback of the {@link MediaAuthListener} to inform the framework.
 * <p/>
 * If the library decides to to interrupt the authorization process (say, a user decides to
 * interrupt the process or if it times out), it will call
 * {@link #abortAuthorization(MediaAuthStatus)} and provides a reason. Implementation has to make
 * sure that it will not call any of the framework callbacks after it has received an abort message.
 * <p/>
 * Since authorization process can be involved and may require network access, the
 * {@link MediaAuthService#startAuthorization()} method is called on a non-UI thread. Callbacks into
 * the framework can happen on or off the UI thread.
 */
public interface MediaAuthService {

    /**
     * Starts the authorization process. Before this call, it is assumed that the implementor has
     * all the information required to perform the authorization task. This is where the dynamic
     * life cycle of this class starts.
     */
    void startAuthorization();

    /**
     * Registers an {@link MediaAuthListener} listener to be notified when the authentication
     * service has obtained its result. To remove a previously set listener, pass a
     * <code>null</code> argument.
     */
    void setMediaAuthListener(MediaAuthListener listener);

    /**
     * Returns the current {@link MediaInfo} object that is the subject of authorization. At a
     * minimum, it is expected to have images for the media at any stage.
     */
    MediaInfo getMediaInfo();

    /**
     * In pending state, implementors can provide an optional localized message to be shown to the
     * user. If <code>null</code> is returned, no message will be shown to the user.
     */
    String getPendingMessage();

    /**
     * Returns the current status of the service.
     */
    MediaAuthStatus getStatus();

    /**
     * Returns the length of time within which the library expects to have heard back from the
     * authorization service. If it doesn't, it will call
     * {@link #abortAuthorization(MediaAuthStatus)}.
     *
     * @return Timeout in milliseconds
     */
    long getTimeout();

    /**
     * If authorization times out or user cancels the authorization process, this method will be
     * called.
     *
     * @param abortReason One of the {@code MediaAuthStatus#ABORT_*} reasons
     */
    void abortAuthorization(MediaAuthStatus abortReason);

}
