package com.simplecity.amp_library.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import io.reactivex.Observable;

/**
 * Broadcasts as Observables. The receiver is registered on subscribe and unregistered on dispose.
 */
public final class RxBroadcast {

    private RxBroadcast() {
    }

    /**
     * Observes broadcasts sent by this app (registered {@link ContextCompat#RECEIVER_NOT_EXPORTED}, as required for
     * custom actions from targetSdk 34).
     */
    public static Observable<Intent> fromBroadcast(Context context, IntentFilter intentFilter) {
        Context appContext = context.getApplicationContext();
        return Observable.create(emitter -> {
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    emitter.onNext(intent);
                }
            };
            ContextCompat.registerReceiver(appContext, receiver, intentFilter, ContextCompat.RECEIVER_NOT_EXPORTED);
            emitter.setCancellable(() -> appContext.unregisterReceiver(receiver));
        });
    }

    public static Observable<Intent> fromLocalBroadcast(Context context, IntentFilter intentFilter) {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context.getApplicationContext());
        return Observable.create(emitter -> {
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    emitter.onNext(intent);
                }
            };
            localBroadcastManager.registerReceiver(receiver, intentFilter);
            emitter.setCancellable(() -> localBroadcastManager.unregisterReceiver(receiver));
        });
    }
}
