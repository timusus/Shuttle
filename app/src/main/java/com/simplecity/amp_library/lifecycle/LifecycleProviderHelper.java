package com.simplecity.amp_library.lifecycle;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.OnLifecycleEvent;

import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.subjects.BehaviorSubject;

import static android.arch.lifecycle.Lifecycle.Event.ON_ANY;

/**
 * Created by Khang NT on 11/29/17.
 * Email: khang.neon.1997@gmail.com
 */

public class LifecycleProviderHelper implements LifecycleProvider, LifecycleObserver {

    private Lifecycle mHostLifecycle;
    private BehaviorSubject<Lifecycle.Event> mSubject;

    public LifecycleProviderHelper(LifecycleOwner lifecycleOwner) {
        this.mHostLifecycle = lifecycleOwner.getLifecycle();
        this.mSubject = BehaviorSubject.create();

        mHostLifecycle.addObserver(this);
    }

    @SuppressWarnings("unused")
    @OnLifecycleEvent(ON_ANY)
    void onAny(LifecycleOwner source, Lifecycle.Event event) {
        mSubject.onNext(event);
        if (event == Lifecycle.Event.ON_DESTROY) {
            mHostLifecycle.removeObserver(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observable<Lifecycle.Event> lifecycle() {
        return mSubject;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observable<Long> onCreated() {
        return mSubject.filter(event -> event == Lifecycle.Event.ON_CREATE).to(eventNotification());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observable<Long> onStarted() {
        return mSubject.filter(event -> event == Lifecycle.Event.ON_START).to(eventNotification());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observable<Long> onResumed() {
        return mSubject.filter(event -> event == Lifecycle.Event.ON_RESUME).to(eventNotification());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observable<Long> onPaused() {
        return mSubject.filter(event -> event == Lifecycle.Event.ON_PAUSE).to(eventNotification());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observable<Long> onStopped() {
        return mSubject.filter(event -> event == Lifecycle.Event.ON_STOP).to(eventNotification());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observable<Long> onDestroyed() {
        return mSubject.filter(event -> event == Lifecycle.Event.ON_DESTROY).to(eventNotification());
    }

    private Function<Observable<Lifecycle.Event>, Observable<Long>> eventNotification() {
        return eventObservable -> eventObservable
                // take 1 or until destroy
                .take(1).takeUntil(event -> event == Lifecycle.Event.ON_DESTROY)
                .map(event -> 0L);
    }

}
