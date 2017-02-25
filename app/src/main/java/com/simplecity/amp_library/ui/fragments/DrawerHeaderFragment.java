package com.simplecity.amp_library.ui.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.glide.utils.GlideUtils;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.MusicService;
import com.simplecity.amp_library.ui.views.CircleImageView;
import com.simplecity.amp_library.utils.ColorUtils;
import com.simplecity.amp_library.utils.MusicUtils;

public class DrawerHeaderFragment extends BaseFragment {

    private static final String TAG = "DrawerHeaderFragment";

    public static final String UPDATE_DRAWER_HEADER = "update_drawer_header";

    private BroadcastReceiver mStatusListener;

    private View mRootView;

    private ImageView mBackgroundImage;
    private CircleImageView mArtistImage;

    private Drawable mBackgroundDrawable;

    private SharedPreferences mPrefs;

    private SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferenceChangeListener;

    public DrawerHeaderFragment() {

    }

    public static DrawerHeaderFragment newInstance() {
        DrawerHeaderFragment fragment = new DrawerHeaderFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBackgroundDrawable = getResources().getDrawable(R.drawable.ic_drawer_header_placeholder);
        mBackgroundDrawable.setColorFilter(new LightingColorFilter(ColorUtils.getPrimaryColor(), 0x00222222));

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());

        mSharedPreferenceChangeListener = (sharedPreferences, key) -> {
            if (key.equals("pref_theme_highlight_color")
                    || key.equals("pref_theme_accent_color")
                    || key.equals("pref_theme_white_accent")) {
                themeUIComponents();
            }
        };

        mPrefs.registerOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (mRootView == null) {
            mRootView = inflater.inflate(R.layout.fragment_drawer_header, container, false);
            mBackgroundImage = (ImageView) mRootView.findViewById(R.id.background_image);
            mBackgroundImage.setImageDrawable(mBackgroundDrawable);
            mArtistImage = (CircleImageView) mRootView.findViewById(R.id.artist_image);
        }
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        mStatusListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                if (action != null) {
                    switch (action) {
                        case MusicService.InternalIntents.META_CHANGED:
                            updateTrackInfo();
                            break;
                        case MusicService.InternalIntents.PLAY_STATE_CHANGED:
                            updateTrackInfo();
                            break;
                        case UPDATE_DRAWER_HEADER:
                            updateTrackInfo();
                            break;
                    }
                }
            }
        };
        final IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.InternalIntents.META_CHANGED);
        filter.addAction(MusicService.InternalIntents.PLAY_STATE_CHANGED);
        filter.addAction(UPDATE_DRAWER_HEADER);
        getActivity().registerReceiver(mStatusListener, filter);

        updateTrackInfo();
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(mStatusListener);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mPrefs.unregisterOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
        super.onDestroy();
    }

    private void themeUIComponents() {
        if (mBackgroundDrawable != null) {
            mBackgroundDrawable.setColorFilter(new LightingColorFilter(ColorUtils.getPrimaryColor(), 0x00222222));
        }
    }

    void updateTrackInfo() {

        Song song = MusicUtils.getSong();
        if (song != null) {
            TextView trackNameView = (TextView) mRootView.findViewById(R.id.line1);
            TextView artistNameView = (TextView) mRootView.findViewById(R.id.line2);
            TextView placeholderText = (TextView) mRootView.findViewById(R.id.placeholder_text);
            trackNameView.setText(song.name);
            artistNameView.setText(String.format("%s - %s", song.albumArtistName, song.albumName));
            placeholderText.setText(R.string.app_name);

            Glide.with(getContext())
                    .load(song)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .error(mBackgroundDrawable)
                    .fallback(mBackgroundDrawable)
                    .into(mBackgroundImage);

            Glide.with(getContext())
                    .load(song.getAlbumArtist())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(GlideUtils.getMediumPlaceHolderResId())
                    .into(mArtistImage);

            if (song.name == null || (song.albumName == null && song.albumArtistName == null)) {
                placeholderText.setVisibility(View.VISIBLE);
                trackNameView.setVisibility(View.GONE);
                artistNameView.setVisibility(View.GONE);
            } else {
                placeholderText.setVisibility(View.GONE);
                trackNameView.setVisibility(View.VISIBLE);
                artistNameView.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}