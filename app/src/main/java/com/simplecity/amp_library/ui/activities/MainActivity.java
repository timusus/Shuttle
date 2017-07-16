package com.simplecity.amp_library.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.afollestad.aesthetic.Aesthetic;
import com.greysonparrelli.permiso.Permiso;
import com.simplecity.amp_library.IabManager;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ui.drawer.DrawerProvider;
import com.simplecity.amp_library.ui.fragments.MainController;

import java.util.ArrayList;
import java.util.List;

import test.com.androidnavigation.fragment.BackPressHandler;
import test.com.androidnavigation.fragment.BackPressListener;

public class MainActivity extends BaseCastActivity implements
        ToolbarListener,
        BackPressHandler,
        DrawerProvider {

    private static final String TAG = "MainActivity";

    private List<BackPressListener> backPressListeners = new ArrayList<>();

    private DrawerLayout drawerLayout;

    private View navigationView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If we haven't set any defaults, do that now
        if (Aesthetic.isFirstTime(this)) {
            Aesthetic.get()
                    .activityTheme(R.style.AppTheme_Light)
                    .isDark(false)
                    .colorPrimaryRes(R.color.blue_500)
                    .colorAccentRes(R.color.amber_300)
                    .colorStatusBarAuto()
                    .apply();
        }

        setContentView(R.layout.activity_main);

        Permiso.getInstance().setActivity(this);

        navigationView = findViewById(R.id.navView);

        //Ensure the drawer draws a content scrim over the status bar.
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerLayout.setOnApplyWindowInsetsListener((view, windowInsets) -> {
            navigationView.dispatchApplyWindowInsets(windowInsets);
            return windowInsets.replaceSystemWindowInsets(0, 0, 0, 0);
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.mainContainer, MainController.newInstance())
                    .commit();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (IabManager.getInstance().iabHelper == null) return;

        if (!IabManager.getInstance().iabHelper.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            if (!backPressListeners.isEmpty()) {
                for (int i = backPressListeners.size() - 1; i >= 0; i--) {
                    BackPressListener backPressListener = backPressListeners.get(i);
                    if (backPressListener.consumeBackPress()) {
                        return;
                    }
                }
            }
            super.onBackPressed();
        }
    }

    @Override
    public void toolbarAttached(Toolbar toolbar) {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, 0);
            }
        };
        drawer.addDrawerListener(toggle);
        toggle.syncState();
    }

    @Override
    public void addBackPressListener(@NonNull BackPressListener listener) {
        if (!backPressListeners.contains(listener)) {
            backPressListeners.add(listener);
        }
    }

    @Override
    public void removeBackPressListener(@NonNull BackPressListener listener) {
        if (backPressListeners.contains(listener)) {
            backPressListeners.remove(listener);
        }
    }

    @Override
    protected String screenName() {
        return "MainActivity";
    }

    @Override
    public DrawerLayout getDrawerLayout() {
        return drawerLayout;
    }

    @Nullable
    @Override
    public String key() {
        return "main_activity";
    }
}