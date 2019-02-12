package com.simplecity.amp_library.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.target.SimpleTarget;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.data.Repository;
import com.simplecity.amp_library.glide.loader.ArtworkModelLoader;
import com.simplecity.amp_library.model.ArtworkProvider;
import com.simplecity.amp_library.notifications.NotificationHelper;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ShuttleUtils;
import dagger.android.AndroidInjection;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.inject.Inject;

/**
 * A service which will download all artist & album artworkProvider, via an AsyncTask, and display the progress in a notification.
 * The notification includes a 'cancel' button, and the AsyncTask & associated HttpRequests can be cancelled.
 */
public class ArtworkDownloadService extends Service {

    private static final String TAG = "ArtworkDownloadService";

    private static final String ACTION_CANCEL = "com.simplecity.shuttle.artwork_cancel";

    private static final int NOTIFICATION_ID = 200;

    private int progress = 0;
    private int max = 100;

    private CompositeDisposable disposables = new CompositeDisposable();

    NotificationHelper notificationHelper;

    @Inject
    Repository.AlbumsRepository albumsRepository;

    @Inject
    Repository.AlbumArtistsRepository albumArtistsRepository;

    @Inject
    SettingsManager settingsManager;

    private NotificationCompat.Builder getNotificationBuilder() {

        final ComponentName serviceName = new ComponentName(this, ArtworkDownloadService.class);
        Intent intent = new Intent(ACTION_CANCEL);
        intent.setComponent(serviceName);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);

        return new NotificationCompat.Builder(this, NotificationHelper.NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getResources().getString(R.string.notif_downloading_art))
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setOngoing(true)
                .setProgress(100, 0, true)
                .addAction(new NotificationCompat.Action(R.drawable.ic_close_24dp, getString(R.string.cancel), pendingIntent));
    }

    @Override
    public void onCreate() {
        AndroidInjection.inject(this);
        super.onCreate();

        notificationHelper = new NotificationHelper(this);

        if (!ShuttleUtils.isOnline(this, false)) {
            Toast toast = Toast.makeText(this, getResources().getString(R.string.connection_unavailable), Toast.LENGTH_SHORT);
            toast.show();
            stopSelf();
            return;
        }

        notificationHelper.notify(NOTIFICATION_ID, getNotificationBuilder().build());

        Single<List<ArtworkProvider>> sharedItemsSingle = albumArtistsRepository.getAlbumArtists()
                .first(Collections.emptyList())
                .<ArtworkProvider>flatMapObservable(Observable::fromIterable)
                .mergeWith(albumsRepository.getAlbums()
                        .first(Collections.emptyList())
                        .flatMapObservable(Observable::fromIterable))
                .toList();

        disposables.add(sharedItemsSingle
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    max = list.size();
                    updateProgress();
                }, error -> LogUtils.logException(TAG, "Error determining max", error)));

        disposables.add(sharedItemsSingle.flatMapObservable(Observable::fromIterable)
                .flatMap(artworkProvider -> Observable.just(artworkProvider)
                        .map(artwork -> {
                            FutureTarget<File> futureTarget = Glide.with(ArtworkDownloadService.this)
                                    .using(new ArtworkModelLoader(this, true), InputStream.class)
                                    .load(artwork)
                                    .as(InputStream.class)
                                    .downloadOnly(SimpleTarget.SIZE_ORIGINAL, SimpleTarget.SIZE_ORIGINAL);
                            try {
                                futureTarget.get(30, TimeUnit.SECONDS);
                            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                                Log.e(TAG, "Error downloading artworkProvider: " + e);
                            }
                            Glide.clear(futureTarget);
                            return artwork;
                        }))
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(item -> updateProgress(), error -> LogUtils.logException(TAG, "Error downloading artwork", error)));
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {

        // Some users like to crash the entire app and then wonder why the service stop working.
        // If they remove the task, shut everything down.

        stopSelf();

        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        if (disposables != null) {
            disposables.clear();
        }
        notificationHelper.cancel(NOTIFICATION_ID);
        super.onDestroy();
    }

    /**
     * Increments the progress count and updates the notification with the new value.
     * If the progress is equal to (or greater) than our count, then this task is finished.
     * The notification is dismissed and this service is stopped.
     */
    private void updateProgress() {
        progress++;

        NotificationCompat.Builder notificationBuilder = getNotificationBuilder();
        notificationBuilder.setProgress(max, progress, false);
        notificationHelper.notify(NOTIFICATION_ID, notificationBuilder.build());

        if (progress >= max) {
            notificationHelper.cancel(NOTIFICATION_ID);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            final String action = intent.getAction();
            if (action != null && action.equals(ACTION_CANCEL)) {
                //Handle a notification cancel action click:
                disposables.clear();
                notificationHelper.cancel(NOTIFICATION_ID);
                stopSelf();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        //Nothing to do.
        return null;
    }
}