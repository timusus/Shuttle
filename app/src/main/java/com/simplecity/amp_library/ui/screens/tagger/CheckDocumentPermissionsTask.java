package com.simplecity.amp_library.ui.screens.tagger;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.provider.DocumentFile;
import com.simplecity.amp_library.utils.SettingsManager;
import java.util.List;

public class CheckDocumentPermissionsTask extends AsyncTask<Void, Void, Boolean> {

    public interface PermissionCheckListener {
        void onPermissionCheck(boolean hasPermission);
    }

    private Context applicationContext;

    private SettingsManager settingsManager;

    private List<String> paths;
    private List<DocumentFile> documentFiles;

    private PermissionCheckListener listener;

    public CheckDocumentPermissionsTask(Context context, SettingsManager settingsManager, List<String> paths, List<DocumentFile> documentFiles, PermissionCheckListener listener) {
        this.applicationContext = context.getApplicationContext();
        this.settingsManager = settingsManager;
        this.paths = paths;
        this.documentFiles = documentFiles;
        this.listener = listener;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        return !TaggerUtils.requiresPermission(applicationContext, paths) || TaggerUtils.hasDocumentTreePermission(applicationContext, settingsManager, documentFiles, paths);
    }

    @Override
    protected void onPostExecute(Boolean hasPermission) {
        super.onPostExecute(hasPermission);

        if (listener != null) {
            listener.onPermissionCheck(hasPermission);
        }
    }
}