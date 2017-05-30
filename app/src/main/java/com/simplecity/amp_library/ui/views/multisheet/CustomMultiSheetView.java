package com.simplecity.amp_library.ui.views.multisheet;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import com.simplecity.amp_library.ShuttleApplication;

import javax.inject.Inject;

import rx.subscriptions.CompositeSubscription;
import test.com.multisheetview.ui.view.MultiSheetView;

/**
 * A custom MultiSheetView with an RXRelay for responding to expand/collapse events.
 */
public class CustomMultiSheetView extends MultiSheetView {

    @Inject MultiSheetEventRelay multiSheetEventRelay;

    private CompositeSubscription subscriptions;

    public CustomMultiSheetView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        subscriptions = new CompositeSubscription();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        ShuttleApplication.getInstance().getAppComponent().inject(this);

        subscriptions.add(multiSheetEventRelay.getEvents().subscribe(event -> {
            switch (event.action) {
                case MultiSheetEventRelay.MultiSheetEvent.Action.GOTO:
                    goToSheet(event.sheet);
                    break;
                case MultiSheetEventRelay.MultiSheetEvent.Action.HIDE:
                    hideSheet(event.sheet);
                    break;
                case MultiSheetEventRelay.MultiSheetEvent.Action.SHOW_IF_HIDDEN:
                    if (isHidden(getCurrentSheet())) {
                        goToSheet(event.sheet);
                    }
                    break;
            }
        }));
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        subscriptions.clear();
    }

}