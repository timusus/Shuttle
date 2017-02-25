package com.simplecity.amp_library.ui.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
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
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.MusicService;
import com.simplecity.amp_library.playback.PlaybackMonitor;
import com.simplecity.amp_library.ui.activities.MainActivity;
import com.simplecity.amp_library.ui.views.PlayPauseView;
import com.simplecity.amp_library.ui.views.RepeatingImageButton;
import com.simplecity.amp_library.ui.views.SizableSeekBar;
import com.simplecity.amp_library.utils.ColorUtils;
import com.simplecity.amp_library.utils.DrawableUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.StringUtils;
import com.simplecity.amp_library.utils.ThemeUtils;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class PlayerFragment extends BaseFragment implements
        View.OnClickListener,
        MusicUtils.Defs {

    private final String TAG = ((Object) this).getClass().getSimpleName();

    public static final String UPDATE_PLAYING_FRAGMENT = "update_playing_fragment";

    private SizableSeekBar mSeekBar;
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
    private long startSeekPos = 0;
    private long lastSeekEventTime;

    private BroadcastReceiver statusListener;

    private static final String QUEUE_FRAGMENT = "queue_fragment";
    private static final String QUEUE_PAGER_FRAGMENT = "queue_pager_fragment";
    private static final String LYRICS_FRAGMENT = "lyrics_fragment";

    private SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferenceChangeListener;

    boolean fabIsAnimating = false;

    private View dragView;

    private CompositeSubscription subscriptions;

    private long currentMediaPlayerTime;

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
        repeatButton = (ImageButton) rootView.findViewById(R.id.repeat);
        shuffleButton = (ImageButton) rootView.findViewById(R.id.shuffle);
        nextButton = (RepeatingImageButton) rootView.findViewById(R.id.next);
        prevButton = (RepeatingImageButton) rootView.findViewById(R.id.prev);

        currentTime = (TextView) rootView.findViewById(R.id.current_time);
        totalTime = (TextView) rootView.findViewById(R.id.total_time);
        queuePosition = (TextView) rootView.findViewById(R.id.queue_position);
        track = (TextView) rootView.findViewById(R.id.text1);
        album = (TextView) rootView.findViewById(R.id.text2);
        artist = (TextView) rootView.findViewById(R.id.text3);

        textViewContainer = rootView.findViewById(R.id.textContainer);
        buttonContainer = rootView.findViewById(R.id.button_container);

        fab = (FloatingActionButton) rootView.findViewById(R.id.fab);

        playPauseView.setOnClickListener(this);
        nextButton.setOnClickListener(this);
        nextButton.setRepeatListener(mFastForwardListener);
        prevButton.setOnClickListener(this);
        prevButton.setRepeatListener(mRewindListener);
        repeatButton.setOnClickListener(this);
        shuffleButton.setOnClickListener(this);
        if (fab != null) {
            fab.setOnClickListener(this);
        }

        mSeekBar = (SizableSeekBar) rootView.findViewById(R.id.seekbar);
        mSeekBar.setMax(1000);

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

        updateTrackInfo();
        setPauseButtonImage();
        setShuffleButtonImage();
        setRepeatButtonImage();

        return rootView;
    }

    public void themeUIComponents() {

        if (nextButton != null) {
            nextButton.setImageDrawable(DrawableUtils.getColoredStateListDrawableWithThemeColor(getActivity(), nextButton.getDrawable(), ThemeUtils.WHITE));
        }
        if (prevButton != null) {
            prevButton.setImageDrawable(DrawableUtils.getColoredStateListDrawableWithThemeColor(getActivity(), prevButton.getDrawable(), ThemeUtils.WHITE));
        }
        if (mSeekBar != null) {
            ThemeUtils.themeSeekBar(getActivity(), mSeekBar, true);
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

        setShuffleButtonImage();
        setRepeatButtonImage();
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
                            break;
                        case MusicService.InternalIntents.PLAY_STATE_CHANGED:
                            updateTrackInfo();
                            setPauseButtonImage();
                            break;
                        case MusicService.InternalIntents.SHUFFLE_CHANGED:
                            updateTrackInfo();
                            setShuffleButtonImage();
                            break;
                        case MusicService.InternalIntents.REPEAT_CHANGED:
                            setRepeatButtonImage();
                            break;
                        case UPDATE_PLAYING_FRAGMENT:
                            update();
                            break;
                    }
                }
            }
        };
        final IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.InternalIntents.META_CHANGED);
        filter.addAction(MusicService.InternalIntents.PLAY_STATE_CHANGED);
        filter.addAction(MusicService.InternalIntents.SHUFFLE_CHANGED);
        filter.addAction(MusicService.InternalIntents.REPEAT_CHANGED);
        filter.addAction(UPDATE_PLAYING_FRAGMENT);
        getActivity().registerReceiver(statusListener, filter);

        subscriptions = new CompositeSubscription();

        subscriptions.add(PlaybackMonitor.getInstance().getProgressObservable()
                .filter(progress -> !isSeeking)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(progress -> {
                    mSeekBar.setProgress((int) (progress * 1000));
                }));

        subscriptions.add(PlaybackMonitor.getInstance().getCurrentTimeObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pos -> refreshCurrentTimeText(pos / 1000)));

        subscriptions.add(Observable.interval(500, TimeUnit.MILLISECONDS)
                .onBackpressureDrop()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aLong -> {
                    if (MusicUtils.isPlaying()) {
                        currentTime.setVisibility(View.VISIBLE);
                    } else {
                        currentTime.setVisibility(currentTime.getVisibility() == View.INVISIBLE ? View.VISIBLE : View.INVISIBLE);
                    }
                }));

        Observable<SeekBarChangeEvent> sharedSeekBarEvents = RxSeekBar.changeEvents(mSeekBar)
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
        }));

        subscriptions.add(sharedSeekBarEvents
                .ofType(SeekBarProgressChangeEvent.class)
                .filter(SeekBarProgressChangeEvent::fromUser)
                .debounce(15, TimeUnit.MILLISECONDS)
                .subscribe(seekBarChangeEvent -> {
                    MusicUtils.seekTo(MusicUtils.getDuration() * seekBarChangeEvent.progress() / 1000);
                }));
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(statusListener);

        subscriptions.unsubscribe();

        super.onPause();
    }

    @Override
    public void onDestroy() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
        super.onDestroy();
    }

    void update() {
        updateTrackInfo();
        setPauseButtonImage();
        setShuffleButtonImage();
        setRepeatButtonImage();
    }

    @Override
    public void onClick(View view) {
        if (view == playPauseView) {
            playPauseView.toggle();
            MusicUtils.playOrPause();
        } else if (view == nextButton) {
            MusicUtils.next();
        } else if (view == prevButton) {
            MusicUtils.previous(true);
        } else if (view == repeatButton) {
            cycleRepeat();
        } else if (view == shuffleButton) {
            toggleShuffle();
        } else if (view == fab) {
            if (fabIsAnimating) {
                return;
            }
            toggleQueue();
        }
    }

    /**
     * Method refreshCurrentTimeText.
     *
     * @param pos the {@link long} getPosition of the current track}
     */
    private void refreshCurrentTimeText(final long pos) {
        if (pos != currentMediaPlayerTime) {
            currentTime.setText(StringUtils.makeTimeString(this.getActivity(), pos));
        }
        currentMediaPlayerTime = pos;
    }

    public void updateTrackInfo() {

        String totalTime = StringUtils.makeTimeString(this.getActivity(), MusicUtils.getDuration() / 1000);
        String currentQueuePos = String.valueOf(MusicUtils.getQueuePosition() + 1);
        String queueLength = String.valueOf(MusicUtils.getQueue().size());

        if (totalTime != null && totalTime.length() != 0) {
            this.totalTime.setText(String.format(" / %s", totalTime));
        }

        Song song = MusicUtils.getSong();
        if (song != null) {
            track.setText(song.name);
            track.setSelected(true);
            album.setText(String.format("%s | %s", song.artistName, song.albumName));
        }

        queuePosition.setText(String.format("%s / %s", currentQueuePos, queueLength));

        FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.supportInvalidateOptionsMenu();
        }

        Fragment fragment = getChildFragmentManager().findFragmentById(R.id.main_container);
        if (fragment != null && fragment instanceof LyricsFragment) {
            ((LyricsFragment) fragment).updateLyrics();
        }
    }

    private final RepeatingImageButton.RepeatListener mRewindListener = (v, howlong, repcnt) -> scanBackward(repcnt, howlong);

    private final RepeatingImageButton.RepeatListener mFastForwardListener = (v, howlong, repcnt) -> scanForward(repcnt, howlong);

    public void scanForward(final int repcnt, long delta) {
        if (repcnt == 0) {
            startSeekPos = MusicUtils.getPosition();
            lastSeekEventTime = 0;
        } else {
            if (delta < 5000) {
                // seek at 10x speed for the first 5 seconds
                delta = delta * 10;
            } else {
                // seek at 40x after that
                delta = 50000 + (delta - 5000) * 40;
            }
            long newpos = startSeekPos + delta;
            final long duration = MusicUtils.getDuration();
            if (newpos >= duration) {
                // move to next track
                MusicUtils.next();
                startSeekPos -= duration; // is OK to go negative
                newpos -= duration;
            }
            if (delta - lastSeekEventTime > 250 || repcnt < 0) {
                MusicUtils.seekTo(newpos);
                lastSeekEventTime = delta;
            }
        }
    }

    public void scanBackward(final int repcnt, long delta) {
        if (repcnt == 0) {
            startSeekPos = MusicUtils.getPosition();
            lastSeekEventTime = 0;
        } else {
            if (delta < 5000) {
                // seek at 10x speed for the first 5 seconds
                delta = delta * 10;
            } else {
                // seek at 40x after that
                delta = 50000 + (delta - 5000) * 40;
            }
            long newpos = startSeekPos - delta;
            if (newpos < 0) {
                // move to previous track
                MusicUtils.previous(true);
                final long duration = MusicUtils.getDuration();
                startSeekPos += duration;
                newpos += duration;
            }
            if (delta - lastSeekEventTime > 250 || repcnt < 0) {
                MusicUtils.seekTo(newpos);
                lastSeekEventTime = delta;
            }
        }
    }

    public void setShuffleButtonImage() {
        if (shuffleButton == null) {
            return;
        }

        switch (MusicUtils.getShuffleMode()) {

            case MusicService.ShuffleMode.OFF:
                shuffleButton.setImageDrawable(DrawableUtils.getWhiteDrawable(getActivity(), R.drawable.ic_shuffle_white));
                shuffleButton.setContentDescription(getString(R.string.btn_shuffle_off));
                break;

            default:
                shuffleButton.setImageDrawable(DrawableUtils.getColoredAccentDrawableNonWhite(getActivity(), getResources().getDrawable(R.drawable.ic_shuffle_white)));
                shuffleButton.setContentDescription(getString(R.string.btn_shuffle_on));
                break;
        }
    }

    public void setPauseButtonImage() {
        if (playPauseView == null) {
            return;
        }
        if (MusicUtils.isPlaying()) {
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

    public void setRepeatButtonImage() {
        if (repeatButton == null) {
            return;
        }
        switch (MusicUtils.getRepeatMode()) {

            case MusicService.RepeatMode.ALL:
                repeatButton.setImageDrawable(DrawableUtils.getColoredAccentDrawableNonWhite(getActivity(), getResources().getDrawable(R.drawable.ic_repeat_white)));
                repeatButton.setContentDescription(getResources().getString(R.string.btn_repeat_all));
                break;

            case MusicService.RepeatMode.ONE:
                repeatButton.setImageDrawable(DrawableUtils.getColoredAccentDrawableNonWhite(getActivity(), getResources().getDrawable(R.drawable.ic_repeat_one_white)));
                repeatButton.setContentDescription(getResources().getString(R.string.btn_repeat_current));
                break;

            default:
                repeatButton.setImageDrawable(DrawableUtils.getWhiteDrawable(getActivity(), R.drawable.ic_repeat_white));
                repeatButton.setContentDescription(getResources().getString(R.string.btn_repeat_off));
                break;
        }
    }

    private void cycleRepeat() {
        MusicUtils.cycleRepeat();
        setRepeatButtonImage();
    }

    private void toggleShuffle() {
        MusicUtils.toggleShuffleMode();
        setRepeatButtonImage();
        setShuffleButtonImage();
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
}