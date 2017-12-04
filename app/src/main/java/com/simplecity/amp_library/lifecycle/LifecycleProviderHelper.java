package com.simplecity.amp_library.lifecycle;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.OnLifecycleEvent;

import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.subjects.BehaviorSubject;

import static android.arch.lifecycle.Lifecycle.Event.ON_ANY;

public class LifecycleProviderHelper implements LifecycleProvider, LifecycleObserver {

    private Lifecycle hostLifecycle;
    private BehaviorSubject<Lifecycle.Event> subject = BehaviorSubject.create();

    public LifecycleProviderHelper(LifecycleOwner lifecycleOwner) {
        hostLifecycle = lifecycleOwner.getLifecycle();
        hostLifecycle.addObserver(this);
    }

    @SuppressWarnings("unused")
    @OnLifecycleEvent(ON_ANY)
    void onAny(LifecycleOwner source, Lifecycle.Event event) {
        subject.onNext(event);
        if (event == Lifecycle.Event.ON_DESTROY) {
            hostLifecycle.removeObserver(this);
        }
    }


    @Override
    public Observable<Lifecycle.Event> lifecycle() {
        return subject;
    }


    @Override
    public Observable<Long> onCreated() {
        return subject.filter(event -> event == Lifecycle.Event.ON_CREATE).to(eventNotification());
    }


    @Override
    public Observable<Long> onStarted() {
        return subject.filter(event -> event == Lifecycle.Event.ON_START).to(eventNotification());
    }


    @Override
    public Observable<Long> onResumed() {
        return subject.filter(event -> event == Lifecycle.Event.ON_RESUME).to(eventNotification());
    }


    @Override
    public Observable<Long> onPaused() {
        return subject.filter(event -> event == Lifecycle.Event.ON_PAUSE).to(eventNotification());
    }


    @Override
    public Observable<Long> onDestroyed() {
        return subject.filter(event -> event == Lifecycle.Event.ON_DESTROY).to(eventNotification());
    }


    @Override
    public Observable<Long> onStopped() {
        return subject.filter(event -> event == Lifecycle.Event.ON_STOP).to(eventNotification());
    }

    private Function<Observable<Lifecycle.Event>, Observable<Long>> eventNotification() {
        return eventObservable -> eventObservable
                // take 1 or until destroy
                .take(1).takeUntil(event -> event == Lifecycle.Event.ON_DESTROY)
                .map(event -> 0L);
    }

}