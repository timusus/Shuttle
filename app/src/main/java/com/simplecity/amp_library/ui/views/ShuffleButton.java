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

public class ShuffleButton extends android.support.v7.widget.AppCompatImageButton {

    @QueueManager.ShuffleMode
    private int shuffleMode;

    @Nullable
    private Disposable aestheticDisposable;

    int normalColor = Color.WHITE;
    int selectedColor = Color.WHITE;

    @NonNull
    Drawable shuffleOff;
    @NonNull
    Drawable shuffleTracks;

    public ShuffleButton(Context context) {
        this(context, null);
    }

    public ShuffleButton(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ShuffleButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        shuffleOff = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_shuffle_24dp_scaled)).mutate();
        shuffleOff.setAlpha((int) (0.6 * 255));
        shuffleTracks = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_shuffle_24dp_scaled)).mutate();

        setShuffleMode(QueueManager.ShuffleMode.OFF);
        setImageDrawable(shuffleOff);
    }

    public void setShuffleMode(@QueueManager.ShuffleMode int shuffleMode) {
        if (this.shuffleMode != shuffleMode) {
            this.shuffleMode = shuffleMode;

            invalidateColors(normalColor, selectedColor);

            switch (shuffleMode) {
                case QueueManager.ShuffleMode.OFF:
                    setContentDescription(getResources().getString(R.string.btn_shuffle_off));
                    setImageDrawable(shuffleOff);
                    break;
                case QueueManager.ShuffleMode.ON:
                    setContentDescription(getResources().getString(R.string.btn_shuffle_on));
                    setImageDrawable(shuffleTracks);
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

        DrawableCompat.setTint(shuffleOff, normal);
        DrawableCompat.setTint(shuffleTracks, selected);
    }
}
