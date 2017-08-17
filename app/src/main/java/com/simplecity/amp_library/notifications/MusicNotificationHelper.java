package com.simplecity.amp_library.notifications;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.session.MediaSessionCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.simplecity.amp_library.BuildConfig;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.glide.utils.GlideUtils;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.MusicService;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.PlaceholderProvider;
import com.simplecity.amp_library.utils.PlaylistUtils;

import java.util.ConcurrentModificationException;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class MusicNotificationHelper extends NotificationHelper {

    private static final String TAG = "MusicNotificationHelper";

    private static final int NOTIFICATION_ID = 150;

    private Notification notification;

    private boolean isFavorite = false;
    private Bitmap bitmap;

    private Handler handler;

    public MusicNotificationHelper(Context context) {
        super(context);

        handler = new Handler(Looper.getMainLooper());
    }

    public NotificationCompat.Builder getBuilder(Context context, @NonNull Song song, @NonNull MediaSessionCompat mediaSessionCompat, @Nullable Bitmap bitmap, boolean isPlaying, boolean isFavorite) {

        Intent intent = new Intent(BuildConfig.APPLICATION_ID + ".PLAYBACK_VIEWER");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentIntent(contentIntent)
                .setChannelId(NOTIFICATION_CHANNEL_ID)
                .setPriority(Notification.PRIORITY_MAX)
                .setContentTitle(song.name)
                .setContentText(song.albumArtistName + " - " + song.albumName)
                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2)
                        .setMediaSession(mediaSessionCompat.getSessionToken()))
                .addAction(
                        R.drawable.ic_skip_previous_24dp,
                        context.getString(R.string.btn_prev),
                        MusicService.retrievePlaybackAction(context, MusicService.ServiceCommand.PREV_ACTION)
                )
                .addAction(
                        isPlaying ? R.drawable.ic_pause_24dp : R.drawable.ic_play_24dp,
                        context.getString(isPlaying ? R.string.btn_pause : R.string.btn_play),
                        MusicService.retrievePlaybackAction(context, MusicService.ServiceCommand.TOGGLE_PAUSE_ACTION)
                )
                .addAction(
                        R.drawable.ic_skip_next_24dp,
                        context.getString(R.string.btn_skip),
                        MusicService.retrievePlaybackAction(context, MusicService.ServiceCommand.NEXT_ACTION)
                )
                //Todo:
                // Adding to favorites works, but the wrap-around call to update the notification seems to happen
                // before isPlaylist() returns true.. A bug for another day.
//                .addAction(
//                        isFavorite ? R.drawable.ic_favorite_24dp_scaled : R.drawable.ic_favorite_border_24dp_scaled,
//                        context.getString(R.string.fav_add),
//                        MusicService.retrievePlaybackAction(context, MusicService.ServiceCommand.TOGGLE_FAVORITE)
//                )
                .setShowWhen(false)
                .setVisibility(android.support.v4.app.NotificationCompat.VISIBILITY_PUBLIC);

        if (bitmap != null) {
            builder.setLargeIcon(bitmap);
        }

        return builder;
    }

    public void notify(Context context, @NonNull Song song, @NonNull MediaSessionCompat mediaSessionCompat) {

        notification = getBuilder(context, song, mediaSessionCompat, bitmap, MusicUtils.isPlaying(), isFavorite).build();
        notify(NOTIFICATION_ID, notification);

        PlaylistUtils.isFavorite(song)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(isFavorite -> {
                    this.isFavorite = isFavorite;
                    notification = getBuilder(context, song, mediaSessionCompat, MusicNotificationHelper.this.bitmap, MusicUtils.isPlaying(), isFavorite).build();
                    notify(notification);
                });

        handler.post(() -> Glide.with(context)
                .load(song)
                .asBitmap()
                .priority(Priority.IMMEDIATE)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .override(600, 600)
                .placeholder(PlaceholderProvider.getInstance().getPlaceHolderDrawable(song.albumName, false))
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                        MusicNotificationHelper.this.bitmap = resource;
                        try {
                            notification = getBuilder(context, song, mediaSessionCompat, bitmap, MusicUtils.isPlaying(), isFavorite).build();
                            MusicNotificationHelper.this.notify(notification);
                        } catch (NullPointerException | ConcurrentModificationException e) {
                            LogUtils.logException(TAG, "Exception while attempting to update notification with glide image.", e);
                        }
                    }

                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        MusicNotificationHelper.this.bitmap = GlideUtils.drawableToBitmap(errorDrawable);
                        super.onLoadFailed(e, errorDrawable);
                        notification = getBuilder(context, song, mediaSessionCompat, bitmap, MusicUtils.isPlaying(), isFavorite).build();
                        MusicNotificationHelper.this.notify(NOTIFICATION_ID, notification);
                    }
                }));
    }

    public void startForeground(Service service, @NonNull Song song, @NonNull MediaSessionCompat mediaSessionCompat) {
        notify(service, song, mediaSessionCompat);
        service.startForeground(NOTIFICATION_ID, notification);
    }

    public void notify(Notification notification) {
        super.notify(NOTIFICATION_ID, notification);
    }

    public void cancel() {
        super.cancel(NOTIFICATION_ID);
    }
}
