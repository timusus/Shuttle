package com.simplecity.amp_library.ui.dialog;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;
import com.afollestad.materialdialogs.MaterialDialog;
import com.android.billingclient.api.BillingClient;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.billing.BillingManager;
import com.simplecity.amp_library.constants.Config;
import com.simplecity.amp_library.ui.activities.BaseActivity;
import com.simplecity.amp_library.ui.activities.MainActivity;
import com.simplecity.amp_library.utils.ShuttleUtils;

public class UpgradeDialog {

    private static final String TAG = "UpgradeDialog";

    private UpgradeDialog() {
        //no instance
    }

    public static MaterialDialog getUpgradeDialog(@NonNull Activity activity) {
        return new MaterialDialog.Builder(activity)
                .title(activity.getResources().getString(R.string.get_pro_title))
                .content(activity.getResources().getString(R.string.upgrade_dialog_message))
                .positiveText(R.string.btn_upgrade)
                .onPositive((dialog, which) -> {
                    if (ShuttleUtils.isAmazonBuild()) {
                        Intent storeIntent = ShuttleUtils.getShuttleStoreIntent("com.simplecity.amp_pro");
                        if (storeIntent.resolveActivity(ShuttleApplication.getInstance().getPackageManager()) != null) {
                            activity.startActivity(storeIntent);
                        } else {
                            activity.startActivity(ShuttleUtils.getShuttleWebIntent("com.simplecity.amp_pro"));
                        }
                    } else {
                        purchaseUpgrade(activity);
                    }
                })
                .negativeText(R.string.get_pro_button_no)
                .build();
    }

    private static void purchaseUpgrade(@NonNull Activity activity) {
        if (!(activity instanceof BaseActivity)) {
            Log.e(TAG, "Purchase may only be initiated with a BaseActivity");
            return;
        }
        BillingManager billingManager = ((BaseActivity) activity).getBillingManager();
        if (billingManager != null) {
            billingManager.initiatePurchaseFlow(Config.SKU_PREMIUM, BillingClient.SkuType.INAPP);
        }
    }

    public static MaterialDialog getUpgradeSuccessDialog(@NonNull Activity activity) {
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