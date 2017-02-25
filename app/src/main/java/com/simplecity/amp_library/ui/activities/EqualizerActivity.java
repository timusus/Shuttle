package com.simplecity.amp_library.ui.activities;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.audiofx.AudioEffect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.constants.OpenSLESConstants;
import com.simplecity.amp_library.services.EqualizerService;
import com.simplecity.amp_library.ui.adapters.RobotoSpinnerAdapter;
import com.simplecity.amp_library.ui.views.CustomSwitch;
import com.simplecity.amp_library.ui.views.SizableSeekBar;
import com.simplecity.amp_library.utils.ColorUtils;
import com.simplecity.amp_library.utils.DrawableUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.ThemeUtils;

import java.lang.ref.WeakReference;
import java.util.Formatter;
import java.util.Locale;
import java.util.UUID;

public class EqualizerActivity extends BaseActivity {

    private static final String TAG = "EqualizerActivity";

    private SystemBarTintManager mTintManager;

    private static final String EFFECT_TYPE_EQUALIZER = "47382d60-ddd8-11db-bf3a-0002a5d5c51b";

    private static final String EFFECT_TYPE_BASS_BOOST = "0634f220-ddd4-11db-a0fc-0002a5d5c51b";

    private static final String EFFECT_TYPE_VIRTUALIZER = "37cc2c00-dddd-11db-8577-0002a5d5c51b";

    SharedPreferences mPrefs;

    /**
     * Max number of EQ bands supported
     */
    private final static int EQUALIZER_MAX_BANDS = 6;

    /**
     * Indicates if Equalizer effect is supported.
     */
    private boolean mEqualizerSupported;
    /**
     * Indicates if BassBoost effect is supported.
     */
    private boolean mBassBoostSupported;
    /**
     * Indicates if Virtualizer effect is supported.
     */
    private boolean mVirtualizerSupported;
    private boolean mVirtualizerIsHeadphoneOnly;

    private ServiceConnection mServiceConnection;

    public EqualizerService service;

    // Equalizer fields
    private int mNumberEqualizerBands;
    int mEqCustomPresetPosition = 1;
    int mEqPreset;
    private String[] mEqPresetNames;

    private final SizableSeekBar[] mEqualizerSeekBar = new SizableSeekBar[EQUALIZER_MAX_BANDS];

    Spinner mSpinner;
    RobotoSpinnerAdapter mSpinnerAdapter;

    private CustomSwitch mInternalEqSwitch;

    private StringBuilder mFormatBuilder = new StringBuilder();
    private Formatter mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());


    /**
     * Mapping for the EQ widget ids per band
     */
    static final int[][] eqViewElementIds = {
            {R.id.EqBand0TopTextView, R.id.EqBand0SeekBar},
            {R.id.EqBand1TopTextView, R.id.EqBand1SeekBar},
            {R.id.EqBand2TopTextView, R.id.EqBand2SeekBar},
            {R.id.EqBand3TopTextView, R.id.EqBand3SeekBar},
            {R.id.EqBand4TopTextView, R.id.EqBand4SeekBar},
            {R.id.EqBand5TopTextView, R.id.EqBand5SeekBar}
    };

    /**
     * Mapping for the EQ widget ids per band
     */
    private static final int[][] eqViewTextElementIds = {
            {R.id.EqBand0LeftTextView, R.id.EqBand0RightTextView},
            {R.id.EqBand1LeftTextView, R.id.EqBand1RightTextView},
            {R.id.EqBand2LeftTextView, R.id.EqBand2RightTextView},
            {R.id.EqBand3LeftTextView, R.id.EqBand3RightTextView},
            {R.id.EqBand4LeftTextView, R.id.EqBand4RightTextView},
            {R.id.EqBand5LeftTextView, R.id.EqBand5RightTextView}
    };

    @Override
    public void themeColorChanged() {

    }

    @Override
    protected String screenName() {
        return TAG;
    }

    private static class UpdateHandler extends Handler {
        private final WeakReference<EqualizerActivity> mActivity;

        public UpdateHandler(EqualizerActivity equalizerActivity) {
            mActivity = new WeakReference<>(equalizerActivity);
        }

        @Override
        public void handleMessage(Message msg) {

            final EqualizerActivity activity = mActivity.get();
            if (activity == null) {
                Log.w(TAG, "Active null");
                return;
            }

            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_UPDATE_SERVICE:
                    if (activity.service != null) {
                        activity.service.update();
                    } else {
                        Log.w(TAG, "Service null");
                    }
                    break;
            }
        }

    }

    UpdateHandler mHandler = new UpdateHandler(this);

    @SuppressLint("InlinedApi")
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @Override
    public void onCreate(Bundle savedInstanceState) {

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        ThemeUtils.setTheme(this);

        if (!ShuttleUtils.hasLollipop() && ShuttleUtils.hasKitKat()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            mTintManager = new SystemBarTintManager(this);
        }
        if (!ShuttleUtils.hasKitKat()) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        }

        if (SettingsManager.getInstance().canTintNavBar()) {
            getWindow().setNavigationBarColor(ColorUtils.getPrimaryColorDark(this));
        }

        super.onCreate(savedInstanceState);

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        setContentView(R.layout.activity_equalizer);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ThemeUtils.themeActionBar(this);
        ThemeUtils.themeStatusBar(this, mTintManager);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getString(R.string.equalizer));

        try {
            //Query available effects
            final AudioEffect.Descriptor[] effects = AudioEffect.queryEffects();

            //Determine available/supported effects
            if (effects != null && effects.length != 0) {
                for (final AudioEffect.Descriptor effect : effects) {
                    //Equalizer
                    if (effect.type.equals(UUID.fromString(EFFECT_TYPE_EQUALIZER))) {
                        mEqualizerSupported = true;
                    } else if (effect.type.equals(UUID.fromString(EFFECT_TYPE_BASS_BOOST))) {
                        mBassBoostSupported = true;
                    } else if (effect.type.equals(UUID.fromString(EFFECT_TYPE_VIRTUALIZER))) {
                        mVirtualizerSupported = true;
                        if (effect.uuid.equals(UUID.fromString("1d4033c0-8557-11df-9f2d-0002a5d5c51b"))
                                || effect.uuid.equals(UUID.fromString("e6c98a16-22a3-11e2-b87b-f23c91aec05e"))
                                || effect.uuid.equals(UUID.fromString("d3467faa-acc7-4d34-acaf-0002a5d5c51b"))) {
                            mVirtualizerIsHeadphoneOnly = true;
                        }
                    }
                }
            }
        } catch (NoClassDefFoundError ignored) {
            //The user doesn't have the AudioEffect/AudioEffect.Descriptor class. How sad.
        }

        mSpinner = (Spinner) findViewById(R.id.eqSpinner);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mEqPreset = position;
                equalizerSetPreset(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        setupPresets();
        if (mSpinnerAdapter != null && mSpinnerAdapter.getCount() > mEqPreset) {
            mSpinner.setSelection(mEqPreset);
        }

        //Initialize the equalizer elements
        View eqContainer = findViewById(R.id.eqContainer);
        mNumberEqualizerBands = Integer.parseInt(mPrefs.getString("equalizer.number_of_bands", "5"));
        final int[] centerFreqs = getCenterFreqs();
        final int[] bandLevelRange = getBandLevelRange();

        for (int band = 0; band < mNumberEqualizerBands; band++) {
            //Unit conversion from mHz to Hz and use k prefix if necessary to display
            float centerFreqHz = centerFreqs[band] / 1000;
            String unitPrefix = "";
            if (centerFreqHz >= 1000) {
                centerFreqHz = centerFreqHz / 1000;
                unitPrefix = "k";
            }
            (eqContainer.findViewById(eqViewElementIds[band][0])).setVisibility(View.VISIBLE);
            (eqContainer.findViewById(eqViewTextElementIds[band][0])).setVisibility(View.VISIBLE);
            (eqContainer.findViewById(eqViewElementIds[band][1])).setVisibility(View.VISIBLE);
            (eqContainer.findViewById(eqViewTextElementIds[band][1])).setVisibility(View.VISIBLE);
            ((TextView) eqContainer.findViewById(eqViewElementIds[band][0])).setText(format("%.0f ", centerFreqHz) + unitPrefix + "Hz");
            mEqualizerSeekBar[band] = (SizableSeekBar) eqContainer.findViewById(eqViewElementIds[band][1]);
            ThemeUtils.themeSeekBar(this, mEqualizerSeekBar[band]);
            mEqualizerSeekBar[band].setMax((bandLevelRange[1] / 100) - (bandLevelRange[0] / 100));
            mEqualizerSeekBar[band].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {

                    if (fromUser) {
                        //Determine which band changed
                        int seekbarId = seekBar.getId();
                        int band = 0;
                        for (int i = 0; i < eqViewElementIds.length; i++) {
                            if (eqViewElementIds[i][1] == seekbarId) {
                                band = i;
                            }
                        }

                        if (mEqPreset != mEqCustomPresetPosition) {
                            equalizerCopyToCustom();
                            if (mSpinnerAdapter != null && mSpinnerAdapter.getCount() > mEqCustomPresetPosition) {
                                mSpinner.setSelection(mEqCustomPresetPosition);
                            }
                        } else {
                            int level = getBandLevelRange()[0] + (progress * 100);
                            equalizerBandUpdate(band, level);
                        }
                    }

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    mHandler.sendEmptyMessage(MSG_UPDATE_SERVICE);
                }
            });
        }


        // Initialize the Bass Boost elements.
        // Set the SeekBar listener.
        if (mBassBoostSupported) {

            final SizableSeekBar seekbar = (SizableSeekBar) findViewById(R.id.bb_strength);
            ThemeUtils.themeSeekBar(this, seekbar);
            seekbar.setMax(OpenSLESConstants.BASSBOOST_MAX_STRENGTH - OpenSLESConstants.BASSBOOST_MIN_STRENGTH);

            seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
                    // set parameter and state
                    if (fromUser) {
                        mPrefs.edit().putBoolean("audiofx.bass.enable", true).apply();
                        mPrefs.edit().putString("audiofx.bass.strength", String.valueOf(progress)).apply();
                        mHandler.sendEmptyMessage(MSG_UPDATE_SERVICE);
                    }
                }

                // If slider pos was 0 when starting re-enable effect
                @Override
                public void onStartTrackingTouch(final SeekBar seekBar) {
                    if (seekBar.getProgress() == 0) {
                        mPrefs.edit().putBoolean("audiofx.bass.enable", true).apply();
                        mHandler.sendEmptyMessage(MSG_UPDATE_SERVICE);
                    }
                }

                // If slider pos = 0 when stopping disable effect
                @Override
                public void onStopTrackingTouch(final SeekBar seekBar) {
                    if (seekBar.getProgress() == 0) {
                        // disable
                        mPrefs.edit().putBoolean("audiofx.bass.enable", false).apply();
                        mHandler.sendEmptyMessage(MSG_UPDATE_SERVICE);
                    }
                }
            });
        }

        // Initialize the Virtualizer elements.
        // Set the SeekBar listener.
        if (mVirtualizerSupported) {

            final SizableSeekBar seekbar = (SizableSeekBar) findViewById(R.id.virtualizer_strength);
            ThemeUtils.themeSeekBar(this, seekbar);
            seekbar.setMax(OpenSLESConstants.VIRTUALIZER_MAX_STRENGTH - OpenSLESConstants.VIRTUALIZER_MIN_STRENGTH);

            seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
                    // set parameter and state
                    if (fromUser) {
                        mPrefs.edit().putBoolean("audiofx.virtualizer.enable", true).apply();
                        mPrefs.edit().putString("audiofx.virtualizer.strength", String.valueOf(progress)).apply();
                        mHandler.sendEmptyMessage(MSG_UPDATE_SERVICE);
                    }
                }

                // If slider pos was 0 when starting re-enable effect
                @Override
                public void onStartTrackingTouch(final SeekBar seekBar) {
                    if (seekBar.getProgress() == 0) {
                        mPrefs.edit().putBoolean("audiofx.virtualizer.enable", true).apply();
                        mHandler.sendEmptyMessage(MSG_UPDATE_SERVICE);
                    }
                }

                // If slider pos = 0 when stopping disable effect
                @Override
                public void onStopTrackingTouch(final SeekBar seekBar) {
                    if (seekBar.getProgress() == 0) {
                        // disable
                        mPrefs.edit().putBoolean("audiofx.virtualizer.enable", false).apply();
                        mHandler.sendEmptyMessage(MSG_UPDATE_SERVICE);
                    }
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mServiceConnection == null) {
            mServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder binder) {
                    service = ((EqualizerService.LocalBinder) binder).getService();
                    updateUI();
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    service = null;
                }
            };
        }
        Intent serviceIntent = new Intent(this, EqualizerService.class);
        bindService(serviceIntent, mServiceConnection, 0);

        updateUI();
    }

    @Override
    protected void onPause() {

        unbindService(mServiceConnection);

        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_equalizer_activity, menu);
        MenuItem switchItem = menu.findItem(R.id.action_equalizer);
        mInternalEqSwitch = (CustomSwitch) MenuItemCompat.getActionView(switchItem);
        //Todo: Thumb drawable
//        mInternalEqSwitch.setThumbDrawable(DrawableUtils.getSwitchThumbDrawable(this));
        mInternalEqSwitch.setTrackDrawable(DrawableUtils.getSwitchTrackDrawable(this));
        mInternalEqSwitch.setOnCheckedChangeListener(mOnCheckedChangeListener);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        final boolean isEnabled = mPrefs.getBoolean("audiofx.global.enable", false);
        MenuItem switchItem = menu.findItem(R.id.action_equalizer);
        mInternalEqSwitch = (CustomSwitch) MenuItemCompat.getActionView(switchItem);
        if (mInternalEqSwitch != null) {
            mInternalEqSwitch.setOnCheckedChangeListener(null);
            mInternalEqSwitch.setChecked(isEnabled);
            mInternalEqSwitch.setOnCheckedChangeListener(mOnCheckedChangeListener);
        }

        //Hide the 'open DSP' button if DSP/Other audio effects aren't available
        final Intent intent = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
        if (getPackageManager().resolveActivity(intent, 0) == null) {
            MenuItem openDSPItem = menu.findItem(R.id.menu_DSP);
            if (openDSPItem != null) {
                openDSPItem.setVisible(false);
            }
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.menu_DSP) {
            try {
                final Intent openDSP = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
                startActivityForResult(openDSP, 1000);
            } catch (final ActivityNotFoundException notFound) {
                Log.e(TAG, "External EQ not found. " + notFound.getLocalizedMessage());
            }
        } else if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
    }

    /**
     * Sets the given EQ preset.
     *
     * @param preset EQ preset id.
     */
    void equalizerSetPreset(final int preset) {
        mEqPreset = preset;
        mPrefs.edit().putString("audiofx.eq.preset", String.valueOf(preset)).apply();

        String newLevels;
        if (preset == mEqCustomPresetPosition) {
            // load custom if possible
            newLevels = mPrefs.getString("audiofx.eq.bandlevels.custom", EqualizerService.getZeroedBandsString(mNumberEqualizerBands));
        } else {
            newLevels = mPrefs.getString("equalizer.preset." + preset, EqualizerService.getZeroedBandsString(mNumberEqualizerBands));
        }
        mPrefs.edit().putString("audiofx.eq.bandlevels", newLevels).apply();
        updateUI();

        updateService();
    }

    void updateUI() {

        setupPresets();

        supportInvalidateOptionsMenu();
        if (mEqualizerSupported) {
            equalizerUpdateDisplay();
        }
        if (mBassBoostSupported) {
            ((SeekBar) findViewById(R.id.bb_strength)).setProgress(Integer.valueOf(mPrefs.getString("audiofx.bass.strength", "0")));
        }
        if (mVirtualizerSupported) {
            ((SeekBar) findViewById(R.id.virtualizer_strength)).setProgress(Integer.valueOf(mPrefs.getString("audiofx.virtualizer.strength", "0")));
        }

        // Initialize the Equalizer elements.
        if (mEqualizerSupported) {
            String preset = String.valueOf(mNumberEqualizerBands);
            mEqPreset = Integer.valueOf(mPrefs.getString("audiofx.eq.preset", preset));
            if (mSpinnerAdapter != null && mSpinnerAdapter.getCount() > mEqPreset) {
                mSpinner.setSelection(mEqPreset);
            }
        }
    }

    /**
     * Updates the EQ by getting the parameters.
     */
    private void equalizerUpdateDisplay() {

        String levelsString;
        float[] floats;

        if (mEqPreset == mEqCustomPresetPosition) {
            // load custom preset for current device
            // here mEQValues needs to be pre-populated with the user's preset values.
            String[] customEq = mPrefs.getString("audiofx.eq.bandlevels.custom", EqualizerService.getZeroedBandsString(mNumberEqualizerBands)).split(";");
            floats = new float[mNumberEqualizerBands];
            for (int band = 0; band < floats.length; band++) {
                final float level = Float.parseFloat(customEq[band]);
                floats[band] = level / 100.0f;
                mEqualizerSeekBar[band].setProgress((int) ((getBandLevelRange()[1] / 100.0f) + (level / 100.0f)));
            }
        } else {
            // try to load preset
            levelsString = mPrefs.getString("equalizer.preset." + mEqPreset, EqualizerService.getZeroedBandsString(mNumberEqualizerBands));
            String[] bandLevels = levelsString.split(";");
            floats = new float[bandLevels.length];
            for (int band = 0; band < bandLevels.length; band++) {
                final float level = Float.parseFloat(bandLevels[band]);
                floats[band] = level / 100.0f;
                mEqualizerSeekBar[band].setProgress((int) ((getBandLevelRange()[1] / 100.0f) + (level / 100.0f)));
            }
        }
    }


    void equalizerBandUpdate(final int band, final int level) {

        String[] currentCustomLevels = mPrefs.getString("audiofx.eq.bandlevels.custom", EqualizerService.getZeroedBandsString(mNumberEqualizerBands)).split(";");

        currentCustomLevels[band] = String.valueOf(level);

        // save
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < mNumberEqualizerBands; i++) {
            builder.append(currentCustomLevels[i]);
            builder.append(";");
        }
        builder.deleteCharAt(builder.length() - 1);
        mPrefs.edit().putString("audiofx.eq.bandlevels", builder.toString()).apply();
        mPrefs.edit().putString("audiofx.eq.bandlevels.custom", builder.toString()).apply();

        updateService();
    }

    /**
     * Called when user starts touch eq on a preset
     */
    void equalizerCopyToCustom() {
        Log.d(TAG, "equalizerCopyToCustom()");
        StringBuilder bandLevels = new StringBuilder();
        for (int band = 0; band < mNumberEqualizerBands; band++) {
            final float level = (getBandLevelRange()[0] / 100) + mEqualizerSeekBar[band].getProgress();
            bandLevels.append(level * 100);
            bandLevels.append(";");
        }
        // remove trailing ";"
        bandLevels.deleteCharAt(bandLevels.length() - 1);
        mPrefs.edit().putString("audiofx.eq.bandlevels.custom", bandLevels.toString()).apply();
        mPrefs.edit().putString("audiofx.eq.preset", String.valueOf(mEqCustomPresetPosition)).apply();
    }


    private String format(String format, Object... args) {
        mFormatBuilder.setLength(0);
        mFormatter.format(format, args);
        return mFormatBuilder.toString();
    }

    private static final int MSG_UPDATE_SERVICE = 1;

    void updateService() {
        mHandler.removeMessages(MSG_UPDATE_SERVICE);
        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_SERVICE, 100);
    }

    int[] getBandLevelRange() {
        String savedCenterFreqs = mPrefs.getString("equalizer.band_level_range", null);
        if (savedCenterFreqs == null || savedCenterFreqs.isEmpty()) {
            return new int[]{-1500, 1500};
        } else {
            String[] split = savedCenterFreqs.split(";");
            int[] freqs = new int[split.length];
            for (int i = 0; i < split.length; i++) {
                freqs[i] = Integer.valueOf(split[i]);
            }
            return freqs;
        }
    }

    private int[] getCenterFreqs() {
        String savedCenterFreqs = mPrefs.getString("equalizer.center_freqs", EqualizerService.getZeroedBandsString(mNumberEqualizerBands));
        String[] split = savedCenterFreqs.split(";");
        int[] freqs = new int[split.length];
        for (int i = 0; i < split.length; i++) {
            freqs[i] = Integer.valueOf(split[i]);
        }
        return freqs;
    }

    private void setupPresets() {
        // setup equalizer presets
        final int numPresets = Integer.parseInt(mPrefs.getString("equalizer.number_of_presets", "0"));
        mEqPresetNames = new String[numPresets + 1];

        String[] presetNames = mPrefs.getString("equalizer.preset_names", "").split("\\|");
        for (short i = 0; i < numPresets; i++) {
            mEqPresetNames[i] = presetNames[i];
        }
        mEqPresetNames[numPresets] = getString(R.string.custom);
        mEqCustomPresetPosition = numPresets;

        if (mSpinnerAdapter == null || mSpinnerAdapter.getCount() != mEqPresetNames.length) {
            mSpinnerAdapter = new RobotoSpinnerAdapter<>(this, R.layout.spinner_item, mEqPresetNames);
            mSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mSpinner.setAdapter(mSpinnerAdapter);
        }
    }

    private CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {

            EqualizerService.closeEqualizerSessions(EqualizerActivity.this, !isChecked, MusicUtils.getAudioSessionId());

            EqualizerService.openEqualizerSession(EqualizerActivity.this, isChecked, MusicUtils.getAudioSessionId());

            // set parameter and state
            mPrefs.edit().putBoolean("audiofx.global.enable", isChecked).apply();
            updateService();
        }
    };
}
