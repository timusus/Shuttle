package com.simplecity.amp_library.saf;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;
import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.utils.SettingsManager;
import dagger.android.support.AndroidSupportInjection;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

public class SafManager {

    private static final String TAG = "SafManager";

    private Context applicationContext;

    private SettingsManager settingsManager;

    private static SafManager instance;

    static final int DOCUMENT_TREE_REQUEST_CODE = 901;

    public static SafManager getInstance(Context context, SettingsManager settingsManager) {
        if (instance == null) {
            instance = new SafManager(context, settingsManager);
        }
        return instance;
    }

    private SafManager(Context context, SettingsManager settingsManager) {
        this.applicationContext = context.getApplicationContext();
        this.settingsManager = settingsManager;
    }

    /**
     * Check whether files require Storage Access Framework (SAF) / DocumentsProvider for access
     *
     * @param files the files to check
     * @return true if files are located on the SD Card (and thus require the SAF and DocumentsProvider for access)
     */
    public boolean requiresPermission(List<File> files) {
        for (File file : files) {
            if (requiresPermission(file)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether a file requires Storage Access Framework (SAF) / DocumentsProvider for access
     *
     * @param file the File to check
     * @return true if the file is located on the SD Card (and thus require the SAF and DocumentsProvider for access)
     */
    public boolean requiresPermission(File file) {
        return getExtSdCardFolder(file) != null;
    }

    /**
     * Checks the passed in paths to see whether the file at the given path is available in our
     * document tree. If it is, and we have write permission, the document file is added to the
     * passed in list of document files.
     */
    public List<DocumentFile> getWriteableDocumentFiles(List<File> files) {

        List<DocumentFile> documentFiles = new ArrayList<>();

        String treeUri = getDocumentTree();
        if (treeUri == null) {
            //We don't have any document tree at all - so we're not going to have permission for any files.
            return documentFiles;
        }

        //Find the file in the document tree. If it's not there, or it doesn't have permission,
        //we're satisfied we don't have permission.
        for (File file : files) {
            DocumentFile documentFile = getWriteableDocumentFile(file);
            if (documentFile != null && documentFile.canWrite()) {
                documentFiles.add(documentFile);
            }
        }

        return documentFiles;
    }

    /**
     * Retrieve a DocumentFile for the passed in file, or null if it can't be created, or is not writeable.
     *
     * @param file File
     * @return a DocumentFile for the passed in file, or null if it can't be created, or is not writeable.
     */
    @Nullable
    public DocumentFile getWriteableDocumentFile(File file) {
        DocumentFile documentFile = getDocumentFile(file);
        if (documentFile != null && documentFile.canWrite()) {
            return documentFile;
        }
        return null;
    }

    /**
     * Retrieve a DocumentFile for the passed in File, or null it can't be created.
     *
     * @param file File
     * @return a DocumentFile for the passed in File, or null it can't be created.
     */
    @Nullable
    public DocumentFile getDocumentFile(final File file) {
        String baseFolder = getExtSdCardFolder(file);

        if (baseFolder == null) {
            return null;
        }

        String treeUri = getDocumentTree();
        if (treeUri == null) {
            return null;
        }

        String relativePath;
        try {
            String fullPath = file.getCanonicalPath();
            relativePath = fullPath.substring(baseFolder.length() + 1);
        } catch (IOException e) {
            return null;
        }

        // Start with root of SD card and then parse through document tree.
        DocumentFile document = DocumentFile.fromTreeUri(applicationContext, Uri.parse(treeUri));

        String[] parts = relativePath.split("/");
        for (String part : parts) {
            DocumentFile nextDocument = document.findFile(part);
            if (nextDocument != null) {
                document = nextDocument;
            }
        }
        if (document.isFile()) {
            return document;
        }
        return null;
    }

    /**
     * @return the persisted document tree, or null if it does not exist.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Nullable
    public String getDocumentTree() {
        String treeUri = settingsManager.getDocumentTreeUri();
        List<UriPermission> perms = applicationContext.getContentResolver().getPersistedUriPermissions();
        for (UriPermission perm : perms) {
            if (perm.getUri().toString().equals(treeUri) && perm.isWritePermission()) {
                return treeUri;
            }
        }
        return null;
    }

    /**
     * Check whether the file is stored on an SD Card, and if so, return the SD Card path
     *
     * @param file File
     * @return the SD Card path, or null if none is found.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Nullable
    private String getExtSdCardFolder(final File file) {
        List<String> extSdPaths = getExtSdCardPaths();
        try {
            for (String extSdPath : extSdPaths) {
                if (file.getCanonicalPath().startsWith(extSdPath)) {
                    return extSdPath;
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    /**
     * @return a list of potential SD Card paths
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private List<String> getExtSdCardPaths() {
        List<String> paths = new ArrayList<>();
        try {
            File[] externalFilesDirs = applicationContext.getExternalFilesDirs("external");
            if (externalFilesDirs != null && externalFilesDirs.length > 0) {
                for (File file : externalFilesDirs) {
                    if (file != null && !file.equals(applicationContext.getExternalFilesDir("external"))) {
                        int index = file.getAbsolutePath().lastIndexOf("/Android/data");
                        if (index < 0) {
                            Log.w(TAG, "Unexpected external file dir: " + file.getAbsolutePath());
                        } else {
                            String path = file.getAbsolutePath().substring(0, index);
                            try {
                                path = new File(path).getCanonicalPath();
                            } catch (IOException e) {
                                // Keep non-canonical path.
                            }
                            paths.add(path);
                        }
                    }
                }
            }
        } catch (NoSuchMethodError e) {
            Crashlytics.log("getExtSdCardPaths() failed. " + e.getMessage());
        }
        return paths;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void openDocumentTreePicker(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        if (intent.resolveActivity(applicationContext.getPackageManager()) != null) {
            activity.startActivityForResult(intent, DOCUMENT_TREE_REQUEST_CODE);
        } else {
            Toast.makeText(activity, R.string.R_string_toast_no_document_provider, Toast.LENGTH_LONG).show();
        }
    }

    public static class SafDialog extends DialogFragment {

        public static final String TAG = "SafDialog";

        @Inject
        SettingsManager settingsManager;

        public interface SafResultListener {
            void onResult(@Nullable Uri treeUri);
        }

        public SafDialog() {

        }

        public static <T extends AppCompatActivity & SafResultListener> void show(T activity) {
            new SafDialog().show(activity.getSupportFragmentManager(), TAG);
        }

        public static <T extends Fragment & SafResultListener> void show(T fragment) {
            new SafDialog().show(fragment.getChildFragmentManager(), TAG);
        }

        @Nullable
        private SafResultListener getListener() {
            if (getParentFragment() instanceof SafResultListener) {
                return (SafResultListener) getParentFragment();
            } else if (getActivity() instanceof SafResultListener) {
                return (SafResultListener) getActivity();
            }
            return null;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            AndroidSupportInjection.inject(this);

            return new MaterialDialog.Builder(getContext())
                    .title(R.string.saf_access_required_title)
                    .content(R.string.saf_access_required_message)
                    .positiveText(R.string.saf_show_files_button)
                    .onPositive((dialog, which) -> {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                        if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                            startActivityForResult(intent, DOCUMENT_TREE_REQUEST_CODE);
                        } else {
                            Toast.makeText(getContext(), R.string.R_string_toast_no_document_provider, Toast.LENGTH_LONG).show();
                        }
                    })
                    .autoDismiss(false)
                    .build();
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            SafResultListener listener = getListener();
            if (requestCode == DOCUMENT_TREE_REQUEST_CODE) {
                if (resultCode == Activity.RESULT_OK) {
                    Uri treeUri = data.getData();
                    if (treeUri != null) {
                        getContext().getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        settingsManager.setDocumentTreeUri(data.getData().toString());
                        if (listener != null) {
                            listener.onResult(treeUri);
                        }
                    }
                } else {
                    if (listener != null) {
                        listener.onResult(null);
                    }
                }
                dismiss();
            }
        }
    }
}