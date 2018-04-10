package com.simplecity.amp_library.playback;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.RemoteControlClient;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.cantrowitz.rxbroadcast.RxBroadcast;
import com.simplecity.amp_library.playback.constants.InternalIntents;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.MediaButtonIntentReceiver;
import com.simplecity.amp_library.utils.SettingsManager;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class MediaSessionManager {

    private static final String TAG = "MediaSessionManager";

    private Context context;

    private MediaSessionCompat mediaSession;

    private QueueManager queueManager;

    private PlaybackManager playbackManager;

    private CompositeDisposable disposables = new CompositeDisposable();

    MediaSessionManager(Context context, QueueManager queueManager, PlaybackManager playbackManager, MusicService.Callbacks musicServiceCallbacks) {
        this.context = context.getApplicationContext();
        this.queueManager = queueManager;
        this.playbackManager = playbackManager;

        ComponentName mediaButtonReceiverComponent = new ComponentName(context.getPackageName(), MediaButtonIntentReceiver.class.getName());
        mediaSession = new MediaSessionCompat(context, "Shuttle", mediaButtonReceiverComponent, null);
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPause() {
                playbackManager.pause();
                playbackManager.setPausedByTransientLossOfFocus(false);
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
                playbackManager.previous();
            }

            @Override
            public void onStop() {
                playbackManager.pause();
                playbackManager.setPausedByTransientLossOfFocus(false);
                musicServiceCallbacks.releaseServiceUiAndStop();
            }

            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
                Log.e("MediaButtonReceiver", "OnMediaButtonEvent called");
                MediaButtonIntentReceiver.MediaButtonReceiverHelper.onReceive(context, mediaButtonEvent);
                return true;
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

        int playState = playbackManager.getIsSupposedToBePlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;

        long playbackActions = getMediaSessionActions();

        if (action.equals(InternalIntents.PLAY_STATE_CHANGED) || action.equals(InternalIntents.POSITION_CHANGED)) {
            //noinspection WrongConstant
            mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setActions(playbackActions)
                    .setState(playState, playbackManager.getSeekPosition(), 1.0f)
                    .build());
        } else if (action.equals(InternalIntents.META_CHANGED) || action.equals(InternalIntents.QUEUE_CHANGED)) {

            if (queueManager.getCurrentSong() != null) {
                MediaMetadataCompat.Builder metaData = new MediaMetadataCompat.Builder()
                        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, queueManager.getCurrentSong().artistName)
                        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, queueManager.getCurrentSong().albumArtistName)
                        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, queueManager.getCurrentSong().albumName)
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, queueManager.getCurrentSong().name)
                        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, queueManager.getCurrentSong().duration)
                        .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, (long) (queueManager.queuePosition + 1))
                        //Getting the genre is expensive.. let's not bother for now.
                        //.putString(MediaMetadataCompat.METADATA_KEY_GENRE, getGenreName())
                        .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, null)
                        .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, (long) (queueManager.getCurrentPlaylist().size()));

                if (SettingsManager.getInstance().showLockscreenArtwork()) {
                    disposables.add(
                            Completable.defer(() -> Completable.fromAction(() ->
                                    Glide.with(context)
                                            .load(queueManager.getCurrentSong().getAlbum())
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
                } else {
                    mediaSession.setMetadata(metaData.build());
                }

                try {
                    mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                            .setActions(playbackActions)
                            .setState(playState, playbackManager.getSeekPosition(), 1.0f)
                            .build());
                } catch (IllegalStateException e) {
                    LogUtils.logException(TAG, "Error setting playback state", e);
                }
            }
        }
    }

    private long getMediaSessionActions() {
        return PlaybackStateCompat.ACTION_PLAY
                | PlaybackStateCompat.ACTION_PAUSE
                | PlaybackStateCompat.ACTION_PLAY_PAUSE
                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                | PlaybackStateCompat.ACTION_STOP
                | PlaybackStateCompat.ACTION_SEEK_TO;
    }

    public MediaSessionCompat.Token getSessionToken() {
        return mediaSession.getSessionToken();
    }

    public void setActive(boolean active) {
        mediaSession.setActive(active);
    }

    public void destroy() {
        disposables.clear();
        mediaSession.release();
    }
}