package com.simplecity.amp_library.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.Virtualizer;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.simplecity.amp_library.utils.SettingsManager;

import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>This calls listen to events that affect DSP function and responds to them.</p>
 * <ol>
 * <li>new audio session declarations</li>
 * <li>headset plug / unplug events</li>
 * <li>preference update events.</li>
 * </ol>
 *
 * @author alankila
 */
public class EqualizerService extends Service {

    public static final String ACTION_OPEN_EQUALIZER_SESSION = "com.simplecity.amp_library.audiofx.OPEN_SESSION";
    public static final String ACTION_CLOSE_EQUALIZER_SESSION = "com.simplecity.amp_library.audiofx.CLOSE_SESSION";

    private SharedPreferences mPrefs;

    public static String getZeroedBandsString(int length) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            stringBuilder.append("0;");
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        return stringBuilder.toString();
    }

    /**
     * Helper class representing the full complement of effects attached to one
     * audio session.
     *
     * @author alankila
     */
    private static class EffectSet {
        /**
         * Session-specific equalizer
         */
        Equalizer equalizer;
        /**
         * Session-specific bassboost
         */
        private BassBoost bassBoost;
        /**
         * Session-specific virtualizer
         */
        private Virtualizer virtualizer;

//        private final PresetReverb mPresetReverb;

        private short mEqNumPresets = -1;
        private short mEqNumBands = -1;

        public EffectSet(int sessionId) {
            equalizer = new Equalizer(1, sessionId);
            bassBoost = new BassBoost(1, sessionId);
            virtualizer = new Virtualizer(1, sessionId);
//            mPresetReverb = new PresetReverb(0, sessionId);
        }

        /*
         * Take lots of care to not poke values that don't need
         * to be poked- this can cause audible pops.
         */

        public void enableEqualizer(boolean enable) {
            if (enable != equalizer.getEnabled()) {
                if (!enable) {
                    for (short i = 0; i < getNumEqualizerBands(); i++) {
                        equalizer.setBandLevel(i, (short) 0);
                    }
                }
                equalizer.setEnabled(enable);
            }
        }

        public void setEqualizerLevels(short[] levels) {
            if (equalizer.getEnabled()) {
                for (short i = 0; i < levels.length; i++) {
                    if (equalizer.getBandLevel(i) != levels[i]) {
                        equalizer.setBandLevel(i, levels[i]);
                    }
                }
            }
        }

        public short getNumEqualizerBands() {
            if (mEqNumBands < 0) {
                mEqNumBands = equalizer.getNumberOfBands();
            }
            if (mEqNumBands > 6) {
                mEqNumBands = 6;
            }
            return mEqNumBands;
        }

        public short getNumEqualizerPresets() {
            if (mEqNumPresets < 0) {
                mEqNumPresets = equalizer.getNumberOfPresets();
            }
            return mEqNumPresets;
        }

        public void enableBassBoost(boolean enable) {
            if (enable != bassBoost.getEnabled()) {
                if (!enable) {
                    bassBoost.setStrength((short) 1);
                    bassBoost.setStrength((short) 0);
                }
                bassBoost.setEnabled(enable);
            }
        }

        public void setBassBoostStrength(short strength) {
            if (bassBoost.getEnabled() && bassBoost.getRoundedStrength() != strength) {
                bassBoost.setStrength(strength);
            }
        }

        public void enableVirtualizer(boolean enable) {
            if (enable != virtualizer.getEnabled()) {
                if (!enable) {
                    virtualizer.setStrength((short) 1);
                    virtualizer.setStrength((short) 0);
                }
                virtualizer.setEnabled(enable);
            }
        }

        public void setVirtualizerStrength(short strength) {
            if (virtualizer.getEnabled() && virtualizer.getRoundedStrength() != strength) {
                virtualizer.setStrength(strength);
            }
        }

//        public void enableReverb(boolean enable) {
//            if (enable != mPresetReverb.getEnabled()) {
//                if (!enable) {
//                    mPresetReverb.setPreset((short) 0);
//                }
//                mPresetReverb.setEnabled(enable);
//            }
//        }

//        public void setReverbPreset(short preset) {
//            if (mPresetReverb.getEnabled() && mPresetReverb.getPreset() != preset) {
//                mPresetReverb.setPreset(preset);
//            }
//        }

        public void release() {
            equalizer.release();
            bassBoost.release();
            virtualizer.release();
//            mPresetReverb.release();
        }
    }

    protected static final String TAG = EqualizerService.class.getSimpleName();

    public class LocalBinder extends Binder {
        public EqualizerService getService() {
            return EqualizerService.this;
        }
    }

    private final LocalBinder mBinder = new LocalBinder();

    /**
     * Known audio sessions and their associated audioeffect suites.
     */
    protected final ConcurrentHashMap<Integer, EffectSet> mAudioSessions = new ConcurrentHashMap<>();

    /**
     * Receive new broadcast intents for adding DSP to session
     */
    private final BroadcastReceiver mAudioSessionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, 0);
            if (action.equals(ACTION_OPEN_EQUALIZER_SESSION)) {
//                Log.i(TAG, "Open session called. Session: " + sessionId);
                if (!mAudioSessions.containsKey(sessionId)) {
                    try {
                        EffectSet effectSet = new EffectSet(sessionId);
                        mAudioSessions.put(sessionId, effectSet);
                    } catch (Exception | ExceptionInInitializerError e) {
                        Log.e(TAG, "Failed to open EQ session.. EffectSet error " + e);
                    }

                }
            }
            if (action.equals(ACTION_CLOSE_EQUALIZER_SESSION)) {
//                Log.i(TAG, "Close session called");
                EffectSet gone = mAudioSessions.remove(sessionId);
                if (gone != null) {
                    gone.release();
                }
            }
            update();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        IntentFilter audioFilter = new IntentFilter();
        audioFilter.addAction(ACTION_OPEN_EQUALIZER_SESSION);
        audioFilter.addAction(ACTION_CLOSE_EQUALIZER_SESSION);
        registerReceiver(mAudioSessionReceiver, audioFilter);

        saveDefaults();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {

        unregisterReceiver(mAudioSessionReceiver);

        for (EffectSet gone : mAudioSessions.values()) {
            if (gone != null) {
                gone.release();
            }
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void saveDefaults() {
        EffectSet temp;
        try {
            temp = new EffectSet(0);
        } catch (Exception | ExceptionInInitializerError | UnsatisfiedLinkError e) {
            // this is really bad- likely the media stack is broken.
            // disable ourself if we get into this state, as the service
            // will restart itself repeatedly!
            Log.e(TAG, e.getMessage(), e);
            stopSelf();
            return;
        }

        final int numBands = temp.getNumEqualizerBands();
        final int numPresets = temp.getNumEqualizerPresets();
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString("equalizer.number_of_presets", String.valueOf(numPresets)).apply();
        editor.putString("equalizer.number_of_bands", String.valueOf(numBands)).apply();

        // range
        short[] rangeShortArr = temp.equalizer.getBandLevelRange();


        editor.putString("equalizer.band_level_range", rangeShortArr[0] + ";" + rangeShortArr[1]).apply();

        // center freqs
        StringBuilder centerFreqs = new StringBuilder();
        // audiofx.global.centerfreqs
        for (short i = 0; i < numBands; i++) {
            centerFreqs.append(temp.equalizer.getCenterFreq(i));
            centerFreqs.append(";");

        }
        centerFreqs.deleteCharAt(centerFreqs.length() - 1);
        editor.putString("equalizer.center_freqs", centerFreqs.toString()).apply();

        // populate preset names
        StringBuilder presetNames = new StringBuilder();
        for (int i = 0; i < numPresets; i++) {
            String presetName = temp.equalizer.getPresetName((short) i);
            presetNames.append(presetName);
            presetNames.append("|");

            // populate preset band values
            StringBuilder presetBands = new StringBuilder();
            try {
                temp.equalizer.usePreset((short) i);
            } catch (RuntimeException e) {
                Log.e(TAG, "equalizer.usePreset() failed");
            }

            for (int j = 0; j < numBands; j++) {
                // loop through preset bands
                presetBands.append(temp.equalizer.getBandLevel((short) j));
                presetBands.append(";");
            }
            presetBands.deleteCharAt(presetBands.length() - 1);
            editor.putString("equalizer.preset." + i, presetBands.toString()).apply();
        }
        if (presetNames.length() != 0) {
            presetNames.deleteCharAt(presetNames.length() - 1);
            editor.putString("equalizer.preset_names", presetNames.toString()).apply();
        }
        temp.release();
    }

    /**
     * Push new configuration to audio stack.
     */
    public synchronized void update() {
        for (Integer sessionId : mAudioSessions.keySet()) {
            updateDsp(mAudioSessions.get(sessionId));
        }
    }

    private void updateDsp(EffectSet session) {
        final boolean globalEnabled = SettingsManager.getInstance().getEqualizerEnabled();

        try {
            session.enableBassBoost(globalEnabled && mPrefs.getBoolean("audiofx.bass.enable", false));
            session.setBassBoostStrength(Short.valueOf(mPrefs.getString("audiofx.bass.strength", "0")));

        } catch (Exception e) {
            Log.e(TAG, "Error enabling bass boost!", e);
        }

//        try {
//            short preset = Short.decode(mPrefs.getString("audiofx.reverb.preset", String.valueOf(PresetReverb.PRESET_NONE)));
//            session.enableReverb(globalEnabled && (preset > 0));
//            session.setReverbPreset(preset);
//
//        } catch (Exception e) {
//            Log.e(TAG, "Error enabling reverb preset", e);
//        }

        try {
            session.enableEqualizer(globalEnabled);
            final int customPresetPos = session.getNumEqualizerPresets();
            final int preset = Integer.valueOf(mPrefs.getString("audiofx.eq.preset", String.valueOf(customPresetPos)));
            final int bands = session.getNumEqualizerBands();

            /*
             * Equalizer state is in a single string preference with all values
             * separated by ;
             */
            String[] levels;

            if (preset == customPresetPos) {
                levels = mPrefs.getString("audiofx.eq.bandlevels.custom", getZeroedBandsString(bands)).split(";");
            } else {
                levels = mPrefs.getString("equalizer.preset." + preset, getZeroedBandsString(bands)).split(";");
            }

            short[] equalizerLevels = new short[levels.length];
            for (int i = 0; i < levels.length; i++) {
                equalizerLevels[i] = Short.parseShort(levels[i]);
            }

            session.setEqualizerLevels(equalizerLevels);

        } catch (Exception e) {
            Log.e(TAG, "Error enabling equalizer!", e);
        }

        try {
            session.enableVirtualizer(globalEnabled && mPrefs.getBoolean("audiofx.virtualizer.enable", false));
            session.setVirtualizerStrength(Short.valueOf(mPrefs.getString("audiofx.virtualizer.strength", "0")));

        } catch (Exception e) {
            Log.e(TAG, "Error enabling virtualizer!");
        }
    }

    /**
     * Sends a broadcast to close any existing audio effect sessions
     */
    public static void closeEqualizerSessions(Context context, boolean internal, int audioSessionId) {

        if (internal) {
            //Close the internal audio session
            Intent intent = new Intent(EqualizerService.ACTION_CLOSE_EQUALIZER_SESSION);
            intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId);
            intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.getPackageName());
            context.sendBroadcast(intent);
        } else {
            Intent intent = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
            intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.getPackageName());
            intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId);
            context.sendBroadcast(intent);

            //Close any external audio sessions on session 0
            intent = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
            intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.getPackageName());
            intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, 0);
            context.sendBroadcast(intent);
        }
    }

    public static void openEqualizerSession(Context context, boolean internal, int audioSessionId) {

        final Intent intent = new Intent();
        intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId);
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.getPackageName());
        intent.setAction(internal ? EqualizerService.ACTION_OPEN_EQUALIZER_SESSION : AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
        context.sendBroadcast(intent);
    }
}
