package com.simplecity.amp_library.ui.common;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.playback.MediaManager;
import com.simplecity.amp_library.ui.screens.drawer.NavigationEventRelay;
import com.simplecity.amp_library.ui.views.multisheet.MultiSheetEventRelay;
import com.simplecity.amp_library.utils.AnalyticsManager;
import com.squareup.leakcanary.RefWatcher;
import dagger.android.support.AndroidSupportInjection;
import java.lang.reflect.Field;
import javax.inject.Inject;
import test.com.androidnavigation.fragment.BaseController;

public abstract class BaseFragment extends BaseController {

    private static final String TAG = "BaseFragment";

    // Arbitrary value; set it to some reasonable default
    private static final int DEFAULT_CHILD_ANIMATION_DURATION = 250;

    @Inject
    MultiSheetEventRelay multiSheetEventRelay;

    @Inject
    protected MediaManager mediaManager;

    @Inject
    protected NavigationEventRelay navigationEventRelay;

    @Inject
    public AnalyticsManager analyticsManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidSupportInjection.inject(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        analyticsManager.logScreenName(getActivity(), screenName());
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {

        /*
         * When a fragment transaction is performed on a parent fragment containing nested children,
         * the children disappear as soon as the transaction begins.. So if you're relying on some
         * fancy animation for the parent, we need to ensure the child waits before performing its
         * own animation.
         * see https://code.google.com/p/android/issues/detail?id=55228
         */
        final Fragment parent = getParentFragment();

        // Apply the workaround only if this is a child fragment, and the parent
        // is being removed.
        if (!enter && parent != null && parent.isRemoving()) {
            // This is a workaround for the bug where child fragments disappear when
            // the parent is removed (as all children are first removed from the parent)
            // See https://code.google.com/p/android/issues/detail?id=55228
            Animation doNothingAnim = new AlphaAnimation(1, 1);
            doNothingAnim.setDuration(getNextAnimationDuration(parent, DEFAULT_CHILD_ANIMATION_DURATION));
            return doNothingAnim;
        } else {
            return super.onCreateAnimation(transit, enter, nextAnim);
        }
    }

    /**
     * Attempt to get the resource ID of the next animation that
     * will be applied to the given fragment.
     *
     * @param fragment the parent fragment
     * @param defValue default animation value
     * @return the duration of the parent fragment's animation
     */
    private static long getNextAnimationDuration(Fragment fragment, long defValue) {
        try {
            // Attempt to get the resource ID of the next animation that
            // will be applied to the given fragment.
            Field nextAnimField = Fragment.class.getDeclaredField("mNextAnim");
            nextAnimField.setAccessible(true);
            int nextAnimResource = nextAnimField.getInt(fragment);
            Animation nextAnim = AnimationUtils.loadAnimation(fragment.getActivity(), nextAnimResource);

            // ...and if it can be loaded, return that animation's duration
            return (nextAnim == null) ? defValue : nextAnim.getDuration();
        } catch (NoSuchFieldException | IllegalAccessException | Resources.NotFoundException ignored) {
            return defValue;
        }
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
        RefWatcher refWatcher = ((ShuttleApplication) getContext().getApplicationContext()).getRefWatcher();
        refWatcher.watch(this);
    }

    public MediaManager getMediaManager() {
        return mediaManager;
    }

    public NavigationEventRelay getNavigationEventRelay() {
        return navigationEventRelay;
    }

    protected abstract String screenName();
}
