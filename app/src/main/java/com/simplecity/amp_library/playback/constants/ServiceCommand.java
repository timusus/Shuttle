package com.simplecity.amp_library.playback.constants;

public interface ServiceCommand {
    String COMMAND = "com.simplecityapps.shuttle.service_command";
    String TOGGLE_PLAYBACK = COMMAND + ".toggle_playback";
    String PAUSE = COMMAND + ".pause";
    String PLAY = COMMAND + ".play";
    String PREV = COMMAND + ".prev";
    String NEXT = COMMAND + ".next";
    String STOP = COMMAND + ".stop";
    String SHUFFLE = COMMAND + ".shuffle";
    String REPEAT = COMMAND + ".repeat";
    String SHUTDOWN = COMMAND + ".shutdown";
    String TOGGLE_FAVORITE = COMMAND + ".toggle_favorite";
}