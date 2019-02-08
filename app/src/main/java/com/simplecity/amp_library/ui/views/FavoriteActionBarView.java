package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.aesthetic.BgIconColorState;
import com.afollestad.aesthetic.Rx;
import com.simplecity.amp_library.R;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

import static com.afollestad.aesthetic.Rx.onErrorLogAndRethrow;

public class FavoriteActionBarView extends FrameLayout {

    @BindView(R.id.imageView)
    ImageView imageView;

    boolean isFavorite = false;

    private Drawable normalDrawable;
    private Drawable selectedDrawable;

    private Disposable aestheticDisposable;

    private int normalColor = Color.WHITE;
    private int selectedColor = Color.WHITE;

    public FavoriteActionBarView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        ButterKnife.bind(this);

        normalDrawable = DrawableCompat.wrap(ContextCompat.getDrawable(getContext(), R.drawable.ic_favorite_border_24dp).mutate());
        selectedDrawable = DrawableCompat.wrap(ContextCompat.getDrawable(getContext(), R.drawable.ic_favorite_24dp).mutate());

        imageView.setImageDrawable(isFavorite ? selectedDrawable : normalDrawable);

        setIsFavorite(isFavorite);
    }

    public void setIsFavorite(boolean isFavorite) {
        if (isFavorite != this.isFavorite) {
            this.isFavorite = isFavorite;
            imageView.setImageDrawable(isFavorite ? selectedDrawable : normalDrawable);
        }
    }

    public void toggle() {
        setIsFavorite(!isFavorite);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (isInEditMode()) {
            return;
        }

        if (!":aesthetic_ignore".equals(getTag())) {
            aestheticDisposable = Observable.combineLatest(
                    Aesthetic.get(getContext()).colorPrimary(),
                    Aesthetic.get(getContext()).colorIconTitle(null),
                    BgIconColorState.creator())
                    .compose(Rx.distinctToMainThread())
                    .subscribe(
                            bgIconColorState -> invalidateColors(bgIconColorState),
                            onErrorLogAndRethrow());
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (aestheticDisposable != null) {
            aestheticDisposable.dispose();
        }
        super.onDetachedFromWindow();
    }

    private void invalidateColors(BgIconColorState bgIconColorState) {

        this.normalColor = bgIconColorState.iconTitleColor.activeColor();
        this.selectedColor = bgIconColorState.iconTitleColor.activeColor();

        DrawableCompat.setTint(normalDrawable, normalColor);
        DrawableCompat.setTint(selectedDrawable, selectedColor);
    }
}