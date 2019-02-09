package com.simplecity.amp_library.di.app

import com.simplecity.amp_library.ShuttleApplication
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AndroidSupportInjectionModule::class,
        AppModule::class,
        AppAssistedModule::class,
        RepositoryModule::class
    ]
)
interface AppComponent : AndroidInjector<ShuttleApplication> {
    @Component.Builder
    abstract class Builder : AndroidInjector.Builder<ShuttleApplication>()
}