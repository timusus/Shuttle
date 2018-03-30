package com.simplecity.amp_library.playback.constants;

public interface ServiceCommand {
    String SERVICE_COMMAND = "com.simplecity.shuttle.music_service_command";
    String TOGGLE_PAUSE_ACTION = SERVICE_COMMAND + ".togglepause";
    String PAUSE_ACTION = SERVICE_COMMAND + ".pause";
    String PREV_ACTION = SERVICE_COMMAND + ".prev";
    String NEXT_ACTION = SERVICE_COMMAND + ".next";
    String STOP_ACTION = SERVICE_COMMAND + ".stop";
    String SHUFFLE_ACTION = SERVICE_COMMAND + ".shuffle";
    String REPEAT_ACTION = SERVICE_COMMAND + ".repeat";
    String SHUTDOWN = SERVICE_COMMAND + ".shutdown";
    String TOGGLE_FAVORITE = SERVICE_COMMAND + ".togglefavorite";
}
