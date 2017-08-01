package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.aesthetic.Rx;
import com.afollestad.aesthetic.ViewBackgroundAction;
import com.simplecity.amp_library.utils.ActionBarUtils;
import com.simplecity.amp_library.utils.ShuttleUtils;

import io.reactivex.disposables.Disposable;

import static com.afollestad.aesthetic.Rx.onErrorLogAndRethrow;


public class StatusBarView extends View {

    private Disposable bgSubscription;

    public StatusBarView(Context context) {
        super(context);
    }

    public StatusBarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public StatusBarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (ShuttleUtils.canDrawBehindStatusBar()) {
            setMeasuredDimension(widthMeasureSpec, (int) ActionBarUtils.getStatusBarHeight(getContext()));
        } else {
            setMeasuredDimension(0, 0);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (isInEditMode()) {
            return;
        }

        // Need to invalidate the colors as early as possible. When subscribing to the continuous observable
        // below (subscription = ...), we're using distinctToMainThread(), which introduces a slight delay. During
        // this delay, we see the original colors, which are then swapped once the emission is consumed.
        // So, we'll just do a take(1), and since we're calling from the main thread, we don't need to worry
        // about distinctToMainThread() for this call. This prevents the 'flickering' of colors.

        Aesthetic.get(getContext())
                .colorStatusBar()
                .take(1)
                .subscribe(
                        ViewBackgroundAction.create(this), onErrorLogAndRethrow()
                );

        bgSubscription = Aesthetic.get(getContext()).colorStatusBar()
                .compose(Rx.distinctToMainThread())
                .subscribe(
                        ViewBackgroundAction.create(this), onErrorLogAndRethrow()
                );
    }

    @Override
    protected void onDetachedFromWindow() {
        bgSubscription.dispose();
        super.onDetachedFromWindow();
    }
}