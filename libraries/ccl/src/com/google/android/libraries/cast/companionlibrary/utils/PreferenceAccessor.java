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

package com.google.android.libraries.cast.companionlibrary.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * A class to streamline access to the Preference storage for both reading and writing.
 */
public class PreferenceAccessor {
    private final SharedPreferences mSharedPreference;

    public PreferenceAccessor(Context context) {
        mSharedPreference = PreferenceManager.getDefaultSharedPreferences(context);
    }


    /**
     * Saves a string value under the provided key in the preference manager. If <code>value</code>
     * is <code>null</code>, then the provided key will be removed from the preferences.
     */
    public void saveStringToPreference(String key, String value) {
        if (value == null) {
            // we want to remove
            mSharedPreference.edit().remove(key).apply();
        } else {
            mSharedPreference.edit().putString(key, value).apply();
        }
    }

    /**
     * Saves a float value under the provided key in the preference manager. If {@code value}
     * is {@code null}, then the provided key will be removed from the preferences.
     */
    public void saveFloatToPreference(String key, Float value) {
        if (value == null) {
            // we want to remove
            mSharedPreference.edit().remove(key).apply();
        } else {
            mSharedPreference.edit().putFloat(key, value).apply();
        }
    }

    /**
     * Saves an integer value under the provided key in the preference manager. If {@code value}
     * is {@code null}, then the provided key will be removed from the preferences.
     */
    public void saveIntToPreference(String key, Integer value) {
        if (value == null) {
            // we want to remove
            mSharedPreference.edit().remove(key).apply();
        } else {
            mSharedPreference.edit().putInt(key, value).apply();
        }
    }

    /**
     * Saves a long value under the provided key in the preference manager. If {@code value}
     * is {@code null}, then the provided key will be removed from the preferences.
     */
    public void saveLongToPreference(String key, Long value) {
        if (value == null) {
            // we want to remove
            mSharedPreference.edit().remove(key).apply();
        } else {
            mSharedPreference.edit().putLong(key, value).apply();
        }

    }

    /**
     * Saves a boolean value under the provided key in the preference manager. If <code>value</code>
     * is <code>null</code>, then the provided key will be removed from the preferences.
     */
    public void saveBooleanToPreference(String key, Boolean value) {
        if (value == null) {
            // we want to remove
            mSharedPreference.edit().remove(key).apply();
        } else {
            mSharedPreference.edit().putBoolean(key, value).apply();
        }
    }

    /**
     * Retrieves a String value from preference manager. If no such key exists, it will return
     * <code>null</code>.
     */
    public String getStringFromPreference(String key) {
        return getStringFromPreference(key, null);
    }

    /**
     * Retrieves a String value from preference manager. If no such key exists, it will return the
     * <code>defaultValue</code>.
     */
    public String getStringFromPreference(String key, String defaultValue) {
        return mSharedPreference.getString(key, defaultValue);
    }

    /**
     * Retrieves a float value from preference manager. If no such key exists, it will return
     * <code>Float.MIN_VALUE</code>.
     */
    public float getFloatFromPreference(String key) {
        return mSharedPreference.getFloat(key, Float.MIN_VALUE);
    }

    /**
     * Retrieves an integer value from preference manager. If no such key exists, it will return
     * <code>Integer.MIN_VALUE</code>.
     */
    public int getIntFromPreference(String key) {
        return mSharedPreference.getInt(key, Integer.MIN_VALUE);
    }

    /**
     * Retrieves an integer value from preference manager. If no such key exists, it will return
     * value provided by the {@code defaultValue}.
     */
    public int getIntFromPreference(String key, int defaultValue) {
        return mSharedPreference.getInt(key, defaultValue);
    }

    /**
     * Retrieves a long value from preference manager. If no such key exists, it will return the
     * value provided as <code>defaultValue</code>
     */
    public long getLongFromPreference(String key, long defaultValue) {
        return mSharedPreference.getLong(key, defaultValue);
    }

    /**
     * Retrieves a boolean value from preference manager. If no such key exists, it will return the
     * value provided as <code>defaultValue</code>
     */
    public boolean getBooleanFromPreference(String key, boolean defaultValue) {
        return mSharedPreference.getBoolean(key, defaultValue);
    }



}
