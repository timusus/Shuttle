package com.simplecity.amp_library.ui.activities;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.playback.MusicService;
import com.simplecity.amp_library.ui.fragments.WidgetFragment;
import com.simplecity.amp_library.ui.views.SizableSeekBar;
import com.simplecity.amp_library.ui.views.SlidingTabLayout;
import com.simplecity.amp_library.ui.widgets.BaseWidgetProvider;
import com.simplecity.amp_library.utils.ColorUtils;
import com.simplecity.amp_library.utils.DialogUtils;
import com.simplecity.amp_library.utils.DrawableUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.ThemeUtils;

public abstract class BaseWidgetConfigure extends BaseActivity implements
        View.OnClickListener,
        CheckBox.OnCheckedChangeListener,
        SeekBar.OnSeekBarChangeListener,
        ViewPager.OnPageChangeListener {

    abstract int[] getWidgetLayouts();

    abstract String getLayoutIdString();

    abstract String getUpdateCommandString();

    abstract int getRootViewId();

    int[] mLayouts;
    private int mLayoutId;
    private int mAppWidgetId;

    private float mAlpha = 0.15f;

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private ViewPager mPager;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private WidgetPagerAdapter mPagerAdapter;

    private SharedPreferences mPrefs;

    private Button mBackgroundColorButton;
    private Button mTextColorButton;
    private SizableSeekBar mSeekBar;

    private int mBackgroundColor;
    private int mTextColor;
    private boolean mShowAlbumArt;
    private boolean mInvertIcons;

    SparseArray<Fragment> registeredFragments = new SparseArray<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Bundle extras = this.getIntent().getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }

        ThemeUtils.setTheme(this);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mLayoutId = mPrefs.getInt(getLayoutIdString() + mAppWidgetId, getWidgetLayouts()[0]);
        mBackgroundColor = mPrefs.getInt(BaseWidgetProvider.ARG_WIDGET_BACKGROUND_COLOR + mAppWidgetId, getResources().getColor(R.color.white));
        mTextColor = mPrefs.getInt(BaseWidgetProvider.ARG_WIDGET_TEXT_COLOR + mAppWidgetId, Color.WHITE);
        mShowAlbumArt = mPrefs.getBoolean(BaseWidgetProvider.ARG_WIDGET_SHOW_ARTWORK + mAppWidgetId, true);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_widget_config);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ThemeUtils.themeActionBar(this);

        mLayouts = getWidgetLayouts();

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new WidgetPagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);

        SlidingTabLayout slidingTabLayout = (SlidingTabLayout) findViewById(R.id.tabs);
        slidingTabLayout.setShouldExpand(true);
        slidingTabLayout.setViewPager(mPager);
        slidingTabLayout.setOnPageChangeListener(this);
        ThemeUtils.themeTabLayout(this, slidingTabLayout);

        Button doneButton = (Button) findViewById(R.id.btn_done);
        doneButton.setOnClickListener(this);

        mBackgroundColorButton = (Button) findViewById(R.id.btn_background_color);
        mBackgroundColorButton.setOnClickListener(this);

        mTextColorButton = (Button) findViewById(R.id.btn_text_color);
        mTextColorButton.setOnClickListener(this);

        CheckBox showAlbumArtCheckbox = (CheckBox) findViewById(R.id.checkBox1);
        showAlbumArtCheckbox.setOnCheckedChangeListener(this);

        CheckBox invertedIconsCheckbox = (CheckBox) findViewById(R.id.checkBox2);
        invertedIconsCheckbox.setOnCheckedChangeListener(this);

        mSeekBar = (SizableSeekBar) findViewById(R.id.seekBar1);
        mSeekBar.setOnSeekBarChangeListener(this);
        ThemeUtils.themeSeekBar(this, mSeekBar);

        updateWidgetUI();
    }

    @Override
    public void onBackPressed() {
        if (mPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else {
            // Otherwise, select the previous step.
            mPager.setCurrentItem(mPager.getCurrentItem() - 1);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {

        if (compoundButton.getId() == R.id.checkBox1) {
            mShowAlbumArt = checked;
            mPrefs.edit().putBoolean(BaseWidgetProvider.ARG_WIDGET_SHOW_ARTWORK + mAppWidgetId, mShowAlbumArt).apply();

        }
        if (compoundButton.getId() == R.id.checkBox2) {
            mInvertIcons = checked;
            mPrefs.edit().putBoolean(BaseWidgetProvider.ARG_WIDGET_INVERT_ICONS + mAppWidgetId, mInvertIcons).apply();
        }
        updateWidgetUI();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_done) {

            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);

            RemoteViews remoteViews = new RemoteViews(this.getPackageName(), mLayoutId);
            BaseWidgetProvider.setupButtons(this, remoteViews, mAppWidgetId, getRootViewId());
            appWidgetManager.updateAppWidget(mAppWidgetId, remoteViews);

            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_OK, resultValue);

            // Send broadcast intent to any running MediaPlaybackService so it can
            // wrap around with an immediate update.
            Intent updateIntent = new Intent(MusicService.ServiceCommand.SERVICE_COMMAND);
            updateIntent.putExtra(MusicService.MediaButtonCommand.CMD_NAME, getUpdateCommandString());
            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{mAppWidgetId});
            updateIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
            sendBroadcast(updateIntent);

            finish();
        }

        if (view.getId() == R.id.btn_background_color) {
            DialogUtils.showCustomColorPickerDialog(this, ColorUtils.adjustAlpha(mBackgroundColor, mAlpha), color -> {
                mBackgroundColor = color;
                mPrefs.edit()
                        .putInt(BaseWidgetProvider.ARG_WIDGET_BACKGROUND_COLOR + mAppWidgetId, color)
                        .apply();

                Fragment fragment = mPagerAdapter.getRegisteredFragment(mPager.getCurrentItem());
                if (fragment != null) {
                    View fragmentView = fragment.getView();
                    if (fragmentView != null) {
                        View layout = fragmentView.findViewById(getRootViewId());
                        layout.setBackgroundColor(ColorUtils.adjustAlpha(mBackgroundColor, mAlpha));
                    }
                }
            });
        }
        if (view.getId() == R.id.btn_text_color) {
            DialogUtils.showCustomColorPickerDialog(this, mTextColor, color -> {
                mTextColor = color;
                mPrefs.edit()
                        .putInt(BaseWidgetProvider.ARG_WIDGET_TEXT_COLOR + mAppWidgetId, color)
                        .apply();

                Fragment fragment = mPagerAdapter.getRegisteredFragment(mPager.getCurrentItem());
                if (fragment != null) {
                    View widgetView = fragment.getView();
                    if (widgetView != null) {

                        TextView text1 = (TextView) widgetView.findViewById(R.id.text1);
                        TextView text2 = (TextView) widgetView.findViewById(R.id.text2);
                        TextView text3 = (TextView) widgetView.findViewById(R.id.text3);

                        if (text1 != null) {
                            text1.setTextColor(mTextColor);
                        }
                        if (text2 != null) {
                            text2.setTextColor(mTextColor);
                        }
                        if (text3 != null) {
                            text3.setTextColor(mTextColor);
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean byUser) {
        Fragment fragment = mPagerAdapter.getRegisteredFragment(mPager.getCurrentItem());
        if (fragment != null) {
            View view = fragment.getView();
            if (view != null) {
                View layout = view.findViewById(getRootViewId());
                mAlpha = 1 - (progress / 255f);
                int adjustedColor = ColorUtils.adjustAlpha(mBackgroundColor, mAlpha);
                layout.setBackgroundColor(adjustedColor);
                mPrefs.edit()
                        .putInt(BaseWidgetProvider.ARG_WIDGET_BACKGROUND_COLOR + mAppWidgetId, adjustedColor)
                        .apply();
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onPageScrolled(int i, float v, int i2) {

    }

    @Override
    public void onPageSelected(int position) {
        mLayoutId = mLayouts[position];
        mPrefs.edit().putInt(getLayoutIdString() + mAppWidgetId, mLayoutId).apply();
        updateWidgetUI();
    }

    @Override
    public void onPageScrollStateChanged(int i) {

    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        updateWidgetUI();
        super.onServiceConnected(componentName, iBinder);
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private class WidgetPagerAdapter extends FragmentStatePagerAdapter {
        public WidgetPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return new WidgetFragment().newInstance(mLayouts[position]);
        }

        @Override
        public int getCount() {
            return mLayouts.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "Layout " + String.valueOf(position + 1);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            registeredFragments.put(position, fragment);
            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            registeredFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        public Fragment getRegisteredFragment(int position) {
            return registeredFragments.get(position);
        }
    }

    public void updateWidgetUI() {

        mBackgroundColor = mPrefs.getInt(BaseWidgetProvider.ARG_WIDGET_BACKGROUND_COLOR + mAppWidgetId, getResources().getColor(R.color.white));
        mTextColor = mPrefs.getInt(BaseWidgetProvider.ARG_WIDGET_TEXT_COLOR + mAppWidgetId, getResources().getColor(R.color.white));

        Drawable backgroundButtonDrawable = getResources().getDrawable(R.drawable.bg_rounded);
        backgroundButtonDrawable.setBounds(0, 0, 60, 60);
        backgroundButtonDrawable = DrawableUtils.getColoredDrawable(backgroundButtonDrawable, mBackgroundColor);
        mBackgroundColorButton.setCompoundDrawables(backgroundButtonDrawable, null, null, null);

        Drawable textButtonDrawable = getResources().getDrawable(R.drawable.bg_rounded);
        textButtonDrawable.setBounds(0, 0, 60, 60);
        textButtonDrawable = DrawableUtils.getColoredDrawable(textButtonDrawable, mTextColor);
        mTextColorButton.setCompoundDrawables(textButtonDrawable, null, null, null);

        Fragment fragment = mPagerAdapter.getRegisteredFragment(mPager.getCurrentItem());
        if (fragment != null) {
            View view = fragment.getView();
            if (view != null) {
                View widgetLayout = view.findViewById(getRootViewId());
                widgetLayout.setBackgroundColor(ColorUtils.adjustAlpha(mBackgroundColor, mAlpha));
                TextView text1 = (TextView) widgetLayout.findViewById(R.id.text1);
                TextView text2 = (TextView) widgetLayout.findViewById(R.id.text2);
                TextView text3 = (TextView) widgetLayout.findViewById(R.id.text3);
                String trackName = MusicUtils.getSongName();
                String artistName = MusicUtils.getAlbumArtistName();
                final String albumName = MusicUtils.getAlbumName();
                if (trackName != null && text1 != null) {
                    text1.setText(trackName);
                    text1.setTextColor(mTextColor);
                }
                if (artistName != null && albumName != null && text2 != null && text3 == null) {
                    text2.setText(artistName + " | " + albumName);
                    text2.setTextColor(mTextColor);
                } else if (artistName != null && albumName != null && text2 != null) {
                    text2.setText(albumName);
                    text2.setTextColor(mTextColor);
                    text3.setText(artistName);
                    text3.setTextColor(mTextColor);
                }

                ImageButton shuffleButton = (ImageButton) widgetLayout.findViewById(R.id.shuffle_button);
                ImageButton prevButton = (ImageButton) widgetLayout.findViewById(R.id.prev_button);
                ImageButton playButton = (ImageButton) widgetLayout.findViewById(R.id.play_button);
                ImageButton skipButton = (ImageButton) widgetLayout.findViewById(R.id.next_button);
                ImageButton repeatButton = (ImageButton) widgetLayout.findViewById(R.id.repeat_button);

                if (shuffleButton != null) {
                    shuffleButton.setImageDrawable(DrawableUtils.getColoredStateListDrawable(this, shuffleButton.getDrawable(), mInvertIcons));
                }
                if (prevButton != null) {
                    prevButton.setImageDrawable(DrawableUtils.getColoredStateListDrawable(this, prevButton.getDrawable(), mInvertIcons));
                }
                if (playButton != null) {
                    playButton.setImageDrawable(DrawableUtils.getColoredStateListDrawable(this, playButton.getDrawable(), mInvertIcons));
                }
                if (skipButton != null) {
                    skipButton.setImageDrawable(DrawableUtils.getColoredStateListDrawable(this, skipButton.getDrawable(), mInvertIcons));
                }
                if (repeatButton != null) {
                    repeatButton.setImageDrawable(DrawableUtils.getColoredStateListDrawable(this, repeatButton.getDrawable(), mInvertIcons));
                }

                final ImageView albumArt = (ImageView) widgetLayout.findViewById(R.id.album_art);
                if (albumArt != null) {

                    if (!mShowAlbumArt) {
                        albumArt.setVisibility(View.GONE);
                        return;
                    } else {
                        albumArt.setVisibility(View.VISIBLE);
                        if (mPager.getCurrentItem() == 1) {
                            int colorFilterColor = getResources().getColor(R.color.color_filter);
                            albumArt.setColorFilter(colorFilterColor);
                            mPrefs.edit().putInt(BaseWidgetProvider.ARG_WIDGET_COLOR_FILTER + mAppWidgetId, colorFilterColor).apply();
                        } else {
                            mPrefs.edit().putInt(BaseWidgetProvider.ARG_WIDGET_COLOR_FILTER + mAppWidgetId, -1).apply();
                        }
                    }

                    Glide.with(this)
                            .load(MusicUtils.getSong())
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.ic_placeholder_light_medium)
                            .into(albumArt);
                }
            }
        }
    }
}
