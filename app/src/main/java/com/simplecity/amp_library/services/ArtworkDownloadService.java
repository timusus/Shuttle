package com.simplecity.amp_library.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
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
import com.simplecity.amp_library.glide.loader.ArtworkModelLoader;
import com.simplecity.amp_library.model.ArtworkProvider;
import com.simplecity.amp_library.utils.DataManager;
import com.simplecity.amp_library.utils.ShuttleUtils;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * A service which will download all artist & album artworkProvider, via an AsyncTask, and display the progress in a notification.
 * The notification includes a 'cancel' button, and the AsyncTask & associated HttpRequests can be cancelled.
 */
public class ArtworkDownloadService extends Service {

    private static final String TAG = "ArtworkDownloadService";

    private static final String ACTION_CANCEL = "com.simplecity.shuttle.artwork_cancel";

    private static final int NOTIFICATION_ID = 200;

    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;

    private int progress = 0;
    private int max = 100;

    private CompositeSubscription subscription;

    @Override
    public void onCreate() {
        super.onCreate();

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        final ComponentName serviceName = new ComponentName(this, ArtworkDownloadService.class);
        Intent intent = new Intent(ACTION_CANCEL);
        intent.setComponent(serviceName);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);

        notificationBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(getResources().getString(R.string.notif_downloading_art))
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setOngoing(true)
                .setProgress(100, 0, true)
                .addAction(new NotificationCompat.Action(R.drawable.ic_action_navigation_close, getString(R.string.cancel), pendingIntent));

        if (!ShuttleUtils.isOnline(false)) {
            Toast toast = Toast.makeText(this, getResources().getString(R.string.connection_unavailable), Toast.LENGTH_SHORT);
            toast.show();
            stopSelf();
            return;
        }

        if (notificationBuilder != null) {
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        }

        subscription = new CompositeSubscription();

        Observable<List<ArtworkProvider>> sharedItemsObservable = DataManager.getInstance()
                .getAlbumArtistsRelay()
                .first()
                .<ArtworkProvider>flatMap(Observable::from)
                .mergeWith(DataManager.getInstance().getAlbumsRelay()
                        .first()
                        .flatMap(Observable::from))
                .toList()
                .share();

        subscription.add(sharedItemsObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    max = list.size();
                    updateProgress();
                }));


        subscription.add(sharedItemsObservable.flatMap(Observable::from)
                .flatMap(artworkProvider -> Observable.just(artworkProvider)
                        .subscribeOn(Schedulers.computation())
                        .map(artwork -> {
                            FutureTarget<File> futureTarget = Glide.with(ArtworkDownloadService.this)
                                    .using(new ArtworkModelLoader(true), InputStream.class)
                                    .load(artwork)
                                    .as(InputStream.class)
                                    .downloadOnly(SimpleTarget.SIZE_ORIGINAL, SimpleTarget.SIZE_ORIGINAL);
                            try {
                                futureTarget.get(30, TimeUnit.SECONDS);
                            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                                Log.e(TAG, "Error downloading artworkProvider: " + e);
                            }
                            Glide.clear(futureTarget);
                            return null;
                        }))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(item -> {
                    updateProgress();
                }));
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
        if (subscription != null) {
            subscription.unsubscribe();
        }
        notificationManager.cancel(NOTIFICATION_ID);
        super.onDestroy();
    }

    /**
     * Increments the progress count and updates the notification with the new value.
     * If the progress is equal to (or greater) than our count, then this task is finished.
     * The notification is dismissed and this service is stopped.
     */
    private void updateProgress() {
        progress++;

        if (notificationBuilder != null) {
            notificationBuilder.setProgress(max, progress, false);
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        }

        if (progress >= max) {
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            final String action = intent.getAction();
            if (action != null && action.equals(ACTION_CANCEL)) {
                //Handle a notification cancel action click:
                subscription.unsubscribe();
                notificationBuilder = null;
                notificationManager.cancel(NOTIFICATION_ID);
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