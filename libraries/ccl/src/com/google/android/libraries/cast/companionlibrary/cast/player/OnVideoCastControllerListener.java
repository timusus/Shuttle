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

import com.google.android.libraries.cast.companionlibrary.cast.exceptions.CastException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.tracks.OnTracksSelectedListener;

import android.view.View;
import android.widget.SeekBar;

/**
 * An interface that enables an alternative implementation of
 * {@link com.google.android.libraries.cast.companionlibrary.cast.player.VideoCastControllerFragment}. // NOLINT
 */
public interface OnVideoCastControllerListener extends OnTracksSelectedListener {

    /**
     * Called when seeking is stopped by user.
     */
    void onStopTrackingTouch(SeekBar seekBar);

    /**
     * Called when seeking starts by user
     */
    void onStartTrackingTouch(SeekBar seekBar);

    /**
     * Called while seeking is happening by the user
     */
    void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser);

    /**
     * Notification that user has clicked on the Play/Pause button
     *
     * @throws TransientNetworkDisconnectionException
     * @throws NoConnectionException
     * @throws CastException
     */
    void onPlayPauseClicked(View v) throws CastException,
            TransientNetworkDisconnectionException, NoConnectionException;

    /**
     * Called when a configuration change happens (for example device is rotated)
     */
    void onConfigurationChanged();

    /**
     * Called when user clicks on the Skip Next button
     *
     * @throws TransientNetworkDisconnectionException
     * @throws NoConnectionException
     */
    void onSkipNextClicked(View v) throws TransientNetworkDisconnectionException,
            NoConnectionException;

    /**
     * Called when user clicks on the Skip Previous button
     *
     * @throws TransientNetworkDisconnectionException
     * @throws NoConnectionException
     */
    void onSkipPreviousClicked(View v)
            throws TransientNetworkDisconnectionException, NoConnectionException;

}
