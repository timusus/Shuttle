package com.simplecity.amp_library.utils

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.content.Context
import android.content.Intent
import android.os.Build
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import java.util.concurrent.TimeUnit

class ResumingServiceManager(val lifecycle: Lifecycle, val analyticsManager: AnalyticsManager) : LifecycleObserver {

    init {
        lifecycle.addObserver(this)
    }

    val disposable: CompositeDisposable = CompositeDisposable()

    fun startService(context: Context, intent: Intent, completion: (() -> Unit)? = null) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            context.startService(intent)
            completion?.invoke()
        } else {
            Single.just(true)
                    .delaySubscription(300, TimeUnit.MILLISECONDS)
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                            onSuccess = {
                                analyticsManager.dropBreadcrumb("ResumingServiceManager", "Starting service after 300ms delay")
                                context.startService(intent)
                                completion?.invoke()
                            }

                    ).addTo(disposable)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun stopped() {
        disposable.clear()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun destroy() {
        lifecycle.removeObserver(this)
    }
}