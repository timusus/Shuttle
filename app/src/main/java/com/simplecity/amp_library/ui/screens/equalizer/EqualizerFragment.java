package com.simplecity.amp_library.ui.screens.equalizer;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.audiofx.AudioEffect;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.constants.OpenSLESConstants;
import com.simplecity.amp_library.services.Equalizer;
import com.simplecity.amp_library.ui.adapters.RobotoSpinnerAdapter;
import com.simplecity.amp_library.ui.screens.drawer.DrawerLockManager;
import com.simplecity.amp_library.ui.screens.drawer.MiniPlayerLockManager;
import com.simplecity.amp_library.ui.common.BaseFragment;
import com.simplecity.amp_library.ui.views.SizableSeekBar;
import java.util.Formatter;
import java.util.Locale;
import java.util.UUID;

public class EqualizerFragment extends BaseFragment implements
        Toolbar.OnMenuItemClickListener,
        CompoundButton.OnCheckedChangeListener,
        DrawerLockManager.DrawerLock,
        MiniPlayerLockManager.MiniPlayerLock {

    private static final String TAG = "EqualizerFragment";

    private static final String EFFECT_TYPE_EQUALIZER = "47382d60-ddd8-11db-bf3a-0002a5d5c51b";

    private static final String EFFECT_TYPE_BASS_BOOST = "0634f220-ddd4-11db-a0fc-0002a5d5c51b";

    private static final String EFFECT_TYPE_VIRTUALIZER = "37cc2c00-dddd-11db-8577-0002a5d5c51b";

    SharedPreferences prefs;

    /**
     * Max number of EQ bands supported
     */
    private final static int EQUALIZER_MAX_BANDS = 6;

    /**
     * Indicates if Equalizer effect is supported.
     */
    private boolean equalizerSupported;
    /**
     * Indicates if BassBoost effect is supported.
     */
    private boolean bassBoostSupported;
    /**
     * Indicates if Virtualizer effect is supported.
     */
    private boolean virtualizerSupported;

    // Equalizer fields
    private int numberEqualizerBands;
    int eqCustomPresetPosition = 1;
    int eqPreset;
    private String[] eqPresetNames;

    private final SizableSeekBar[] mEqualizerSeekBar = new SizableSeekBar[EQUALIZER_MAX_BANDS];

    Unbinder unbinder;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.eqSpinner)
    Spinner spinner;

    @BindView(R.id.eqContainer)
    View eqContainer;

    @BindView(R.id.bb_strength)
    SizableSeekBar baseBoostSeekbar;

    @BindView(R.id.virtualizer_strength)
    SizableSeekBar virtualizerSeekbar;

    RobotoSpinnerAdapter spinnerAdapter;

    private StringBuilder formatBuilder = new StringBuilder();
    private Formatter formatter = new Formatter(formatBuilder, Locale.getDefault());

    /**
     * Mapping for the EQ widget ids per band
     */
    static final int[][] eqViewElementIds = {
            { R.id.EqBand0TopTextView, R.id.EqBand0SeekBar },
            { R.id.EqBand1TopTextView, R.id.EqBand1SeekBar },
            { R.id.EqBand2TopTextView, R.id.EqBand2SeekBar },
            { R.id.EqBand3TopTextView, R.id.EqBand3SeekBar },
            { R.id.EqBand4TopTextView, R.id.EqBand4SeekBar },
            { R.id.EqBand5TopTextView, R.id.EqBand5SeekBar }
    };

    /**
     * Mapping for the EQ widget ids per band
     */
    private static final int[][] eqViewTextElementIds = {
            { R.id.EqBand0LeftTextView, R.id.EqBand0RightTextView },
            { R.id.EqBand1LeftTextView, R.id.EqBand1RightTextView },
            { R.id.EqBand2LeftTextView, R.id.EqBand2RightTextView },
            { R.id.EqBand3LeftTextView, R.id.EqBand3RightTextView },
            { R.id.EqBand4LeftTextView, R.id.EqBand4RightTextView },
            { R.id.EqBand5LeftTextView, R.id.EqBand5RightTextView }
    };

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_dsp:
                Intent openDSP = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
                if (getActivity().getPackageManager().resolveActivity(openDSP, 0) != null) {
                    startActivityForResult(openDSP, 1000);
                }
                break;
        }
        return true;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mediaManager.closeEqualizerSessions(!isChecked, mediaManager.getAudioSessionId());

        mediaManager.openEqualizerSession(isChecked, mediaManager.getAudioSessionId());

        // set parameter and state
        prefs.edit().putBoolean("audiofx.global.enable", isChecked).apply();
        mediaManager.updateEqualizer();
    }

    public static EqualizerFragment newInstance() {
        Bundle args = new Bundle();
        EqualizerFragment fragment = new EqualizerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @SuppressLint("InlinedApi")
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        try {
            //Query available effects
            final AudioEffect.Descriptor[] effects = AudioEffect.queryEffects();

            //Determine available/supported effects
            if (effects != null && effects.length != 0) {
                for (final AudioEffect.Descriptor effect : effects) {
                    //Equalizer
                    if (effect.type.equals(UUID.fromString(EFFECT_TYPE_EQUALIZER))) {
                        equalizerSupported = true;
                    } else if (effect.type.equals(UUID.fromString(EFFECT_TYPE_BASS_BOOST))) {
                        bassBoostSupported = true;
                    } else if (effect.type.equals(UUID.fromString(EFFECT_TYPE_VIRTUALIZER))) {
                        virtualizerSupported = true;
                    }
                }
            }
        } catch (NoClassDefFoundError ignored) {
            //The user doesn't have the AudioEffect/AudioEffect.Descriptor class. How sad.
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_equalizer, container, false);

        unbinder = ButterKnife.bind(this, rootView);

        toolbar.inflateMenu(R.menu.menu_equalizer);
        toolbar.setNavigationOnClickListener(v -> getNavigationController().popViewController());
        toolbar.setOnMenuItemClickListener(this);

        MenuItem item = toolbar.getMenu().findItem(R.id.action_equalizer);
        SwitchCompat switchItem = (SwitchCompat) item.getActionView();

        boolean isEnabled = prefs.getBoolean("audiofx.global.enable", false);
        switchItem.setChecked(isEnabled);
        switchItem.setOnCheckedChangeListener(this);

        //Hide the 'open DSP' button if DSP/Other audio effects aren't available
        final Intent intent = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
        if (getContext().getPackageManager().resolveActivity(intent, 0) == null) {
            MenuItem openDSPItem = toolbar.getMenu().findItem(R.id.menu_dsp);
            if (openDSPItem != null) {
                openDSPItem.setVisible(false);
            }
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                eqPreset = position;
                equalizerSetPreset(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        setupPresets();
        if (spinnerAdapter != null && spinnerAdapter.getCount() > eqPreset) {
            spinner.setSelection(eqPreset);
        }

        //Initialize the equalizer elements
        numberEqualizerBands = Integer.parseInt(prefs.getString("equalizer.number_of_bands", "5"));
        final int[] centerFreqs = getCenterFreqs();
        final int[] bandLevelRange = getBandLevelRange();

        for (int band = 0; band < numberEqualizerBands; band++) {
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
            mEqualizerSeekBar[band] = eqContainer.findViewById(eqViewElementIds[band][1]);
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

                        if (eqPreset != eqCustomPresetPosition) {
                            equalizerCopyToCustom();
                            if (spinnerAdapter != null && spinnerAdapter.getCount() > eqCustomPresetPosition) {
                                spinner.setSelection(eqCustomPresetPosition);
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
                    mediaManager.updateEqualizer();
                }
            });
        }

        // Initialize the Bass Boost elements.
        // Set the SeekBar listener.
        if (bassBoostSupported) {

            baseBoostSeekbar.setMax(OpenSLESConstants.BASSBOOST_MAX_STRENGTH - OpenSLESConstants.BASSBOOST_MIN_STRENGTH);

            baseBoostSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
                    // set parameter and state
                    if (fromUser) {
                        prefs.edit().putBoolean("audiofx.bass.enable", true).apply();
                        prefs.edit().putString("audiofx.bass.strength", String.valueOf(progress)).apply();
                        mediaManager.updateEqualizer();
                    }
                }

                // If slider pos was 0 when starting re-enable effect
                @Override
                public void onStartTrackingTouch(final SeekBar seekBar) {
                    if (seekBar.getProgress() == 0) {
                        prefs.edit().putBoolean("audiofx.bass.enable", true).apply();
                        mediaManager.updateEqualizer();
                    }
                }

                // If slider pos = 0 when stopping disable effect
                @Override
                public void onStopTrackingTouch(final SeekBar seekBar) {
                    if (seekBar.getProgress() == 0) {
                        // disable
                        prefs.edit().putBoolean("audiofx.bass.enable", false).apply();
                        mediaManager.updateEqualizer();
                    }
                }
            });
        }

        // Initialize the Virtualizer elements.
        // Set the SeekBar listener.
        if (virtualizerSupported) {

            virtualizerSeekbar.setMax(OpenSLESConstants.VIRTUALIZER_MAX_STRENGTH - OpenSLESConstants.VIRTUALIZER_MIN_STRENGTH);

            virtualizerSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
                    // set parameter and state
                    if (fromUser) {
                        prefs.edit().putBoolean("audiofx.virtualizer.enable", true).apply();
                        prefs.edit().putString("audiofx.virtualizer.strength", String.valueOf(progress)).apply();
                        mediaManager.updateEqualizer();
                    }
                }

                // If slider pos was 0 when starting re-enable effect
                @Override
                public void onStartTrackingTouch(final SeekBar seekBar) {
                    if (seekBar.getProgress() == 0) {
                        prefs.edit().putBoolean("audiofx.virtualizer.enable", true).apply();
                        mediaManager.updateEqualizer();
                    }
                }

                // If slider pos = 0 when stopping disable effect
                @Override
                public void onStopTrackingTouch(final SeekBar seekBar) {
                    if (seekBar.getProgress() == 0) {
                        // disable
                        prefs.edit().putBoolean("audiofx.virtualizer.enable", false).apply();
                        mediaManager.updateEqualizer();
                    }
                }
            });
        }

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        DrawerLockManager.getInstance().addDrawerLock(this);
        MiniPlayerLockManager.getInstance().addMiniPlayerLock(this);

        updateUI();
    }

    @Override
    public void onPause() {
        DrawerLockManager.getInstance().removeDrawerLock(this);
        MiniPlayerLockManager.getInstance().removeMiniPlayerLock(this);

        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        unbinder.unbind();
    }

    /**
     * Sets the given EQ preset.
     *
     * @param preset EQ preset id.
     */
    void equalizerSetPreset(final int preset) {
        eqPreset = preset;
        prefs.edit().putString("audiofx.eq.preset", String.valueOf(preset)).apply();

        String newLevels;
        if (preset == eqCustomPresetPosition) {
            // load custom if possible
            newLevels = prefs.getString("audiofx.eq.bandlevels.custom", Equalizer.getZeroedBandsString(numberEqualizerBands));
        } else {
            newLevels = prefs.getString("equalizer.preset." + preset, Equalizer.getZeroedBandsString(numberEqualizerBands));
        }
        prefs.edit().putString("audiofx.eq.bandlevels", newLevels).apply();
        updateUI();

        mediaManager.updateEqualizer();
    }

    void updateUI() {

        setupPresets();

        if (equalizerSupported) {
            equalizerUpdateDisplay();
        }
        if (bassBoostSupported) {
            baseBoostSeekbar.setProgress(Integer.valueOf(prefs.getString("audiofx.bass.strength", "0")));
        }
        if (virtualizerSupported) {
            virtualizerSeekbar.setProgress(Integer.valueOf(prefs.getString("audiofx.virtualizer.strength", "0")));
        }

        // Initialize the Equalizer elements.
        if (equalizerSupported) {
            String preset = String.valueOf(numberEqualizerBands);
            eqPreset = Integer.valueOf(prefs.getString("audiofx.eq.preset", preset));
            if (spinnerAdapter != null && spinnerAdapter.getCount() > eqPreset) {
                spinner.setSelection(eqPreset);
            }
        }
    }

    /**
     * Updates the EQ by getting the parameters.
     */
    private void equalizerUpdateDisplay() {

        String levelsString;
        float[] floats;

        if (eqPreset == eqCustomPresetPosition) {
            // load custom preset for current device
            // here mEQValues needs to be pre-populated with the user's preset values.
            String[] customEq = prefs.getString("audiofx.eq.bandlevels.custom", Equalizer.getZeroedBandsString(numberEqualizerBands)).split(";");
            floats = new float[numberEqualizerBands];
            for (int band = 0; band < floats.length; band++) {
                final float level = Float.parseFloat(customEq[band]);
                floats[band] = level / 100.0f;
                mEqualizerSeekBar[band].setProgress((int) ((getBandLevelRange()[1] / 100.0f) + (level / 100.0f)));
            }
        } else {
            // try to load preset
            levelsString = prefs.getString("equalizer.preset." + eqPreset, Equalizer.getZeroedBandsString(numberEqualizerBands));
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

        String[] currentCustomLevels = prefs.getString("audiofx.eq.bandlevels.custom", Equalizer.getZeroedBandsString(numberEqualizerBands)).split(";");

        currentCustomLevels[band] = String.valueOf(level);

        // save
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < numberEqualizerBands; i++) {
            builder.append(currentCustomLevels[i]);
            builder.append(";");
        }
        builder.deleteCharAt(builder.length() - 1);
        prefs.edit().putString("audiofx.eq.bandlevels", builder.toString()).apply();
        prefs.edit().putString("audiofx.eq.bandlevels.custom", builder.toString()).apply();

        mediaManager.updateEqualizer();
    }

    /**
     * Called when user starts touch eq on a preset
     */
    void equalizerCopyToCustom() {
        Log.d(TAG, "equalizerCopyToCustom()");
        StringBuilder bandLevels = new StringBuilder();
        for (int band = 0; band < numberEqualizerBands; band++) {
            final float level = (getBandLevelRange()[0] / 100) + mEqualizerSeekBar[band].getProgress();
            bandLevels.append(level * 100);
            bandLevels.append(";");
        }
        // remove trailing ";"
        bandLevels.deleteCharAt(bandLevels.length() - 1);
        prefs.edit().putString("audiofx.eq.bandlevels.custom", bandLevels.toString()).apply();
        prefs.edit().putString("audiofx.eq.preset", String.valueOf(eqCustomPresetPosition)).apply();
    }

    private String format(String format, Object... args) {
        formatBuilder.setLength(0);
        formatter.format(format, args);
        return formatBuilder.toString();
    }

    private static final int MSG_UPDATE_EQUALIZER = 1;

    int[] getBandLevelRange() {
        String savedCenterFreqs = prefs.getString("equalizer.band_level_range", null);
        if (savedCenterFreqs == null || savedCenterFreqs.isEmpty()) {
            return new int[] { -1500, 1500 };
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
        String savedCenterFreqs = prefs.getString("equalizer.center_freqs", Equalizer.getZeroedBandsString(numberEqualizerBands));
        String[] split = savedCenterFreqs.split(";");
        int[] freqs = new int[split.length];
        for (int i = 0; i < split.length; i++) {
            freqs[i] = Integer.valueOf(split[i]);
        }
        return freqs;
    }

    private void setupPresets() {
        // setup equalizer presets
        final int numPresets = Integer.parseInt(prefs.getString("equalizer.number_of_presets", "0"));
        eqPresetNames = new String[numPresets + 1];

        String[] presetNames = prefs.getString("equalizer.preset_names", "").split("\\|");
        System.arraycopy(presetNames, 0, eqPresetNames, 0, numPresets);
        eqPresetNames[numPresets] = getString(R.string.custom);
        eqCustomPresetPosition = numPresets;

        if (spinnerAdapter == null || spinnerAdapter.getCount() != eqPresetNames.length) {
            spinnerAdapter = new RobotoSpinnerAdapter<>(getContext(), R.layout.spinner_item, eqPresetNames);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(spinnerAdapter);
        }
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}
