package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.simplecity.amp_library.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FavoriteActionBarView extends FrameLayout {

    @BindView(R.id.imageView)
    ImageView imageView;

    boolean isFavorite = false;

    private Drawable normalDrawable;
    private Drawable selectedDrawable;

    public FavoriteActionBarView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        ButterKnife.bind(this);

        normalDrawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_favorite_border_24dp);
        selectedDrawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_favorite_24dp);

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
}