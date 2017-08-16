package com.simplecity.amp_library.rx;

import java.util.concurrent.Callable;

/**
 * A callable which does not throw on error.
 */
public interface UnsafeCallable<T> extends Callable<T> {

    @Override
    T call();
}