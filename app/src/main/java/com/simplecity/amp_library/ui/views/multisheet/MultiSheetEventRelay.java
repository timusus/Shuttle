package com.simplecity.amp_library.ui.views.multisheet;

import com.jakewharton.rxrelay.PublishRelay;

import javax.inject.Inject;

import rx.Observable;
import test.com.multisheetview.ui.view.MultiSheetView;

public class MultiSheetEventRelay {

    private PublishRelay<MultiSheetEvent> eventRelay = PublishRelay.create();

    @Inject
    public MultiSheetEventRelay() {
    }

    public void sendEvent(MultiSheetEvent event) {
        eventRelay.call(event);
    }

    public Observable<MultiSheetEvent> getEvents() {
        return eventRelay;
    }

    public static class MultiSheetEvent {

        public @interface Action {
            int GOTO = 0;
            int HIDE = 1;
        }

        @Action int action;
        @MultiSheetView.Sheet int sheet;

        public MultiSheetEvent(int action, int sheet) {
            this.action = action;
            this.sheet = sheet;
        }
    }
}
