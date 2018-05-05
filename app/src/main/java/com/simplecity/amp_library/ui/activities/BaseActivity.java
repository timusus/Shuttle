package com.simplecity.amp_library.ui.activities;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.Toast;
import com.afollestad.aesthetic.AestheticActivity;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.Purchase;
import com.greysonparrelli.permiso.Permiso;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.billing.BillingManager;
import com.simplecity.amp_library.constants.Config;
import com.simplecity.amp_library.playback.MediaManager;
import com.simplecity.amp_library.playback.constants.InternalIntents;
import com.simplecity.amp_library.ui.dialog.UpgradeDialog;
import com.simplecity.amp_library.utils.MusicServiceConnectionUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import java.util.List;

import static android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;

public abstract class BaseActivity extends AestheticActivity implements ServiceConnection {

    @Nullable
    private MusicServiceConnectionUtils.ServiceToken token;

    @Nullable
    private BillingManager billingManager;

    protected MediaManager mediaManager = new MusicUtils();

    @CallSuper
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Permiso.getInstance().setActivity(this);

        Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
            @Override
            public void onPermissionResult(Permiso.ResultSet resultSet) {
                if (resultSet.areAllPermissionsGranted()) {
                    bindService();
                } else {
                    Toast.makeText(BaseActivity.this, "Permission check failed", Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                callback.onRationaleProvided();
            }
        }, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.WAKE_LOCK);

        billingManager = new BillingManager(this, new BillingManager.BillingUpdatesListener() {
            @Override
            public void onPurchasesUpdated(List<Purchase> purchases) {
                for (Purchase purchase : purchases) {
                    if (purchase.getSku().equals(Config.SKU_PREMIUM)) {
                        ShuttleApplication.getInstance().setIsUpgraded(true);
                    }
                }
            }

            @Override
            public void onPremiumPurchaseCompleted() {
                ShuttleApplication.getInstance().setIsUpgraded(true);
                UpgradeDialog.getUpgradeSuccessDialog(BaseActivity.this).show();
            }

            @Override
            public void onPremiumPurchaseRestored() {
                ShuttleApplication.getInstance().setIsUpgraded(true);
                Toast.makeText(BaseActivity.this, R.string.iab_purchase_restored, Toast.LENGTH_SHORT).show();
            }
        });

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onResume() {
        keepScreenOn(SettingsManager.getInstance().keepScreenOn());
        super.onResume();

        if (token == null) {
            bindService();
        }

        Permiso.getInstance().setActivity(this);

        if (billingManager != null && billingManager.getBillingClientResponseCode() == BillingClient.BillingResponse.OK) {
            billingManager.queryPurchases();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Permiso.getInstance().onRequestPermissionResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onDestroy() {
        unbindService();

        if (billingManager != null) {
            billingManager.destroy();
        }

        super.onDestroy();
    }

    @Nullable
    public BillingManager getBillingManager() {
        return billingManager;
    }

    void bindService() {
        token = MusicServiceConnectionUtils.bindToService(this, this);
    }

    void unbindService() {
        if (token != null) {
            MusicServiceConnectionUtils.unbindFromService(token);
            token = null;
        }
    }

    @Override
    @CallSuper
    public void onServiceConnected(ComponentName name, IBinder service) {
        sendBroadcast(new Intent(InternalIntents.SERVICE_CONNECTED));
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        unbindService();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //Fix for issue on LG devices
        if (keyCode == KeyEvent.KEYCODE_MENU && "LGE".equalsIgnoreCase(Build.BRAND)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        //Fix for issue on LG devices
        if (keyCode == KeyEvent.KEYCODE_MENU && "LGE".equalsIgnoreCase(Build.BRAND)) {
            openOptionsMenu();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void keepScreenOn(boolean on) {
        final Window window = getWindow();
        if (on) {
            window.addFlags(FLAG_KEEP_SCREEN_ON);
        } else {
            window.clearFlags(FLAG_KEEP_SCREEN_ON);
        }
    }

    protected abstract String screenName();

    public MediaManager getMusicUtils() {
        return mediaManager;
    }
}