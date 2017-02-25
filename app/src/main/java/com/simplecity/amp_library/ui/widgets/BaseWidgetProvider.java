package com.simplecity.amp_library.ui.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.widget.RemoteViews;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.playback.MusicService;
import com.simplecity.amp_library.ui.activities.MainActivity;

import rx.functions.Action0;

public abstract class BaseWidgetProvider extends AppWidgetProvider {

    public abstract String getUpdateCommandString();

    public abstract String getLayoutIdString();

    public abstract int getWidgetLayoutId();

    public abstract int getRootViewId();

    protected void doOnMainThread(Action0 action) {
        new Handler(Looper.getMainLooper()).post(action::call);
    }

    public static final String ARG_WIDGET_BACKGROUND_COLOR = "widget_background_color_";
    public static final String ARG_WIDGET_TEXT_COLOR = "widget_text_color_";
    public static final String ARG_WIDGET_INVERT_ICONS = "widget_invert_icons_";
    public static final String ARG_WIDGET_SHOW_ARTWORK = "widget_show_artwork_";
    public static final String ARG_WIDGET_COLOR_FILTER = "widget_color_filter_";

    public SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(ShuttleApplication.getInstance());

    @LayoutRes
    public int mLayoutId;

    public abstract void update(MusicService service, int[] appWidgetIds, boolean updateArtwork);

    protected abstract void initialiseWidget(Context context, int appWidgetId);

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        for (int appWidgetId : appWidgetIds) {
            mLayoutId = mPrefs.getInt(getLayoutIdString() + appWidgetId, getWidgetLayoutId());
            initialiseWidget(context, appWidgetId);
        }

        // Send broadcast intent to any running MusicService so it can wrap around with an immediate update.
        Intent updateIntent = new Intent(MusicService.ServiceCommand.SERVICE_COMMAND);
        updateIntent.putExtra(MusicService.MediaButtonCommand.CMD_NAME, getUpdateCommandString());
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        updateIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        context.sendBroadcast(updateIntent);
    }

    protected void pushUpdate(Context context, int appWidgetId, RemoteViews views) {
        // Update specific list of appWidgetIds if given, otherwise default to all
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        if (appWidgetId != -1) {
            appWidgetManager.updateAppWidget(appWidgetId, views);
        } else {
            appWidgetManager.updateAppWidget(new ComponentName(context, this.getClass()), views);
        }
    }

    private int[] getInstances(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        return (appWidgetManager.getAppWidgetIds(new ComponentName(context, this.getClass())));
    }

    public void notifyChange(MusicService service, String what) {
        if (getInstances(service) != null) {
            if (MusicService.InternalIntents.META_CHANGED.equals(what)
                    || MusicService.InternalIntents.PLAY_STATE_CHANGED.equals(what)
                    || MusicService.InternalIntents.SHUFFLE_CHANGED.equals(what)
                    || MusicService.InternalIntents.REPEAT_CHANGED.equals(what)) {
                update(service, getInstances(service), MusicService.InternalIntents.META_CHANGED.equals(what));
            }
        }
    }

    public static void setupButtons(Context context, RemoteViews views, int appWidgetId, int rootViewId) {

        Intent intent;
        PendingIntent pendingIntent;

        final ComponentName serviceName = new ComponentName(context, MusicService.class);

        intent = new Intent(context, MainActivity.class);
        pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        views.setOnClickPendingIntent(rootViewId, pendingIntent);

        intent = new Intent(MusicService.ServiceCommand.TOGGLE_PAUSE_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context, appWidgetId, intent, 0);
        views.setOnClickPendingIntent(R.id.play_button, pendingIntent);

        intent = new Intent(MusicService.ServiceCommand.NEXT_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context, appWidgetId, intent, 0);
        views.setOnClickPendingIntent(R.id.next_button, pendingIntent);

        intent = new Intent(MusicService.ServiceCommand.PREV_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context, appWidgetId, intent, 0);
        views.setOnClickPendingIntent(R.id.prev_button, pendingIntent);

        intent = new Intent(MusicService.ServiceCommand.SHUFFLE_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context, appWidgetId, intent, 0);
        views.setOnClickPendingIntent(R.id.shuffle_button, pendingIntent);

        intent = new Intent(MusicService.ServiceCommand.REPEAT_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context, appWidgetId, intent, 0);
        views.setOnClickPendingIntent(R.id.repeat_button, pendingIntent);
    }
}
