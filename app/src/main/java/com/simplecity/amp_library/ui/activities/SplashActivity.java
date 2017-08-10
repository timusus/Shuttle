package com.simplecity.amp_library.ui.activities;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.Toast;

import com.simplecity.amp_library.R;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";

    private static final long DELAY = 1000;

    @Nullable
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Check if the Paranoid Android system property exists..
        handler = new Handler();
        if (!TextUtils.isEmpty(getSystemProperty("ro.pa.version"))) {
            // User has PA. Start MainActivity after delay.
            handler.postDelayed(() -> {
                startActivity(new Intent().setClass(SplashActivity.this, MainActivity.class));
                finish();
            }, DELAY);
        } else {
            // User doesn't have PA. Show alert & exit.
            Toast.makeText(getApplicationContext(), R.string.non_aospa, Toast.LENGTH_SHORT).show();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setNavigationBarColor(getResources().getColor(R.color.red_500));
                getWindow().setStatusBarColor(getResources().getColor(R.color.red_500));
            }
            setContentView(R.layout.splash_no_aospa);

            handler.postDelayed(SplashActivity.this::finish, DELAY);
        }
    }

    @Override
    protected void onPause() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
        super.onPause();
    }

    public String getSystemProperty(String key) {
        try {
            return (String) Class.forName("android.os.SystemProperties").getMethod("get", String.class).invoke(null, key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}