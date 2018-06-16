package com.simplecity.amp_library.ui.fragments;

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
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.f2prateek.rx.preferences2.RxSharedPreferences;
import com.jakewharton.rxbinding2.widget.RxSeekBar;
import com.jakewharton.rxbinding2.widget.SeekBarChangeEvent;
import com.jakewharton.rxbinding2.widget.SeekBarProgressChangeEvent;
import com.jakewharton.rxbinding2.widget.SeekBarStartChangeEvent;
import com.jakewharton.rxbinding2.widget.SeekBarStopChangeEvent;
import com.jp.wasabeef.glide.transformations.BlurTransformation;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.dagger.module.ActivityModule;
import com.simplecity.amp_library.dagger.module.FragmentModule;
import com.simplecity.amp_library.glide.palette.ColorSet;
import com.simplecity.amp_library.glide.palette.ColorSetTranscoder;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.Genre;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.QueueManager;
import com.simplecity.amp_library.rx.UnsafeAction;
import com.simplecity.amp_library.rx.UnsafeConsumer;
import com.simplecity.amp_library.tagger.TaggerDialog;
import com.simplecity.amp_library.ui.drawer.NavigationEventRelay;
import com.simplecity.amp_library.ui.presenters.PlayerPresenter;
import com.simplecity.amp_library.ui.views.FavoriteActionBarView;
import com.simplecity.amp_library.ui.views.PlayPauseView;
import com.simplecity.amp_library.ui.views.PlayerView;
import com.simplecity.amp_library.ui.views.RepeatButton;
import com.simplecity.amp_library.ui.views.RepeatingImageButton;
import com.simplecity.amp_library.ui.views.ShuffleButton;
import com.simplecity.amp_library.ui.views.SizableSeekBar;
import com.simplecity.amp_library.ui.views.SnowfallView;
import com.simplecity.amp_library.ui.views.multisheet.MultiSheetSlideEventRelay;
import com.simplecity.amp_library.utils.DataManager;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.PlaceholderProvider;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.StringUtils;
import com.simplecity.amp_library.utils.color.ArgbEvaluator;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

public class PlayerFragment extends BaseFragment implements
        PlayerView,
        Toolbar.OnMenuItemClickListener {

    private final String TAG = ((Object) this).getClass().getSimpleName();

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

    @BindView(R.id.let_it_snow)
    SnowfallView snowfallView;

    CompositeDisposable disposables = new CompositeDisposable();

    @Inject
    PlayerPresenter presenter;

    @Inject
    NavigationEventRelay navigationEventRelay;

    @Inject
    MultiSheetSlideEventRelay sheetEventRelay;

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
        super.onCreate(savedInstanceState);

        ShuttleApplication.getInstance().getAppComponent()
                .plus(new ActivityModule(getActivity()))
                .plus(new FragmentModule(this))
                .inject(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_player, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        isLandscape = ShuttleUtils.isLandscape();

        unbinder = ButterKnife.bind(this, view);

        toolbar.setNavigationOnClickListener(v -> getActivity().onBackPressed());
        toolbar.inflateMenu(R.menu.menu_now_playing);
        setupCastMenu(toolbar.getMenu());

        MenuItem favoriteMenuItem = toolbar.getMenu().findItem(R.id.favorite);
        FavoriteActionBarView menuActionView = (FavoriteActionBarView) favoriteMenuItem.getActionView();
        menuActionView.setOnClickListener(v -> onMenuItemClick(favoriteMenuItem));
        toolbar.setOnMenuItemClickListener(this);

        if (playPauseView != null) {
            playPauseView.setOnClickListener(v -> {
                playPauseView.toggle();
                playPauseView.postDelayed(() -> presenter.togglePlayback(), 200);
            });
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
            prevButton.setOnClickListener(v -> presenter.prev(true));
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

        if (!SettingsManager.getInstance().getUsePalette() && !SettingsManager.getInstance().getUsePaletteNowPlayingOnly()) {
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

            disposables.add(sharedSeekBarEvents.subscribe(seekBarChangeEvent -> {
                if (seekBarChangeEvent instanceof SeekBarStartChangeEvent) {
                    isSeeking = true;
                } else if (seekBarChangeEvent instanceof SeekBarStopChangeEvent) {
                    isSeeking = false;
                }
            }, error -> LogUtils.logException(TAG, "Error in seek change event", error)));

            disposables.add(sharedSeekBarEvents
                    .ofType(SeekBarProgressChangeEvent.class)
                    .filter(SeekBarProgressChangeEvent::fromUser)
                    .debounce(15, TimeUnit.MILLISECONDS)
                    .subscribe(seekBarChangeEvent -> presenter.seekTo(seekBarChangeEvent.progress()),
                            error -> LogUtils.logException(TAG, "Error receiving seekbar progress", error)));
        }

        disposables.add(RxSharedPreferences.create(PreferenceManager.getDefaultSharedPreferences(getContext()))
                .getBoolean(SettingsManager.KEY_DISPLAY_REMAINING_TIME)
                .asObservable()
                .subscribe(aBoolean -> presenter.updateRemainingTime()));

        disposables.add(sheetEventRelay.getEvents()
                .subscribe(event -> {
                    if (event.nowPlayingExpanded()) {
                        isExpanded = true;
                        snowfallView.letItSnow();
                    } else if (event.nowPlayingCollapsed()) {
                        isExpanded = false;
                        snowfallView.clear();
                    }
                }, throwable -> Log.e(TAG, "error listening for sheet slide events", throwable)));

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
            snowfallView.letItSnow();
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
                    .error(PlaceholderProvider.getInstance().getPlaceHolderDrawable(song.name, true))
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

        if (SettingsManager.getInstance().getUsePalette()) {

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
        if (!SettingsManager.getInstance().getUsePalette() && !SettingsManager.getInstance().getUsePaletteNowPlayingOnly()) {
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
    public void showToast(String message, int duration) {
        Toast.makeText(getContext(), message, duration).show();
    }

    @Override
    public void showLyricsDialog(MaterialDialog dialog) {
        dialog.show();
    }

    @Override
    public void showTaggerDialog(TaggerDialog taggerDialog) {
        taggerDialog.show(getFragmentManager());
    }

    @Override
    public void showSongInfoDialog(MaterialDialog dialog) {
        dialog.show();
    }

    @Override
    public void showUpgradeDialog(MaterialDialog dialog) {
        dialog.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.favorite:
                ((FavoriteActionBarView) item.getActionView()).toggle();
                presenter.toggleFavorite();
                return true;
            case R.id.lyrics:
                presenter.showLyrics(getContext());
                return true;
            case R.id.goToArtist:
                goToArtist();
                return true;
            case R.id.goToAlbum:
                goToAlbum();
                return true;
            case R.id.goToGenre:
                goToGenre();
                return true;
            case R.id.editTags:
                presenter.editTagsClicked(getActivity());
                return true;
            case R.id.songInfo:
                presenter.songInfoClicked(getContext());
                return true;
            case R.id.share:
                presenter.shareClicked(getContext());
                return true;
        }
        return false;
    }

    @SuppressLint("CheckResult")
    private void goToArtist() {
        AlbumArtist currentAlbumArtist = mediaManager.getAlbumArtist();
        // MusicUtils.getAlbumArtist() is only populate with the album the current Song belongs to.
        // Let's find the matching AlbumArtist in the DataManager.albumArtistRelay
        DataManager.getInstance().getAlbumArtistsRelay()
                .first(Collections.emptyList())
                .flatMapObservable(Observable::fromIterable)
                .filter(albumArtist -> currentAlbumArtist != null && albumArtist.name.equals(currentAlbumArtist.name) && albumArtist.albums.containsAll(currentAlbumArtist.albums))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(albumArtist -> navigationEventRelay.sendEvent(new NavigationEventRelay.NavigationEvent(NavigationEventRelay.NavigationEvent.Type.GO_TO_ARTIST, albumArtist, true)));
    }

    private void goToAlbum() {
        navigationEventRelay.sendEvent(new NavigationEventRelay.NavigationEvent(NavigationEventRelay.NavigationEvent.Type.GO_TO_ALBUM, mediaManager.getAlbum(), true));
    }

    @SuppressLint("CheckResult")
    private void goToGenre() {
        mediaManager.getGenre()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        (UnsafeConsumer<Genre>) genre -> navigationEventRelay.sendEvent(new NavigationEventRelay.NavigationEvent(NavigationEventRelay.NavigationEvent.Type.GO_TO_GENRE, genre, true)),
                        error -> LogUtils.logException(TAG, "Error retrieving genre", error));
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
                        // Update all the colours related to the now playing screen first
                        invalidateColors(intermediateColorSet);

                        // We need to update the nav bar colour at the same time, since it's visible as well.
                        if (SettingsManager.getInstance().getTintNavBar()) {
                            Aesthetic.get(getContext()).colorNavigationBar(intermediateColorSet.getPrimaryColor()).apply();
                        }
                    },
                    () -> {
                        // Wait until the first set of color change animations is complete, before updating Aesthetic.
                        // This allows our invalidateColors() animation to run smoothly, as the Aesthetic color change
                        // introduces some jank.
                        if (!SettingsManager.getInstance().getUsePaletteNowPlayingOnly()) {

                            animateColors(oldColorSet, newColorSet, 450, intermediateColorSet -> {
                                Aesthetic aesthetic = Aesthetic.get(getContext())
                                        .colorPrimary(intermediateColorSet.getPrimaryColor())
                                        .colorAccent(intermediateColorSet.getAccentColor())
                                        .colorStatusBarAuto();

                                aesthetic.apply();
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
}