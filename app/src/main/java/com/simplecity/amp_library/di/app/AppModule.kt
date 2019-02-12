package com.simplecity.amp_library.di.app

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.simplecity.amp_library.ShuttleApplication
import com.simplecity.amp_library.di.app.activity.ActivityScope
import com.simplecity.amp_library.playback.MusicService
import com.simplecity.amp_library.services.ArtworkDownloadService
import com.simplecity.amp_library.ui.screens.main.MainActivity
import com.simplecity.amp_library.ui.screens.main.MainActivityModule
import com.simplecity.amp_library.ui.screens.shortcut.ShortcutTrampolineActivity
import com.simplecity.amp_library.ui.widgets.WidgetConfigureActivityExtraLarge
import com.simplecity.amp_library.ui.widgets.WidgetConfigureActivityExtraLargeModule
import com.simplecity.amp_library.ui.widgets.WidgetConfigureActivityLarge
import com.simplecity.amp_library.ui.widgets.WidgetConfigureActivityLargeModule
import com.simplecity.amp_library.ui.widgets.WidgetConfigureActivityMedium
import com.simplecity.amp_library.ui.widgets.WidgetConfigureActivityMediumModule
import com.simplecity.amp_library.ui.widgets.WidgetConfigureActivitySmall
import com.simplecity.amp_library.ui.widgets.WidgetConfigureActivitySmallModule
import com.simplecity.amp_library.utils.MediaButtonIntentReceiver
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Module(includes = [AppModuleBinds::class])
class AppModule {

    @Provides
    fun provideContext(application: ShuttleApplication): Context = application.applicationContext

    @Provides
    @Singleton
    fun provideSharedPreferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }
}

@Module(includes = [AndroidSupportInjectionModule::class])
abstract class AppModuleBinds {

    @ActivityScope
    @ContributesAndroidInjector(modules = [MainActivityModule::class])
    abstract fun mainActivityInjector(): MainActivity

    @ContributesAndroidInjector
    abstract fun musicServiceInjector(): MusicService

    @ContributesAndroidInjector
    abstract fun artworkServiceInjector(): ArtworkDownloadService

    @ContributesAndroidInjector
    abstract fun mediaButtonIntentReceiverInjector(): MediaButtonIntentReceiver

    @ActivityScope
    @ContributesAndroidInjector
    abstract fun shortcutTrampolineActivityInjector(): ShortcutTrampolineActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [WidgetConfigureActivitySmallModule::class])
    abstract fun widgetConfigureActivitySmallInjector(): WidgetConfigureActivitySmall

    @ActivityScope
    @ContributesAndroidInjector(modules = [WidgetConfigureActivityMediumModule::class])
    abstract fun widgetConfigureActivityMediumInjector(): WidgetConfigureActivityMedium

    @ActivityScope
    @ContributesAndroidInjector(modules = [WidgetConfigureActivityLargeModule::class])
    abstract fun widgetConfigureActivityLargeInjector(): WidgetConfigureActivityLarge

    @ActivityScope
    @ContributesAndroidInjector(modules = [WidgetConfigureActivityExtraLargeModule::class])
    abstract fun widgetConfigureActivityExtraLargeInjector(): WidgetConfigureActivityExtraLarge
}