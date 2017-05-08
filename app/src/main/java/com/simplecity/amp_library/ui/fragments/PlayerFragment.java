package com.simplecity.amp_library.ui.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.jakewharton.rxbinding.widget.RxSeekBar;
import com.jakewharton.rxbinding.widget.SeekBarChangeEvent;
import com.jakewharton.rxbinding.widget.SeekBarProgressChangeEvent;
import com.jakewharton.rxbinding.widget.SeekBarStartChangeEvent;
import com.jakewharton.rxbinding.widget.SeekBarStopChangeEvent;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.glide.palette.PaletteBitmap;
import com.simplecity.amp_library.glide.palette.PaletteBitmapTranscoder;
import com.simplecity.amp_library.lyrics.LyricsFragment;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.MusicService;
import com.simplecity.amp_library.ui.presenters.PlayerPresenter;
import com.simplecity.amp_library.ui.views.FavoriteActionBarView;
import com.simplecity.amp_library.ui.views.PlayPauseView;
import com.simplecity.amp_library.ui.views.PlayerView;
import com.simplecity.amp_library.ui.views.RepeatingImageButton;
import com.simplecity.amp_library.ui.views.SizableSeekBar;
import com.simplecity.amp_library.utils.ColorUtils;
import com.simplecity.amp_library.utils.DrawableUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.StringUtils;
import com.simplecity.amp_library.utils.ThemeUtils;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

public class PlayerFragment extends BaseFragment implements PlayerView, Toolbar.OnMenuItemClickListener {

    private final String TAG = ((Object) this).getClass().getSimpleName();

    private SizableSeekBar seekBar;

    private boolean isSeeking;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.play)
    PlayPauseView playPauseView;

    @BindView(R.id.shuffle)
    ImageButton shuffleButton;

    @BindView(R.id.repeat)
    ImageButton repeatButton;

    @BindView(R.id.next)
    RepeatingImageButton nextButton;

    @BindView(R.id.prev)
    RepeatingImageButton prevButton;

    @BindView(R.id.current_time)
    TextView currentTime;

    @BindView(R.id.total_time)
    TextView totalTime;

    @BindView(R.id.text1)
    TextView track;

    @BindView(R.id.text2)
    TextView album;

    @Nullable @BindView(R.id.text3)
    TextView artist;

    @Nullable @BindView(R.id.queue_position)
    TextView queuePosition;

    @BindView(R.id.backgroundView)
    View backgroundView;

    private SharedPreferences sharedPreferences;

    private static final String QUEUE_FRAGMENT = "queue_fragment";
    private static final String QUEUE_PAGER_FRAGMENT = "queue_pager_fragment";
    private static final String LYRICS_FRAGMENT = "lyrics_fragment";

    private SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferenceChangeListener;

    private CompositeSubscription subscriptions;

    private PlayerPresenter presenter = new PlayerPresenter();

    private int backgroundColor;

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

        ButterKnife.bind(this, rootView);

        toolbar.setNavigationOnClickListener(v -> getActivity().onBackPressed());
        toolbar.inflateMenu(R.menu.menu_now_playing);
        setupCastMenu(toolbar.getMenu());

        MenuItem favoriteMenuItem = toolbar.getMenu().findItem(R.id.menu_favorite);
        FavoriteActionBarView menuActionView = (FavoriteActionBarView) favoriteMenuItem.getActionView();
        menuActionView.setOnClickListener(v -> onMenuItemClick(favoriteMenuItem));
        toolbar.setOnMenuItemClickListener(this);

        playPauseView.setOnClickListener(v -> {
            playPauseView.toggle();
            playPauseView.postDelayed(() -> presenter.togglePlayback(), 200);
        });

        repeatButton.setOnClickListener(v -> presenter.toggleRepeat());

        shuffleButton = (ImageButton) rootView.findViewById(R.id.shuffle);
        shuffleButton.setOnClickListener(v -> presenter.toggleShuffle());

        nextButton.setOnClickListener(v -> presenter.skip());
        nextButton.setRepeatListener((v, duration, repeatcount) -> presenter.scanForward(repeatcount, duration));

        prevButton.setOnClickListener(v -> presenter.prev(true));
        prevButton.setRepeatListener((v, duration, repeatcount) -> presenter.scanBackward(repeatcount, duration));

        seekBar = (SizableSeekBar) rootView.findViewById(R.id.seekbar);
        seekBar.setMax(1000);

        themeUIComponents();

        if (savedInstanceState == null) {
            getChildFragmentManager().beginTransaction()
                    .add(R.id.main_container, QueuePagerFragment.newInstance(), QUEUE_PAGER_FRAGMENT)
                    .commit();
        }

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
            ThemeUtils.themeSeekBar(getContext(), seekBar, true);
            if (seekBar != null) {
                ThemeUtils.themeSeekBar(getContext(), seekBar, true);
            }
            if (backgroundView != null) {
                backgroundView.setBackgroundColor(ColorUtils.getPrimaryColor());
            }

            if (presenter != null) {
                shuffleChanged(MusicUtils.getShuffleMode());
                repeatChanged(MusicUtils.getRepeatMode());
            }
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
        }));

        subscriptions.add(sharedSeekBarEvents
                .ofType(SeekBarProgressChangeEvent.class)
                .filter(SeekBarProgressChangeEvent::fromUser)
                .debounce(15, TimeUnit.MILLISECONDS)
                .subscribe(seekBarChangeEvent -> presenter.seekTo(seekBarChangeEvent.progress())));
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

    private void toggleLyrics() {
        Fragment fragment = getChildFragmentManager().findFragmentById(R.id.main_container);
        if (fragment instanceof LyricsFragment) {
            return;
        }
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.abc_fade_in, R.anim.abc_fade_out);
        if (fragment instanceof QueueFragment) {
            ft.replace(R.id.main_container, new QueuePagerFragment(), QUEUE_PAGER_FRAGMENT);
        }
        ft.add(R.id.main_container, new LyricsFragment(), LYRICS_FRAGMENT);
        ft.commit();
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
        currentTime.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void currentTimeChanged(long seconds) {
        currentTime.setText(StringUtils.makeTimeString(this.getActivity(), seconds));
    }

    @Override
    public void queueChanged(int queuePosition, int queueLength) {

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
    public void favoriteChanged(boolean isFavorite) {
        FavoriteActionBarView favoriteActionBarView = (FavoriteActionBarView) toolbar.getMenu().findItem(R.id.menu_favorite).getActionView();
        favoriteActionBarView.setIsFavorite(isFavorite);
    }

    @Override
    public void trackInfoChanged(@Nullable Song song) {

        if (song == null) return;

        String totalTime = StringUtils.makeTimeString(this.getActivity(), song.duration / 1000);
        if (!TextUtils.isEmpty(totalTime)) {
            this.totalTime.setText(totalTime);
        }

        track.setText(song.name);
        track.setSelected(true);
        album.setText(String.format("%s | %s", song.artistName, song.albumName));

        //noinspection unchecked
        Glide.with(this)
                .load(song)
                .asBitmap()
                .transcode(new PaletteBitmapTranscoder(getContext()), PaletteBitmap.class)
                .override(100, 100)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(new SimpleTarget<PaletteBitmap>() {
                    @Override
                    public void onResourceReady(PaletteBitmap resource, GlideAnimation<? super PaletteBitmap> glideAnimation) {

                        Palette.Swatch swatch = resource.palette.getDarkMutedSwatch();

                        int newColor = ColorUtils.getPrimaryColor();

                        if (swatch != null) {
                            newColor = swatch.getRgb();
                        }

                        if (backgroundColor != newColor) {
                            // Todo:
                            // This null check is only necessary because backgroundView is null in landscape mode.
                            // Can be removed when that problem is solved.
                            if (backgroundView != null) {
                                ColorUtils.startBackgroundTransition(backgroundView, backgroundColor, newColor);
                            }
                            backgroundColor = newColor;
                        }
                    }
                });
    }

    @Override
    public void showToast(String message, int duration) {
        Toast.makeText(getContext(), message, duration).show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_favorite:
                ((FavoriteActionBarView) item.getActionView()).toggle();
                presenter.toggleFavorite();
                return true;
            case R.id.menu_lyrics:

                return true;
            case R.id.go_to:

                return true;
            case R.id.edit_tags:

                return true;
            case R.id.song_info:

                return true;
        }
        return false;
    }
}