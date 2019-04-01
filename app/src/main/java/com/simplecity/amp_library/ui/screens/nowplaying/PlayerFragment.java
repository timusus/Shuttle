package com.simplecity.amp_library.ui.screens.nowplaying;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.aesthetic.ColorIsDarkState;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.f2prateek.rx.preferences2.RxSharedPreferences;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.jakewharton.rxbinding2.widget.RxSeekBar;
import com.jakewharton.rxbinding2.widget.SeekBarChangeEvent;
import com.jakewharton.rxbinding2.widget.SeekBarProgressChangeEvent;
import com.jakewharton.rxbinding2.widget.SeekBarStartChangeEvent;
import com.jakewharton.rxbinding2.widget.SeekBarStopChangeEvent;
import com.jp.wasabeef.glide.transformations.BlurTransformation;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.cast.CastManager;
import com.simplecity.amp_library.data.Repository;
import com.simplecity.amp_library.glide.palette.ColorSet;
import com.simplecity.amp_library.glide.palette.ColorSetTranscoder;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.QueueManager;
import com.simplecity.amp_library.rx.UnsafeAction;
import com.simplecity.amp_library.rx.UnsafeConsumer;
import com.simplecity.amp_library.ui.common.BaseFragment;
import com.simplecity.amp_library.ui.dialog.ShareDialog;
import com.simplecity.amp_library.ui.dialog.SongInfoDialog;
import com.simplecity.amp_library.ui.dialog.UpgradeDialog;
import com.simplecity.amp_library.ui.screens.drawer.NavigationEventRelay;
import com.simplecity.amp_library.ui.screens.lyrics.LyricsDialog;
import com.simplecity.amp_library.ui.screens.queue.pager.QueuePagerFragment;
import com.simplecity.amp_library.ui.screens.tagger.TaggerDialog;
import com.simplecity.amp_library.ui.views.FavoriteActionBarView;
import com.simplecity.amp_library.ui.views.PlayPauseView;
import com.simplecity.amp_library.ui.views.RepeatButton;
import com.simplecity.amp_library.ui.views.RepeatingImageButton;
import com.simplecity.amp_library.ui.views.ShuffleButton;
import com.simplecity.amp_library.ui.views.SizableSeekBar;
import com.simplecity.amp_library.ui.views.SnowfallView;
import com.simplecity.amp_library.ui.views.multisheet.MultiSheetSlideEventRelay;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.PlaceholderProvider;
import com.simplecity.amp_library.utils.RingtoneManager;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.StringUtils;
import com.simplecity.amp_library.utils.color.ArgbEvaluator;
import com.simplecity.amp_library.utils.menu.song.SongMenuUtils;
import dagger.android.support.AndroidSupportInjection;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;

public class PlayerFragment extends BaseFragment implements
        PlayerView,
        Toolbar.OnMenuItemClickListener {

    private static final String TAG = "PlayerFragment";

    private boolean isSeeking;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @Nullable
    @BindView(R.id.play)
    PlayPauseView playPauseView;

    @Nullable
    @BindView(R.id.shuffle)
    ShuffleButton shuffleButton;

    @Nullable
    @BindView(R.id.repeat)
    RepeatButton repeatButton;

    @Nullable
    @BindView(R.id.next)
    RepeatingImageButton nextButton;

    @Nullable
    @BindView(R.id.prev)
    RepeatingImageButton prevButton;

    @Nullable
    @BindView(R.id.current_time)
    TextView currentTime;

    @Nullable
    @BindView(R.id.total_time)
    TextView totalTime;

    @Nullable
    @BindView(R.id.text1)
    TextView track;

    @Nullable
    @BindView(R.id.text2)
    TextView album;

    @Nullable
    @BindView(R.id.text3)
    TextView artist;

    @BindView(R.id.backgroundView)
    ImageView backgroundView;

    @Nullable
    @BindView(R.id.seekbar)
    SizableSeekBar seekBar;

    @BindView(R.id.snowfallView)
    SnowfallView snowfallView;

    CompositeDisposable disposables = new CompositeDisposable();

    @Inject
    PlayerPresenter presenter;

    @Inject
    NavigationEventRelay navigationEventRelay;

    @Inject
    MultiSheetSlideEventRelay sheetEventRelay;

    @Inject
    Repository.AlbumArtistsRepository albumArtistsRepository;

    @Inject
    SettingsManager settingsManager;

    private Unbinder unbinder;

    ColorSet colorSet = ColorSet.Companion.empty();

    @Nullable
    private Target<GlideDrawable> target;

    private boolean isLandscape;

    private boolean isExpanded;

    @Nullable
    private ValueAnimator colorAnimator;

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
        AndroidSupportInjection.inject(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_player, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        isLandscape = ShuttleUtils.isLandscape(getContext());

        unbinder = ButterKnife.bind(this, view);

        toolbar.setNavigationOnClickListener(v -> getActivity().onBackPressed());
        toolbar.inflateMenu(R.menu.menu_now_playing);

        if (CastManager.isCastAvailable(getContext(), settingsManager)) {
            MenuItem menuItem = CastButtonFactory.setUpMediaRouteButton(getContext(), toolbar.getMenu(), R.id.media_route_menu_item);
            menuItem.setVisible(true);
        }

        MenuItem favoriteMenuItem = toolbar.getMenu().findItem(R.id.favorite);
        FavoriteActionBarView menuActionView = (FavoriteActionBarView) favoriteMenuItem.getActionView();
        menuActionView.setOnClickListener(v -> onMenuItemClick(favoriteMenuItem));
        toolbar.setOnMenuItemClickListener(this);

        if (playPauseView != null) {
            playPauseView.setOnClickListener(v -> playPauseView.toggle(() -> {
                presenter.togglePlayback();
                return Unit.INSTANCE;
            }));
        }

        if (repeatButton != null) {
            repeatButton.setOnClickListener(v -> presenter.toggleRepeat());
            repeatButton.setTag(":aesthetic_ignore");
        }

        if (shuffleButton != null) {
            shuffleButton.setOnClickListener(v -> presenter.toggleShuffle());
            shuffleButton.setTag(":aesthetic_ignore");
        }

        if (nextButton != null) {
            nextButton.setOnClickListener(v -> presenter.skip());
            nextButton.setRepeatListener((v, duration, repeatCount) -> presenter.scanForward(repeatCount, duration));
        }

        if (prevButton != null) {
            prevButton.setOnClickListener(v -> presenter.prev(false));
            prevButton.setRepeatListener((v, duration, repeatCount) -> presenter.scanBackward(repeatCount, duration));
        }

        if (seekBar != null) {
            seekBar.setMax(1000);
        }

        if (savedInstanceState == null) {
            getChildFragmentManager().beginTransaction()
                    .add(R.id.main_container, QueuePagerFragment.newInstance(), "QueuePagerFragment")
                    .commit();
        }

        getAestheticColorSetDisposable()
                .take(1)
                .subscribe(
                        this::invalidateColors,
                        error -> {
                            // Nothing to do
                        }
                );

        presenter.bindView(this);
    }

    @Override
    public void onDestroyView() {
        if (target != null) {
            Glide.clear(target);
        }
        snowfallView.clear();

        if (colorAnimator != null) {
            colorAnimator.cancel();
        }

        presenter.unbindView(this);
        unbinder.unbind();
        super.onDestroyView();
    }

    public void update() {
        if (presenter != null) {
            presenter.updateTrackInfo();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!settingsManager.getUsePalette() && !settingsManager.getUsePaletteNowPlayingOnly()) {
            disposables.add(getAestheticColorSetDisposable().subscribe(
                    colorSet -> animateColors(PlayerFragment.this.colorSet, colorSet, 800, this::invalidateColors, null),
                    error -> {
                        // Nothing to do
                    })
            );
        }

        if (seekBar != null) {
            Flowable<SeekBarChangeEvent> sharedSeekBarEvents = RxSeekBar.changeEvents(seekBar)
                    .toFlowable(BackpressureStrategy.LATEST)
                    .ofType(SeekBarChangeEvent.class)
                    .observeOn(AndroidSchedulers.mainThread())
                    .share();

            disposables.add(sharedSeekBarEvents.subscribe(
                    seekBarChangeEvent -> {
                        if (seekBarChangeEvent instanceof SeekBarStartChangeEvent) {
                            isSeeking = true;
                        } else if (seekBarChangeEvent instanceof SeekBarStopChangeEvent) {
                            isSeeking = false;
                        }
                    },
                    error -> LogUtils.logException(TAG, "Error in seek change event", error))
            );

            disposables.add(sharedSeekBarEvents
                    .ofType(SeekBarProgressChangeEvent.class)
                    .filter(SeekBarProgressChangeEvent::fromUser)
                    .debounce(15, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            seekBarChangeEvent -> presenter.seekTo(seekBarChangeEvent.progress()),
                            error -> LogUtils.logException(TAG, "Error receiving seekbar progress", error))
            );
        }

        disposables.add(RxSharedPreferences.create(PreferenceManager.getDefaultSharedPreferences(getContext()))
                .getBoolean(SettingsManager.KEY_DISPLAY_REMAINING_TIME)
                .asObservable()
                .subscribe(
                        aBoolean -> presenter.updateRemainingTime(),
                        error -> LogUtils.logException(TAG, "Remaining time changed", error)
                )
        );

        disposables.add(sheetEventRelay.getEvents()
                .subscribe(
                        event -> {
                            if (event.nowPlayingExpanded()) {
                                isExpanded = true;
                                snowfallView.letItSnow(analyticsManager);
                            } else if (event.nowPlayingCollapsed()) {
                                isExpanded = false;
                                snowfallView.clear();
                            }
                        },
                        throwable -> Log.e(TAG, "error listening for sheet slide events", throwable))
        );

        update();
    }

    @Override
    public void onPause() {
        disposables.clear();
        super.onPause();
    }

    @Override
    protected String screenName() {
        return TAG;
    }

    // View implementation

    @Override
    public void setSeekProgress(int progress) {
        if (!isSeeking && seekBar != null) {
            seekBar.setProgress(progress);
        }
    }

    @Override
    public void currentTimeVisibilityChanged(boolean visible) {
        if (currentTime != null) {
            currentTime.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }

    @Override
    public void currentTimeChanged(long seconds) {
        if (currentTime != null) {
            currentTime.setText(StringUtils.makeTimeString(getContext(), seconds));
        }
    }

    @Override
    public void totalTimeChanged(long seconds) {
        if (totalTime != null) {
            totalTime.setText(StringUtils.makeTimeString(getContext(), seconds));
        }
    }

    @Override
    public void queueChanged(int queuePosition, int queueLength) {

    }

    @Override
    public void playbackChanged(boolean isPlaying) {
        if (playPauseView != null) {
            if (isPlaying) {
                if (playPauseView.isPlay()) {
                    playPauseView.toggle(null);
                    playPauseView.setContentDescription(getString(R.string.btn_pause));
                }
            } else {
                if (!playPauseView.isPlay()) {
                    playPauseView.toggle(null);
                    playPauseView.setContentDescription(getString(R.string.btn_play));
                }
            }
        }

        if (!isPlaying) {
            snowfallView.removeSnow();
        }
    }

    @Override
    public void shuffleChanged(@QueueManager.ShuffleMode int shuffleMode) {
        if (shuffleButton != null) {
            shuffleButton.setShuffleMode(shuffleMode);
        }
    }

    @Override
    public void repeatChanged(@QueueManager.RepeatMode int repeatMode) {
        if (repeatButton != null) {
            repeatButton.setRepeatMode(repeatMode);
        }
    }

    @Override
    public void favoriteChanged(boolean isFavorite) {
        FavoriteActionBarView favoriteActionBarView = (FavoriteActionBarView) toolbar.getMenu().findItem(R.id.favorite).getActionView();
        favoriteActionBarView.setIsFavorite(isFavorite);
    }

    Song song = null;

    @Override
    public void trackInfoChanged(@Nullable Song song) {

        if (song == null) return;

        if (isExpanded && !snowfallView.isSnowing()) {
            snowfallView.letItSnow(analyticsManager);
        } else {
            snowfallView.removeSnow();
        }

        String totalTimeString = StringUtils.makeTimeString(getContext(), song.duration / 1000);
        if (!TextUtils.isEmpty(totalTimeString)) {
            if (totalTime != null) {
                totalTime.setText(totalTimeString);
            }
        }

        if (track != null) {
            track.setText(song.name);
            track.setSelected(true);
        }
        if (album != null) {
            album.setText(String.format("%s • %s", song.artistName, song.albumName));
        }

        if (isLandscape) {
            toolbar.setTitle(song.name);
            toolbar.setSubtitle(String.format("%s • %s", song.artistName, song.albumName));

            target = Glide.with(this)
                    .load(song)
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .bitmapTransform(new BlurTransformation(getContext(), 15, 4))
                    .error(PlaceholderProvider.getInstance(getContext()).getPlaceHolderDrawable(song.name, true, settingsManager))
                    .thumbnail(Glide
                            .with(this)
                            .load(this.song)
                            .bitmapTransform(new BlurTransformation(getContext(), 15, 4)))
                    .crossFade(600)
                    .into(backgroundView);

            this.song = song;
        } else {
            backgroundView.setImageDrawable(null);
            toolbar.setTitle(null);
            toolbar.setSubtitle(null);
        }

        if (settingsManager.getUsePalette()) {

            if (paletteTarget != null) {
                Glide.clear(paletteTarget);
            }

            Glide.with(this)
                    .load(song)
                    .asBitmap()
                    .transcode(new ColorSetTranscoder(getContext()), ColorSet.class)
                    .override(250, 250)
                    .priority(Priority.HIGH)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(paletteTarget);
        }
    }

    void invalidateColors(ColorSet colorSet) {

        boolean ignorePalette = false;
        if (!settingsManager.getUsePalette() && !settingsManager.getUsePaletteNowPlayingOnly()) {
            // If we're not using Palette at all, use non-tinted colors for text.
            colorSet.setPrimaryTextColorTinted(colorSet.getPrimaryTextColor());
            colorSet.setSecondaryTextColorTinted(colorSet.getSecondaryTextColor());
            ignorePalette = true;
        }

        if (!isLandscape && backgroundView != null) {
            backgroundView.setBackgroundColor(colorSet.getPrimaryColor());
        }

        if (!isLandscape && currentTime != null) {
            currentTime.setTextColor(colorSet.getPrimaryTextColor());
        }

        if (!isLandscape && totalTime != null) {
            totalTime.setTextColor(colorSet.getPrimaryTextColor());
        }

        if (track != null) {
            track.setTextColor(colorSet.getPrimaryTextColorTinted());
        }

        if (album != null) {
            album.setTextColor(colorSet.getSecondaryTextColorTinted());
        }

        if (artist != null) {
            artist.setTextColor(colorSet.getSecondaryTextColorTinted());
        }

        if (seekBar != null) {
            seekBar.invalidateColors(new ColorIsDarkState(ignorePalette ? colorSet.getAccentColor() : colorSet.getPrimaryTextColorTinted(), false));
        }

        if (shuffleButton != null) {
            shuffleButton.invalidateColors(colorSet.getPrimaryTextColor(), colorSet.getPrimaryTextColorTinted());
        }

        if (repeatButton != null) {
            repeatButton.invalidateColors(colorSet.getPrimaryTextColor(), colorSet.getPrimaryTextColorTinted());
        }

        if (prevButton != null) {
            prevButton.invalidateColors(colorSet.getPrimaryTextColor());
        }

        if (nextButton != null) {
            nextButton.invalidateColors(colorSet.getPrimaryTextColor());
        }

        if (playPauseView != null) {
            playPauseView.setDrawableColor(colorSet.getPrimaryTextColor());
        }

        this.colorSet = colorSet;
    }

    @Override
    public void showLyricsDialog() {
        LyricsDialog.Companion.newInstance().show(getChildFragmentManager());
    }

    @Override
    public void showUpgradeDialog() {
        UpgradeDialog.Companion.newInstance().show(getChildFragmentManager());
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (!SongMenuUtils.INSTANCE.getSongMenuClickListener(mediaManager.getSong(), presenter).onMenuItemClick(item)) {
            switch (item.getItemId()) {
                case R.id.favorite:
                    ((FavoriteActionBarView) item.getActionView()).toggle();
                    presenter.toggleFavorite();
                    return true;
                case R.id.lyrics:
                    presenter.showLyrics();
                    return true;
            }
        }

        return true;
    }

    void animateColors(@NonNull ColorSet from, @NonNull ColorSet to, int duration, @NonNull UnsafeConsumer<ColorSet> consumer, @Nullable UnsafeAction onComplete) {
        colorAnimator = ValueAnimator.ofFloat(1, 0);
        colorAnimator.setDuration(duration);
        colorAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        ArgbEvaluator argbEvaluator = ArgbEvaluator.getInstance();
        colorAnimator.addUpdateListener(animator -> {
            ColorSet colorSet = new ColorSet(
                    (int) argbEvaluator.evaluate(animator.getAnimatedFraction(), from.getPrimaryColor(), to.getPrimaryColor()),
                    (int) argbEvaluator.evaluate(animator.getAnimatedFraction(), from.getAccentColor(), to.getAccentColor()),
                    (int) argbEvaluator.evaluate(animator.getAnimatedFraction(), from.getPrimaryTextColorTinted(), to.getPrimaryTextColorTinted()),
                    (int) argbEvaluator.evaluate(animator.getAnimatedFraction(), from.getSecondaryTextColorTinted(), to.getSecondaryTextColorTinted()),
                    (int) argbEvaluator.evaluate(animator.getAnimatedFraction(), from.getPrimaryTextColor(), to.getPrimaryTextColor()),
                    (int) argbEvaluator.evaluate(animator.getAnimatedFraction(), from.getSecondaryTextColor(), to.getSecondaryTextColor())
            );
            consumer.accept(colorSet);
        });
        colorAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                animation.removeAllListeners();
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
        colorAnimator.start();
    }

    private SimpleTarget<ColorSet> paletteTarget = new SimpleTarget<ColorSet>() {
        @Override
        public void onResourceReady(ColorSet newColorSet, GlideAnimation<? super ColorSet> glideAnimation) {

            if (!isAdded() || getContext() == null) {
                return;
            }

            if (colorSet == newColorSet) {
                return;
            }

            ColorSet oldColorSet = colorSet;

            animateColors(
                    oldColorSet,
                    newColorSet,
                    800,
                    intermediateColorSet -> {

                        if (!isAdded() || getContext() == null) return;

                        // Update all the colours related to the now playing screen first
                        invalidateColors(intermediateColorSet);

                        // We need to update the nav bar colour at the same time, since it's visible as well.
                        if (settingsManager.getTintNavBar()) {
                            Aesthetic.get(getContext()).colorNavigationBar(intermediateColorSet.getPrimaryColor()).apply();
                        }
                    },
                    () -> {
                        if (!isAdded() || getContext() == null) return;

                        // Wait until the first set of color change animations is complete, before updating Aesthetic.
                        // This allows our invalidateColors() animation to run smoothly, as the Aesthetic color change
                        // introduces some jank.
                        if (!settingsManager.getUsePaletteNowPlayingOnly()) {

                            animateColors(oldColorSet, newColorSet, 450, intermediateColorSet -> {

                                if (!isAdded() || getContext() == null) return;

                                Aesthetic.get(getContext())
                                        .colorPrimary(intermediateColorSet.getPrimaryColor())
                                        .colorAccent(intermediateColorSet.getAccentColor())
                                        .colorStatusBarAuto().apply();
                            }, null);
                        }
                    }
            );
        }

        @SuppressLint("CheckResult")
        @Override
        public void onLoadFailed(Exception e, Drawable errorDrawable) {
            super.onLoadFailed(e, errorDrawable);

            getAestheticColorSetDisposable()
                    .take(1)
                    .subscribe(
                            colorSet -> animateColors(PlayerFragment.this.colorSet, colorSet, 800, intermediateColorSet -> invalidateColors(intermediateColorSet), null),
                            error -> {
                                // Nothing ot do
                            }
                    );
        }
    };

    private Observable<ColorSet> getAestheticColorSetDisposable() {
        return Observable.combineLatest(
                Aesthetic.get(getContext()).colorPrimary(),
                Aesthetic.get(getContext()).colorAccent(),
                Pair::new
        ).map(pair -> ColorSet.Companion.fromPrimaryAccentColors(getContext(), pair.first, pair.second));
    }

    // SongMenuContract.View implementation

    @Override
    public void presentCreatePlaylistDialog(@NotNull List<? extends Song> songs) {

    }

    @Override
    public void presentSongInfoDialog(@NotNull Song song) {
        SongInfoDialog.Companion.newInstance(song).show(getChildFragmentManager());
    }

    @Override
    public void onSongsAddedToPlaylist(@NotNull Playlist playlist, int numSongs) {

    }

    @Override
    public void onSongsAddedToQueue(int numSongs) {

    }

    @Override
    public void presentTagEditorDialog(@NotNull Song song) {
        TaggerDialog.newInstance(song).show(getChildFragmentManager());
    }

    @Override
    public void presentDeleteDialog(@NotNull List<? extends Song> songs) {

    }

    @Override
    public void shareSong(@NotNull Song song) {
        ShareDialog.Companion.newInstance(song).show(getChildFragmentManager());
    }

    @Override
    public void presentRingtonePermissionDialog() {
        RingtoneManager.Companion.getDialog(getContext()).show();
    }

    @Override
    public void showRingtoneSetMessage() {
        Toast.makeText(getContext(), R.string.ringtone_set_new, Toast.LENGTH_SHORT).show();
    }
}