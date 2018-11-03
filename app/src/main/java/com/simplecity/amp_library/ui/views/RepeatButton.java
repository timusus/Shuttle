package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import com.afollestad.aesthetic.Aesthetic;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.playback.QueueManager;
import io.reactivex.disposables.Disposable;

public class RepeatButton extends android.support.v7.widget.AppCompatImageButton {

    @QueueManager.RepeatMode
    private int repeatMode;

    @Nullable
    private Disposable aestheticDisposable;

    int normalColor = Color.WHITE;
    int selectedColor = Color.WHITE;

    @NonNull
    Drawable offDrawable;

    @NonNull
    Drawable oneDrawable;

    @NonNull
    Drawable allDrawable;

    public RepeatButton(Context context) {
        this(context, null);
    }

    public RepeatButton(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RepeatButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        offDrawable = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_repeat_24dp_scaled)).mutate();
        offDrawable.setAlpha((int) (0.6 * 255));
        oneDrawable = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_repeat_one_24dp_scaled)).mutate();
        allDrawable = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_repeat_24dp_scaled)).mutate();

        setRepeatMode(QueueManager.RepeatMode.OFF);
        setImageDrawable(offDrawable);
    }

    public void setRepeatMode(@QueueManager.RepeatMode int repeatMode) {

        if (repeatMode != this.repeatMode) {
            this.repeatMode = repeatMode;

            invalidateColors(normalColor, selectedColor);

            switch (repeatMode) {
                case QueueManager.RepeatMode.ALL:
                    setContentDescription(getResources().getString(R.string.btn_repeat_all));
                    setImageDrawable(allDrawable);
                    break;
                case QueueManager.RepeatMode.ONE:
                    setContentDescription(getResources().getString(R.string.btn_repeat_current));
                    setImageDrawable(oneDrawable);
                    break;
                case QueueManager.RepeatMode.OFF:
                    setContentDescription(getResources().getString(R.string.btn_repeat_off));
                    setImageDrawable(offDrawable);
                    break;
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (isInEditMode()) {
            return;
        }

        if (!":aesthetic_ignore".equals(getTag())) {
            aestheticDisposable = Aesthetic.get(getContext()).colorAccent()
                    .subscribe(colorAccent -> {
                        selectedColor = colorAccent;
                        invalidateColors(Color.WHITE, selectedColor);
                    });
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (aestheticDisposable != null) {
            aestheticDisposable.dispose();
        }
        super.onDetachedFromWindow();
    }

    public void invalidateColors(int normal, int selected) {

        this.normalColor = normal;
        this.selectedColor = selected;

        DrawableCompat.setTint(offDrawable, normal);
        DrawableCompat.setTint(oneDrawable, selected);
        DrawableCompat.setTint(allDrawable, selected);
    }
}
