package com.simplecity.amp_library.ui.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;

public class PlayPauseView extends FrameLayout {

    private static final String TAG = "PlayPauseView";

    private static final long PLAY_PAUSE_ANIMATION_DURATION = 200;

    private final PlayPauseDrawable drawable;
    private final Paint paint = new Paint();

    private Animator animator;
    private int backgroundColor;
    private int width;
    private int height;

    public PlayPauseView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        drawable = new PlayPauseDrawable(context);
        drawable.setCallback(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int size = Math.min(getMeasuredWidth(), getMeasuredHeight());
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onSizeChanged(final int w, final int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        drawable.setBounds(0, 0, w, h);
        width = w;
        height = h;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setOutlineProvider(new ViewOutlineProvider() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setOval(0, 0, view.getWidth(), view.getHeight());
                }
            });
            setClipToOutline(true);
        }
    }

    private void setColor(int color) {
        backgroundColor = color;
        invalidate();
    }

    private int getColor() {
        return backgroundColor;
    }

    public void setDrawableColor(int color) {
        drawable.setColor(color);
        invalidate();
    }

    @Override
    protected boolean verifyDrawable(@NonNull Drawable who) {
        return who == drawable || super.verifyDrawable(who);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setColor(backgroundColor);
        final float radius = Math.min(width, height) / 2f;
        canvas.drawCircle(width / 2f, height / 2f, radius, paint);
        drawable.draw(canvas);
    }

    public void update() {

    }

    public void toggle(@Nullable Function0<Unit> completion) {
        if (animator != null) {
            animator.cancel();
        }

        animator = drawable.getPausePlayAnimator();
        animator.setInterpolator(new DecelerateInterpolator());
        animator.setDuration(PLAY_PAUSE_ANIMATION_DURATION);
        animator.start();
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                animation.removeListener(this);

                if (completion != null) {
                    completion.invoke();
                }
            }
        });
    }

    public boolean isPlay() {
        return drawable != null && drawable.isPlay();
    }
}