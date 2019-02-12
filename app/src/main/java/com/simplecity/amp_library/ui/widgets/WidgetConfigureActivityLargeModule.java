package com.simplecity.amp_library.ui.widgets;

import android.support.v7.app.AppCompatActivity;
import com.simplecity.amp_library.billing.BillingManager;
import com.simplecity.amp_library.di.app.activity.ActivityModule;
import com.simplecity.amp_library.di.app.activity.ActivityScope;
import com.simplecity.amp_library.di.app.activity.fragment.FragmentScope;
import com.simplecity.amp_library.ui.screens.widgets.WidgetFragment;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;

@Module(includes = ActivityModule.class)
public abstract class WidgetConfigureActivityLargeModule {

    @Binds
    @ActivityScope
    abstract AppCompatActivity appCompatActivity(WidgetConfigureActivityLarge activity);

    @Provides
    static BillingManager.BillingUpdatesListener provideBillingUpdatesListener(WidgetConfigureActivityLarge activity) {
        return activity;
    }

    @FragmentScope
    @ContributesAndroidInjector(modules = WidgetFragmentModule.class)
    abstract WidgetFragment widgetFragmentInjector();
}