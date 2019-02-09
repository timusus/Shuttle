package com.simplecity.amp_library.billing;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.Nullable;
import android.util.Log;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.simplecity.amp_library.constants.Config;
import com.simplecity.amp_library.rx.UnsafeAction;
import com.simplecity.amp_library.utils.LogUtils;
import java.util.List;
import javax.inject.Inject;

public class BillingManager implements PurchasesUpdatedListener {

    private static final String TAG = "BillingManager";

    public static final int BILLING_MANAGER_NOT_INITIALIZED = -1;

    public interface BillingUpdatesListener {
        void onPurchasesUpdated(List<Purchase> purchases);

        void onPremiumPurchaseCompleted();

        void onPremiumPurchaseRestored();
    }

    private Activity activity;

    private BillingUpdatesListener updatesListener;

    @Nullable
    private BillingClient billingClient;

    boolean serviceConnected = false;

    int billingClientResponseCode = BILLING_MANAGER_NOT_INITIALIZED;

    private boolean purchaseFlowInitiated = false;
    private boolean restorePurchasesInitiated = false;

    @Inject
    public BillingManager(Activity activity, BillingUpdatesListener updatesListener) {

        this.activity = activity;
        this.updatesListener = updatesListener;

        billingClient = BillingClient.newBuilder(activity)
                .setListener(this)
                .build();

        startServiceConnection(this::queryPurchases);
    }

    private void startServiceConnection(UnsafeAction executeOnSuccess) {
        if (billingClient != null) {
            billingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(int responseCode) {
                    if (responseCode == BillingClient.BillingResponse.OK) {
                        serviceConnected = true;
                        executeOnSuccess.run();
                    }
                    billingClientResponseCode = responseCode;
                }

                @Override
                public void onBillingServiceDisconnected() {
                    // Try to restart the connection on the next request to
                    // Google Play by calling the startConnection() method.
                    serviceConnected = false;
                }
            });
        }
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onPurchasesUpdated(int resultCode, @Nullable List<Purchase> purchases) {
        if (resultCode == BillingClient.BillingResponse.OK && purchases != null) {
            Purchase premiumPurchase = null;
            for (Purchase purchase : purchases) {
                if (purchase.getSku().equals(Config.SKU_PREMIUM)) {
                    premiumPurchase = purchase;
                }
            }
            if (purchaseFlowInitiated || restorePurchasesInitiated) {
                if (premiumPurchase != null) {
                    if (purchaseFlowInitiated) {
                        updatesListener.onPremiumPurchaseCompleted();
                        purchaseFlowInitiated = false;
                    }
                    if (restorePurchasesInitiated) {
                        updatesListener.onPremiumPurchaseRestored();
                        restorePurchasesInitiated = false;
                    }
                }
            } else {
                updatesListener.onPurchasesUpdated(purchases);
            }
        } else if (resultCode == BillingClient.BillingResponse.USER_CANCELED) {
            Log.i(TAG, "onPurchasesUpdated() - user cancelled the purchase flow - skipping");
        } else {
            LogUtils.logException(TAG, String.format("onPurchasesUpdated() got unknown resultCode: %d", resultCode), null);
        }
    }

    public void queryPurchases() {
        UnsafeAction queryAction = () -> {
            if (billingClient == null) return;
            Purchase.PurchasesResult purchasesResult = billingClient.queryPurchases(BillingClient.SkuType.INAPP);
            if (purchasesResult.getResponseCode() == BillingClient.BillingResponse.OK) {
                onPurchasesUpdated(BillingClient.BillingResponse.OK, purchasesResult.getPurchasesList());
            } else {
                LogUtils.logException(TAG, "Query purchases() got an unknown response code: " + purchasesResult.getResponseCode(), null);
            }
        };

        if (serviceConnected) {
            queryAction.run();
        } else {
            startServiceConnection(queryAction);
        }
    }

    /**
     * Start a purchase or subscription replace flow
     */
    public void initiatePurchaseFlow(final String skuId, final @BillingClient.SkuType String billingType) {
        UnsafeAction purchaseFlowRequest = () -> {
            BillingFlowParams purchaseParams = BillingFlowParams.newBuilder()
                    .setSku(skuId)
                    .setType(billingType)
                    .build();
            if (billingClient != null) {
                billingClient.launchBillingFlow(activity, purchaseParams);
                purchaseFlowInitiated = true;
            }
        };

        if (serviceConnected) {
            purchaseFlowRequest.run();
        } else {
            startServiceConnection(purchaseFlowRequest);
        }
    }

    public void restorePurchases() {
        restorePurchasesInitiated = true;
        queryPurchases();
    }

    /**
     * Returns the value Billing client response code or BILLING_MANAGER_NOT_INITIALIZED if the
     * client connection response was not received yet.
     */
    public int getBillingClientResponseCode() {
        return billingClientResponseCode;
    }

    public void destroy() {
        Log.d(TAG, "Destroying the manager.");

        if (billingClient != null && billingClient.isReady()) {
            billingClient.endConnection();
            billingClient = null;
        }
    }
}
