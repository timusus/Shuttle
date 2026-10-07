package com.simplecity.amp_library.playback;

import android.app.ForegroundServiceStartNotAllowedException;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.os.Build;
import androidx.annotation.Nullable;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.utils.AnalyticsManager;
import io.reactivex.Completable;
import io.reactivex.disposables.Disposable;
import java.util.concurrent.TimeUnit;

class DummyNotificationHelper {

    private static final String TAG = "DummyNotificationHelper";

    private static int NOTIFICATION_ID_DUMMY = 5;

    private boolean isShowingDummyNotification;
    private boolean isForegroundedByApp = false;

    private static String CHANNEL_ID = "channel_dummy";

    // Must be greater than 10000
    // See https://github.com/aosp-mirror/platform_frameworks_base/blob/e80b45506501815061b079dcb10bf87443bd385d/services/core/java/com/android/server/am/ActiveServices.java
    // (SERVICE_START_FOREGROUND_TIMEOUT = 10*1000)
    //
    private static int NOTIFICATION_STOP_DELAY = 12500;

    @Nullable
    private Disposable dummyNotificationDisposable = null;

    void setForegroundedByApp(boolean foregroundedByApp) {
        AnalyticsManager.dropBreadcrumb(TAG, "setForegroundedByApp: " + foregroundedByApp);
        isForegroundedByApp = foregroundedByApp;
    }

    void showDummyNotification(Service service) {

        AnalyticsManager.dropBreadcrumb(TAG, "showDummyNotification() called");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = service.getSystemService(NotificationManager.class);
            if (!isShowingDummyNotification) {
                NotificationChannel channel = notificationManager.getNotificationChannel(CHANNEL_ID);
                if (channel == null) {
                    channel = new NotificationChannel(CHANNEL_ID, service.getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT);
                    channel.enableLights(false);
                    channel.enableVibration(false);
                    channel.setSound(null, null);
                    channel.setShowBadge(false);
                    channel.setImportance(NotificationManager.IMPORTANCE_LOW);
                    notificationManager.createNotificationChannel(channel);
                }

            }

            Notification notification = new Notification.Builder(service, CHANNEL_ID)
                    .setContentTitle(service.getString(R.string.app_name))
                    .setContentText(service.getString(R.string.notification_text_shuttle_running))
                    .setSmallIcon(R.drawable.ic_stat_notification)
                    .build();

            // Every startForegroundService() must be followed by startForeground(), even if the dummy notification is
            // already showing (the service may have left the foreground since).
            if (!isForegroundedByApp) {
                AnalyticsManager.dropBreadcrumb(TAG, "Starting dummy notification in foreground..");
                try {
                    service.startForeground(NOTIFICATION_ID_DUMMY, notification);
                } catch (IllegalStateException e) {
                    // Started with startService() and the app went to the background before we got here. The command
                    // still runs; there's just no foreground.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e instanceof ForegroundServiceStartNotAllowedException) {
                        AnalyticsManager.dropBreadcrumb(TAG, "startForeground() not allowed: " + e);
                        return;
                    }
                    throw e;
                }
            } else {
                AnalyticsManager.dropBreadcrumb(TAG, "Already foregrounded by app, showing dummy notification without foregrounding");
                notificationManager.notify(NOTIFICATION_ID_DUMMY, notification);
            }

            isShowingDummyNotification = true;
        }

        if (dummyNotificationDisposable != null) {
            dummyNotificationDisposable.dispose();
        }
        dummyNotificationDisposable = Completable.timer(NOTIFICATION_STOP_DELAY, TimeUnit.MILLISECONDS).doOnComplete(() -> removeDummyNotification(service)).subscribe();
    }

    void teardown(Service service) {

        AnalyticsManager.dropBreadcrumb(TAG, "teardown() called");

        removeDummyNotification(service);

        if (dummyNotificationDisposable != null) {
            dummyNotificationDisposable.dispose();
        }
    }

    private void removeDummyNotification(Service service) {
        AnalyticsManager.dropBreadcrumb(TAG, "removeDummyNotification() called");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isShowingDummyNotification) {

                if (dummyNotificationDisposable != null) {
                    dummyNotificationDisposable.dispose();
                }

                if (!isForegroundedByApp) {
                    AnalyticsManager.dropBreadcrumb(TAG, "service.stopForeground() called");
                    service.stopForeground(true);
                }

                AnalyticsManager.dropBreadcrumb(TAG, "Cancelling dummy notification");
                NotificationManager notificationManager = service.getSystemService(NotificationManager.class);
                notificationManager.cancel(NOTIFICATION_ID_DUMMY);

                isShowingDummyNotification = false;
            }
        }
    }
}