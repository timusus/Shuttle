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

import static com.google.android.libraries.cast.companionlibrary.utils.LogUtils.LOGE;

import com.google.android.libraries.cast.companionlibrary.R;
import com.google.android.libraries.cast.companionlibrary.cast.CastConfiguration;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.utils.LogUtils;
import com.google.android.libraries.cast.companionlibrary.utils.Utils;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.provider.Settings;

/**
 * An Activity to show the Captions Preferences for Android versions prior to KitKat
 */
public class CaptionsPreferenceActivity extends PreferenceActivity {

    private static final String TAG = LogUtils.makeLogTag(CaptionsPreferenceActivity.class);
    // For getPreferenceScreen() and addPreferenceFromResource(); due to the API levels supported
    // in this library, we cannot move to fragment-based preferences.
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VideoCastManager castManager = VideoCastManager.getInstance();
        if (!castManager.isFeatureEnabled(CastConfiguration.FEATURE_CAPTIONS_PREFERENCE)) {
            LOGE(TAG, "Did you forget to enable FEATURE_CAPTIONS_PREFERENCE when you initialized"
                    + " the VideoCastManage?");
            finish();
            return;
        }
        if (Utils.IS_KITKAT_OR_ABOVE) {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            finish();
            return;
        }
        addPreferencesFromResource(R.xml.caption_preference);
        castManager.getTracksPreferenceManager().setUpPreferences(getPreferenceScreen());
    }
}
