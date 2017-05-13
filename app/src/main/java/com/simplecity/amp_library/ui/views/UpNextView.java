package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.ui.presenters.PlayerPresenter;
import com.simplecity.amp_library.utils.ColorUtils;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class UpNextView extends LinearLayout {

    @BindView(R.id.arrow)
    ImageView arrow;

    @BindView(R.id.queueText)
    TextView queueText;

    @BindView(R.id.queuePosition)
    TextView queuePositionTextView;

    @Inject
    PlayerPresenter playerPresenter;

    public UpNextView(Context context) {
        this(context, null);
    }

    public UpNextView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public UpNextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setOrientation(HORIZONTAL);

        View.inflate(context, R.layout.up_next_view, this);

        ButterKnife.bind(this);

        setBackgroundColor(Color.WHITE);

        Drawable arrowDrawable = DrawableCompat.wrap(arrow.getDrawable());
        DrawableCompat.setTint(arrowDrawable, ColorUtils.getTextColorPrimary());
        arrow.setImageDrawable(arrowDrawable);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        ShuttleApplication.getInstance().getAppComponent().inject(this);

        playerPresenter.bindView(playerViewAdapter);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        playerPresenter.unbindView(playerViewAdapter);
    }

    private PlayerViewAdapter playerViewAdapter = new PlayerViewAdapter() {
        @Override
        public void queueChanged(int queuePosition, int queueLength) {
            super.queueChanged(queuePosition, queueLength);

            queuePositionTextView.setText(String.format("%d / %d", queuePosition, queueLength));
        }
    };

}