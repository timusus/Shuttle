package com.simplecity.amp_library.di.app

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.simplecity.amp_library.ShuttleApplication
import com.simplecity.amp_library.di.app.activity.ActivityScope
import com.simplecity.amp_library.playback.MusicService
import com.simplecity.amp_library.ui.screens.main.MainActivity
import com.simplecity.amp_library.ui.screens.main.MainActivityModule
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
}