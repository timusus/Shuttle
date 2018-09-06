package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

public class FFTView extends AppCompatImageView {

    private Rect[] rectangle;

    private int rectWidth;
    private int paddingLeft;
    private int drawingColor;

    public FFTView(Context context) {
        super(context);
        initialize();
    }

    public FFTView(Context context, AttributeSet attrst) {
        super(context, attrst);
        initialize();
    }

    public FFTView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    private void initialize() {
        rectangle = new Rect[128];

        drawingColor = Color.WHITE;

        for(int i=0; i<128; i++) {
            rectangle[i] = new Rect();
        }

        rectWidth = (int) this.getWidth() / 64;
        paddingLeft = (this.getWidth() - rectWidth*64) / 2;
    }

    public void setColor(int newColor) {
        drawingColor = newColor;
    }

    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld){
        super.onSizeChanged(xNew, yNew, xOld, yOld);

        rectWidth = this.getWidth() / 64;
        paddingLeft = (this.getWidth() - rectWidth*64) / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(Color.TRANSPARENT);
        Paint paint = new Paint();
        paint.setColor(drawingColor);
        for(int i=0; i<64; i++) {
            canvas.drawRect(rectangle[i], paint);
        }
        this.postInvalidate();
    }

    public void plotData(int[] data) {
        for(int i=0; i<data.length; i++) {
            // create a rectangle that we'll draw later
            rectangle[i] = new Rect(rectWidth*i + paddingLeft, this.getHeight() - data[i], rectWidth*(i+1) + paddingLeft, this.getHeight());
        }
        invalidate();
    }
}