package com.simplecity.amp_library.playback.constants;

public interface PlayerHandler {
    int TRACK_ENDED = 1;
    int RELEASE_WAKELOCK = 2;
    int SERVER_DIED = 3;
    int FOCUS_CHANGE = 4;
    int FADE_DOWN = 5;
    int FADE_UP = 6;
    int TRACK_WENT_TO_NEXT = 7;
    int FADE_DOWN_STOP = 9;
    int GO_TO_NEXT = 10;
    int GO_TO_PREV = 11;
    int SHUFFLE_ALL = 12;
}
