package com.simplecity.amp_library.playback;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.RemoteControlClient;
import android.os.Bundle;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import com.annimon.stream.Stream;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.cantrowitz.rxbroadcast.RxBroadcast;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.androidauto.CarHelper;
import com.simplecity.amp_library.androidauto.MediaIdHelper;
import com.simplecity.amp_library.data.Repository;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.constants.InternalIntents;
import com.simplecity.amp_library.ui.screens.queue.QueueItem;
import com.simplecity.amp_library.ui.screens.queue.QueueItemKt;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.MediaButtonIntentReceiver;
import com.simplecity.amp_library.utils.SettingsManager;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import java.util.List;
import kotlin.Unit;

class MediaSessionManager {

    private static final String TAG = "MediaSessionManager";

    private Context context;

    private MediaSessionCompat mediaSession;

    private QueueManager queueManager;

    private PlaybackManager playbackManager;

    private PlaybackSettingsManager playbackSettingsManager;

    private SettingsManager settingsManager;

    private CompositeDisposable disposables = new CompositeDisposable();

    private MediaIdHelper mediaIdHelper;

    private static String SHUFFLE_ACTION = "ACTION_SHUFFLE";

    MediaSessionManager(
            Context context,
            QueueManager queueManager,
            PlaybackManager playbackManager,
            PlaybackSettingsManager playbackSettingsManager,
            SettingsManager settingsManager,
            Repository.SongsRepository songsRepository,
            Repository.AlbumsRepository albumsRepository,
            Repository.AlbumArtistsRepository albumArtistsRepository,
            Repository.GenresRepository genresRepository,
            Repository.PlaylistsRepository playlistsRepository
    ) {
        this.context = context.getApplicationContext();
        this.queueManager = queueManager;
        this.playbackManager = playbackManager;
        this.settingsManager = settingsManager;
        this.playbackSettingsManager = playbackSettingsManager;

        mediaIdHelper = new MediaIdHelper((ShuttleApplication) context.getApplicationContext(), songsRepository, albumsRepository, albumArtistsRepository, genresRepository, playlistsRepository);

        ComponentName mediaButtonReceiverComponent = new ComponentName(context.getPackageName(), MediaButtonIntentReceiver.class.getName());
        mediaSession = new MediaSessionCompat(context, "Shuttle", mediaButtonReceiverComponent, null);
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPause() {
                playbackManager.pause(true);
            }

            @Override
            public void onPlay() {
                playbackManager.play();
            }

            @Override
            public void onSeekTo(long pos) {
                playbackManager.seekTo(pos);
            }

            @Override
            public void onSkipToNext() {
                playbackManager.next(true);
            }

            @Override
            public void onSkipToPrevious() {
                playbackManager.previous(false);
            }

            @Override
            public void onSkipToQueueItem(long id) {
                List<QueueItem> queueItems = queueManager.getCurrentPlaylist();

                QueueItem queueItem = Stream.of(queueItems)
                        .filter(aQueueItem -> (long) aQueueItem.hashCode() == id)
                        .findFirst()
                        .orElse(null);

                if (queueItem != null) {
                    playbackManager.setQueuePosition(queueItems.indexOf(queueItem));
                }
            }

            @Override
            public void onStop() {
                playbackManager.stop(true);
            }

            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
                Log.e("MediaButtonReceiver", "OnMediaButtonEvent called");
                MediaButtonIntentReceiver.handleIntent(context, mediaButtonEvent, playbackSettingsManager);
                return true;
            }

            @Override
            public void onPlayFromMediaId(String mediaId, Bundle extras) {
                mediaIdHelper.getSongListForMediaId(mediaId, (songs, position) -> {
                    playbackManager.load((List<Song>) songs, position, true, 0);
                    return Unit.INSTANCE;
                });
            }

            @SuppressWarnings("ResultOfMethodCallIgnored")
            @SuppressLint("CheckResult")
            @Override
            public void onPlayFromSearch(String query, Bundle extras) {
                if (TextUtils.isEmpty(query)) {
                    playbackManager.play();
                } else {
                    mediaIdHelper.handlePlayFromSearch(query, extras)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    pair -> {
                                        if (!pair.getFirst().isEmpty()) {
                                            playbackManager.load(pair.getFirst(), pair.getSecond(), true, 0);
                                        } else {
                                            playbackManager.pause(false);
                                        }
                                    },
                                    error -> LogUtils.logException(TAG, "Failed to gather songs from search. Query: " + query, error)
                            );
                }
            }

            @Override
            public void onCustomAction(String action, Bundle extras) {
                if (action.equals(SHUFFLE_ACTION)) {
                    queueManager.setShuffleMode(queueManager.shuffleMode == QueueManager.ShuffleMode.ON ? QueueManager.ShuffleMode.OFF : QueueManager.ShuffleMode.ON);
                }
                updateMediaSession(action);
            }
        });

        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS | MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS);

        //For some reason, MediaSessionCompat doesn't seem to pass all of the available 'actions' on as
        //transport control flags for the RCC, so we do that manually
        RemoteControlClient remoteControlClient = (RemoteControlClient) mediaSession.getRemoteControlClient();
        if (remoteControlClient != null) {
            remoteControlClient.setTransportControlFlags(
                    RemoteControlClient.FLAG_KEY_MEDIA_PAUSE
                            | RemoteControlClient.FLAG_KEY_MEDIA_PLAY
                            | RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE
                            | RemoteControlClient.FLAG_KEY_MEDIA_NEXT
                            | RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS
                            | RemoteControlClient.FLAG_KEY_MEDIA_STOP);
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(InternalIntents.QUEUE_CHANGED);
        intentFilter.addAction(InternalIntents.META_CHANGED);
        intentFilter.addAction(InternalIntents.PLAY_STATE_CHANGED);
        intentFilter.addAction(InternalIntents.POSITION_CHANGED);
        disposables.add(RxBroadcast.fromBroadcast(context, intentFilter).subscribe(intent -> {
            String action = intent.getAction();
            if (action != null) {
                updateMediaSession(intent.getAction());
            }
        }));
    }

    private void updateMediaSession(final String action) {

        int playState = playbackManager.isPlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;

        long playbackActions = getMediaSessionActions();

        QueueItem currentQueueItem = queueManager.getCurrentQueueItem();

        PlaybackStateCompat.Builder builder = new PlaybackStateCompat.Builder();
        builder.setActions(playbackActions);

        switch (queueManager.shuffleMode) {
            case QueueManager.ShuffleMode.OFF:
                builder.addCustomAction(
                        new PlaybackStateCompat.CustomAction.Builder(SHUFFLE_ACTION, context.getString(R.string.btn_shuffle_on), R.drawable.ic_shuffle_off_circled).build());
                break;
            case QueueManager.ShuffleMode.ON:
                builder.addCustomAction(
                        new PlaybackStateCompat.CustomAction.Builder(SHUFFLE_ACTION, context.getString(R.string.btn_shuffle_off), R.drawable.ic_shuffle_on_circled).build());
                break;
        }

        builder.setState(playState, playbackManager.getSeekPosition(), 1.0f);

        if (currentQueueItem != null) {
            builder.setActiveQueueItemId((long) currentQueueItem.hashCode());
        }

        PlaybackStateCompat playbackState = builder.build();

        if (action.equals(InternalIntents.PLAY_STATE_CHANGED) || action.equals(InternalIntents.POSITION_CHANGED) || action.equals(SHUFFLE_ACTION)) {
            mediaSession.setPlaybackState(playbackState);
        } else if (action.equals(InternalIntents.META_CHANGED) || action.equals(InternalIntents.QUEUE_CHANGED)) {

            if (currentQueueItem != null) {
                MediaMetadataCompat.Builder metaData = new MediaMetadataCompat.Builder()
                        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, String.valueOf(currentQueueItem.getSong().id))
                        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentQueueItem.getSong().artistName)
                        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, currentQueueItem.getSong().albumArtistName)
                        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currentQueueItem.getSong().albumName)
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentQueueItem.getSong().name)
                        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, currentQueueItem.getSong().duration)
                        .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, (long) (queueManager.queuePosition + 1))
                        //Getting the genre is expensive.. let's not bother for now.
                        //.putString(MediaMetadataCompat.METADATA_KEY_GENRE, getGenreName())
                        .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, null)
                        .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, (long) (queueManager.getCurrentPlaylist().size()));

                // If we're in car mode, don't wait for the artwork to load before setting session metadata.
                if (CarHelper.isCarUiMode(context)) {
                    mediaSession.setMetadata(metaData.build());
                }

                mediaSession.setPlaybackState(playbackState);

                mediaSession.setQueue(QueueItemKt.toMediaSessionQueueItems(queueManager.getCurrentPlaylist()));
                mediaSession.setQueueTitle(context.getString(R.string.menu_queue));

                if (settingsManager.showLockscreenArtwork() || CarHelper.isCarUiMode(context)) {
                    updateMediaSessionArtwork(metaData);
                } else {
                    mediaSession.setMetadata(metaData.build());
                }
            }
        }
    }

    private void updateMediaSessionArtwork(MediaMetadataCompat.Builder metaData) {
        QueueItem currentQueueItem = queueManager.getCurrentQueueItem();
        if (currentQueueItem != null) {
            disposables.add(Completable.defer(() -> Completable.fromAction(() ->
                            Glide.with(context)
                                    .load(currentQueueItem.getSong().getAlbum())
                                    .asBitmap()
                                    .override(1024, 1024)
                                    .into(new SimpleTarget<Bitmap>() {
                                        @Override
                                        public void onResourceReady(Bitmap bitmap, GlideAnimation<? super Bitmap> glideAnimation) {
                                            if (bitmap != null) {
                                                metaData.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap);
                                            }
                                            try {
                                                mediaSession.setMetadata(metaData.build());
                                            } catch (NullPointerException e) {
                                                metaData.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, null);
                                                mediaSession.setMetadata(metaData.build());
                                            }
                                        }

                                        @Override
                                        public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                            super.onLoadFailed(e, errorDrawable);
                                            mediaSession.setMetadata(metaData.build());
                                        }
                                    })
                    ))
                            .subscribeOn(AndroidSchedulers.mainThread())
                            .subscribe()
            );
        }
    }

    private long getMediaSessionActions() {
        return PlaybackStateCompat.ACTION_PLAY
                | PlaybackStateCompat.ACTION_PAUSE
                | PlaybackStateCompat.ACTION_PLAY_PAUSE
                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                | PlaybackStateCompat.ACTION_STOP
                | PlaybackStateCompat.ACTION_SEEK_TO
                | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                | PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
                | PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM;
    }

    MediaSessionCompat.Token getSessionToken() {
        return mediaSession.getSessionToken();
    }

    void setActive(boolean active) {
        mediaSession.setActive(active);
    }

    void destroy() {
        disposables.clear();
        mediaSession.release();
    }
}