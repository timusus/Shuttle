package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.util.AttributeSet;

import com.afollestad.aesthetic.Aesthetic;

import io.reactivex.Observable;

public class DragHandle extends AestheticTintedImageView {

    public DragHandle(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected Observable<Integer> getColorObservable() {
        Observable<Integer> obs;
        if (isActivated()) {
            obs = Aesthetic.get().colorAccent();
        } else {
            obs = Aesthetic.get().textColorSecondary();
        }
        return obs;
    }

    @Override
    public void setActivated(boolean activated) {
        super.setActivated(activated);

        getColorObservable().take(1).subscribe(this::invalidateColors);
    }
}