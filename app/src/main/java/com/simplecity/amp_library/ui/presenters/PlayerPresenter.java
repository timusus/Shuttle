package com.simplecity.amp_library.ui.presenters;

import android.content.Context;
import android.content.IntentFilter;
import android.support.annotation.NonNull;

import com.cantrowitz.rxbroadcast.RxBroadcast;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.lyrics.LyricsDialog;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.MusicService;
import com.simplecity.amp_library.playback.PlaybackMonitor;
import com.simplecity.amp_library.tagger.TaggerDialog;
import com.simplecity.amp_library.ui.dialog.BiographyDialog;
import com.simplecity.amp_library.ui.dialog.ShareDialog;
import com.simplecity.amp_library.ui.views.PlayerView;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.PlaylistUtils;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class PlayerPresenter extends Presenter<PlayerView> {

    private static final String TAG = "PlayerPresenter";

    private long startSeekPos = 0;
    private long lastSeekEventTime;

    private long currentPlaybackTime;
    private boolean currentPlaybackTimeVisible;

    @Inject
    public PlayerPresenter() {
    }

    @Override
    public void unbindView(@NonNull PlayerView view) {
        super.unbindView(view);
    }

    @Override
    public void bindView(@NonNull PlayerView view) {
        super.bindView(view);

        updateTrackInfo();
        updateShuffleMode();
        updatePlaystate();
        updateRepeatMode();

        addDisposable(PlaybackMonitor.getInstance().getProgressObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(progress -> view.setSeekProgress((int) (progress * 1000)),
                        error -> LogUtils.logException(TAG, "PlayerPresenter: Error updating seek progress", error)));

        addDisposable(PlaybackMonitor.getInstance().getCurrentTimeObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pos -> refreshCurrentTimeText(pos / 1000),
                        error -> LogUtils.logException(TAG, "PlayerPresenter: Error refreshing time text", error)));

        addDisposable(Flowable.interval(500, TimeUnit.MILLISECONDS)
                .onBackpressureDrop()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aLong -> setCurrentTimeVisibility(MusicUtils.isPlaying() || !currentPlaybackTimeVisible),
                        error -> LogUtils.logException(TAG, "PlayerPresenter: Error emitting current time", error)));

        final IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.InternalIntents.META_CHANGED);
        filter.addAction(MusicService.InternalIntents.QUEUE_CHANGED);
        filter.addAction(MusicService.InternalIntents.PLAY_STATE_CHANGED);
        filter.addAction(MusicService.InternalIntents.SHUFFLE_CHANGED);
        filter.addAction(MusicService.InternalIntents.REPEAT_CHANGED);
        filter.addAction(MusicService.InternalIntents.SERVICE_CONNECTED);
        filter.addAction(MusicService.InternalIntents.FAVORITE_CHANGED);

        addDisposable(RxBroadcast.fromBroadcast(ShuttleApplication.getInstance(), filter)
                .toFlowable(BackpressureStrategy.LATEST)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(intent -> {
                    final String action = intent.getAction();
                    if (action != null) {
                        switch (action) {
                            case MusicService.InternalIntents.META_CHANGED:
                                updateTrackInfo();
                                break;
                            case MusicService.InternalIntents.QUEUE_CHANGED:
                                updateTrackInfo();
                                break;
                            case MusicService.InternalIntents.PLAY_STATE_CHANGED:
                                updateTrackInfo();
                                updatePlaystate();
                                break;
                            case MusicService.InternalIntents.SHUFFLE_CHANGED:
                                updateTrackInfo();
                                updateShuffleMode();
                                break;
                            case MusicService.InternalIntents.REPEAT_CHANGED:
                                updateRepeatMode();
                                break;
                            case MusicService.InternalIntents.SERVICE_CONNECTED:
                                updateTrackInfo();
                                updatePlaystate();
                                updateShuffleMode();
                                updateRepeatMode();
                                break;
                            case MusicService.InternalIntents.FAVORITE_CHANGED:
                                PlaylistUtils.isFavorite(MusicUtils.getSong())
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(this::updateFavorite);
                                break;
                        }
                    }
                }, error -> LogUtils.logException(TAG, "PlayerPresenter: Error sending broadcast", error)));
    }


    private void refreshCurrentTimeText(long playbackTime) {
        if (playbackTime != currentPlaybackTime) {
            PlayerView view = getView();
            if (view != null) {
                view.currentTimeChanged(playbackTime);
            }
        }
        currentPlaybackTime = playbackTime;
    }

    private void setCurrentTimeVisibility(boolean visible) {
        if (visible != currentPlaybackTimeVisible) {
            PlayerView view = getView();
            if (view != null) {
                view.currentTimeVisibilityChanged(visible);
            }
        }
        currentPlaybackTimeVisible = visible;
    }

    private void updateFavorite(boolean isFavorite) {
        PlayerView view = getView();
        if (view != null) {
            view.favoriteChanged(isFavorite);
        }
    }

    public void updateTrackInfo() {
        PlayerView view = getView();
        if (view != null) {
            view.trackInfoChanged(MusicUtils.getSong());
            view.currentTimeChanged(MusicUtils.getPosition() / 1000);
            view.queueChanged(MusicUtils.getQueuePosition() + 1, MusicUtils.getQueue().size());

            addDisposable(PlaylistUtils.isFavorite(MusicUtils.getSong())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(isFavorite -> updateFavorite((isFavorite))));
        }
    }

    private void updatePlaystate() {
        PlayerView view = getView();
        if (view != null) {
            view.playbackChanged(MusicUtils.isPlaying());
        }
    }

    private void updateShuffleMode() {
        PlayerView view = getView();
        if (view != null) {
            view.repeatChanged(MusicUtils.getRepeatMode());
            view.shuffleChanged(MusicUtils.getShuffleMode());
        }
    }

    private void updateRepeatMode() {
        PlayerView view = getView();
        if (view != null) {
            view.repeatChanged(MusicUtils.getRepeatMode());
        }
    }

    public void togglePlayback() {
        MusicUtils.playOrPause();
        updatePlaystate();
    }

    public void toggleFavorite() {
        MusicUtils.toggleFavorite();
    }

    public void skip() {
        MusicUtils.next();
    }

    public void prev(boolean allowTrackRestart) {
        MusicUtils.previous(allowTrackRestart);
    }

    public void toggleShuffle() {
        MusicUtils.toggleShuffleMode();
        updateShuffleMode();
    }

    public void toggleRepeat() {
        MusicUtils.cycleRepeat();
        updateRepeatMode();
    }

    public void seekTo(int progress) {
        MusicUtils.seekTo(MusicUtils.getDuration() * progress / 1000);
    }

    public void scanForward(final int repeatCount, long delta) {
        if (repeatCount == 0) {
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
            if (delta - lastSeekEventTime > 250 || repeatCount < 0) {
                MusicUtils.seekTo(newpos);
                lastSeekEventTime = delta;
            }
        }
    }

    public void scanBackward(final int repeatCount, long delta) {
        if (repeatCount == 0) {
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
            if (delta - lastSeekEventTime > 250 || repeatCount < 0) {
                MusicUtils.seekTo(newpos);
                lastSeekEventTime = delta;
            }
        }
    }

    public void showLyrics(Context context) {
        PlayerView playerView = getView();
        if (playerView != null) {
            playerView.showLyricsDialog(new LyricsDialog().getDialog(context));
        }
    }

    public void editTagsClicked() {
        PlayerView playerView = getView();
        if (playerView != null) {
            playerView.showTaggerDialog(TaggerDialog.newInstance(MusicUtils.getSong()));
        }
    }

    public void songInfoClicked(Context context) {
        PlayerView playerView = getView();
        if (playerView != null) {
            Song song = MusicUtils.getSong();
            if (song != null) {
                playerView.showSongInfoDialog(BiographyDialog.getSongInfoDialog(context, song));
            }
        }
    }

    public void shareClicked(Context context) {
        PlayerView playerView = getView();
        if (playerView != null) {
            Song song = MusicUtils.getSong();
            if (song != null) {
                ShareDialog.getDialog(context, song).show();
            }
        }
    }
}