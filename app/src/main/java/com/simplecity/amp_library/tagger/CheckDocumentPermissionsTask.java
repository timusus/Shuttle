package com.simplecity.amp_library.tagger;

import android.os.AsyncTask;
import android.support.v4.provider.DocumentFile;

import com.simplecity.amp_library.utils.ShuttleUtils;

import java.util.List;

public class CheckDocumentPermissionsTask extends AsyncTask<Void, Void, Boolean> {

    public interface PermissionCheckListener {
        void onPermissionCheck(boolean hasPermission);
    }

    private List<String> paths;
    private List<DocumentFile> documentFiles;

    private PermissionCheckListener listener;

    public CheckDocumentPermissionsTask(List<String> paths, List<DocumentFile> documentFiles, PermissionCheckListener listener) {
        this.paths = paths;
        this.documentFiles = documentFiles;
        this.listener = listener;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        return !ShuttleUtils.hasLollipop() ||
                !TaggerUtils.requiresPermission(paths) ||
                TaggerUtils.hasDocumentTreePermission(documentFiles, paths);
    }

    @Override
    protected void onPostExecute(Boolean hasPermission) {
        super.onPostExecute(hasPermission);

        if (listener != null) {
            listener.onPermissionCheck(hasPermission);
        }
    }
}