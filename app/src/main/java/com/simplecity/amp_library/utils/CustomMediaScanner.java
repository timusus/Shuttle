package com.simplecity.amp_library.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.afollestad.materialdialogs.MaterialDialog;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.FolderObject;
import com.simplecity.amp_library.rx.UnsafeConsumer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import java.io.File;
import java.util.Collections;
import java.util.List;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class CustomMediaScanner implements MediaScannerConnection.MediaScannerConnectionClient {

    private static final String TAG = "CustomMediaScanner";

    public interface ScanCompletionListener {
        void onPathScanned(String path);

        void onScanCompleted();
    }

    private Context applicationContext;

    private final List<String> paths;

    @Nullable
    private final ScanCompletionListener scanCompletionListener;

    private MediaScannerConnection connection;
    private int nextPath;

    private Handler handler;

    private CustomMediaScanner(Context context, List<String> paths, @Nullable ScanCompletionListener listener) {
        this.applicationContext = context.getApplicationContext();
        this.paths = paths;
        scanCompletionListener = listener;
        handler = new Handler(context.getMainLooper());
    }

    public static void scanFiles(Context context, List<String> paths, @Nullable ScanCompletionListener listener) {
        CustomMediaScanner client = new CustomMediaScanner(context, paths, listener);
        MediaScannerConnection connection = new MediaScannerConnection(context, client);
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
            scanComplete(applicationContext);
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

    private void scanComplete(Context context) {
        connection.disconnect();

        //Notify all media uris of change. This will in turn update any content observers.
        context.getContentResolver().notifyChange(Uri.parse("content://media"), null);

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

    // Todo: Remove context requirement
    public static Disposable scanFile(Context context, FolderObject folderObject) {

        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_progress, null);
        TextView pathsTextView = view.findViewById(R.id.paths);
        pathsTextView.setText(folderObject.path);

        MaterialProgressBar indeterminateProgress = view.findViewById(R.id.indeterminateProgress);
        MaterialProgressBar horizontalProgress = view.findViewById(R.id.horizontalProgress);

        MaterialDialog dialog = new MaterialDialog.Builder(context)
                .title(R.string.scanning)
                .customView(view, false)
                .negativeText(R.string.close)
                .show();

        return FileHelper.getPathList(new File(folderObject.path), true, false)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(paths -> {
                    ViewUtils.fadeOut(indeterminateProgress, null);
                    ViewUtils.fadeIn(horizontalProgress, null);
                    horizontalProgress.setMax(paths.size());

                    CustomMediaScanner.scanFiles(context, paths, new ScanCompletionListener() {
                        @Override
                        public void onPathScanned(String path) {
                            horizontalProgress.setProgress(horizontalProgress.getProgress() + 1);
                            pathsTextView.setText(path);
                        }

                        @Override
                        public void onScanCompleted() {
                            if (dialog.isShowing()) {
                                dialog.dismiss();
                            }
                        }
                    });
                });
    }

    public static void scanFile(Context context, String path, UnsafeConsumer<String> message) {
        CustomMediaScanner.scanFiles(context, Collections.singletonList(path), new CustomMediaScanner.ScanCompletionListener() {
            @Override
            public void onPathScanned(String path) {

            }

            @Override
            public void onScanCompleted() {
                message.accept(context.getString(R.string.scan_complete));
            }
        });
    }
}