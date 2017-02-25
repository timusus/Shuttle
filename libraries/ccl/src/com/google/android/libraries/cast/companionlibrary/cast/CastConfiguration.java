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

package com.google.android.libraries.cast.companionlibrary.cast;

import com.google.android.gms.cast.LaunchOptions;
import com.google.android.libraries.cast.companionlibrary.utils.Utils;

import android.app.Service;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v7.app.MediaRouteDialogFactory;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A class that enables clients to customize certain configuration parameters for this library.
 * Clients need to use {@link Builder} to build an instance of this class and pass that to the
 * {@link VideoCastManager#initialize(Context, CastConfiguration)} or
 * {@link DataCastManager#initialize(Context, CastConfiguration)}.
 */
public class CastConfiguration {

    public static final int NOTIFICATION_ACTION_PLAY_PAUSE = 1;
    public static final int NOTIFICATION_ACTION_SKIP_NEXT = 2;
    public static final int NOTIFICATION_ACTION_SKIP_PREVIOUS = 3;
    public static final int NOTIFICATION_ACTION_DISCONNECT = 4;
    public static final int NOTIFICATION_ACTION_REWIND = 5;
    public static final int NOTIFICATION_ACTION_FORWARD = 6;

    /**
     * Available built-in actions for adding to cast notifications
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({NOTIFICATION_ACTION_PLAY_PAUSE, NOTIFICATION_ACTION_SKIP_NEXT,
            NOTIFICATION_ACTION_SKIP_PREVIOUS, NOTIFICATION_ACTION_DISCONNECT,
            NOTIFICATION_ACTION_REWIND, NOTIFICATION_ACTION_FORWARD})
    @interface NotificationAction {}

    public static final int FEATURE_DEBUGGING = 1;
    public static final int FEATURE_LOCKSCREEN = 1 << 1;
    public static final int FEATURE_NOTIFICATION = 1 << 2;
    public static final int FEATURE_WIFI_RECONNECT = 1 << 3;
    public static final int FEATURE_CAPTIONS_PREFERENCE = 1 << 4;
    public static final int FEATURE_AUTO_RECONNECT = 1 << 5;

    public static final int NEXT_PREV_VISIBILITY_POLICY_HIDDEN = 1;
    public static final int NEXT_PREV_VISIBILITY_POLICY_DISABLED = 2;
    public static final int NEXT_PREV_VISIBILITY_POLICY_ALWAYS = 3;

    /**
     * Constants representing various policies that can be enforced for the visibility of the
     * "Skip Next" or "Skip Previous" buttons used in navigating to the next or previous queue item
     * in the {@link
     * com.google.android.libraries.cast.companionlibrary.cast.player.VideoCastControllerActivity}
     * <ul>
     *     <li>{@code NEXT_PREV_VISIBILITY_POLICY_HIDDEN}: buttons should be hidden when there is
     *     no more queue item in the  corresponding direction.
     *     <li>{@code NEXT_PREV_VISIBILITY_POLICY_DISABLED}: buttons should be visible but disabled
     *     when there is no more queue item in the corresponding direction.
     *     <li>{@code NEXT_PREV_VISIBILITY_POLICY_ALWAYS}: buttons should remain visible and
     *     enabled all the time.
     * </ul>
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({NEXT_PREV_VISIBILITY_POLICY_HIDDEN, NEXT_PREV_VISIBILITY_POLICY_DISABLED,
            NEXT_PREV_VISIBILITY_POLICY_ALWAYS})
    public @interface PrevNextPolicy {}

    public static final int CCL_DEFAULT_FORWARD_STEP_S = 30;

    private static final int NOTIFICATION_MAX_EXPANDED_ACTIONS = 5;
    private static final int NOTIFICATION_MAX_COMPACT_ACTIONS = 3;

    private List<Integer> mNotificationActions;
    private List<Integer> mNotificationCompactActions;
    private int mNextPrevVisibilityPolicy;
    private int mCapabilities;
    private String mApplicationId;
    private Class<?> mTargetActivity;
    private Class<? extends Service> mCustomNotificationService;
    private List<String> mNamespaces;
    private LaunchOptions mLaunchOptions;
    private boolean mCastControllerImmersive;
    private int mForwardStep;
    private MediaRouteDialogFactory mMediaRouteDialogFactory;
    private final boolean mDisableLaunchOnConnect;

    private CastConfiguration(Builder builder) {
        if (builder.mDebugEnabled) {
            mCapabilities |= FEATURE_DEBUGGING;
        }
        if (builder.mLockScreenEnabled) {
            mCapabilities |= FEATURE_LOCKSCREEN;
        }
        if (builder.mNotificationEnabled) {
            mCapabilities |= FEATURE_NOTIFICATION;
        }
        if (builder.mWifiReconnectEnabled) {
            mCapabilities |= FEATURE_WIFI_RECONNECT;
        }
        if (builder.mCaptionPreferenceEnabled) {
            mCapabilities |= FEATURE_CAPTIONS_PREFERENCE;
        }
        if (builder.mAutoReconnectEnabled) {
            mCapabilities |= FEATURE_AUTO_RECONNECT;
        }
        mNotificationActions = new ArrayList<>(builder.mNotificationActions);
        mNotificationCompactActions = new ArrayList<>(builder.mNotificationCompactActions);
        mNextPrevVisibilityPolicy = builder.mNextPrevVisibilityPolicy;
        mApplicationId = builder.mApplicationId;
        mTargetActivity = builder.mTargetActivity;
        if (!builder.mNamespaces.isEmpty()) {
            mNamespaces = new ArrayList<>(builder.mNamespaces);
        }
        if (builder.mLocale != null) {
            mLaunchOptions = new LaunchOptions.Builder().setLocale(builder.mLocale)
                    .setRelaunchIfRunning(builder.mRelaunchIfRunning).build();
        } else {
            mLaunchOptions = new LaunchOptions.Builder().setRelaunchIfRunning(false).build();
        }
        mCastControllerImmersive = builder.mCastControllerImmersive;
        mForwardStep = builder.mForwardStep;
        mCustomNotificationService = builder.mCustomNotificationService;
        mMediaRouteDialogFactory = builder.mMediaRouteDialogFactory;
        mDisableLaunchOnConnect = builder.mDisableLaunchOnConnect;
    }

    public List<Integer> getNotificationActions() {
        return mNotificationActions;
    }

    public List<Integer> getNotificationCompactActions() {
        return mNotificationCompactActions;
    }

    public int getCapabilities() {
        return mCapabilities;
    }

    @PrevNextPolicy
    public int getNextPrevVisibilityPolicy() {
        return mNextPrevVisibilityPolicy;
    }

    public String getApplicationId() {
        return mApplicationId;
    }

    public Class<?> getTargetActivity() {
        return mTargetActivity;
    }

    public List<String> getNamespaces() {
        return mNamespaces;
    }

    public LaunchOptions getLaunchOptions() {
        return mLaunchOptions;
    }

    public boolean isCastControllerImmersive() {
        return mCastControllerImmersive;
    }

    public boolean isDisableLaunchOnConnect() {
        return mDisableLaunchOnConnect;
    }

    public int getForwardStep() {
        return mForwardStep;
    }

    public Class<? extends Service> getCustomNotificationService() {
        return mCustomNotificationService;
    }

    public MediaRouteDialogFactory getMediaRouteDialogFactory() {
        return mMediaRouteDialogFactory;
    }

    /**
     * Builder for instantiating the {@link CastConfiguration}.
     */
    public static class Builder {

        private List<Integer> mNotificationActions;
        private List<Integer> mNotificationCompactActions;
        private boolean mDebugEnabled;
        private boolean mLockScreenEnabled;
        private boolean mNotificationEnabled;
        private boolean mWifiReconnectEnabled;
        private boolean mCaptionPreferenceEnabled;
        private boolean mAutoReconnectEnabled;
        private int mNextPrevVisibilityPolicy = NEXT_PREV_VISIBILITY_POLICY_DISABLED;
        private String mApplicationId;
        private Class<?> mTargetActivity;
        private List<String> mNamespaces;
        private boolean mRelaunchIfRunning;
        private Locale mLocale;
        private boolean mCastControllerImmersive = true;
        private int mForwardStep = CCL_DEFAULT_FORWARD_STEP_S;
        private Class<? extends Service> mCustomNotificationService;
        private MediaRouteDialogFactory mMediaRouteDialogFactory;
        private boolean mDisableLaunchOnConnect;

        public Builder(String applicationId) {
            mApplicationId = Utils.assertNotEmpty(applicationId, "applicationId");
            mNotificationActions = new ArrayList<>();
            mNotificationCompactActions = new ArrayList<>();
            mNamespaces = new ArrayList<>();
        }

        public CastConfiguration build() {
            if (!mNotificationEnabled && !mNotificationActions.isEmpty()) {
                throw new IllegalArgumentException(
                        "Notification was not enabled but some notification actions were "
                                + "configured");
            }
            if (mNotificationActions.size() > NOTIFICATION_MAX_EXPANDED_ACTIONS) {
                throw new IllegalArgumentException(
                        "You cannot add more than " + NOTIFICATION_MAX_EXPANDED_ACTIONS
                                + " notification actions for the expanded view");
            }

            if (mNotificationCompactActions.size() > NOTIFICATION_MAX_COMPACT_ACTIONS) {
                throw new IllegalArgumentException(
                        "You cannot add more than " + NOTIFICATION_MAX_COMPACT_ACTIONS
                                + " compact notification actions for the compact view");
            }

            if (mCustomNotificationService != null && !mNotificationEnabled) {
                throw new IllegalArgumentException("For custom notifications, you should enable "
                        + "notifications first");
            }

            return new CastConfiguration(this);
        }

        /**
         * Enables debugging in Play Services
         */
        public Builder enableDebug() {
            mDebugEnabled = true;
            return this;
        }

        /**
         * Enables lock screen controllers
         */
        public Builder enableLockScreen() {
            mLockScreenEnabled = true;
            return this;
        }

        /**
         * Prevents the automatic launch of the app when a connection to the cast device has been
         * established.
         */
        public Builder disableLaunchOnConnect() {
            mDisableLaunchOnConnect = true;
            return this;
        }

        /**
         * Enables notifications. If you enable this feature, you would most likely want to add
         * one or more notification actions.
         *
         * @see #addNotificationAction(int, boolean)
         */
        public Builder enableNotification() {
            mNotificationEnabled = true;
            return this;
        }

        /**
         * Enable reconnection attempt when wifi connectivity is re-established
         */
        public Builder enableWifiReconnection() {
            mWifiReconnectEnabled = true;
            return this;
        }

        /**
         * Enable caption management. Enabling this feature provides a preference page for
         * configuring closed captions across all versions of android.
         */
        public Builder enableCaptionManagement() {
            mCaptionPreferenceEnabled = true;
            return this;
        }

        /**
         * Enables auto-reconnection.
         */
        public Builder enableAutoReconnect() {
            mAutoReconnectEnabled = true;
            return this;
        }

        /**
         * Adds a notification action to the MediaStyle notification. To use this, you have to
         * first enable the notifications feature by calling {@link #enableNotification()}. Note
         * that you cannot have more than 5 actions.
         *
         * @param actionType Type of action to be added. It can be one of the predefined actions:
         * <ul>
         * <li>{@link #NOTIFICATION_ACTION_PLAY_PAUSE}
         * <li>{@link #NOTIFICATION_ACTION_SKIP_NEXT}
         * <li>{@link #NOTIFICATION_ACTION_SKIP_PREVIOUS}
         * <li>{@link #NOTIFICATION_ACTION_FORWARD}
         * <li>{@link #NOTIFICATION_ACTION_REWIND}
         * <li>{@link #NOTIFICATION_ACTION_DISCONNECT}
         * </ul>.
         * @param showInCompact If {@code true}, this action will be shown in the compact view as
         * well. Note that there can't be more than three actions in the compact view.
         */
        public Builder addNotificationAction(@NotificationAction int actionType,
                boolean showInCompact) {
            if (!mNotificationActions.contains(actionType)) {
                if (showInCompact) {
                    mNotificationCompactActions.add(mNotificationActions.size());
                }
                mNotificationActions.add(actionType);
            }

            return this;
        }

        /**
         * Sets the "Target Activity". If called, this will replace the default
         * {@link com.google.android.libraries.cast.companionlibrary.cast.player.VideoCastControllerActivity}
         * that the library provides and will be invoked if user taps on the notification content or
         * mini-controller. If you use this method to define a custom activity, be aware that the
         * {@link VideoCastManager#startVideoCastControllerActivity(Context, Bundle, int, boolean)}
         * (or other variations of that method) will not start your custom activity.
         */
        public Builder setTargetActivity(@NonNull Class<?> targetActivity) {
            mTargetActivity = Utils.assertNotNull(targetActivity, "targetActivity");
            return this;
        }

        /**
         * (Optional) Sets the policy for the visibility/status of the Skip Next/Prev buttons in
         * the
         * {@link com.google.android.libraries.cast.companionlibrary.cast.player.VideoCastControllerActivity}.
         * The policy declares what should the visibility or status of these buttons be when the
         * position of the current item is at the edges of the queue. For example, if the current
         * item is the last item in the queue, what should be the visibility or status of the
         * "Skip Next" button. Available policies are:
         * <ul>
         * <li>{@link CastConfiguration#NEXT_PREV_VISIBILITY_POLICY_ALWAYS}: always show the
         * button
         * <li>{@link CastConfiguration#NEXT_PREV_VISIBILITY_POLICY_DISABLED}: disable the button
         * <li>{@link CastConfiguration#NEXT_PREV_VISIBILITY_POLICY_HIDDEN}: hide the button
         * </ul>
         * The default behavior is {@link CastConfiguration#NEXT_PREV_VISIBILITY_POLICY_DISABLED}
         */
        public Builder setNextPrevVisibilityPolicy(@PrevNextPolicy int policy) {
            mNextPrevVisibilityPolicy = policy;
            return this;
        }

        /**
         * (Optional) Adds a namespace so that the library can set up a custom channel based on
         * that
         * name. Note that for {@link VideoCastManager}, at most one namespace can be added but
         * that
         * restriction doesn't exist for {@link DataCastManager}
         */
        public Builder addNamespace(@NonNull String namespace) {
            mNamespaces.add(Utils.assertNotEmpty(namespace, "namespace"));
            return this;
        }

        /**
         * (Optional) Sets launch options. If you are calling this method, you need to provide a
         * non-null {@link Locale} as well.
         */
        public Builder setLaunchOptions(boolean relaunchIfRunning, @NonNull Locale locale) {
            mLocale = Utils.assertNotNull(locale, "locale");
            mRelaunchIfRunning = relaunchIfRunning;
            return this;
        }

        /**
         * (Optional) Sets whether the
         * {@link com.google.android.libraries.cast.companionlibrary.cast.player.VideoCastControllerActivity}
         * should be shown in immersive mode or not. The default behavior is immersive.
         */
        public Builder setCastControllerImmersive(boolean isImmersive) {
            mCastControllerImmersive = isImmersive;
            return this;
        }

        /**
         * Sets the amount to jump if {@link #NOTIFICATION_ACTION_FORWARD} or
         * {@link #NOTIFICATION_ACTION_REWIND} are included for the notification actions. Any tap
         * on those actions will result in moving the media position forward or backward by
         * {@code lengthInSeconds} seconds; positive values will move the position forward and
         * negative values rewind the position..
         */
        public Builder setForwardStep(int lengthInSeconds) {
            mForwardStep = lengthInSeconds;
            return this;
        }

        /**
         * (Optional) Sets a custom notification service, to be controlled by the library. Clients
         * can either write their own service, or they can extend
         * {@link com.google.android.libraries.cast.companionlibrary.notification.VideoCastNotificationService}.
         * CCL controls the lifecycle of this service by calling
         * {@link VideoCastManager#startNotificationService()} and
         * {@link VideoCastManager#stopNotificationService()}. It also notifies this service when
         * the notification should become visible or hidden.
         *
         * @see VideoCastManager#startNotificationService()
         * @see VideoCastManager#stopNotificationService()
         */
        public Builder setCustomNotificationService(
                Class<? extends Service> customNotificationService) {
            mCustomNotificationService = Utils
                    .assertNotNull(customNotificationService, "customNotificationService");
            return this;
        }

        /**
         * (Optional) Sets the {@link MediaRouteDialogFactory}. This is optional and if not called,
         * the default dialog will be used.
         */
        public Builder setMediaRouteDialogFactory(MediaRouteDialogFactory factory) {
            mMediaRouteDialogFactory = factory;
            return this;
        }
    }


}
