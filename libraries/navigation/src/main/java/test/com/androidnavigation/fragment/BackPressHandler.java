package test.com.androidnavigation.fragment;

import android.support.annotation.NonNull;

import test.com.androidnavigation.base.NavigationController;

/**
 * Classes which are capable of handling the Android back-button press (such as {@link android.app.Activity})
 * should implement this method to allow back-presses to be propagated to the NavigationController, which then
 * gets an opportunity to consume the back press event via {@link NavigationController#consumeBackPress()}
 *
 * @see {@link BaseNavigationController#onResume()}
 */
public interface BackPressHandler {

    void addBackPressListener(@NonNull BackPressListener listener);

    void removeBackPressListener(@NonNull BackPressListener listener);
}
