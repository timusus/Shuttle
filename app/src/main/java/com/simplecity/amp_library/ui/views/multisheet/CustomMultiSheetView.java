package com.simplecity.amp_library.ui.views.multisheet;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.ui.drawer.DrawerLockManager;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import javax.inject.Inject;

import rx.subscriptions.CompositeSubscription;
import test.com.multisheetview.ui.view.MultiSheetView;

/**
 * A custom MultiSheetView with an RXRelay for responding to expand/collapse events.
 */
public class CustomMultiSheetView extends MultiSheetView {

    @Inject MultiSheetEventRelay multiSheetEventRelay;

    private CompositeSubscription subscriptions;

    private DrawerLockManager.DrawerLock sheet1Lock = () -> "Sheet 1";
    private DrawerLockManager.DrawerLock sheet2Lock = () -> "Sheet 2";

    public CustomMultiSheetView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        subscriptions = new CompositeSubscription();

        setSheetStateChangeListener((sheet, state) -> {
            if (state == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                switch (sheet) {
                    case Sheet.FIRST:
                        DrawerLockManager.getInstance().removeDrawerLock(sheet1Lock);
                        break;
                    case Sheet.SECOND:
                        DrawerLockManager.getInstance().removeDrawerLock(sheet2Lock);
                        break;
                }
            } else if (state == SlidingUpPanelLayout.PanelState.EXPANDED) {
                switch (sheet) {
                    case Sheet.FIRST:
                        DrawerLockManager.getInstance().addDrawerLock(sheet1Lock);
                        break;
                    case Sheet.SECOND:
                        DrawerLockManager.getInstance().addDrawerLock(sheet2Lock);
                        break;
                }
            }
        });
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