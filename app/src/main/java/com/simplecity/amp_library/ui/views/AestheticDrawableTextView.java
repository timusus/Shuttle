package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.aesthetic.Rx;
import com.afollestad.aesthetic.ViewUtil;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

import static com.afollestad.aesthetic.Rx.onErrorLogAndRethrow;
import static com.afollestad.aesthetic.Util.resolveResId;

public class AestheticDrawableTextView extends AppCompatTextView {

    private static String IGNORE_TAG = ":aesthetic_ignore";

    private Disposable subscription;

    private int textColorResId;

    public AestheticDrawableTextView(Context context) {
        super(context);
    }

    public AestheticDrawableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public AestheticDrawableTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            textColorResId = resolveResId(context, attrs, android.R.attr.textColor);
        }
    }

    void invalidateColors(int color) {

        setTextColor(color);

        Drawable[] drawables = getCompoundDrawables();
        for (Drawable drawable : drawables) {
            if (drawable == null) {
                continue;
            }
            drawable = DrawableCompat.wrap(drawable);
            DrawableCompat.setTint(drawable, color);
        }
        setCompoundDrawables(drawables[0], drawables[1], drawables[2], drawables[3]);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (IGNORE_TAG.equals(getTag())) {
            invalidateColors(getCurrentTextColor());
            return;
        }

        Observable<Integer> obs =
                ViewUtil.getObservableForResId(
                        getContext(), textColorResId, Aesthetic.get(getContext()).textColorSecondary());
        //noinspection ConstantConditions
        subscription =
                obs.compose(Rx.distinctToMainThread())
                        .subscribe(this::invalidateColors, onErrorLogAndRethrow());
    }

    @Override
    protected void onDetachedFromWindow() {
        if (subscription != null) {
            subscription.dispose();
        }
        super.onDetachedFromWindow();
    }
}
