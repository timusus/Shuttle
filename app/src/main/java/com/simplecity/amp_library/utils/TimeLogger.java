package com.simplecity.amp_library.utils;

import android.util.Log;

public class TimeLogger {

    private long initialTime;
    private long intervalTime;

    /**
     * Call to begin tracking time intervals. Subsequent calls to {@link #logInterval(String, String)} will
     * output the time since this call.
     */
    public void startLog() {
        initialTime = System.currentTimeMillis();
        intervalTime = System.currentTimeMillis();
    }

    /**
     * Lpg the time since the last logInterval() was called.
     * <p>
     * Note: Must call startLog() or the 'total' time won't be accurate.
     * <p>
     *
     * @param tag the tag to use for the log message
     * @param message the message to output
     */
    public void logInterval(String tag, String message) {

        Log.i(tag, message
                + "\n Interval: " + (System.currentTimeMillis() - intervalTime)
                + "\n Total: " + (System.currentTimeMillis() - initialTime)
        );
        intervalTime = System.currentTimeMillis();
    }
}