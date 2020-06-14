package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.appcompat.widget.AppCompatTextView;
import android.util.AttributeSet;
import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.aesthetic.Util;
import com.simplecity.amp_library.R;
import io.reactivex.disposables.Disposable;

public class PlayCountView extends AppCompatTextView {

    private Disposable disposable;

    private Drawable backgroundDrawable;

    public PlayCountView(Context context) {
        super(context);

        init();
    }

    public PlayCountView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    private void init() {
        backgroundDrawable = DrawableCompat.wrap(ContextCompat.getDrawable(getContext(), R.drawable.bg_rounded));
    }

    public void setCount(int count) {
        setText(String.valueOf(count));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        disposable = Aesthetic.get(getContext())
                .colorPrimary()
                .subscribe(colorPrimary -> {
                    DrawableCompat.setTint(backgroundDrawable, colorPrimary);
                    setBackground(backgroundDrawable);
                    if (Util.isColorLight(colorPrimary)) {
                        setTextColor(Color.BLACK);
                    } else {
                        setTextColor(Color.WHITE);
                    }
                });
    }

    @Override
    protected void onDetachedFromWindow() {

        disposable.dispose();

        super.onDetachedFromWindow();
    }
}
