package com.simplecity.amp_library.ui.fragments;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.aesthetic.Util;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.jakewharton.rxbinding2.widget.RxSeekBar;
import com.jakewharton.rxbinding2.widget.SeekBarChangeEvent;
import com.jakewharton.rxbinding2.widget.SeekBarProgressChangeEvent;
import com.jakewharton.rxbinding2.widget.SeekBarStartChangeEvent;
import com.jakewharton.rxbinding2.widget.SeekBarStopChangeEvent;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.dagger.module.FragmentModule;
import com.simplecity.amp_library.glide.palette.PaletteBitmap;
import com.simplecity.amp_library.glide.palette.PaletteBitmapTranscoder;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.MusicService;
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
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.StringUtils;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class PlayerFragment extends BaseFragment implements
        PlayerView,
        Toolbar.OnMenuItemClickListener {

    private final String TAG = ((Object) this).getClass().getSimpleName();

    private SizableSeekBar seekBar;

    private boolean isSeeking;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.play)
    PlayPauseView playPauseView;

    @BindView(R.id.shuffle)
    ShuffleButton shuffleButton;

    @BindView(R.id.repeat)
    RepeatButton repeatButton;

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

    @BindView(R.id.backgroundView)
    View backgroundView;

    private CompositeDisposable disposables = new CompositeDisposable();

    @Inject PlayerPresenter presenter;

    @Inject NavigationEventRelay navigationEventRelay;
    private Unbinder unbinder;

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
                .plus(new FragmentModule(this))
                .inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_player, container, false);

        unbinder = ButterKnife.bind(this, rootView);

        toolbar.setNavigationOnClickListener(v -> getActivity().onBackPressed());
        toolbar.inflateMenu(R.menu.menu_now_playing);
        setupCastMenu(toolbar.getMenu());

        MenuItem favoriteMenuItem = toolbar.getMenu().findItem(R.id.favorite);
        FavoriteActionBarView menuActionView = (FavoriteActionBarView) favoriteMenuItem.getActionView();
        menuActionView.setOnClickListener(v -> onMenuItemClick(favoriteMenuItem));
        toolbar.setOnMenuItemClickListener(this);

        playPauseView.setOnClickListener(v -> {
            playPauseView.toggle();
            playPauseView.postDelayed(() -> presenter.togglePlayback(), 200);
        });

        repeatButton.setOnClickListener(v -> presenter.toggleRepeat());

        shuffleButton = (ShuffleButton) rootView.findViewById(R.id.shuffle);
        shuffleButton.setOnClickListener(v -> presenter.toggleShuffle());

        nextButton.setOnClickListener(v -> presenter.skip());
        nextButton.setRepeatListener((v, duration, repeatcount) -> presenter.scanForward(repeatcount, duration));

        prevButton.setOnClickListener(v -> presenter.prev(true));
        prevButton.setRepeatListener((v, duration, repeatcount) -> presenter.scanBackward(repeatcount, duration));

        seekBar = (SizableSeekBar) rootView.findViewById(R.id.seekbar);
        seekBar.setMax(1000);

        if (savedInstanceState == null) {
            getChildFragmentManager().beginTransaction()
                    .add(R.id.main_container, QueuePagerFragment.newInstance(), "QueuePagerFragment")
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

        disposables.add(Aesthetic.get()
                .colorPrimary()
                .subscribe(this::invalidateColors));

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

    @Override
    public void onPause() {
        disposables.clear();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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
        shuffleButton.setShuffleMode(shuffleMode);
    }

    @Override
    public void repeatChanged(@MusicService.RepeatMode int repeatMode) {
        repeatButton.setRepeatMode(repeatMode);
    }

    @Override
    public void favoriteChanged(boolean isFavorite) {
        FavoriteActionBarView favoriteActionBarView = (FavoriteActionBarView) toolbar.getMenu().findItem(R.id.favorite).getActionView();
        favoriteActionBarView.setIsFavorite(isFavorite);
    }

    @Override
    public void trackInfoChanged(@Nullable Song song) {

        if (song == null) return;

        String totalTimeString = StringUtils.makeTimeString(this.getActivity(), song.duration / 1000);
        if (!TextUtils.isEmpty(totalTimeString)) {
            totalTime.setText(totalTimeString);
        }

        track.setText(song.name);
        track.setSelected(true);
        album.setText(String.format("%s | %s", song.artistName, song.albumName));

        if (SettingsManager.getInstance().getUsePalette()) {
            //noinspection unchecked
            Glide.with(this)
                    .load(song)
                    .asBitmap()
                    .transcode(new PaletteBitmapTranscoder(getContext()), PaletteBitmap.class)
                    .override(250, 250)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(new SimpleTarget<PaletteBitmap>() {
                        @Override
                        public void onResourceReady(PaletteBitmap resource, GlideAnimation<? super PaletteBitmap> glideAnimation) {
                            Palette.Swatch swatch = resource.palette.getDarkMutedSwatch();
                            if (swatch != null) {
                                if (!SettingsManager.getInstance().getUsePaletteNowPlayingOnly()) {
                                    // Set Aesthetic colors globally, based on the current Palette swatch
                                    Aesthetic.get()
                                            .colorPrimary()
                                            .take(1)
                                            .subscribe(integer -> {
                                                ValueAnimator valueAnimator = ValueAnimator.ofInt(integer, swatch.getRgb());
                                                valueAnimator.setEvaluator(new ArgbEvaluator());
                                                valueAnimator.setDuration(450);
                                                valueAnimator.addUpdateListener(animator -> Aesthetic.get()
                                                        .colorPrimary((Integer) animator.getAnimatedValue())
                                                        .colorStatusBarAuto()
                                                        .apply());
                                                valueAnimator.start();
                                            });
                                }
                            } else {
                                Aesthetic.get()
                                        .colorPrimary()
                                        .take(1)
                                        .subscribe(color -> invalidateColors(color));
                            }
                        }

                        @Override
                        public void onLoadFailed(Exception e, Drawable errorDrawable) {
                            super.onLoadFailed(e, errorDrawable);
                            Aesthetic.get()
                                    .colorPrimary()
                                    .take(1)
                                    .subscribe(color -> invalidateColors(color));
                        }
                    });
        }
    }

    private void invalidateColors(int color) {
        boolean isColorLight = Util.isColorLight(color);
        int textColor = isColorLight ? Color.BLACK : Color.WHITE;
        backgroundView.setBackgroundColor(color);
        currentTime.setTextColor(textColor);
        totalTime.setTextColor(textColor);
        track.setTextColor(textColor);
        album.setTextColor(textColor);
        if (artist != null) {
            artist.setTextColor(textColor);
        }
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
                navigationEventRelay.sendEvent(new NavigationEventRelay.NavigationEvent(NavigationEventRelay.NavigationEvent.Type.GO_TO_ARTIST, MusicUtils.getAlbumArtist(), true));
                return true;
            case R.id.goToAlbum:
                navigationEventRelay.sendEvent(new NavigationEventRelay.NavigationEvent(NavigationEventRelay.NavigationEvent.Type.GO_TO_ALBUM, MusicUtils.getAlbum(), true));
                return true;
            case R.id.editTags:
                presenter.editTagsClicked();
                return true;
            case R.id.songInfo:
                presenter.songInfoClicked(getContext());
                return true;
        }
        return false;
    }
}