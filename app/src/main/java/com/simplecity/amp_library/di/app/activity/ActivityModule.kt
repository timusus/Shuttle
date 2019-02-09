package com.simplecity.amp_library.di.app.activity

import android.app.Activity
import android.support.v4.app.FragmentManager
import android.support.v7.app.AppCompatActivity
import dagger.Module
import dagger.Provides

@Module
class ActivityModule {

    @Provides
    @ActivityScope
    fun activity(activity: AppCompatActivity): Activity {
        return activity
    }

    @Provides
    @ActivityScope
    fun fragmentManager(activity: AppCompatActivity): FragmentManager {
        return activity.supportFragmentManager
    }
}