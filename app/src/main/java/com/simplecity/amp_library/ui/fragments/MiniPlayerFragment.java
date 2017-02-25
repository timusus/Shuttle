package com.simplecity.amp_library.ui.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import com.simplecity.amp_library.glide.utils.GlideUtils;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.MusicService;
import com.simplecity.amp_library.playback.PlaybackMonitor;
import com.simplecity.amp_library.ui.activities.MainActivity;
import com.simplecity.amp_library.ui.activities.PlayerActivity;
import com.simplecity.amp_library.ui.views.PlayPauseView;
import com.simplecity.amp_library.utils.ColorUtils;
import com.simplecity.amp_library.utils.DrawableUtils;
import com.simplecity.amp_library.utils.MusicServiceConnectionUtils;
import com.simplecity.amp_library.utils.MusicUtils;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class MiniPlayerFragment extends BaseFragment {

    private static final String TAG = "MiniPlayerFragment";

    public static final String UPDATE_MINI_PLAYER = "update_mini_player";

    private BroadcastReceiver statusListener;

    View rootView;
    private PlayPauseView playPauseView;
    private ProgressBar progressBar;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener;

    private CompositeSubscription subscriptions;

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
        rootView.setOnTouchListener(new OnSwipeTouchListener(getActivity()));

        playPauseView = (PlayPauseView) rootView.findViewById(R.id.mini_play);
        playPauseView.setOnClickListener(view -> {
            playPauseView.toggle();
            view.postDelayed(MusicUtils::playOrPause, 200);
        });

        progressBar = (ProgressBar) rootView.findViewById(R.id.progressbar);
        progressBar.setMax(1000);

        themeUIComponents();

        rootView.setBackgroundColor(ColorUtils.getPrimaryColor());
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        update();

        statusListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                if (action != null) {
                    switch (action) {
                        case MusicService.InternalIntents.META_CHANGED:
                            updateTrackInfo();
                            updateMiniPlayerVisibility();
                            setPauseButtonImage();
                            break;
                        case MusicService.InternalIntents.PLAY_STATE_CHANGED:
                            updateTrackInfo();
                            setPauseButtonImage();
                            break;
                        case UPDATE_MINI_PLAYER:
                            update();
                            break;
                    }
                }
            }
        };
        final IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.InternalIntents.META_CHANGED);
        filter.addAction(MusicService.InternalIntents.PLAY_STATE_CHANGED);
        filter.addAction(UPDATE_MINI_PLAYER);
        getActivity().registerReceiver(statusListener, filter);

        subscriptions = new CompositeSubscription();

        Observable<Float> progressObservable = PlaybackMonitor.getInstance().getProgressObservable();
        if (progressObservable != null) {
            subscriptions.add(progressObservable
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(progress -> {
                        progressBar.setProgress((int) (progress * 1000));
                    }));
        }
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(statusListener);
        subscriptions.unsubscribe();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
        rootView.setOnTouchListener(null);

        super.onDestroy();
    }

    void update() {
        updateMiniPlayerVisibility();
        updateTrackInfo();
        setPauseButtonImage();
    }

    private void themeUIComponents() {
        if (progressBar != null) {
            progressBar.setProgressDrawable(DrawableUtils.getProgressDrawable(getActivity(), (LayerDrawable) progressBar.getProgressDrawable()));
        }
        if (rootView != null) {
            rootView.setBackgroundColor(ColorUtils.getPrimaryColor());
        }
    }

    void updateMiniPlayerVisibility() {
        boolean show = !(MusicServiceConnectionUtils.sServiceBinder == null || MusicUtils.getSongId() == -1);
        ((MainActivity) getActivity()).togglePanelVisibility(show);
    }

    void updateTrackInfo() {

        Song song = MusicUtils.getSong();

        if (song != null) {
            TextView trackName = (TextView) rootView.findViewById(R.id.track_name);
            TextView artistName = (TextView) rootView.findViewById(R.id.artist_name);
            ImageView miniArtwork = (ImageView) rootView.findViewById(R.id.mini_album_artwork);

            trackName.setText(song.name);
            artistName.setText(String.format("%s | %s", song.artistName, song.albumName));

            Glide.with(getContext())
                    .load(song)
                    .priority(Priority.HIGH)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(GlideUtils.getMediumPlaceHolderResId())
                    .into(miniArtwork);

            if (rootView != null) {
                rootView.setContentDescription(getString(R.string.btn_now_playing, song.name, song.artistName));
            }
        }
    }

    void setPauseButtonImage() {
        if (playPauseView == null) {
            return;
        }
        if (MusicUtils.isPlaying()) {
            if (playPauseView.isPlay()) {
                playPauseView.toggle();
            }
        } else {
            if (!playPauseView.isPlay()) {
                playPauseView.toggle();
            }
        }
    }

    private class OnSwipeTouchListener implements View.OnTouchListener {

        private final GestureDetector gestureDetector;

        OnSwipeTouchListener(Context context) {
            gestureDetector = new GestureDetector(context, new GestureListener());
        }

        void onSwipeLeft() {
            MusicUtils.next();
        }

        void onSwipeRight() {
            MusicUtils.previous(false);
        }

        public boolean onTouch(View v, MotionEvent event) {

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                rootView.setPressed(true);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                rootView.setPressed(false);
            }

            return gestureDetector.onTouchEvent(event);
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
            public boolean onSingleTapUp(MotionEvent e) {
                Activity parent = getActivity();
                if (getResources().getBoolean(R.bool.isSlidingEnabled)) {
                    if (parent instanceof MainActivity) {
                        ((MainActivity) parent).togglePane();
                    }
                } else {
                    Intent intent = new Intent(parent, PlayerActivity.class);
                    parent.startActivityForResult(intent, MainActivity.REQUEST_SEARCH);
                }
                return super.onSingleTapUp(e);
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
}



