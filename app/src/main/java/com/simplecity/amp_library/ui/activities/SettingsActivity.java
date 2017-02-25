package com.simplecity.amp_library.ui.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import com.android.vending.billing.utils.IabHelper;
import com.android.vending.billing.utils.IabResult;
import com.android.vending.billing.utils.Purchase;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.simplecity.amp_library.IabManager;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.constants.Config;
import com.simplecity.amp_library.ui.fragments.SettingsFragment;
import com.simplecity.amp_library.utils.ColorUtils;
import com.simplecity.amp_library.utils.DialogUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.ThemeUtils;

import static android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;

public class SettingsActivity extends BaseActivity {

    private static final String TAG = "SettingsActivity";

    public static final String EXTRA_SUPPORT = "support";

    private SystemBarTintManager mTintManager;

    SharedPreferences mPrefs;

    // (arbitrary) request code for the purchase flow
    static final int RC_REQUEST = 10001;

    @SuppressLint("InlinedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (!ShuttleUtils.isUpgraded() && !ShuttleUtils.isAmazonBuild()) {
            IabManager.getInstance();
        }

        ThemeUtils.setTheme(this);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (!ShuttleUtils.hasLollipop() && ShuttleUtils.hasKitKat()) {
            getWindow().setFlags(FLAG_TRANSLUCENT_STATUS, FLAG_TRANSLUCENT_STATUS);
            mTintManager = new SystemBarTintManager(this);
        }
        if (!ShuttleUtils.hasKitKat()) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        }

        if (SettingsManager.getInstance().canTintNavBar()) {
            getWindow().setNavigationBarColor(ColorUtils.getPrimaryColorDark(this));
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ThemeUtils.themeActionBar(this);
        ThemeUtils.themeStatusBar(this, mTintManager);

        if (savedInstanceState == null) {
            if (!getIntent().getBooleanExtra(EXTRA_SUPPORT, false)) {
                getSupportActionBar().setTitle(getString(R.string.settings));
                getSupportFragmentManager().beginTransaction().replace(R.id.main_container, new SettingsFragment()).commit();
            } else {
                getSupportActionBar().setTitle(getString(R.string.pref_title_support));
                getSupportFragmentManager().beginTransaction().replace(R.id.main_container, SettingsFragment.newInstance(R.xml.settings_support)).commit();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {

                if (getSupportFragmentManager().getBackStackEntryCount() != 0) {
                    getSupportFragmentManager().popBackStackImmediate();
                } else {
                    finish();
                }

                return true;
            }
        }
        return false;
    }

    public void swapSettingsFragment(int preferenceResId) {
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.main_container, SettingsFragment.newInstance(preferenceResId));
        ft.addToBackStack(null);
        ft.commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (IabManager.getInstance().iabHelper == null) return;

        if (!IabManager.getInstance().iabHelper.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {

            // if we were disposed of in the meantime, quit.
            if (IabManager.getInstance().iabHelper == null) return;

            if (result.isFailure()) {
                Log.e(TAG, "Error purchasing: " + result);
                return;
            }

            String sku = purchase.getSku();

            if (sku.equals(Config.SKU_PREMIUM)) {
                mPrefs.edit().putBoolean("pref_theme_gold", true).apply();
                ShuttleApplication.getInstance().setIsUpgraded(true);
                DialogUtils.showUpgradeThankyouDialog(SettingsActivity.this);
            }
        }
    };

    public void purchasePremiumUpgrade() {
        try {
            IabManager.getInstance().iabHelper.launchPurchaseFlow(this, Config.SKU_PREMIUM, RC_REQUEST,
                    mPurchaseFinishedListener, "");
        } catch (IllegalStateException ignored) {
            Toast.makeText(this, R.string.iab_purchase_failed, Toast.LENGTH_SHORT).show();
        }
    }

    public void restorePurchases() {
        IabManager.getInstance().restorePurchases();
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}
