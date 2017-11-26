package com.simplecity.amp_library;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.vending.billing.utils.IabHelper;
import com.android.vending.billing.utils.IabResult;
import com.android.vending.billing.utils.Inventory;
import com.android.vending.billing.utils.Purchase;
import com.simplecity.amp_library.constants.Config;
import com.simplecity.amp_library.rx.UnsafeConsumer;
import com.simplecity.amp_library.ui.activities.MainActivity;
import com.simplecity.amp_library.utils.AnalyticsManager;
import com.simplecity.amp_library.utils.LogUtils;

public class IabManager {

    private static final String TAG = "IabManager";

    public static IabManager instance;

    public static int REQUEST_CODE = 100;

    public static IabManager getInstance() {
        if (instance == null) {
            instance = new IabManager();
        }
        return instance;
    }

    public IabHelper iabHelper;

    private IabManager() {

        String base64EncodedPublicKey = ShuttleApplication.getInstance().getResources().getString(R.string.base64EncodedPublicKey);

        iabHelper = new IabHelper(ShuttleApplication.getInstance(), base64EncodedPublicKey);

        setup(null);
    }

    private void setup(@Nullable UnsafeConsumer<Integer> messageCallback) {
        iabHelper.startSetup(result -> {

            if (!result.isSuccess()) {
                Log.e(TAG, "In-app Billing setup failed: " + result);
                return;
            }

            // Have we been disposed of in the meantime? If so, quit.
            if (iabHelper == null) return;

            Log.d(TAG, "In-app Billing is set up");
            iabHelper.queryInventoryAsync(new QueryFinishedListener(iabHelper, messageCallback));
        });
    }

    public void purchaseUpgrade(Activity activity, UnsafeConsumer<Boolean> successHandler) {
        AnalyticsManager.logUpgrade(AnalyticsManager.UpgradeType.UPGRADE);

        try {
            iabHelper.launchPurchaseFlow(activity, Config.SKU_PREMIUM, REQUEST_CODE, (result, purchase) -> {
                // if we were disposed of in the meantime, quit.
                if (iabHelper == null) return;

                if (result.isFailure()) {
                    LogUtils.logError("IabManager", "Purchase result failure: " + result.getMessage());
                    successHandler.accept(false);
                    return;
                }

                String sku = purchase.getSku();

                if (sku.equals(Config.SKU_PREMIUM)) {
                    setIsUpgraded(true);
                    successHandler.accept(true);
                }
            }, "");
        } catch (IllegalStateException error) {
            LogUtils.logException(TAG, "Error purchasing", error);
            successHandler.accept(false);
        }
    }

    public void restorePurchases(@Nullable UnsafeConsumer<Integer> messageCallback) {
        if (iabHelper.setupDone) {
            iabHelper.queryInventoryAsync(new QueryFinishedListener(iabHelper, messageCallback));
        } else {
            setup(messageCallback);
        }
    }

    static void setIsUpgraded(boolean isUpgraded) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ShuttleApplication.getInstance());

        sharedPreferences.edit()
                .putBoolean("pref_theme_gold", isUpgraded)
                .apply();

        ShuttleApplication.getInstance().setIsUpgraded(isUpgraded);
    }

    static class QueryFinishedListener implements IabHelper.QueryInventoryFinishedListener {

        private IabHelper iabHelper;

        @Nullable
        private UnsafeConsumer<Integer> messageCallback;

        public QueryFinishedListener(IabHelper iabHelper, @Nullable UnsafeConsumer<Integer> messageCallback) {
            this.iabHelper = iabHelper;
            this.messageCallback = messageCallback;
        }

        @Override
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            // Have we been disposed of in the meantime? If so, quit.
            if (iabHelper == null) return;

            if (result.isFailure()) {
                Log.e(TAG, "Failed to query inventory: " + result);
                return;
            }

            // Do we have the premium upgrade?
            Purchase premiumPurchase = inventory.getPurchase(Config.SKU_PREMIUM);
            boolean wasPremium = ShuttleApplication.getInstance().getIsUpgraded();

            setIsUpgraded(premiumPurchase != null);

            if (messageCallback != null) {
                if (!wasPremium && premiumPurchase != null) {
                    //We didn't have a purchase, but we do now
                    messageCallback.accept(R.string.iab_purchase_restored);
                } else if (premiumPurchase == null) {
                    //No purchases found
                    messageCallback.accept(R.string.iab_purchase_not_found);
                } else {
                    //We did have a purchase, and we still do
                    messageCallback.accept(R.string.iab_already_upgraded);
                }
            }
        }
    }
}