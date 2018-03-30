package com.simplecity.amp_library.playback.constants;

public interface InternalIntents {
    String INTERNAL_INTENT_PREFIX = "com.simplecity.shuttle";
    String PLAY_STATE_CHANGED = INTERNAL_INTENT_PREFIX + ".playstatechanged";
    String POSITION_CHANGED = INTERNAL_INTENT_PREFIX + ".positionchanged";
    String TRACK_ENDING = INTERNAL_INTENT_PREFIX + ".trackending";
    String META_CHANGED = INTERNAL_INTENT_PREFIX + ".metachanged";
    String QUEUE_CHANGED = INTERNAL_INTENT_PREFIX + ".queuechanged";
    String SHUFFLE_CHANGED = INTERNAL_INTENT_PREFIX + ".shufflechanged";
    String REPEAT_CHANGED = INTERNAL_INTENT_PREFIX + ".repeatchanged";
    String FAVORITE_CHANGED = INTERNAL_INTENT_PREFIX + ".favoritechanged";
    String SERVICE_CONNECTED = INTERNAL_INTENT_PREFIX + ".serviceconnected";
}
