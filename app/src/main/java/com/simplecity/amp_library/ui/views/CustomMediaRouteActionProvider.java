package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.app.MediaRouteButton;
import android.util.AttributeSet;
import com.afollestad.aesthetic.ActiveInactiveColors;
import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.aesthetic.Rx;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.Disposable;

import static com.afollestad.aesthetic.Rx.onErrorLogAndRethrow;

public class CustomMediaRouteActionProvider extends MediaRouteActionProvider {

    /**
     * Creates the action provider.
     *
     * @param context The context.
     */
    public CustomMediaRouteActionProvider(Context context) {
        super(context);
    }

    @Override
    public MediaRouteButton onCreateMediaRouteButton() {
        return new CustomMediaRouteButton(getContext());
    }

    public static class CustomMediaRouteButton extends MediaRouteButton {

        private Disposable subscription;

        @Nullable
        Drawable drawable;

        public CustomMediaRouteButton(Context context) {
            super(context);
        }

        public CustomMediaRouteButton(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public CustomMediaRouteButton(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        private void invalidateColors(@NonNull ActiveInactiveColors colors, Drawable icon) {
            if (icon != null) {
                drawable.setTintList(colors.toEnabledSl());
            }
        }

        @Override
        public void setRemoteIndicatorDrawable(Drawable d) {
            super.setRemoteIndicatorDrawable(d);

            this.drawable = d;

            Aesthetic.get(getContext())
                    .colorIconTitle(null)
                    .observeOn(AndroidSchedulers.mainThread())
                    .take(1)
                    .subscribe(
                            colors -> invalidateColors(colors, drawable),
                            onErrorLogAndRethrow());
        }

        @Override
        public void onAttachedToWindow() {
            super.onAttachedToWindow();
            subscription =
                    Aesthetic.get(getContext())
                            .colorIconTitle(null)
                            .compose(Rx.distinctToMainThread())
                            .subscribe(
                                    colors -> invalidateColors(colors, drawable),
                                    onErrorLogAndRethrow());
        }

        @Override
        public void onDetachedFromWindow() {
            subscription.dispose();
            super.onDetachedFromWindow();
        }
    }
}
