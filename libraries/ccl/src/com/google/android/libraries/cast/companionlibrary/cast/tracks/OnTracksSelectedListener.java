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

package com.google.android.libraries.cast.companionlibrary.cast.tracks;

import com.google.android.gms.cast.MediaTrack;

import java.util.List;

/**
 * An interface to listen to changes to the active tracks for a media.
 */
public interface OnTracksSelectedListener {

    /**
     * Called to inform the listeners of the new set of active tracks.
     *
     * @param tracks A Non-<code>null</code> list of MediaTracks.
     */
    void onTracksSelected(List<MediaTrack> tracks);
}
