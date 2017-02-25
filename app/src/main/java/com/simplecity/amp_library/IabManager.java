package com.simplecity.amp_library;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.android.vending.billing.utils.IabHelper;
import com.android.vending.billing.utils.IabResult;
import com.android.vending.billing.utils.Inventory;
import com.android.vending.billing.utils.Purchase;
import com.simplecity.amp_library.constants.Config;

public class IabManager {

    private static final String TAG = "IabManager";

    public static IabManager sInstance;

    public static IabManager getInstance() {
        if (sInstance == null) {
            sInstance = new IabManager();
        }
        return sInstance;
    }

    public IabHelper iabHelper;
    Context mApplicationContext;
    SharedPreferences mPrefs;
    public String base64EncodedPublicKey;

    private IabManager() {

        mApplicationContext = ShuttleApplication.getInstance();

        mPrefs = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);

        base64EncodedPublicKey = mApplicationContext.getResources().getString(R.string.base64EncodedPublicKey);

        iabHelper = new IabHelper(mApplicationContext, base64EncodedPublicKey);

        setup(false);
    }

    public void setup(final boolean toast) {
        iabHelper.startSetup(result -> {

            if (!result.isSuccess()) {
                Log.e(TAG, "In-app Billing setup failed: " + result);
                return;
            }

            // Have we been disposed of in the meantime? If so, quit.
            if (iabHelper == null) return;

            Log.d(TAG, "In-app Billing is set up OK");
            iabHelper.queryInventoryAsync(toast ? mReceivedInventoryListenerWithToast : mReceivedInventoryListener);
        });
    }

    public void restorePurchases() {

        if (iabHelper.setupDone) {
            iabHelper.queryInventoryAsync(mReceivedInventoryListenerWithToast);
        } else {
            setup(true);
        }
    }

    private final IabHelper.QueryInventoryFinishedListener mReceivedInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {

            // Have we been disposed of in the meantime? If so, quit.
            if (iabHelper == null) return;

            if (result.isFailure()) {
                Log.e(TAG, "Failed to query inventory: " + result);
                return;
            }

            // Do we have the premium upgrade?
            Purchase premiumPurchase = inventory.getPurchase(Config.SKU_PREMIUM);
            boolean isPremium = (premiumPurchase != null /*&& verifyDeveloperPayload(premiumPurchase)*/);
            mPrefs.edit().putBoolean("pref_theme_gold", isPremium).apply();
            ShuttleApplication.getInstance().setIsUpgraded(isPremium);
        }
    };

    private final IabHelper.QueryInventoryFinishedListener mReceivedInventoryListenerWithToast = new IabHelper.QueryInventoryFinishedListener() {
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
            boolean isPremium = premiumPurchase != null;
            mPrefs.edit().putBoolean("pref_theme_gold", isPremium).apply();
            ShuttleApplication.getInstance().setIsUpgraded(isPremium);

            if (!wasPremium && premiumPurchase != null) {
                //We didn't have a purchase, but we do now
                Toast.makeText(mApplicationContext, R.string.iab_purchase_restored, Toast.LENGTH_LONG).show();
            } else if (premiumPurchase == null) {
                //No purchases found
                Toast.makeText(mApplicationContext, R.string.iab_purchase_not_found, Toast.LENGTH_LONG).show();
            } else {
                //We did have a purchase, and we still do
                //No purchases found
                Toast.makeText(mApplicationContext, R.string.iab_already_upgraded, Toast.LENGTH_LONG).show();
            }
        }
    };
}
