package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;

import com.afollestad.aesthetic.Aesthetic;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.playback.MusicService;

import io.reactivex.disposables.Disposable;

public class RepeatButton extends android.support.v7.widget.AppCompatImageButton {

    @MusicService.RepeatMode
    private int repeatMode;

    private Disposable aestheticDisposable;

    int selectedColor = Color.WHITE;

    @NonNull Drawable offDrawable;
    @NonNull Drawable oneDrawable;
    @NonNull Drawable allDrawable;

    public RepeatButton(Context context) {
        this(context, null);
    }

    public RepeatButton(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RepeatButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        offDrawable = getResources().getDrawable(R.drawable.ic_repeat_24dp_scaled);
        oneDrawable = DrawableCompat.wrap(getResources().getDrawable(R.drawable.ic_repeat_one_24dp_scaled));
        allDrawable = DrawableCompat.wrap(getResources().getDrawable(R.drawable.ic_repeat_24dp_scaled)).mutate();

        setRepeatMode(MusicService.RepeatMode.OFF);
    }

    public void setRepeatMode(@MusicService.RepeatMode int repeatMode) {

        if (repeatMode != this.repeatMode) {
            this.repeatMode = repeatMode;

            invalidateColors(selectedColor);

            switch (repeatMode) {
                case MusicService.RepeatMode.ALL:
                    setContentDescription(getResources().getString(R.string.btn_repeat_all));
                    setImageDrawable(allDrawable);
                    break;
                case MusicService.RepeatMode.ONE:
                    setContentDescription(getResources().getString(R.string.btn_repeat_current));
                    setImageDrawable(oneDrawable);
                    break;
                case MusicService.RepeatMode.OFF:
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

        aestheticDisposable = Aesthetic.get(getContext()).colorAccent()
                .subscribe(colorAccent -> {
                    selectedColor = colorAccent;
                    invalidateColors(selectedColor);
                });
    }

    @Override
    protected void onDetachedFromWindow() {
        aestheticDisposable.dispose();
        super.onDetachedFromWindow();
    }

    private void invalidateColors(int selectedColor) {
        DrawableCompat.setTint(oneDrawable, selectedColor);
        DrawableCompat.setTint(allDrawable, selectedColor);
    }
}
