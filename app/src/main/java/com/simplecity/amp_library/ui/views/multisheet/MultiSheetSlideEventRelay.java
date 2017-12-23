package com.simplecity.amp_library.ui.views.multisheet;

import android.support.design.widget.BottomSheetBehavior;

import com.jakewharton.rxrelay2.BehaviorRelay;
import com.simplecity.multisheetview.ui.view.MultiSheetView.Sheet;

import javax.inject.Inject;

import io.reactivex.Observable;

public class MultiSheetSlideEventRelay {

    private final BehaviorRelay<SlideEvent> eventRelay = BehaviorRelay.create();

    @Inject
    public MultiSheetSlideEventRelay() {
    }

    public void sendEvent(SlideEvent event) {
        eventRelay.accept(event);
    }

    public Observable<SlideEvent> getEvents() {
        return eventRelay;
    }

    public static class SlideEvent {

        @Sheet
        public final int sheet;
        @BottomSheetBehavior.State
        public final int state;
        public final float slideOffset;

        public SlideEvent(int sheet, int state, float slideOffset) {
            this.sheet = sheet;
            this.state = state;
            this.slideOffset = slideOffset;
        }

        public SlideEvent(int sheet, int state) {
            this(sheet, state, -1f);
        }

        public SlideEvent(int sheet, float slideOffset) {
            this(sheet, -1, slideOffset);
        }

        public boolean nowPlayingExpanded() {
            return sheet == Sheet.FIRST && state == BottomSheetBehavior.STATE_EXPANDED;
        }

        public boolean nowPlayingCollapsed() {
            return sheet == Sheet.FIRST && state == BottomSheetBehavior.STATE_COLLAPSED;
        }

    }

}
