package com.simplecity.amp_library.ui.drawer;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.jakewharton.rxrelay.PublishRelay;

import javax.inject.Inject;

import rx.Observable;

public class DrawerEventRelay {

    static DrawerEvent librarySelectedEvent = new DrawerEvent(DrawerEvent.Type.LIBRARY_SELECTED);
    static DrawerEvent foldersSelectedEvent = new DrawerEvent(DrawerEvent.Type.FOLDERS_SELECTED);
    static DrawerEvent equalizerSelectedEvent = new DrawerEvent(DrawerEvent.Type.EQUALIZER_SELECTED);
    static DrawerEvent settingsSelectedEvent = new DrawerEvent(DrawerEvent.Type.SETTINGS_SELECTED);
    static DrawerEvent supportSelectedEvent = new DrawerEvent(DrawerEvent.Type.SUPPORT_SELECTED);

    private PublishRelay<DrawerEvent> relay = PublishRelay.create();

    @Inject
    public DrawerEventRelay() {

    }

    void sendEvent(@NonNull DrawerEvent event) {
        relay.call(event);
    }

    public Observable<DrawerEvent> getEvents() {
        // Delay the event a tiny bit, to allow the drawer to close.
        return relay;//.delay(250, TimeUnit.MILLISECONDS);
    }

    public static class DrawerEvent {

        public @interface Type {
            int LIBRARY_SELECTED = 0;
            int FOLDERS_SELECTED = 1;
            int EQUALIZER_SELECTED = 5;
            int SETTINGS_SELECTED = 2;
            int SUPPORT_SELECTED = 3;
            int PLAYLIST_SELECTED = 4;
        }

        @Type public int type;

        @Nullable public Object data;

        /**
         * @param type the {@link Type of event}
         * @param data optional Object to be passed with this event
         */
        DrawerEvent(int type, @Nullable Object data) {
            this.type = type;
            this.data = data;
        }

        /**
         * @param type the {@link Type of event}
         */
        DrawerEvent(int type) {
            this.type = type;
        }
    }
}