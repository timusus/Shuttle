package com.simplecity.amp_library.ui.views.multisheet;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.util.AttributeSet;
import com.simplecity.amp_library.ui.screens.drawer.DrawerLockManager;
import com.simplecity.amp_library.ui.views.multisheet.MultiSheetSlideEventRelay.SlideEvent;
import com.simplecity.multisheetview.ui.view.MultiSheetView;
import io.reactivex.disposables.CompositeDisposable;

/**
 * A custom MultiSheetView with an RXRelay for responding to expand/collapse events.
 */
public class CustomMultiSheetView extends MultiSheetView {

    private static final String TAG = "CustomMultiSheetView";

    MultiSheetEventRelay multiSheetEventRelay;

    MultiSheetSlideEventRelay multiSheetSlideEventRelay;

    private CompositeDisposable disposables;

    DrawerLockManager.DrawerLock sheet1Lock = () -> "Sheet 1";
    DrawerLockManager.DrawerLock sheet2Lock = () -> "Sheet 2";

    public CustomMultiSheetView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        disposables = new CompositeDisposable();

        setSheetStateChangeListener(new SheetStateChangeListener() {
            @Override
            public void onSheetStateChanged(int sheet, int state) {
                if (state == BottomSheetBehavior.STATE_COLLAPSED) {
                    switch (sheet) {
                        case Sheet.FIRST:
                            DrawerLockManager.getInstance().removeDrawerLock(sheet1Lock);
                            break;
                        case Sheet.SECOND:
                            DrawerLockManager.getInstance().removeDrawerLock(sheet2Lock);
                            break;
                    }
                } else if (state == BottomSheetBehavior.STATE_EXPANDED) {
                    switch (sheet) {
                        case Sheet.FIRST:
                            DrawerLockManager.getInstance().addDrawerLock(sheet1Lock);
                            break;
                        case Sheet.SECOND:
                            DrawerLockManager.getInstance().addDrawerLock(sheet2Lock);
                            break;
                    }
                }
                multiSheetSlideEventRelay.sendEvent(new SlideEvent(sheet, state));
            }

            @Override
            public void onSlide(int sheet, float slideOffset) {
                multiSheetSlideEventRelay.sendEvent(new SlideEvent(sheet, slideOffset));
            }
        });
    }

    public void setMultiSheetEventRelay(MultiSheetEventRelay multiSheetEventRelay) {
        this.multiSheetEventRelay = multiSheetEventRelay;
    }

    public void setMultiSheetSlideEventRelay(MultiSheetSlideEventRelay multiSheetSlideEventRelay) {
        this.multiSheetSlideEventRelay = multiSheetSlideEventRelay;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        disposables.add(multiSheetEventRelay.getEvents().subscribe(event -> {
            switch (event.action) {
                case MultiSheetEventRelay.MultiSheetEvent.Action.GOTO:
                    goToSheet(event.sheet);
                    break;
                case MultiSheetEventRelay.MultiSheetEvent.Action.HIDE:
                    hide(false, true);
                    break;
                case MultiSheetEventRelay.MultiSheetEvent.Action.SHOW_IF_HIDDEN:
                    unhide(true);
                    break;
            }
        }));
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        disposables.clear();
    }
}
