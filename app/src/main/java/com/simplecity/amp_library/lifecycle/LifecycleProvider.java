package com.simplecity.amp_library.lifecycle;

import android.arch.lifecycle.Lifecycle;

import io.reactivex.Observable;

/**
 * Created by Khang NT on 11/29/17.
 * Email: khang.neon.1997@gmail.com
 */

public interface LifecycleProvider {

    /**
     * Get {@link Lifecycle} events as an observable stream.
     */
    Observable<Lifecycle.Event> lifecycle();

    /**
     * @return An observable stream emits 0L when component created.
     */
    Observable<Long> onCreated();

    /**
     * @return An observable stream emits 0L when component started.
     */
    Observable<Long> onStarted();

    /**
     * @return An observable stream emits 0L when component resumed.
     */
    Observable<Long> onResumed();

    /**
     * @return An observable stream emits 0L when component paused.
     */
    Observable<Long> onPaused();

    /**
     * @return An observable stream emits 0L when component stopped.
     */
    Observable<Long> onStopped();

    /**
     * @return An observable stream emits 0L when component destroyed.
     */
    Observable<Long> onDestroyed();


}
