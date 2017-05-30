package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.simplecity.amp_library.utils.ActionBarUtils;

import io.reactivex.disposables.Disposable;


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

        setMeasuredDimension(widthMeasureSpec, (int) ActionBarUtils.getStatusBarHeight(getContext()));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

//        bgSubscription = Aesthetic.get().colorStatusBar()
//                .compose(Rx.<Integer>distinctToMainThread())
//                .subscribe(
//                        ViewBackgroundAction.create(this), onErrorLogAndRethrow()
//                );
    }


    @Override
    protected void onDetachedFromWindow() {
//        bgSubscription.dispose();
        super.onDetachedFromWindow();
    }
}
