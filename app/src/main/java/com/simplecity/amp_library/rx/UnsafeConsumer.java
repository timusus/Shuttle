package com.simplecity.amp_library.rx;

import io.reactivex.functions.Consumer;

/**
 * A Consumer which does not throw on error.
 */
public interface UnsafeConsumer<T> extends Consumer<T> {

    @Override
    void accept(T t);
}