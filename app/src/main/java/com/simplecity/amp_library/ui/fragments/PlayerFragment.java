package com.simplecity.amp_library.ui.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.jakewharton.rxbinding.widget.RxSeekBar;
import com.jakewharton.rxbinding.widget.SeekBarChangeEvent;
import com.jakewharton.rxbinding.widget.SeekBarProgressChangeEvent;
import com.jakewharton.rxbinding.widget.SeekBarStartChangeEvent;
import com.jakewharton.rxbinding.widget.SeekBarStopChangeEvent;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.lyrics.LyricsFragment;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.MusicService;
import com.simplecity.amp_library.ui.activities.MainActivity;
import com.simplecity.amp_library.ui.presenters.PlayerPresenter;
import com.simplecity.amp_library.ui.views.PlayPauseView;
import com.simplecity.amp_library.ui.views.PlayerView;
import com.simplecity.amp_library.ui.views.RepeatingImageButton;
import com.simplecity.amp_library.ui.views.SizableSeekBar;
import com.simplecity.amp_library.utils.ColorUtils;
import com.simplecity.amp_library.utils.DrawableUtils;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.StringUtils;
import com.simplecity.amp_library.utils.ThemeUtils;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

public class PlayerFragment extends BaseFragment implements PlayerView {

    private final String TAG = ((Object) this).getClass().getSimpleName();

    private SizableSeekBar seekBar;
    private boolean isSeeking;

    private PlayPauseView playPauseView;

    private ImageButton shuffleButton;
    private ImageButton repeatButton;

    private RepeatingImageButton nextButton;
    private RepeatingImageButton prevButton;

    FloatingActionButton fab;

    private TextView artist;
    private TextView album;
    private TextView track;
    private TextView currentTime;
    private TextView totalTime;
    private TextView queuePosition;

    private View textViewContainer;
    private View buttonContainer;

    private View bottomView;

    private SharedPreferences sharedPreferences;

    private static final String QUEUE_FRAGMENT = "queue_fragment";
    private static final String QUEUE_PAGER_FRAGMENT = "queue_pager_fragment";
    private static final String LYRICS_FRAGMENT = "lyrics_fragment";

    private SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferenceChangeListener;

    boolean fabIsAnimating = false;

    private View dragView;

    private CompositeSubscription subscriptions;

    private PlayerPresenter presenter = new PlayerPresenter();

    public PlayerFragment() {
    }

    public static PlayerFragment newInstance() {
        PlayerFragment playerFragment = new PlayerFragment();
        Bundle args = new Bundle();
        playerFragment.setArguments(args);
        return playerFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getActivity());

        mSharedPreferenceChangeListener = (sharedPreferences, key) -> {
            if (key.equals("pref_theme_highlight_color") || key.equals("pref_theme_accent_color") || key.equals("pref_theme_white_accent")) {
                themeUIComponents();
            }
        };

        sharedPreferences.registerOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_player, container, false);

        bottomView = rootView.findViewById(R.id.bottom_view);

        playPauseView = (PlayPauseView) rootView.findViewById(R.id.play);
        playPauseView.setOnClickListener(v -> {
            playPauseView.toggle();
            playPauseView.postDelayed(() -> presenter.togglePlayback(), 200);
        });

        repeatButton = (ImageButton) rootView.findViewById(R.id.repeat);
        repeatButton.setOnClickListener(v -> presenter.toggleRepeat());

        shuffleButton = (ImageButton) rootView.findViewById(R.id.shuffle);
        shuffleButton.setOnClickListener(v -> presenter.toggleShuffle());

        nextButton = (RepeatingImageButton) rootView.findViewById(R.id.next);
        nextButton.setOnClickListener(v -> presenter.skip());
        nextButton.setRepeatListener((v, duration, repeatcount) -> presenter.scanForward(repeatcount, duration));

        prevButton = (RepeatingImageButton) rootView.findViewById(R.id.prev);
        prevButton.setOnClickListener(v -> presenter.prev(true));
        prevButton.setRepeatListener((v, duration, repeatcount) -> presenter.scanBackward(repeatcount, duration));

        currentTime = (TextView) rootView.findViewById(R.id.current_time);
        totalTime = (TextView) rootView.findViewById(R.id.total_time);
        queuePosition = (TextView) rootView.findViewById(R.id.queue_position);
        track = (TextView) rootView.findViewById(R.id.text1);
        album = (TextView) rootView.findViewById(R.id.text2);
        artist = (TextView) rootView.findViewById(R.id.text3);

        textViewContainer = rootView.findViewById(R.id.textContainer);
        buttonContainer = rootView.findViewById(R.id.button_container);

        fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(v -> {
                if (fabIsAnimating) {
                    return;
                }
                toggleQueue();
            });
        }

        seekBar = (SizableSeekBar) rootView.findViewById(R.id.seekbar);
        seekBar.setMax(1000);

        themeUIComponents();

        //If the queueFragment exists in the child fragment manager, retrieve it
        Fragment queueFragment = getChildFragmentManager().findFragmentByTag(QUEUE_FRAGMENT);

        Fragment queuePagerFragment = getChildFragmentManager().findFragmentByTag(QUEUE_PAGER_FRAGMENT);
        //We only want to add th
        if (queueFragment == null && queuePagerFragment == null) {
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.main_container, QueuePagerFragment.newInstance(), QUEUE_PAGER_FRAGMENT)
                    .commit();
        }

        toggleFabVisibility(queueFragment == null, false);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        presenter.bindView(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        presenter.unbindView(this);
    }

    public void update() {
        if (presenter != null) {
            presenter.updateTrackInfo();
        }
    }

    public void themeUIComponents() {

        if (nextButton != null) {
            nextButton.setImageDrawable(DrawableUtils.getColoredStateListDrawableWithThemeColor(getActivity(), nextButton.getDrawable(), ThemeUtils.WHITE));
        }
        if (prevButton != null) {
            prevButton.setImageDrawable(DrawableUtils.getColoredStateListDrawableWithThemeColor(getActivity(), prevButton.getDrawable(), ThemeUtils.WHITE));
        }
        if (seekBar != null) {
            ThemeUtils.themeSeekBar(getActivity(), seekBar, true);
        }
        if (textViewContainer != null) {
            textViewContainer.setBackgroundColor(ColorUtils.getPrimaryColorDark(getActivity()));
        }
        if (buttonContainer != null) {
            buttonContainer.setBackgroundColor(ColorUtils.getPrimaryColor());
        }
        if (fab != null) {
            fab.setBackgroundTintList(ColorStateList.valueOf(ColorUtils.getAccentColor()));
            fab.setRippleColor(ColorUtils.darkerise(ColorUtils.getAccentColor(), 0.85f));
        }

        if (presenter != null) {
            shuffleChanged(MusicUtils.getShuffleMode());
            repeatChanged(MusicUtils.getRepeatMode());
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        subscriptions = new CompositeSubscription();

        Observable<SeekBarChangeEvent> sharedSeekBarEvents = RxSeekBar.changeEvents(seekBar)
                .onBackpressureLatest()
                .ofType(SeekBarChangeEvent.class)
                .observeOn(AndroidSchedulers.mainThread())
                .share();

        subscriptions.add(sharedSeekBarEvents.subscribe(seekBarChangeEvent -> {
            if (seekBarChangeEvent instanceof SeekBarStartChangeEvent) {
                isSeeking = true;
            } else if (seekBarChangeEvent instanceof SeekBarStopChangeEvent) {
                isSeeking = false;
            }
        }, error -> LogUtils.logException("PlayerFragment: Error in seek change event", error)));

        subscriptions.add(sharedSeekBarEvents
                .ofType(SeekBarProgressChangeEvent.class)
                .filter(SeekBarProgressChangeEvent::fromUser)
                .debounce(15, TimeUnit.MILLISECONDS)
                .subscribe(seekBarChangeEvent -> presenter.seekTo(seekBarChangeEvent.progress()),
                        error -> LogUtils.logException("PlayerFragment: Error receiving seekbar progress", error)));
    }

    @Override
    public void onPause() {
        subscriptions.unsubscribe();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
        super.onDestroy();
    }

    public void toggleLyrics() {
        Fragment fragment = getChildFragmentManager().findFragmentById(R.id.main_container);
        if (fragment instanceof LyricsFragment) {
            return;
        }
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.abc_fade_in, R.anim.abc_fade_out);
        if (fragment instanceof QueueFragment) {
            ft.replace(R.id.main_container, new QueuePagerFragment(), QUEUE_PAGER_FRAGMENT);
            toggleFabVisibility(true, true);
        }
        ft.add(R.id.main_container, new LyricsFragment(), LYRICS_FRAGMENT);
        ft.commit();
    }

    public void toggleQueue() {

        Fragment lyricsFragment = getChildFragmentManager().findFragmentByTag(LYRICS_FRAGMENT);
        Fragment queueFragment = getChildFragmentManager().findFragmentByTag(QUEUE_FRAGMENT);
        Fragment queuePagerFragment = getChildFragmentManager().findFragmentByTag(QUEUE_PAGER_FRAGMENT);

        final FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out);
        //Remove the lyrics fragment

        if (lyricsFragment != null) {
            fragmentTransaction.remove(lyricsFragment);
        }

        if (queueFragment != null) {
            fragmentTransaction.remove(queueFragment);
            fragmentTransaction.replace(R.id.main_container, new QueuePagerFragment(), QUEUE_PAGER_FRAGMENT);
            toggleFabVisibility(true, true);

        } else if (queuePagerFragment != null) {
            fragmentTransaction.remove(queuePagerFragment);
            fragmentTransaction.add(R.id.queue_container, QueueFragment.newInstance(), QUEUE_FRAGMENT);
            bottomView.setClickable(true);
            toggleFabVisibility(false, true);
        }

        fragmentTransaction.commitAllowingStateLoss();
    }

    private void toggleFabVisibility(boolean show, boolean animate) {
        if (fab == null) {
            return;
        }

        if (show && fab.getVisibility() == View.VISIBLE) {
            return;
        }

        if (!show && fab.getVisibility() == View.GONE) {
            return;
        }

        if (fabIsAnimating) {
            return;
        }

        if (!animate) {
            if (show) {
                fab.setVisibility(View.VISIBLE);
            } else {
                fab.setVisibility(View.GONE);
            }
            return;
        }

        fabIsAnimating = true;

        if (show) {

            fab.setScaleX(0f);
            fab.setScaleY(0f);
            fab.setAlpha(0f);
            fab.setVisibility(View.VISIBLE);

            ObjectAnimator fadeAnimator = ObjectAnimator.ofFloat(fab, "alpha", 0f, 1f);
            ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(fab, "scaleX", 0f, 1f);
            ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(fab, "scaleY", 0f, 1f);

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(fadeAnimator, scaleXAnimator, scaleYAnimator);
            animatorSet.setDuration(350);
            animatorSet.start();

            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    fabIsAnimating = false;
                }
            });

        } else {
            ObjectAnimator fadeAnimator = ObjectAnimator.ofFloat(fab, "alpha", 1f, 0f);
            ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(fab, "scaleX", 1f, 0f);
            ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(fab, "scaleY", 1f, 0f);

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(fadeAnimator, scaleXAnimator, scaleYAnimator);
            animatorSet.setDuration(250);
            animatorSet.start();

            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    fab.setVisibility(View.GONE);
                    fabIsAnimating = false;
                }
            });
        }
    }

    public void setDragView(View view) {
        dragView = view;
        ((MainActivity) getActivity()).setDragView(view, true);
    }

    public View getDragView() {
        return dragView;
    }

    public boolean isQueueShowing() {
        return getChildFragmentManager().findFragmentByTag(QUEUE_FRAGMENT) != null;
    }

    @Override
    protected String screenName() {
        return TAG;
    }

    ////////////////////////////////////////////////////////////////////
    // View implementation
    ////////////////////////////////////////////////////////////////////

    @Override
    public void setSeekProgress(int progress) {
        if (!isSeeking) {
            seekBar.setProgress(progress);
        }
    }

    @Override
    public void currentTimeVisibilityChanged(boolean visible) {
        currentTime.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void currentTimeChanged(long seconds) {
        currentTime.setText(StringUtils.makeTimeString(this.getActivity(), seconds));
    }

    @Override
    public void queueChanged(int queuePosition, int queueLength) {
        this.queuePosition.setText(String.format("%s / %s", queuePosition, queueLength));
    }

    @Override
    public void playbackChanged(boolean isPlaying) {
        if (isPlaying) {
            if (playPauseView.isPlay()) {
                playPauseView.toggle();
                playPauseView.setContentDescription(getString(R.string.btn_pause));
            }
        } else {
            if (!playPauseView.isPlay()) {
                playPauseView.toggle();
                playPauseView.setContentDescription(getString(R.string.btn_play));
            }
        }
    }

    @Override
    public void shuffleChanged(@MusicService.ShuffleMode int shuffleMode) {
        switch (MusicUtils.getShuffleMode()) {
            case MusicService.ShuffleMode.OFF:
                shuffleButton.setImageDrawable(DrawableUtils.getWhiteDrawable(getActivity(), R.drawable.ic_shuffle_white));
                shuffleButton.setContentDescription(getString(R.string.btn_shuffle_off));
                break;
            case MusicService.ShuffleMode.ON:
                shuffleButton.setImageDrawable(DrawableUtils.getColoredAccentDrawableNonWhite(getActivity(), getResources().getDrawable(R.drawable.ic_shuffle_white)));
                shuffleButton.setContentDescription(getString(R.string.btn_shuffle_on));
                break;
        }
    }

    @Override
    public void repeatChanged(@MusicService.RepeatMode int repeatMode) {
        switch (MusicUtils.getRepeatMode()) {
            case MusicService.RepeatMode.ALL:
                repeatButton.setImageDrawable(DrawableUtils.getColoredAccentDrawableNonWhite(getActivity(), getResources().getDrawable(R.drawable.ic_repeat_white)));
                repeatButton.setContentDescription(getResources().getString(R.string.btn_repeat_all));
                break;
            case MusicService.RepeatMode.ONE:
                repeatButton.setImageDrawable(DrawableUtils.getColoredAccentDrawableNonWhite(getActivity(), getResources().getDrawable(R.drawable.ic_repeat_one_white)));
                repeatButton.setContentDescription(getResources().getString(R.string.btn_repeat_current));
                break;
            case MusicService.RepeatMode.OFF:
                repeatButton.setImageDrawable(DrawableUtils.getWhiteDrawable(getActivity(), R.drawable.ic_repeat_white));
                repeatButton.setContentDescription(getResources().getString(R.string.btn_repeat_off));
                break;
        }
    }

    @Override
    public void favoriteChanged() {
        getActivity().supportInvalidateOptionsMenu();
    }

    @Override
    public void trackInfoChanged(@Nullable Song song) {

        if (song == null) return;

        String totalTime = StringUtils.makeTimeString(this.getActivity(), song.duration / 1000);
        if (!TextUtils.isEmpty(totalTime)) {
            this.totalTime.setText(String.format(" / %s", totalTime));
        }

        track.setText(song.name);
        track.setSelected(true);
        album.setText(String.format("%s | %s", song.artistName, song.albumName));
    }
}