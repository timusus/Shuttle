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

package com.google.android.libraries.cast.companionlibrary.cast.dialog.video;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.MediaRouteControllerDialogFragment;

/**
 * An extension of MediaRouteControllerDialogFragment which contains a
 * VideoMediaRouteControllerDialog.
 */
public class VideoMediaRouteControllerDialogFragment extends MediaRouteControllerDialogFragment {

    @Override
    public VideoMediaRouteControllerDialog onCreateControllerDialog(
            Context context, Bundle savedInstanceState) {
        VideoMediaRouteControllerDialog customControllerDialog
                = new VideoMediaRouteControllerDialog(context);
        customControllerDialog.setVolumeControlEnabled(false);
        return customControllerDialog;
    }
}
