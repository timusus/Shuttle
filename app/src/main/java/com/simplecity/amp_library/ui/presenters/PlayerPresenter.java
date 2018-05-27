package com.simplecity.amp_library.ui.presenters;

import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import com.cantrowitz.rxbroadcast.RxBroadcast;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.lyrics.LyricsDialog;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.MediaManager;
import com.simplecity.amp_library.playback.PlaybackMonitor;
import com.simplecity.amp_library.playback.constants.InternalIntents;
import com.simplecity.amp_library.tagger.TaggerDialog;
import com.simplecity.amp_library.ui.dialog.BiographyDialog;
import com.simplecity.amp_library.ui.dialog.ShareDialog;
import com.simplecity.amp_library.ui.dialog.UpgradeDialog;
import com.simplecity.amp_library.ui.views.PlayerView;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ShuttleUtils;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

public class PlayerPresenter extends Presenter<PlayerView> {

    private static final String TAG = "PlayerPresenter";

    private long startSeekPos = 0;
    private long lastSeekEventTime;

    private long currentPlaybackTime;
    private boolean currentPlaybackTimeVisible;

    private Disposable isFavoriteDisposable;
    private MediaManager mediaManager;
    private final PlaybackMonitor playbackMonitor;

    @Inject
    public PlayerPresenter(MediaManager mediaManager) {
        this.mediaManager = mediaManager;
        this.playbackMonitor = new PlaybackMonitor(mediaManager);
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

        addDisposable(playbackMonitor.getProgressObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(progress -> view.setSeekProgress((int) (progress * 1000)),
                        error -> LogUtils.logException(TAG, "PlayerPresenter: Error updating seek progress", error)));

        addDisposable(playbackMonitor.getCurrentTimeObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pos -> refreshTimeText(pos / 1000),
                        error -> LogUtils.logException(TAG, "PlayerPresenter: Error refreshing time text", error)));

        addDisposable(Flowable.interval(500, TimeUnit.MILLISECONDS)
                .onBackpressureDrop()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aLong -> setCurrentTimeVisibility(mediaManager.isPlaying() || !currentPlaybackTimeVisible),
                        error -> LogUtils.logException(TAG, "PlayerPresenter: Error emitting current time", error)));

        final IntentFilter filter = new IntentFilter();
        filter.addAction(InternalIntents.META_CHANGED);
        filter.addAction(InternalIntents.QUEUE_CHANGED);
        filter.addAction(InternalIntents.PLAY_STATE_CHANGED);
        filter.addAction(InternalIntents.SHUFFLE_CHANGED);
        filter.addAction(InternalIntents.REPEAT_CHANGED);
        filter.addAction(InternalIntents.SERVICE_CONNECTED);

        addDisposable(RxBroadcast.fromBroadcast(ShuttleApplication.getInstance(), filter)
                .toFlowable(BackpressureStrategy.LATEST)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(intent -> {
                    final String action = intent.getAction();
                    if (action != null) {
                        switch (action) {
                            case InternalIntents.META_CHANGED:
                                updateTrackInfo();
                                break;
                            case InternalIntents.QUEUE_CHANGED:
                                updateTrackInfo();
                                break;
                            case InternalIntents.PLAY_STATE_CHANGED:
                                updateTrackInfo();
                                updatePlaystate();
                                break;
                            case InternalIntents.SHUFFLE_CHANGED:
                                updateTrackInfo();
                                updateShuffleMode();
                                break;
                            case InternalIntents.REPEAT_CHANGED:
                                updateRepeatMode();
                                break;
                            case InternalIntents.SERVICE_CONNECTED:
                                updateTrackInfo();
                                updatePlaystate();
                                updateShuffleMode();
                                updateRepeatMode();
                                break;
                        }
                    }
                }, error -> LogUtils.logException(TAG, "PlayerPresenter: Error sending broadcast", error)));
    }

    private void refreshTimeText(long playbackTime) {
        if (playbackTime != currentPlaybackTime) {
            PlayerView view = getView();
            if (view != null) {
                view.currentTimeChanged(playbackTime);
                if (SettingsManager.getInstance().displayRemainingTime()) {
                    view.totalTimeChanged(-(mediaManager.getDuration() / 1000 - playbackTime));
                }
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
            view.trackInfoChanged(mediaManager.getSong());
            view.queueChanged(mediaManager.getQueuePosition() + 1, mediaManager.getQueue().size());
            view.currentTimeChanged(mediaManager.getPosition() / 1000);
            updateRemainingTime();

            if (isFavoriteDisposable != null) {
                isFavoriteDisposable.dispose();
            }
            isFavoriteDisposable = PlaylistUtils.isFavorite(mediaManager.getSong())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(isFavorite -> updateFavorite((isFavorite)));

            addDisposable(isFavoriteDisposable);
        }
    }

    private void updatePlaystate() {
        PlayerView view = getView();
        if (view != null) {
            view.playbackChanged(mediaManager.isPlaying());
        }
    }

    private void updateShuffleMode() {
        PlayerView view = getView();
        if (view != null) {
            view.repeatChanged(mediaManager.getRepeatMode());
            view.shuffleChanged(mediaManager.getShuffleMode());
        }
    }

    private void updateRepeatMode() {
        PlayerView view = getView();
        if (view != null) {
            view.repeatChanged(mediaManager.getRepeatMode());
        }
    }

    public void togglePlayback() {
        mediaManager.playOrPause();
        updatePlaystate();
    }

    public void toggleFavorite() {
        mediaManager.toggleFavorite();
    }

    public void skip() {
        mediaManager.next();
    }

    public void prev(boolean allowTrackRestart) {
        mediaManager.previous(allowTrackRestart);
    }

    public void toggleShuffle() {
        mediaManager.toggleShuffleMode();
        updateShuffleMode();
    }

    public void toggleRepeat() {
        mediaManager.cycleRepeat();
        updateRepeatMode();
    }

    public void seekTo(int progress) {
        mediaManager.seekTo(mediaManager.getDuration() * progress / 1000);
    }

    public void scanForward(final int repeatCount, long delta) {
        if (repeatCount == 0) {
            startSeekPos = mediaManager.getPosition();
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
            final long duration = mediaManager.getDuration();
            if (newpos >= duration) {
                // move to next track
                mediaManager.next();
                startSeekPos -= duration; // is OK to go negative
                newpos -= duration;
            }
            if (delta - lastSeekEventTime > 250 || repeatCount < 0) {
                mediaManager.seekTo(newpos);
                lastSeekEventTime = delta;
            }
        }
    }

    public void scanBackward(final int repeatCount, long delta) {
        if (repeatCount == 0) {
            startSeekPos = mediaManager.getPosition();
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
                mediaManager.previous(true);
                final long duration = mediaManager.getDuration();
                startSeekPos += duration;
                newpos += duration;
            }
            if (delta - lastSeekEventTime > 250 || repeatCount < 0) {
                mediaManager.seekTo(newpos);
                lastSeekEventTime = delta;
            }
        }
    }

    public void showLyrics(Context context) {
        PlayerView playerView = getView();
        if (playerView != null) {
            playerView.showLyricsDialog(new LyricsDialog(mediaManager).getDialog(context));
        }
    }

    public void editTagsClicked(Activity activity) {
        PlayerView playerView = getView();
        if (playerView != null) {
            if (!ShuttleUtils.isUpgraded()) {
                playerView.showUpgradeDialog(UpgradeDialog.getUpgradeDialog(activity));
            } else {
                playerView.showTaggerDialog(TaggerDialog.newInstance(mediaManager.getSong()));
            }
        }
    }

    public void songInfoClicked(Context context) {
        PlayerView playerView = getView();
        if (playerView != null) {
            Song song = mediaManager.getSong();
            if (song != null) {
                playerView.showSongInfoDialog(BiographyDialog.getSongInfoDialog(context, song));
            }
        }
    }

    public void updateRemainingTime() {
        PlayerView playerView = getView();
        if (playerView != null) {
            if (SettingsManager.getInstance().displayRemainingTime()) {
                playerView.totalTimeChanged(-((mediaManager.getDuration() - mediaManager.getPosition()) / 1000));
            } else {
                playerView.totalTimeChanged(mediaManager.getDuration() / 1000);
            }
        }
    }

    public void shareClicked(Context context) {
        PlayerView playerView = getView();
        if (playerView != null) {
            Song song = mediaManager.getSong();
            if (song != null) {
                ShareDialog.getDialog(context, song).show();
            }
        }
    }
}
