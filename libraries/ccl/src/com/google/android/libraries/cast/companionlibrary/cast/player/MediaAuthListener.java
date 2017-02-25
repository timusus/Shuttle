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

import org.json.JSONObject;

/**
 * A public interface that provides callbacks for the {@link MediaAuthService} to communicate with
 * the framework
 */
public interface MediaAuthListener {

    /**
     * Called when MediaAuthService has successfully obtained a result.
     *
     * @param status Provides the status of result, will be one of
     * {@link MediaAuthStatus#AUTHORIZED} or
     * {@link MediaAuthStatus#NOT_AUTHORIZED}
     * @param info The fully populated {@link MediaInfo} that is obtained through authorization.
     * @param message If authorization was not granted, then an optional message can be provided
     * to be presented to the user. If no message is provided, it will be silently ignored.
     * Implementers have to make sure the message is localized.
     * @param startPoint The position in video to start the playback at (in milliseconds)
     * @param customData Optional {@link org.json.JSONObject}
     */
    void onAuthResult(MediaAuthStatus status, MediaInfo info, String message,
            int startPoint, JSONObject customData);

    /**
     * Called when MediaAuthService returns with a failure message due to some issues such as
     * network, backend issues, etc.
     *
     * @param failureMessage The message stating the reason for failure. This message should be
     * localized.
     */
    void onAuthFailure(String failureMessage);

}
