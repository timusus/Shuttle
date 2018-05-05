package com.simplecity.amp_library.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import com.afollestad.materialdialogs.MaterialDialog;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.rx.UnsafeAction;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import java.util.concurrent.TimeUnit;

public final class SleepTimer {

    private static final String TAG = "SleepTimer";

    private static SleepTimer instance;

    private boolean isActive;

    public boolean playToEnd = false;

    private int timeRemaining = 0;

    private Flowable<Long> currentTimeObservable;

    private BehaviorSubject<Boolean> timerActiveObservable;

    public static SleepTimer getInstance() {
        if (instance == null) {
            instance = new SleepTimer();
        }
        return instance;
    }

    private SleepTimer() {

        timerActiveObservable = BehaviorSubject.create();

        currentTimeObservable = timerActiveObservable
                .doOnNext(isActive -> this.isActive = isActive)
                .switchMap(ignored -> Observable
                        .interval(1, TimeUnit.SECONDS)
                        .filter(aLong -> isActive)
                        .map(time -> timeRemaining - time)
                        .distinctUntilChanged()
                        .skip(1)
                        .doOnNext(aLong -> {
                            if (aLong == -1) {
                                stop();
                            }
                        }))
                .toFlowable(BackpressureStrategy.LATEST)
                .share();
    }

    public Flowable<Long> getCurrentTimeObservable() {
        return currentTimeObservable;
    }

    public BehaviorSubject<Boolean> getTimerActiveSubject() {
        return timerActiveObservable;
    }

    public void start(int seconds, boolean playToEnd) {
        this.timeRemaining = seconds;
        this.playToEnd = playToEnd;
        timerActiveObservable.onNext(true);
    }

    public void stop() {
        isActive = false;
        timerActiveObservable.onNext(false);
    }

    public MaterialDialog getDialog(Context context, UnsafeAction showMinutesPicker, UnsafeAction timerStarted) {

        if (isActive) {
            return new MaterialDialog.Builder(context)
                    .content(R.string.sleep_timer_stop_title)
                    .positiveText(R.string.sleep_timer_stop_button)
                    .negativeText(R.string.close)
                    .onPositive((materialDialog, dialogAction) -> stop())
                    .build();
        } else {
            return new MaterialDialog.Builder(context)
                    .title(R.string.sleep_timer)
                    .items(R.array.timerValues)
                    .checkBoxPromptRes(R.string.sleep_timer_play_to_end, false, (compoundButton, b) -> playToEnd = b)
                    .itemsCallback((materialDialog, view, i, charSequence) -> {
                        switch (i) {
                            case 0:
                                // 5 mins
                                start(5 * 60, playToEnd);
                                timerStarted.run();
                                break;
                            case 1:
                                // 15 mins
                                start(15 * 60, playToEnd);
                                timerStarted.run();
                                break;
                            case 2:
                                // 30 mins
                                start(30 * 60, playToEnd);
                                timerStarted.run();
                                break;
                            case 3:
                                // 1 hour
                                start(60 * 60, playToEnd);
                                timerStarted.run();
                                break;
                            case 4:
                                // Set time manually
                                showMinutesPicker.run();
                                break;
                        }
                    }).build();
        }
    }

    public void showMinutesDialog(Context context, UnsafeAction timerStarted) {

        @SuppressLint("InflateParams")
        View customView = LayoutInflater.from(context).inflate(R.layout.dialog_minutes_picker, null);

        EditText editText = customView.findViewById(R.id.editText);

        new MaterialDialog.Builder(context)
                .title(R.string.sleep_timer_set_minutes)
                .customView(customView, false)
                .positiveText(R.string.button_ok)
                .negativeText(R.string.cancel)
                .autoDismiss(false)
                .onPositive((materialDialog, dialogAction) -> {
                    if (!TextUtils.isEmpty(editText.getText())) {
                        start(Integer.parseInt(editText.getText().toString()) * 60, playToEnd);
                        timerStarted.run();
                        materialDialog.dismiss();
                    }
                })
                .onNegative((materialDialog, dialogAction) -> {
                    materialDialog.dismiss();
                })
                .show();

        new Handler().post(() -> {
            InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        });
    }
}