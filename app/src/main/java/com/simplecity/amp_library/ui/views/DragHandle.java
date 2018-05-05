package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import com.afollestad.aesthetic.Aesthetic;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

public class DragHandle extends AestheticTintedImageView {

    private Disposable aestheticDisposable;

    public DragHandle(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected Observable<Integer> getColorObservable() {
        Observable<Integer> obs;
        if (isActivated()) {
            obs = Aesthetic.get(getContext()).colorAccent();
        } else {
            obs = Aesthetic.get(getContext()).textColorSecondary();
        }
        return obs;
    }

    @Override
    public void setActivated(boolean activated) {
        super.setActivated(activated);
        if (!isInEditMode()) {
            getColorObservable()
                    .take(1)
                    .subscribe(this::invalidateColors);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            aestheticDisposable = getColorObservable()
                    .subscribe(this::invalidateColors);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        aestheticDisposable.dispose();
        super.onDetachedFromWindow();
    }
}