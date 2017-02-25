/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.util.Log;
import android.view.ViewGroup;

public class MyMediaRouteActionProvider extends MediaRouteActionProvider {
    private static final String TAG = "MediaRteActProvider";

    private MediaRouteSelector mSelector = MediaRouteSelector.EMPTY;
    private MediaRouteButtonHoloDark mButton;

    public MyMediaRouteActionProvider(Context context) {
        super(context);
    }

    /**
     * Called when the media route button is being created.
     */
    @SuppressWarnings("deprecation")
    @Override
    public MediaRouteButtonHoloDark onCreateMediaRouteButton() {
        if (mButton != null) {
            Log.e(TAG, "onCreateMediaRouteButton: This ActionProvider is already associated "
                    + "with a menu item. Don't reuse MediaRouteActionProvider instances!  "
                    + "Abandoning the old button...");
        }

        mButton = new MediaRouteButtonHoloDark(getContext());
        // mButton.setCheatSheetEnabled(true);
        mButton.setRouteSelector(mSelector);
        mButton.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.FILL_PARENT));
        return mButton;
    }

}