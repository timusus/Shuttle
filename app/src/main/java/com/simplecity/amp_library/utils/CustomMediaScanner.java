package com.simplecity.amp_library.utils;

import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;

import com.simplecity.amp_library.ShuttleApplication;

import java.util.List;

public class CustomMediaScanner implements MediaScannerConnection.MediaScannerConnectionClient {

    private static final String TAG = "CustomMediaScanner";

    public interface ScanCompletionListener {
        void onPathScanned(String path);

        void onScanCompleted();
    }

    private final List<String> paths;

    @Nullable private final ScanCompletionListener scanCompletionListener;

    private MediaScannerConnection connection;
    private int nextPath;

    private Handler handler;

    private CustomMediaScanner(List<String> paths, @Nullable ScanCompletionListener listener) {
        this.paths = paths;
        scanCompletionListener = listener;
        handler = new Handler(ShuttleApplication.getInstance().getMainLooper());
    }

    public static void scanFiles(List<String> paths, @Nullable ScanCompletionListener listener) {
        CustomMediaScanner client = new CustomMediaScanner(paths, listener);
        MediaScannerConnection connection = new MediaScannerConnection(ShuttleApplication.getInstance(), client);
        client.connection = connection;
        connection.connect();
    }

    @Override
    public void onMediaScannerConnected() {
        scanNextPath();
    }

    @Override
    public void onScanCompleted(String path, Uri uri) {

        Log.d(TAG, "Scan complete. Path: " + path);

        scanNextPath();
    }

    private void scanNextPath() {
        if (nextPath >= paths.size()) {
            scanComplete();
            return;
        }
        String path = paths.get(nextPath);

        connection.scanFile(path, null);
        nextPath++;

        if (scanCompletionListener != null) {
            if (handler != null) {
                handler.post(() -> scanCompletionListener.onPathScanned(path));
            }
        }

        Log.d(TAG, "Scanning file: " + path);
    }

    private void scanComplete() {
        connection.disconnect();

        //Notify all media uris of change. This will in turn update any content observers.
        ShuttleApplication.getInstance().getContentResolver().notifyChange(Uri.parse("content://media"), null);

        if (handler != null) {
            handler.post(() -> {
                if (scanCompletionListener != null) {
                    scanCompletionListener.onScanCompleted();
                }
                cleanup();
            });
        }
    }

    private void cleanup() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
    }
}