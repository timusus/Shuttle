package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.AppCompatSeekBar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.utils.ThemeUtils;

public class CustomColorPicker extends FrameLayout
        implements SeekBar.OnSeekBarChangeListener,
        TextWatcher {

    public int color;

    private View colorView;

    private AppCompatSeekBar redSeekBar;
    private AppCompatSeekBar greenSeekBar;
    private AppCompatSeekBar blueSeekBar;

    private TextView redValue;
    private TextView greenValue;
    private TextView blueValue;

    private EditText editText;

    public CustomColorPicker(Context context, int selectedColor) {
        super(context);

        color = selectedColor;

        View customView = LayoutInflater.from(context).inflate(R.layout.dialog_color_custom, this, true);

        colorView = customView.findViewById(R.id.view);

        redSeekBar = (AppCompatSeekBar) findViewById(R.id.seekbarRed);
        ThemeUtils.themeSeekBar(redSeekBar);
        redSeekBar.setMax(255);
        redSeekBar.setOnSeekBarChangeListener(this);

        greenSeekBar = (AppCompatSeekBar) findViewById(R.id.seekbarGreen);
        ThemeUtils.themeSeekBar(greenSeekBar);
        greenSeekBar.setMax(255);
        greenSeekBar.setOnSeekBarChangeListener(this);

        blueSeekBar = (AppCompatSeekBar) findViewById(R.id.seekbarBlue);
        ThemeUtils.themeSeekBar(blueSeekBar);
        blueSeekBar.setMax(255);
        blueSeekBar.setOnSeekBarChangeListener(this);

        redValue = (TextView) findViewById(R.id.redValue);
        greenValue = (TextView) findViewById(R.id.greenValue);
        blueValue = (TextView) findViewById(R.id.blueValue);

        editText = (EditText) findViewById(R.id.editText);
        editText.addTextChangedListener(this);

        updateView();

        setProgress();
        setProgressValues();
        setText();
    }

    public int getColor() {
        return color;
    }

    private void updateView() {
        colorView.setBackgroundColor(color);
    }

    private void setText() {
        editText.setText(String.format("%06X", (0xFFFFFF & color)));
        editText.setSelection(editText.length());
    }

    private void setProgress() {
        redSeekBar.setProgress(Color.red(color));
        greenSeekBar.setProgress(Color.green(color));
        blueSeekBar.setProgress(Color.blue(color));
    }

    private void setProgressValues() {
        redValue.setText(String.valueOf(Color.red(color)));
        greenValue.setText(String.valueOf(Color.green(color)));
        blueValue.setText(String.valueOf(Color.blue(color)));
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            color = Color.rgb(redSeekBar.getProgress(), greenSeekBar.getProgress(), blueSeekBar.getProgress());
            setProgressValues();
            setText();
            updateView();
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        try {
            color = Color.parseColor("#" + s);
        } catch (IllegalArgumentException ignored) {

        }
        updateView();
        setProgress();
        setProgressValues();
    }
}
