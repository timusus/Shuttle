package com.simplecity.amp_library.ui.views.multisheet;


import com.jakewharton.rxrelay2.BehaviorRelay;
import com.simplecity.multisheetview.ui.view.MultiSheetView;

import javax.inject.Inject;

import io.reactivex.Observable;

public class MultiSheetSlideEventRelay {

    private BehaviorRelay<SlideEvent> eventRelay = BehaviorRelay.create();

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

        @MultiSheetView.Sheet
        public int sheet;

        public float slideOffset;

        public SlideEvent(int sheet, float slideOffset) {
            this.sheet = sheet;
            this.slideOffset = slideOffset;
        }
    }
}
