package com.simplecity.amp_library.notifications;

import android.annotation.SuppressLint;
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
import android.util.Log;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.simplecity.amp_library.BuildConfig;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.data.Repository;
import com.simplecity.amp_library.glide.utils.GlideUtils;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.MusicService;
import com.simplecity.amp_library.playback.constants.ServiceCommand;
import com.simplecity.amp_library.utils.AnalyticsManager;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.PlaceholderProvider;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.playlists.FavoritesPlaylistManager;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.util.ConcurrentModificationException;

public class MusicNotificationHelper extends NotificationHelper {

    private static final String TAG = "MusicNotificationHelper";

    private static final int NOTIFICATION_ID = 150;

    Notification notification;

    boolean isFavorite = false;

    Bitmap bitmap;

    private Handler handler;

    private AnalyticsManager analyticsManager;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    public MusicNotificationHelper(Context context, AnalyticsManager analyticsManager) {
        super(context);

        handler = new Handler(Looper.getMainLooper());
        this.analyticsManager = analyticsManager;
    }

    public NotificationCompat.Builder getBuilder(Context context, @NonNull Song song, @NonNull MediaSessionCompat.Token mediaSessionToken, @Nullable Bitmap bitmap, boolean isPlaying,
            boolean isFavorite) {

        Intent intent = new Intent(BuildConfig.APPLICATION_ID + ".PLAYBACK_VIEWER");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentIntent(contentIntent)
                .setChannelId(NOTIFICATION_CHANNEL_ID)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentTitle(song.name)
                .setContentText(song.artistName + " - " + song.albumName)
                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2)
                        .setMediaSession(mediaSessionToken))
                .addAction(
                        R.drawable.ic_skip_previous_24dp,
                        context.getString(R.string.btn_prev),
                        MusicService.retrievePlaybackAction(context, ServiceCommand.PREV)
                )
                .addAction(
                        isPlaying ? R.drawable.ic_pause_24dp : R.drawable.ic_play_24dp,
                        context.getString(isPlaying ? R.string.btn_pause : R.string.btn_play),
                        MusicService.retrievePlaybackAction(context, ServiceCommand.TOGGLE_PLAYBACK)
                )
                .addAction(
                        R.drawable.ic_skip_next_24dp,
                        context.getString(R.string.btn_skip),
                        MusicService.retrievePlaybackAction(context, ServiceCommand.NEXT)
                )
                .addAction(
                        isFavorite ? R.drawable.ic_favorite_24dp_scaled : R.drawable.ic_favorite_border_24dp_scaled,
                        context.getString(R.string.fav_add),
                        MusicService.retrievePlaybackAction(context, ServiceCommand.TOGGLE_FAVORITE)
                )
                .setShowWhen(false)
                .setVisibility(android.support.v4.app.NotificationCompat.VISIBILITY_PUBLIC);

        if (bitmap != null) {
            builder.setLargeIcon(bitmap);
        }

        return builder;
    }

    @SuppressLint("CheckResult")
    public void notify(
            Context context,
            @NonNull Repository.PlaylistsRepository playlistsRepository,
            @NonNull Repository.SongsRepository songsRepository,
            @NonNull Song song, boolean isPlaying,
            @NonNull MediaSessionCompat.Token mediaSessionToken,
            @NonNull SettingsManager settingsManager,
            FavoritesPlaylistManager favoritesPlaylistManager
    ) {
        notification = getBuilder(context, song, mediaSessionToken, bitmap, isPlaying, isFavorite).build();
        notify(NOTIFICATION_ID, notification);

        compositeDisposable.add(favoritesPlaylistManager.isFavorite(song)
                .first(false)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(isFavorite -> {
                    this.isFavorite = isFavorite;
                    notification = getBuilder(context, song, mediaSessionToken, MusicNotificationHelper.this.bitmap, isPlaying, isFavorite).build();
                    notify(notification);
                }, error -> {
                    LogUtils.logException(TAG, "MusicNotificationHelper failed to present notification", error);
                }));

        handler.post(() -> Glide.with(context)
                .load(song)
                .asBitmap()
                .priority(Priority.IMMEDIATE)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .override(600, 600)
                .placeholder(PlaceholderProvider.getInstance(context).getPlaceHolderDrawable(song.albumName, false, settingsManager))
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                        MusicNotificationHelper.this.bitmap = resource;
                        try {
                            notification = getBuilder(context, song, mediaSessionToken, bitmap, isPlaying, isFavorite).build();
                            MusicNotificationHelper.this.notify(notification);
                        } catch (NullPointerException | ConcurrentModificationException e) {
                            LogUtils.logException(TAG, "Exception while attempting to update notification with glide image.", e);
                        }
                    }

                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        MusicNotificationHelper.this.bitmap = GlideUtils.drawableToBitmap(errorDrawable);
                        super.onLoadFailed(e, errorDrawable);
                        try {
                            notification = getBuilder(context, song, mediaSessionToken, bitmap, isPlaying, isFavorite).build();
                            MusicNotificationHelper.this.notify(NOTIFICATION_ID, notification);
                        } catch (IllegalArgumentException error) {
                            LogUtils.logException(TAG, "Exception while attempting to update notification with error image", error);
                        }
                    }
                }));
    }

    public boolean startForeground(
            Service service,
            @NonNull Repository.PlaylistsRepository playlistsRepository,
            @NonNull Repository.SongsRepository songsRepository,
            @NonNull Song song,
            boolean isPlaying,
            @NonNull MediaSessionCompat.Token mediaSessionToken,
            SettingsManager settingsManager,
            FavoritesPlaylistManager favoritesPlaylistManager
    ) {
        notify(service, playlistsRepository, songsRepository, song, isPlaying, mediaSessionToken, settingsManager, favoritesPlaylistManager);
        try {
            analyticsManager.dropBreadcrumb(TAG, "startForeground() called");
            Log.w(TAG, "service.startForeground called");
            service.startForeground(NOTIFICATION_ID, notification);
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "startForeground not called, error: " + e);
            LogUtils.logException(TAG, "Error starting foreground notification", e);
            return false;
        }
    }

    public void notify(Notification notification) {
        super.notify(NOTIFICATION_ID, notification);
    }

    public void cancel() {
        super.cancel(NOTIFICATION_ID);
    }

    public void tearDown() {
        compositeDisposable.clear();
    }
}
