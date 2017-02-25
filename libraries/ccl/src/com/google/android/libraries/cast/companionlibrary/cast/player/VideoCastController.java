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

import com.google.android.gms.cast.MediaStatus;
import com.google.android.libraries.cast.companionlibrary.cast.CastConfiguration;

import android.graphics.Bitmap;

/**
 * An interface that can be used to display a remote controller for the video that is playing on
 * the cast device.
 */
public interface VideoCastController {

    int CC_ENABLED = 1;
    int CC_DISABLED = 2;
    int CC_HIDDEN = 3;

    /**
     * Sets the bitmap for the album art
     */
    void setImage(Bitmap bitmap);

    /**
     * Sets the title
     */
    void setTitle(String text);

    /**
     * Sets the subtitle
     */
    void setSubTitle(String text);

    /**
     * Sets the playback state, and the idleReason (this is only used when the state is idle).
     * Values that can be passed to this method are from {@link MediaStatus}
     */
    void setPlaybackStatus(int state);

    /**
     * Assigns a {@link OnVideoCastControllerListener} listener to be notified of the changes in
     * the {@link VideoCastController}
     */
    void setOnVideoCastControllerChangedListener(OnVideoCastControllerListener listener);

    /**
     * Sets the type of stream. {@code streamType} can be
     * {@link com.google.android.gms.cast.MediaInfo#STREAM_TYPE_LIVE} or
     * {@link com.google.android.gms.cast.MediaInfo#STREAM_TYPE_BUFFERED}
     */
    void setStreamType(int streamType);

    /**
     * Updates the position and total duration for the seek bar that presents the progress of media.
     * Both of these need to be provided in milliseconds.
     */
    void updateSeekbar(int position, int duration);

    /**
     * Adjust the visibility of control widgets on the UI.
     */
    void updateControllersStatus(boolean enabled);

    /**
     * Can be used to show a loading icon during processes that could take time.
     */
    void showLoading(boolean visible);

    /**
     * Closes the activity related to the UI.
     */
    void closeActivity();

    /**
     * This can be used to adjust the UI for playback of live versus pre-recorded streams. Certain
     * UI widgets may need to be updated when playing a live stream. For example, the progress bar
     * may not be needed for a live stream while it may be required for a pre-recorded stream.
     */
    void adjustControllersForLiveStream(boolean isLive);

    /**
     * Updates the visual status of the Closed Caption icon. Possible states are provided by
     * {@code CC_ENABLED, CC_DISABLED, CC_HIDDEN}
     */
    void setClosedCaptionState(int status);

    /**
     * Called when the queue items are updated and provides information about the updated size of
     * the queue and the position of the current item in the queue. This can be useful to update
     * the UI if the relative position of the current item is relevant (e.g. to disable or hide
     * "skip next/prev" buttons).
     */
    void onQueueItemsUpdated(int queueLength, int position);

    /**
     * Sets the policy for the visibility/status of the Skip Next/Prev buttons. The policy declares
     * what should the visibility or status of these buttons be when the position of the current
     * item is at the edges of the queue. For example, if the current item is the last item in the
     * queue, what should be the visibility or status of the "Skip Next" button. Available policies
     * are:
     * <ul>
     *   <li>{@link CastConfiguration#NEXT_PREV_VISIBILITY_POLICY_ALWAYS}: always show the button
     *   <li>{@link CastConfiguration#NEXT_PREV_VISIBILITY_POLICY_DISABLED}: disable the button
     *   <li>{@link CastConfiguration#NEXT_PREV_VISIBILITY_POLICY_HIDDEN}: hide the button
     * </ul>
     * The default behavior is {@link CastConfiguration#NEXT_PREV_VISIBILITY_POLICY_DISABLED}
     */
    void setNextPreviousVisibilityPolicy(@CastConfiguration.PrevNextPolicy int policy);
}
