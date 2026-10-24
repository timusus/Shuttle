package com.simplecity.amp_library.ui.fragments;

import android.content.res.Resources;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.google.android.libraries.cast.companionlibrary.cast.BaseCastManager;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.dagger.module.ActivityModule;
import com.simplecity.amp_library.dagger.module.FragmentModule;
import com.simplecity.amp_library.playback.MediaManager;
import com.simplecity.amp_library.ui.activities.BaseCastActivity;
import com.simplecity.amp_library.ui.views.CustomMediaRouteActionProvider;
import com.simplecity.amp_library.ui.views.multisheet.MultiSheetEventRelay;
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

    // The animation resource this fragment was last given in onCreateAnimation()
    private int lastNextAnim;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ShuttleApplication.getInstance().getAppComponent()
                .plus(new ActivityModule(getActivity()))
                .plus(new FragmentModule(this))
                .inject(this);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {

        // Remember the animation this fragment was given, so its children can hold for as long as it runs.
        lastNextAnim = nextAnim;

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
     * @param fragment the parent fragment
     * @param defValue default animation duration
     * @return the duration of the animation last handed to the parent fragment, if it is a {@link BaseFragment}
     */
    private static long getNextAnimationDuration(Fragment fragment, long defValue) {
        if (!(fragment instanceof BaseFragment) || fragment.getContext() == null) {
            return defValue;
        }
        int nextAnimResource = ((BaseFragment) fragment).lastNextAnim;
        if (nextAnimResource == 0) {
            return defValue;
        }
        try {
            Animation nextAnim = AnimationUtils.loadAnimation(fragment.getContext(), nextAnimResource);
            return nextAnim.getDuration();
        } catch (Resources.NotFoundException e) {
            // The resource is an Animator, not an Animation
            return defValue;
        }
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
    }

    protected void setupCastMenu(Menu menu) {
        if (getActivity() instanceof BaseCastActivity) {
            BaseCastManager castManager = ((BaseCastActivity) getActivity()).castManager;
            if (castManager == null) {
                return;
            }
            castManager.addMediaRouterButton(menu, R.id.media_route_menu_item);

            MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
            ((CustomMediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem)).setActivity(getActivity());
        }
    }

    public MediaManager getMediaManager() {
        return mediaManager;
    }

    protected abstract String screenName();
}
