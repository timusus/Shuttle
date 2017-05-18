package com.simplecity.amp_library.ui.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.glide.utils.GlideUtils;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.ui.presenters.PlayerPresenter;
import com.simplecity.amp_library.ui.views.PlayPauseView;
import com.simplecity.amp_library.ui.views.PlayerViewAdapter;
import com.simplecity.amp_library.utils.ColorUtils;
import com.simplecity.amp_library.utils.DrawableUtils;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import test.com.multisheetview.ui.view.MultiSheetView;

public class MiniPlayerFragment extends BaseFragment {

    private static final String TAG = "MiniPlayerFragment";

    View rootView;

    @BindView(R.id.mini_play)
    PlayPauseView playPauseView;

    @BindView(R.id.progressbar)
    ProgressBar progressBar;

    @BindView(R.id.track_name)
    TextView trackName;

    @BindView(R.id.artist_name)
    TextView artistName;

    @BindView(R.id.mini_album_artwork)
    ImageView miniArtwork;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener;

    @Inject
    PlayerPresenter presenter;

    public MiniPlayerFragment() {

    }

    public static MiniPlayerFragment newInstance() {
        MiniPlayerFragment fragment = new MiniPlayerFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ShuttleApplication.getInstance().getAppComponent().inject(this);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        onSharedPreferenceChangeListener = (sharedPreferences, key) -> {
            if (key.equals("pref_theme_highlight_color") || key.equals("pref_theme_accent_color") || key.equals("pref_theme_white_accent")) {
                themeUIComponents();
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_mini_player, container, false);

        ButterKnife.bind(this, rootView);

        rootView.setBackgroundColor(ColorUtils.getPrimaryColor());
        rootView.setOnClickListener(v -> {
            MultiSheetView multiSheetView = MultiSheetView.getParentMultiSheetView(rootView);
            if (multiSheetView != null) {
                multiSheetView.expandSheet(MultiSheetView.Sheet.FIRST);
            }
        });
        rootView.setOnTouchListener(new OnSwipeTouchListener(getActivity()));

        playPauseView.setOnClickListener(v -> {
            playPauseView.toggle();
            playPauseView.postDelayed(() -> presenter.togglePlayback(), 200);
        });

        progressBar.setMax(1000);

        themeUIComponents();

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        presenter.bindView(playerViewAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (presenter != null) {
            presenter.updateTrackInfo();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        presenter.unbindView(playerViewAdapter);
    }

    @Override
    public void onDestroy() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
        rootView.setOnTouchListener(null);

        super.onDestroy();
    }

    private void themeUIComponents() {
        progressBar.setProgressDrawable(DrawableUtils.getProgressDrawable(getActivity(), (LayerDrawable) progressBar.getProgressDrawable()));

        rootView.setBackgroundColor(ColorUtils.getPrimaryColor());
    }

    private class OnSwipeTouchListener implements View.OnTouchListener {

        private final GestureDetector gestureDetector;

        OnSwipeTouchListener(Context context) {
            gestureDetector = new GestureDetector(context, new GestureListener());
        }

        void onSwipeLeft() {
            presenter.skip();
        }

        void onSwipeRight() {
            presenter.prev(false);
        }

        public boolean onTouch(View v, MotionEvent event) {

            boolean consumed = gestureDetector.onTouchEvent(event);

            if (!consumed) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    v.performClick();
                }
            }

            return consumed;
        }

        private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

            private static final int SWIPE_DISTANCE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            GestureListener() {
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float distanceX = e2.getX() - e1.getX();
                float distanceY = e2.getY() - e1.getY();
                if (Math.abs(distanceX) > Math.abs(distanceY) && Math.abs(distanceX) > SWIPE_DISTANCE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (distanceX > 0)
                        onSwipeRight();
                    else
                        onSwipeLeft();
                    return true;
                }
                return false;
            }
        }
    }

    @Override
    protected String screenName() {
        return TAG;
    }

    PlayerViewAdapter playerViewAdapter = new PlayerViewAdapter() {

        @Override
        public void setSeekProgress(int progress) {
            progressBar.setProgress(progress);
        }


        @Override
        public void playbackChanged(boolean isPlaying) {
            if (isPlaying) {
                if (playPauseView.isPlay()) {
                    playPauseView.toggle();
                }
            } else {
                if (!playPauseView.isPlay()) {
                    playPauseView.toggle();
                }
            }
        }

        @Override
        public void trackInfoChanged(@Nullable Song song) {

            if (song == null) return;

            trackName.setText(song.name);
            artistName.setText(String.format("%s | %s", song.artistName, song.albumName));

            Glide.with(getContext())
                    .load(song)
                    .priority(Priority.HIGH)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(GlideUtils.getMediumPlaceHolderResId())
                    .into(miniArtwork);

            rootView.setContentDescription(getString(R.string.btn_now_playing, song.name, song.artistName));

        }
    };
}