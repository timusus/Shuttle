package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.TooltipCompat;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.aesthetic.BgIconColorState;
import com.afollestad.aesthetic.Rx;
import com.simplecity.amp_library.R;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

import static com.afollestad.aesthetic.Rx.onErrorLogAndRethrow;

public class LockActionBarView extends FrameLayout {

    private static final String TAG = "LockActionBarView";

    @BindView(R.id.imageView)
    ImageView imageView;

    private boolean locked = false;

    private AnimatedVectorDrawableCompat toLockedAnim;
    private AnimatedVectorDrawableCompat toUnlockedAnim;

    private Disposable aestheticDisposable;

    public LockActionBarView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        ButterKnife.bind(this);

        toLockedAnim = AnimatedVectorDrawableCompat.create(getContext(), R.drawable.lock_anim);
        toUnlockedAnim = AnimatedVectorDrawableCompat.create(getContext(), R.drawable.unlock_anim);
    }

    public void setLocked(boolean locked, boolean animate) {
        if (animate) {
            AnimatedVectorDrawableCompat currentDrawable = locked ? toLockedAnim : toUnlockedAnim;
            imageView.setImageDrawable(currentDrawable);
            currentDrawable.start();
        } else {
            AnimatedVectorDrawableCompat currentDrawable = locked ? toUnlockedAnim : toLockedAnim;
            imageView.setImageDrawable(currentDrawable);
        }

        TooltipCompat.setTooltipText(this, getResources().getString(locked ? R.string.menu_queue_swipe_enable : R.string.menu_queue_swipe_disable));

        this.locked = locked;
    }

    public boolean isLocked() {
        return locked;
    }

    public void toggle() {
        setLocked(!locked, true);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (isInEditMode()) {
            return;
        }

        if (!":aesthetic_ignore".equals(getTag())) {
            aestheticDisposable = Observable.combineLatest(
                    Aesthetic.get(getContext()).colorPrimary(),
                    Aesthetic.get(getContext()).colorIconTitle(null),
                    BgIconColorState.creator())
                    .compose(Rx.distinctToMainThread())
                    .subscribe(
                            bgIconColorState -> invalidateColors(bgIconColorState),
                            onErrorLogAndRethrow());
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (aestheticDisposable != null) {
            aestheticDisposable.dispose();
        }
        super.onDetachedFromWindow();
    }

    private void invalidateColors(BgIconColorState bgIconColorState) {
        int normalColor = bgIconColorState.iconTitleColor.activeColor();
        DrawableCompat.setTint(toLockedAnim, normalColor);
        DrawableCompat.setTint(toUnlockedAnim, normalColor);
    }
}