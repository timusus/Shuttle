package test.com.androidnavigation.base;

import android.support.annotation.Nullable;

public interface Controller<T> {

    /**
     * @return the parent {@link NavigationController}, or null if none exists.
     */
    @Nullable
    NavigationController<T> getNavigationController();

}