package com.simplecity.amp_library.ui.screens.drawer;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.jakewharton.rxrelay2.PublishRelay;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ShuttleUtils;
import io.reactivex.Observable;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NavigationEventRelay {

    static NavigationEvent librarySelectedEvent = new NavigationEvent(NavigationEvent.Type.LIBRARY_SELECTED);
    static NavigationEvent sleepTimerSelectedEvent = new NavigationEvent(NavigationEvent.Type.SLEEP_TIMER_SELECTED);
    static NavigationEvent equalizerSelectedEvent = new NavigationEvent(NavigationEvent.Type.EQUALIZER_SELECTED);
    static NavigationEvent settingsSelectedEvent = new NavigationEvent(NavigationEvent.Type.SETTINGS_SELECTED);
    static NavigationEvent supportSelectedEvent = new NavigationEvent(NavigationEvent.Type.SUPPORT_SELECTED);

    static NavigationEvent getFoldersSelectedEvent(ShuttleApplication application, SettingsManager settingsManager) {
        return new NavigationEvent(NavigationEvent.Type.FOLDERS_SELECTED) {
            @Override
            public boolean isActionable() {
                return ShuttleUtils.isUpgraded(application, settingsManager);
            }
        };
    }

    private PublishRelay<NavigationEvent> relay = PublishRelay.create();

    @Inject
    public NavigationEventRelay() {

    }

    public void sendEvent(@NonNull NavigationEvent event) {
        relay.accept(event);
    }

    public Observable<NavigationEvent> getEvents() {
        // Delay the event a tiny bit, to allow the drawer to close.
        return relay;//.delay(250, TimeUnit.MILLISECONDS);
    }

    public static class NavigationEvent {

        public @interface Type {
            int LIBRARY_SELECTED = 0;
            int FOLDERS_SELECTED = 1;
            int SLEEP_TIMER_SELECTED = 2;
            int EQUALIZER_SELECTED = 3;
            int SETTINGS_SELECTED = 4;
            int SUPPORT_SELECTED = 5;
            int PLAYLIST_SELECTED = 6;
            int GO_TO_ARTIST = 7;
            int GO_TO_ALBUM = 8;
            int GO_TO_GENRE = 9;
        }

        @Type
        public int type;

        @Nullable
        public Object data;

        private boolean isActionable = true;

        /**
         * @param type the {@link Type of event}
         * @param data optional Object to be passed with this event
         * @param isActionable true if navigational changes should be performed in response to this NavigationEvent
         * Defaults to true.
         */
        public NavigationEvent(int type, @Nullable Object data, boolean isActionable) {
            this.type = type;
            this.data = data;
            this.isActionable = isActionable;
        }

        /**
         * @param type the {@link Type of event}
         * @param data optional Object to be passed with this event
         */
        public NavigationEvent(int type, @Nullable Object data) {
            this.type = type;
            this.data = data;
        }

        /**
         * @param type the {@link Type of event}
         */
        NavigationEvent(int type) {
            this.type = type;
        }

        public boolean isActionable() {
            return isActionable;
        }
    }
}