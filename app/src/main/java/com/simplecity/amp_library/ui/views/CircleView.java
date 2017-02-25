package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.utils.ResourceUtils;

public class CircleView extends View {

    private Paint paint;

    private Drawable tickDrawable;

    public CircleView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setWillNotDraw(false);

        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);

        if (isInEditMode()) {
            paint.setColor(Color.RED);
        }

        tickDrawable = getResources().getDrawable(R.drawable.ic_tick_white);
    }

    public void setColor(int color) {
        paint.setColor(color);
        invalidate();
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawCircle(getWidth() / 2, getHeight() / 2, getWidth() / 2, paint);

        if (isActivated()) {
            int padding = ResourceUtils.toPixels(4);
            tickDrawable.setBounds(padding, padding, getWidth() - padding, getHeight() - padding);
            tickDrawable.draw(canvas);
        }
    }
}