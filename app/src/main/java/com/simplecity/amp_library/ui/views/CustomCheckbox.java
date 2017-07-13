package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;

import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.aesthetic.AestheticCheckBox;
import com.afollestad.aesthetic.ColorIsDarkState;
import com.afollestad.aesthetic.Rx;
import com.afollestad.aesthetic.ViewTextColorAction;
import com.afollestad.aesthetic.ViewUtil;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;

import static com.afollestad.aesthetic.Rx.onErrorLogAndRethrow;

/**
 * A Custom AestheticCheckbox which sets its text color to black.
 * (This was surprisingly difficult to achieve)
 */
public class CustomCheckbox extends AestheticCheckBox {

    private CompositeDisposable subscriptions;

    public CustomCheckbox(Context context) {
        super(context);
    }

    public CustomCheckbox(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomCheckbox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void invalidateColors(ColorIsDarkState state) {
        super.invalidateColors(state);

        setTextColor(Color.BLACK);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        subscriptions = new CompositeDisposable();
        //noinspection ConstantConditions
        subscriptions.add(
                Observable.combineLatest(
                        ViewUtil.getObservableForResId(
                                getContext(), backgroundResId, Aesthetic.get().colorAccent()),
                        Aesthetic.get().isDark(),
                        ColorIsDarkState.creator())
                        .compose(Rx.<ColorIsDarkState>distinctToMainThread())
                        .subscribe(
                                new Consumer<ColorIsDarkState>() {
                                    @Override
                                    public void accept(@NonNull ColorIsDarkState colorIsDarkState) {
                                        invalidateColors(colorIsDarkState);
                                    }
                                },
                                onErrorLogAndRethrow()));

        ViewTextColorAction.create(this).accept(Color.BLACK);
    }

    @Override
    protected void onDetachedFromWindow() {
        subscriptions.clear();
        super.onDetachedFromWindow();
    }
}
