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

package com.google.android.libraries.cast.companionlibrary.widgets;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.libraries.cast.companionlibrary.widgets.MiniController.OnMiniControllerChangedListener;

import android.graphics.Bitmap;
import android.net.Uri;

/**
 * An interface to abstract {@link MiniController} so that other components can also control the
 * MiniControllers. Clients should code against this interface when they want to control the
 * provided {@link MiniController} or other custom implementations.
 */
public interface IMiniController {

    /**
     * Sets the uri for the album art
     */
    void setIcon(Uri uri);

    /**
     * Sets the bitmap for the album art
     */
    void setIcon(Bitmap bitmap);

    /**
     * Sets the title
     */
    void setTitle(String title);

    /**
     * Sets the subtitle
     */
    void setSubtitle(String subtitle);

    /**
     * Sets the playback state, and the idleReason (this is only reliable when the state is idle).
     * Values that can be passed to this method are from {@link MediaStatus}
     */
    void setPlaybackStatus(int state, int idleReason);

    /**
     * Sets whether this component should be visible or hidden.
     */
    void setVisibility(int visibility);

    /**
     * Returns the visibility state of this widget
     */
    boolean isVisible();

    /**
     * Assigns a {@link OnMiniControllerChangedListener} listener to be notified of the changes in
     * the mini controller
     */
    void setOnMiniControllerChangedListener(OnMiniControllerChangedListener listener);

    /**
     * Sets the type of stream. {@code streamType} can be {@link MediaInfo#STREAM_TYPE_LIVE}
     * or {@link MediaInfo#STREAM_TYPE_BUFFERED}
     */
    void setStreamType(int streamType);

    /**
     * Sets the progress of stream.
     */
    void setProgress(int progress, int duration);

    /**
     * Sets the visibility of the progress indicator
     */
    void setProgressVisibility(boolean visible);

    /**
     * Sets whether the "upcoming" sub-component should be visible or not
     */
    void setUpcomingVisibility(boolean visible);

    /**
     * Sets the upcoming item, which can be {@code null}.
     */
    void setUpcomingItem(MediaQueueItem item);

    /**
     * Controls the visibility of the currently playing item.
     */
    void setCurrentVisibility(boolean visible);


}
