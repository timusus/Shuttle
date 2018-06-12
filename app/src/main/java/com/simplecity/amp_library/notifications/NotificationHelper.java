package com.simplecity.amp_library.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.utils.LogUtils;

public class NotificationHelper {

    private static final String TAG = "NotificationHelper";

    public static final String NOTIFICATION_CHANNEL_ID = "shuttle_notif_channel";

    protected NotificationManager notificationManager;

    private NotificationChannel notificationChannel;

    public NotificationHelper(Context context) {
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel existingNotificationChannel = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID);
            if (existingNotificationChannel == null) {
                notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                        context.getString(R.string.app_name),
                        NotificationManager.IMPORTANCE_LOW);
                notificationChannel.enableLights(false);
                notificationChannel.enableVibration(false);

                notificationManager.createNotificationChannel(notificationChannel);
            }
        }
    }

    public void notify(int notificationId, Notification notification) {
        try {
            notificationManager.notify(notificationId, notification);
        }  catch (RuntimeException e) {
            LogUtils.logException(TAG, "Error posting notification", e);
        }
    }

    public void cancel(int notificationId) {
        notificationManager.cancel(notificationId);
    }
}