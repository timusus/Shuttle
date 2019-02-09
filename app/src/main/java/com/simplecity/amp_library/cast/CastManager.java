package com.simplecity.amp_library.cast;

import android.content.Context;
import android.util.Log;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManager;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.playback.CastPlayback;
import com.simplecity.amp_library.playback.MediaPlayerPlayback;
import com.simplecity.amp_library.playback.Playback;
import com.simplecity.amp_library.playback.PlaybackManager;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ShuttleUtils;

public class CastManager {

    private static final String TAG = "CastManager";

    private Context applicationContext;

    private SessionManager sessionManager;

    private SessionManagerListener<CastSession> sessionManagerListener;

    private PlaybackManager playbackManager;

    public CastManager(Context context, PlaybackManager playbackManager) {

        this.applicationContext = context.getApplicationContext();

        this.playbackManager = playbackManager;

        sessionManager = CastContext.getSharedInstance(applicationContext).getSessionManager();

        sessionManagerListener = new CastSessionManagerListener();
        sessionManager.addSessionManagerListener(sessionManagerListener, CastSession.class);
    }

    public void destroy() {
        sessionManager.removeSessionManagerListener(sessionManagerListener, CastSession.class);
    }

    private class CastSessionManagerListener implements SessionManagerListener<CastSession> {

        @Override
        public void onSessionStarting(CastSession castSession) {
            Log.d(TAG, "onSessionStarting");
        }

        @Override
        public void onSessionStarted(CastSession castSession, String s) {
            Log.d(TAG, "onSessionStarted");

            Playback playback = new CastPlayback(applicationContext, castSession);
            playbackManager.switchToPlayback(playback, playbackManager.getPlayback().getPosition());
        }

        @Override
        public void onSessionStartFailed(CastSession castSession, int i) {
            Log.e(TAG, "onSessionStartFailed");
        }

        @Override
        public void onSessionEnding(CastSession castSession) {
            Log.d(TAG, "onSessionEnding.. isPlaying" + playbackManager.isPlaying());

            if (playbackManager.getPlayback() instanceof CastPlayback) {

                // This is our final chance to update the underlying stream position In onSessionEnded(), the underlying CastPlayback#mRemoteMediaClient
                // is disconnected and hence we update our local value of stream position to the latest position.
                playbackManager.getPlayback().updateLastKnownStreamPosition();

                Playback playback = new MediaPlayerPlayback(applicationContext);
                playbackManager.switchToPlayback(playback, playbackManager.getPlayback().getPosition());
            }
        }

        @Override
        public void onSessionEnded(CastSession castSession, int i) {
            Log.d(TAG, "onSessionEnded");
        }

        @Override
        public void onSessionResuming(CastSession castSession, String s) {
            Log.d(TAG, "onSessionResuming");
        }

        @Override
        public void onSessionResumed(CastSession castSession, boolean b) {
            Log.d(TAG, "onSessionResumed");

            // If we're not already playing via CastPlayback, switch
            if (!(playbackManager.getPlayback() instanceof CastPlayback)) {
                Playback playback = new CastPlayback(applicationContext, castSession);
                playbackManager.switchToPlayback(playback, playbackManager.getPlayback().getPosition());
            }
        }

        @Override
        public void onSessionResumeFailed(CastSession castSession, int i) {
            Log.e(TAG, "onSessionResumeFailed");
        }

        @Override
        public void onSessionSuspended(CastSession castSession, int i) {
            Log.d(TAG, "onSessionSuspended");
        }
    }

    public static boolean isCastAvailable(Context context, SettingsManager settingsManager) {
        // Cast is only available in the paid version
        if (!ShuttleUtils.isUpgraded((ShuttleApplication) context.getApplicationContext(), settingsManager)) {
            Log.i(TAG, "Cast available false, not upgraded");
            return false;
        }

        // Ensure we can access the CastContext without crashing
        try {
            CastContext.getSharedInstance(context);
            return true;
        } catch (Exception e) {
            LogUtils.logException(TAG, "Cast not available", e);
            return false;
        }
    }
}
