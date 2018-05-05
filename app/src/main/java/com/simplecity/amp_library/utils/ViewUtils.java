package com.simplecity.amp_library.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.support.annotation.Nullable;
import android.view.View;
import com.simplecity.amp_library.rx.UnsafeAction;

public class ViewUtils {

    public static void fadeOut(View view, @Nullable UnsafeAction action) {

        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f).setDuration(250);
        objectAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(View.GONE);
                objectAnimator.removeAllListeners();

                if (action != null) {
                    action.run();
                }
            }
        });

        objectAnimator.start();
    }

    public static void fadeIn(View view, @Nullable UnsafeAction action) {

        view.setVisibility(View.VISIBLE);

        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f).setDuration(250);
        objectAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                animation.removeAllListeners();
                if (action != null) {
                    action.run();
                }
            }
        });
        objectAnimator.start();
    }
}
