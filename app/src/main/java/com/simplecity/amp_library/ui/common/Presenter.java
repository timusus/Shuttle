package com.simplecity.amp_library.ui.common;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class Presenter<V> {

    @NonNull
    private final CompositeDisposable disposables = new CompositeDisposable();

    protected void addDisposable(@NonNull Disposable disposable) {
        this.disposables.add(disposable);
    }

    @Nullable
    private V view;

    @Nullable
    protected V getView() {
        return view;
    }

    @CallSuper
    public void bindView(@NonNull V view) {

        final V previousView = this.view;

        if (previousView != null) {
            throw new IllegalStateException("Previous view is not unbound! previousView = " + previousView);
        }

        this.view = view;
    }

    @CallSuper
    public void unbindView(@NonNull V view) {
        final V previousView = this.view;

        if (previousView == view) {
            this.view = null;
        } else {
            throw new IllegalStateException("Unexpected view! previousView = " + previousView + ", view to unbind = " + view);
        }

        // Unsubscribe all disposables that need to be unsubscribed in this lifecycle state.
        disposables.clear();
    }
}
