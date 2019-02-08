package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import com.afollestad.aesthetic.Aesthetic;
import com.simplecity.amp_library.R;
import io.reactivex.disposables.Disposable;

public class DragGripView extends View {

    Disposable aestheticDisposable;

    private static final int[] ATTRS = new int[] {
            android.R.attr.gravity,
            android.R.attr.color,
    };

    private static final int HORIZ_RIDGES = 2;

    private int mGravity = Gravity.START;
    private int mColor = 0x33333333;

    private Paint mRidgePaint;

    private float mRidgeSize;
    private float mRidgeGap;

    private int mWidth;
    private int mHeight;

    public DragGripView(Context context) {
        this(context, null, 0);
    }

    public DragGripView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DragGripView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final TypedArray a = context.obtainStyledAttributes(attrs, ATTRS);
        mGravity = a.getInteger(0, mGravity);
        mColor = a.getColor(1, mColor);
        a.recycle();

        final Resources res = getResources();
        mRidgeSize = res.getDimensionPixelSize(R.dimen.drag_grip_ridge_size);
        mRidgeGap = res.getDimensionPixelSize(R.dimen.drag_grip_ridge_gap);

        mRidgePaint = new Paint();
        mRidgePaint.setColor(mColor);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(View.resolveSize((int) (HORIZ_RIDGES
                        * (mRidgeSize + mRidgeGap) - mRidgeGap)
                        + getPaddingLeft() + getPaddingRight(), widthMeasureSpec),
                View.resolveSize((int) mRidgeSize, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float drawWidth = HORIZ_RIDGES * (mRidgeSize + mRidgeGap) - mRidgeGap;
        float drawLeft;

        switch (mGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
            case Gravity.CENTER_HORIZONTAL:
                drawLeft = getPaddingLeft()
                        + ((mWidth - getPaddingLeft() - getPaddingRight()) - drawWidth)
                        / 2;
                break;
            case Gravity.END:
                drawLeft = getWidth() - getPaddingRight() - drawWidth;
                break;
            default:
                drawLeft = getPaddingLeft();
        }

        int vertRidges = (int) ((mHeight - getPaddingTop() - getPaddingBottom() + mRidgeGap) / (mRidgeSize + mRidgeGap));
        float drawHeight = vertRidges * (mRidgeSize + mRidgeGap) - mRidgeGap;
        float drawTop = getPaddingTop()
                + ((mHeight - getPaddingTop() - getPaddingBottom()) - drawHeight)
                / 2;

        for (int y = 0; y < vertRidges; y++) {
            for (int x = 0; x < HORIZ_RIDGES; x++) {
                canvas.drawRect(drawLeft + x * (mRidgeSize + mRidgeGap),
                        drawTop + y * (mRidgeSize + mRidgeGap), drawLeft + x
                                * (mRidgeSize + mRidgeGap) + mRidgeSize,
                        drawTop + y * (mRidgeSize + mRidgeGap) + mRidgeSize,
                        mRidgePaint);
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mHeight = h;
        mWidth = w;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        aestheticDisposable = Aesthetic.get(getContext())
                .textColorSecondary()
                .subscribe(color -> {
                    mColor = color;
                    mRidgePaint.setColor(color);
                    invalidate();
                });
    }

    @Override
    protected void onDetachedFromWindow() {
        aestheticDisposable.dispose();

        super.onDetachedFromWindow();
    }
}