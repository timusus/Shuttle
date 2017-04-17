package com.simplecity.amp_library.ui.activities;

import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.greysonparrelli.permiso.Permiso;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ui.fragments.MainController;

import test.com.androidnavigation.base.NavigationController;
import test.com.androidnavigation.fragment.BackPressHandler;

public class MainActivity extends AppCompatActivity implements
        ToolbarListener,
        BackPressHandler {

    private NavigationController backPressListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Permiso.getInstance().setActivity(this);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.container, MainController.newInstance())
                    .commit();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {

            if (backPressListener != null && backPressListener.consumeBackPress()) {
                return;
            }

            super.onBackPressed();
        }
    }

    @Override
    public void toolbarAttached(Toolbar toolbar) {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
    }

    @Override
    public void setBackPressListener(NavigationController listener) {
        backPressListener = listener;
    }
}
