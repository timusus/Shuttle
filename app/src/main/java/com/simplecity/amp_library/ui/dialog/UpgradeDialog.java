package com.simplecity.amp_library.ui.dialog;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.simplecity.amp_library.IabManager;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ui.activities.MainActivity;
import com.simplecity.amp_library.utils.ShuttleUtils;

public class UpgradeDialog {

    private UpgradeDialog() {
        //no instance
    }

    public static MaterialDialog getUpgradeDialog(Activity activity) {
        return new MaterialDialog.Builder(activity)
                .title(activity.getResources().getString(R.string.get_pro_title))
                .content(activity.getResources().getString(R.string.upgrade_dialog_message))
                .positiveText(R.string.btn_upgrade)
                .onPositive((dialog, which) -> {
                    if (ShuttleUtils.isAmazonBuild()) {
                        activity.startActivity(ShuttleUtils.getShuttleStoreIntent("com.simplecity.amp_pro"));
                    } else {
                        purchaseUpgrade(activity);
                    }
                })
                .negativeText(R.string.get_pro_button_no)
                .build();
    }

    private static void purchaseUpgrade(Activity activity) {
        IabManager.getInstance().purchaseUpgrade(activity, success -> {
            if (success) {
                getUpgradeSuccessDialog(activity).show();
            } else {
                Toast.makeText(activity, R.string.iab_purchase_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private static MaterialDialog getUpgradeSuccessDialog(Activity activity) {
        return new MaterialDialog.Builder(activity)
                .title(activity.getResources().getString(R.string.upgraded_title))
                .content(activity.getResources().getString(R.string.upgraded_message))
                .positiveText(R.string.restart_button)
                .onPositive((materialDialog, dialogAction) -> {
                    Intent intent = new Intent(activity, MainActivity.class);
                    ComponentName componentName = intent.getComponent();
                    Intent mainIntent = Intent.makeRestartActivityTask(componentName);
                    activity.startActivity(mainIntent);
                })
                .build();
    }
}