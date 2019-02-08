package com.simplecity.amp_library.utils;

import android.arch.lifecycle.Lifecycle;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.simplecity.amp_library.playback.LocalBinder;
import com.simplecity.amp_library.playback.MusicService;

import java.util.WeakHashMap;

public class MusicServiceConnectionUtils {

    private static final String TAG = "MusicServiceConnectionU";

    public static LocalBinder serviceBinder = null;

    private static final WeakHashMap<Context, ServiceBinder> connectionMap = new WeakHashMap<>();

    private MusicServiceConnectionUtils() {

    }

    /**
     * @param context  The {@link Context} to use
     * @param callback The {@link ServiceConnection} to use
     * @return The new instance of {@link ServiceToken}
     */
    public static ServiceToken bindToService(Lifecycle lifecycle, Context context, ServiceConnection callback) {
        new ResumingServiceManager(lifecycle).startService(context, new Intent(context, MusicService.class));
        final ServiceBinder binder = new ServiceBinder(callback);
        if (context.bindService(new Intent().setClass(context, MusicService.class), binder, 0)) {
            connectionMap.put(context, binder);
            return new ServiceToken(context);
        }
        return null;
    }

    /**
     * @param token The {@link ServiceToken} to unbind from
     */
    public static void unbindFromService(ServiceToken token) {
        if (token == null) {
            return;
        }
        final Context context = token.context;
        final ServiceBinder binder = connectionMap.remove(context);
        if (binder == null) {
            return;
        }
        context.unbindService(binder);
        if (connectionMap.isEmpty()) {
            serviceBinder = null;
        }
    }

    public static final class ServiceBinder implements ServiceConnection {

        private final ServiceConnection callback;

        ServiceBinder(final ServiceConnection callback) {
            this.callback = callback;
        }

        @Override
        public void onServiceConnected(final ComponentName className, final IBinder service) {

            serviceBinder = (LocalBinder) service;

            if (callback != null) {
                callback.onServiceConnected(className, service);
            }
        }

        @Override
        public void onServiceDisconnected(final ComponentName className) {
            if (callback != null) {
                callback.onServiceDisconnected(className);
            }
            serviceBinder = null;
        }
    }

    public static final class ServiceToken {

        public Context context;

        ServiceToken(final Context context) {
            this.context = context;
        }
    }
}