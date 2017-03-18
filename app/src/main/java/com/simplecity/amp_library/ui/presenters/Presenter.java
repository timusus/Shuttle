package com.simplecity.amp_library.ui.presenters;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

public class Presenter<V> {

    @NonNull
    private final CompositeSubscription subscriptions = new CompositeSubscription();

    protected void addSubcscription(@NonNull Subscription subscription) {
        subscriptions.add(subscription);
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

        // Unsubscribe all subscriptions that need to be unsubscribed in this lifecycle state.
        subscriptions.clear();
    }

}
