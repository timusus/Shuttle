package com.simplecity.amp_library.glide.utils;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory;
import com.bumptech.glide.request.transition.Transition;

public class AlwaysCrossFade extends DrawableCrossFadeFactory {
    private final boolean transparentImagesPossible;

    /**
     * @param transparentImagesPossible used to signal that are no transparent images possible with this load.
     * When cross-fading between opaque images a better-looking cross-fade is possible
     * via {@link TransitionDrawable#setCrossFadeEnabled(boolean)}.
     * @see <a href="https://github.com/bumptech/glide/issues/943">#943</a>
     */
    public AlwaysCrossFade(boolean transparentImagesPossible) {
        super(600, true);
        this.transparentImagesPossible = transparentImagesPossible;
    }

    @Override
    public Transition<Drawable> build(DataSource dataSource, boolean isFirstResource) {
        // passing isFirstResource instead of isFromMemoryCache achieves the result we want
        Transition<Drawable> transition = super.build(dataSource, isFirstResource);
        if (!transparentImagesPossible) {
            transition = new RealCrossFadeAnimation(transition);
        }
        return transition;
    }

    private static class RealCrossFadeAnimation implements Transition<Drawable> {
        private final Transition<Drawable> transition;

        public RealCrossFadeAnimation(Transition<Drawable> animation) {
            this.transition = animation;
        }

        @Override
        public boolean transition(Drawable current, ViewAdapter adapter) {
            return transition.transition(current, new CrossFadeDisablingViewAdapter(adapter));
        }
    }

    private static class CrossFadeDisablingViewAdapter extends WrappingViewAdapter {
        public CrossFadeDisablingViewAdapter(Transition.ViewAdapter adapter) {
            super(adapter);
        }

        @Override
        public void setDrawable(Drawable drawable) {
            if (drawable instanceof TransitionDrawable) {
                ((TransitionDrawable) drawable).setCrossFadeEnabled(false);
            }
            super.setDrawable(drawable);
        }
    }
}