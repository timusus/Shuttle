package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.aesthetic.Util;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;
import io.reactivex.disposables.Disposable;

public class AestheticFastScrollRecyclerView extends FastScrollRecyclerView {

    Disposable aestheticDisposable;

    public AestheticFastScrollRecyclerView(Context context) {
        super(context);
    }

    public AestheticFastScrollRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AestheticFastScrollRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        aestheticDisposable = Aesthetic.get(getContext()).colorAccent().subscribe(color -> {
            setThumbColor(color);
            setThumbInactiveColor(color);
            setPopupBgColor(color);
            setPopupTextColor(Util.isColorLight(color) ? Color.BLACK : Color.WHITE);
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        aestheticDisposable.dispose();
        super.onDetachedFromWindow();
    }
}