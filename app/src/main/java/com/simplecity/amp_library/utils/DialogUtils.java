package com.simplecity.amp_library.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;

public class DialogUtils {

    private static final String TAG = "DialogUtils";

    public static MaterialDialog.Builder getBuilder(Context context) {

        return new MaterialDialog.Builder(context);
    }

    /**
     * Displays the popup dialog recommending the user try the paid version
     */
    public static void showUpgradeNagDialog(final Context context, MaterialDialog.SingleButtonCallback listener) {

        //If we're in the free version, the app has been launched more than 15 times,
        //The message hasn't been read before, display the 'upgrade to pro' dialog.
        if (!ShuttleUtils.isUpgraded()
                && SettingsManager.getInstance().getLaunchCount() > 15
                && !SettingsManager.getInstance().getNagMessageRead()) {

            MaterialDialog.Builder builder = getBuilder(context)
                    .title(context.getResources().getString(R.string.get_pro_title))
                    .content(context.getResources().getString(R.string.get_pro_message))
                    .positiveText(R.string.btn_upgrade)
                    .onPositive(listener)
                    .negativeText(R.string.get_pro_button_no);

            builder.show();
            SettingsManager.getInstance().setNagMessageRead();

            AnalyticsManager.logUpgrade(AnalyticsManager.UpgradeType.NAG);
        }
    }

    /**
     * Displayed when the user chooses to upgrade
     */
    public static MaterialDialog getUpgradeDialog(final Context context, MaterialDialog.SingleButtonCallback listener) {
        return getBuilder(context)
                .title(context.getResources().getString(R.string.get_pro_title))
                .content(context.getResources().getString(R.string.upgrade_dialog_message))
                .positiveText(R.string.btn_upgrade)
                .onPositive(listener)
                .negativeText(R.string.get_pro_button_no)
                .build();
    }

    public static void showDownloadWarningDialog(Context context, MaterialDialog.SingleButtonCallback listener) {
        getBuilder(context)
                .title(R.string.pref_title_download_artwork)
                .content(R.string.pref_warning_download_artwork)
                .positiveText(R.string.download)
                .onPositive(listener)
                .negativeText(R.string.cancel)
                .show();
    }

    public static void showWeekSelectorDialog(final Context context) {

        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(context).inflate(R.layout.weekpicker, null);

        final NumberPicker numberPicker;
        numberPicker = view.findViewById(R.id.weeks);
        numberPicker.setMaxValue(12);
        numberPicker.setMinValue(1);
        numberPicker.setValue(MusicUtils.getIntPref(context, "numweeks", 2));

        getBuilder(context)
                .title(R.string.week_selector)
                .customView(view, false)
                .negativeText(R.string.cancel)
                .positiveText(R.string.picker_set)
                .onPositive((materialDialog, dialogAction) -> {
                    int numweeks;
                    numweeks = numberPicker.getValue();
                    MusicUtils.setIntPref(context, "numweeks", numweeks);
                })
                .show();
    }

    public static void showRateSnackbar(final Activity activity, final View view) {
        //If the user hasn't dismissed the snackbar in the past, and we haven't already shown it for this session
        if (!SettingsManager.getInstance().getHasRated() && !SettingsManager.getInstance().hasSeenRateSnackbar) {
            //If this is the tenth launch, or a multiple of 50
            if (SettingsManager.getInstance().getLaunchCount() == 10 || (SettingsManager.getInstance().getLaunchCount() != 0 && SettingsManager.getInstance().getLaunchCount() % 50 == 0)) {
                Snackbar snackbar = Snackbar.make(view, R.string.snackbar_rate_text, Snackbar.LENGTH_INDEFINITE)
                        .setDuration(15000)
                        .setAction(R.string.snackbar_rate_action, v -> ShuttleUtils.openShuttleLink(activity, ShuttleApplication.getInstance().getPackageName(), activity.getPackageManager()))
                        .addCallback(new Snackbar.Callback() {
                            @Override
                            public void onDismissed(Snackbar transientBottomBar, int event) {
                                super.onDismissed(transientBottomBar, event);

                                if (event != BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_TIMEOUT) {
                                    // We don't really care whether the user has rated or not. The snackbar was
                                    // dismissed. Never show it again.
                                    SettingsManager.getInstance().setHasRated();
                                }
                            }
                        });
                snackbar.show();

                TextView snackbarText = snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                if (snackbarText != null) {
                    snackbarText.setTextColor(Color.WHITE);
                }
            }

            SettingsManager.getInstance().hasSeenRateSnackbar = true;
        }
    }
}
