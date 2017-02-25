package com.simplecity.amp_library.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import com.afollestad.materialdialogs.MaterialDialog;
import com.doomonafireball.betterpickers.hmspicker.HmsPicker;
import com.doomonafireball.betterpickers.hmspicker.HmsView;
import com.simplecity.amp_library.R;

/**
 * Puts the music to sleep after a given amount of time
 */
public final class SleepTimer {

    private static final String FONT = "fonts/AndroidClockMono-Thin.ttf";

    public SleepTimer() {
    }

    /**
     * Constructor for <code>SleepTimer</code>
     *
     * @param context   The {@link Activity} to use
     * @param active    True if the timer is active, false otherwise
     * @param remaining The remaining time of the current track
     */
    public static void createTimer(final Context context, boolean active, final long remaining) {

        final View view = LayoutInflater.from(context).inflate(R.layout.dialog_timer, null);
        final HmsPicker hmsPicker = (HmsPicker) view.findViewById(R.id.hms_picker);
        final HmsView hmsView = (HmsView) view.findViewById(R.id.hms_view);

        ThemeUtils.themeHmsPicker(hmsPicker);
        ThemeUtils.themeHmsView(hmsView);

        final long timeMillis = remaining - System.currentTimeMillis();
        final int minutes = (int) ((timeMillis / (1000 * 60)) % 60);
        final int hours = (int) ((timeMillis / (1000 * 60 * 60)) % 24);

        int minutesFirstDigit = 0;
        int minuteSecondDigit = 0;
        if (minutes > 0) {
            minutesFirstDigit = minutes / 10;
            minuteSecondDigit = minutes % 10;
        }
        hmsView.setTime(hours, minutesFirstDigit, minuteSecondDigit);

        final SharedPreferences mPrefs;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        final CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkbox);
        checkBox.setChecked(mPrefs.getBoolean("sleep_timer_wait_til_end", true));
        checkBox.setOnCheckedChangeListener((compoundButton, b) -> mPrefs.edit().putBoolean("sleep_timer_wait_til_end", b).apply());

        final MaterialDialog.Builder builder = DialogUtils.getBuilder(context)
                .customView(view, false)
                .negativeText(R.string.close);

        if (active) {
            hmsView.setVisibility(View.VISIBLE);
            hmsPicker.setVisibility(View.GONE);
            builder.positiveText(R.string.timer_stop)
                    .onPositive((materialDialog, dialogAction) -> MusicUtils.stopTimer());
        } else {
            hmsView.setVisibility(View.GONE);
            hmsPicker.setVisibility(View.VISIBLE);
            builder.positiveText(R.string.timer_set)
                    .onPositive((materialDialog, dialogAction) -> {
                        if (hmsPicker.getTime() != 0) {
                            MusicUtils.setTimer(hmsPicker.getTime() * 1000);
                        }
                        hmsPicker.setVisibility(View.GONE);
                        hmsView.setVisibility(View.VISIBLE);
                    });
        }
        builder.show();

        new CountDownTimer(timeMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                final long timeMillis = remaining - System.currentTimeMillis();
                final int minutes = (int) ((timeMillis / (1000 * 60)) % 60);
                final int hours = (int) ((timeMillis / (1000 * 60 * 60)) % 24);
                int minutesFirstDigit = 0;
                int minuteSecondDigit = 0;
                if (minutes > 0) {
                    minutesFirstDigit = minutes / 10;
                    minuteSecondDigit = minutes % 10;
                }
                hmsView.setTime(hours, minutesFirstDigit, minuteSecondDigit);
            }

            @Override
            public void onFinish() {

            }
        }.start();
    }

}
