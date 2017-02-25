package com.simplecity.amp_library.ui.activities;

import android.Manifest;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.Toast;

import com.greysonparrelli.permiso.Permiso;
import com.simplecity.amp_library.interfaces.ThemeCallbacks;
import com.simplecity.amp_library.utils.MusicServiceConnectionUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ThemeUtils;

import static android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;

public abstract class BaseActivity extends AppCompatActivity implements
        ServiceConnection,
        ThemeCallbacks {

    private SharedPreferences mPreferences;

    private MusicServiceConnectionUtils.ServiceToken mToken;

    @CallSuper
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Permiso.getInstance().setActivity(this);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mPreferences.registerOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);

        Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
            @Override
            public void onPermissionResult(Permiso.ResultSet resultSet) {
                if (resultSet.areAllPermissionsGranted()) {
                    bindToService();
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
    }

    void bindToService() {
        mToken = MusicServiceConnectionUtils.bindToService(this, this);
    }

    @Override
    protected void onResume() {
        keepScreenOn(SettingsManager.getInstance().keepScreenOn());
        super.onResume();

        Permiso.getInstance().setActivity(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Permiso.getInstance().onRequestPermissionResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onDestroy() {

        if (mToken != null) {
            MusicServiceConnectionUtils.unbindFromService(mToken);
            mToken = null;
        }

        mPreferences.unregisterOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);

        super.onDestroy();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

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

    private SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferenceChangeListener = (sharedPreferences, key) -> {
        if (key.equals("pref_theme_highlight_color")
                || key.equals("pref_theme_accent_color")
                || key.equals("pref_theme_white_accent")) {
            themeColorChanged();
        }
        if (key.equals("pref_nav_bar")) {
            navBarThemeChanged(SettingsManager.getInstance().canTintNavBar());
        }
    };

    @Override
    public void themeColorChanged() {
        navBarThemeChanged(SettingsManager.getInstance().canTintNavBar());
    }

    @Override
    public void navBarThemeChanged(boolean canTheme) {
        ThemeUtils.themeNavBar(this, canTheme);
    }

    protected abstract String screenName();
}
