package com.simplecity.amp_library.utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.simplecity.amp_library.playback.LocalBinder;
import com.simplecity.amp_library.playback.MusicService;

import java.util.WeakHashMap;

public class MusicServiceConnectionUtils {

    public static LocalBinder serviceBinder = null;

    private static final WeakHashMap<Context, ServiceBinder> connectionMap = new WeakHashMap<>();

    private MusicServiceConnectionUtils() {

    }

    /**
     * @param context  The {@link Context} to use
     * @param callback The {@link ServiceConnection} to use
     * @return The new instance of {@link ServiceToken}
     */
    public static ServiceToken bindToService(final Context context, final ServiceConnection callback) {
        Activity realActivity = ((Activity) context).getParent();
        if (realActivity == null) {
            realActivity = (Activity) context;
        }
        final ContextWrapper contextWrapper = new ContextWrapper(realActivity);
        contextWrapper.startService(new Intent(contextWrapper, MusicService.class));
        final ServiceBinder binder = new ServiceBinder(callback);
        if (contextWrapper.bindService(new Intent().setClass(contextWrapper, MusicService.class), binder, 0)) {
            connectionMap.put(contextWrapper, binder);
            return new ServiceToken(contextWrapper);
        }
        return null;
    }

    /**
     * @param token The {@link ServiceToken} to unbind from
     */
    public static void unbindFromService(final ServiceToken token) {
        if (token == null) {
            return;
        }
        final ContextWrapper contextWrapper = token.wrappedContext;
        final ServiceBinder binder = connectionMap.remove(contextWrapper);
        if (binder == null) {
            return;
        }
        contextWrapper.unbindService(binder);
        if (connectionMap.isEmpty()) {
            serviceBinder = null;
        }
    }

    public static final class ServiceBinder implements ServiceConnection {

        private final ServiceConnection callback;

        public ServiceBinder(final ServiceConnection callback) {
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

        public ContextWrapper wrappedContext;

        /**
         * Constructor of <code>ServiceToken</code>
         *
         * @param context The {@link ContextWrapper} to use
         */
        public ServiceToken(final ContextWrapper context) {
            wrappedContext = context;
        }
    }
}