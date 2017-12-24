package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.aesthetic.LightDarkColorState;
import com.afollestad.materialdialogs.MaterialDialog;
import com.jakewharton.rxbinding2.widget.RxSeekBar;
import com.jakewharton.rxbinding2.widget.SeekBarChangeEvent;
import com.jakewharton.rxbinding2.widget.SeekBarProgressChangeEvent;
import com.jakewharton.rxbinding2.widget.SeekBarStartChangeEvent;
import com.jakewharton.rxbinding2.widget.SeekBarStopChangeEvent;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.ui.presenters.PlayerPresenter;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.ShuttleUtils;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class UpNextView extends LinearLayout {

    private static final String TAG = "UpNextView";

    @Inject
    PlayerPresenter playerPresenter;

    @BindView(R.id.arrow)
    ImageView arrow;

    @BindView(R.id.queueText)
    TextView queueText;

    @BindView(R.id.queuePosition)
    TextView queuePositionTextView;

    @Nullable
    @BindView(R.id.play)
    PlayPauseView playPauseView;

    @Nullable
    @BindView(R.id.shuffle)
    ShuffleButton shuffleButton;

    @Nullable
    @BindView(R.id.repeat)
    RepeatButton repeatButton;

    @Nullable
    @BindView(R.id.next)
    RepeatingImageButton nextButton;

    @Nullable
    @BindView(R.id.prev)
    RepeatingImageButton prevButton;

    @Nullable
    @BindView(R.id.seekbar)
    SizableSeekBar seekBar;

    @Nullable
    @BindView(R.id.buttonContainer)
    View buttonContainer;

    @Nullable
    @BindView(R.id.textContainer)
    View textcontainer;

    private boolean isSeeking;

    private Drawable arrowDrawable;

    private CompositeDisposable disposables = new CompositeDisposable();

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

        arrowDrawable = DrawableCompat.wrap(arrow.getDrawable());

        if (playPauseView != null) {
            playPauseView.setOnClickListener(v -> {
                playPauseView.toggle();
                playPauseView.postDelayed(() -> playerPresenter.togglePlayback(), 200);
            });
        }

        if (repeatButton != null) {
            repeatButton.setOnClickListener(v -> playerPresenter.toggleRepeat());
        }

        if (shuffleButton != null) {
            shuffleButton.setOnClickListener(v -> playerPresenter.toggleShuffle());
        }

        if (nextButton != null) {
            nextButton.setOnClickListener(v -> playerPresenter.skip());
            nextButton.setRepeatListener((v, duration, repeatCount) -> playerPresenter.scanForward(repeatCount, duration));
        }

        if (prevButton != null) {
            prevButton.setOnClickListener(v -> playerPresenter.prev(true));
            prevButton.setRepeatListener((v, duration, repeatCount) -> playerPresenter.scanBackward(repeatCount, duration));
        }

        if (seekBar != null) {
            seekBar.setMax(1000);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        ShuttleApplication.getInstance().getAppComponent().inject(this);

        playerPresenter.bindView(playerViewAdapter);

        disposables.add(Observable.combineLatest(
                Aesthetic.get(getContext()).textColorPrimary(),
                Aesthetic.get(getContext()).textColorPrimaryInverse(),
                Aesthetic.get(getContext()).isDark().map(isDark -> ShuttleUtils.isLandscape() && !isDark),
                LightDarkColorState.creator())
                .subscribe(colorState -> {
                    DrawableCompat.setTint(arrowDrawable, colorState.color());
                    arrow.setImageDrawable(arrowDrawable);
                }));

        disposables.add(Aesthetic.get(getContext()).isDark()
                .map(isDark -> ShuttleUtils.isLandscape() && !isDark)
                .flatMap(isDark -> isDark ? Aesthetic.get(getContext()).textColorPrimaryInverse() : Aesthetic.get(getContext()).textColorPrimary())
                .subscribe(color -> queueText.setTextColor(color)));

        disposables.add(Aesthetic.get(getContext()).isDark()
                .map(isDark -> ShuttleUtils.isLandscape() && !isDark)
                .flatMap(isDark -> (isDark ? Aesthetic.get(getContext()).textColorSecondaryInverse() : Aesthetic.get(getContext()).textColorSecondary()))
                .subscribe(color -> queuePositionTextView.setTextColor(color)));

        if (ShuttleUtils.isLandscape()) {
            disposables.add(Aesthetic.get(getContext()).colorPrimary()
                    .subscribe(color -> {
                        if (buttonContainer != null) {
                            buttonContainer.setBackgroundColor(color);
                        }
                        if (textcontainer != null) {
                            textcontainer.setBackgroundColor(color);
                        }
                    }));
        }

        if (seekBar != null) {
            Flowable<SeekBarChangeEvent> sharedSeekBarEvents = RxSeekBar.changeEvents(seekBar)
                    .toFlowable(BackpressureStrategy.LATEST)
                    .ofType(SeekBarChangeEvent.class)
                    .observeOn(AndroidSchedulers.mainThread())
                    .share();

            disposables.add(sharedSeekBarEvents.subscribe(seekBarChangeEvent -> {
                if (seekBarChangeEvent instanceof SeekBarStartChangeEvent) {
                    isSeeking = true;
                } else if (seekBarChangeEvent instanceof SeekBarStopChangeEvent) {
                    isSeeking = false;
                }
            }, error -> LogUtils.logException(TAG, "Error in seek change event", error)));

            disposables.add(sharedSeekBarEvents
                    .ofType(SeekBarProgressChangeEvent.class)
                    .filter(SeekBarProgressChangeEvent::fromUser)
                    .debounce(15, TimeUnit.MILLISECONDS)
                    .subscribe(seekBarChangeEvent -> playerPresenter.seekTo(seekBarChangeEvent.progress()),
                            error -> LogUtils.logException(TAG, "Error receiving seekbar progress", error)));
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        playerPresenter.unbindView(playerViewAdapter);

        disposables.dispose();
    }

    private PlayerViewAdapter playerViewAdapter = new PlayerViewAdapter() {
        @Override
        public void queueChanged(int queuePosition, int queueLength) {
            super.queueChanged(queuePosition, queueLength);

            queuePositionTextView.setText(String.format("%d / %d", queuePosition, queueLength));
        }

        @Override
        public void playbackChanged(boolean isPlaying) {
            if (playPauseView != null) {
                if (isPlaying) {
                    if (playPauseView.isPlay()) {
                        playPauseView.toggle();
                        playPauseView.setContentDescription(getContext().getString(R.string.btn_pause));
                    }
                } else {
                    if (!playPauseView.isPlay()) {
                        playPauseView.toggle();
                        playPauseView.setContentDescription(getContext().getString(R.string.btn_play));
                    }
                }
            }
        }

        @Override
        public void shuffleChanged(int shuffleMode) {
            if (shuffleButton != null) {
                shuffleButton.setShuffleMode(shuffleMode);
            }
        }

        @Override
        public void repeatChanged(int repeatMode) {
            if (repeatButton != null) {
                repeatButton.setRepeatMode(repeatMode);
            }
        }

        @Override
        public void showUpgradeDialog(MaterialDialog dialog) {
            dialog.show();
        }

        @Override
        public void setSeekProgress(int progress) {
            if (!isSeeking && seekBar != null) {
                seekBar.setProgress(progress);
            }
        }
    };
}