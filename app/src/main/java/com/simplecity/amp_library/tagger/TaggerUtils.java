package com.simplecity.amp_library.tagger;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.utils.DialogUtils;
import com.simplecity.amp_library.utils.SettingsManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class TaggerUtils {

    private static final String TAG = "TaggerUtils";

    //This class is never instantiated
    private TaggerUtils() {

    }

    /**
     * Checks the passed in paths to see whether the file at the given path is available in our
     * document tree. If it is, and we have write permission, the document file is added to the
     * passed in list of document files.
     *
     * @param documentFiles a list of document files to be populated
     * @param paths         a list of paths
     * @return true if we have permission for all files at the passed in paths
     */
    static boolean hasDocumentTreePermission(List<DocumentFile> documentFiles, List<String> paths) {

        boolean hasDocumentTreePermission = false;

        String treeUri = SettingsManager.getInstance().getDocumentTreeUri();
        if (treeUri == null) {
            //We don't have any document tree at all - so we're not going to have permission for any files.
            return false;
        }

        //Find the file in the document tree. If it's not there, or it doesn't have permission,
        //we're satisfied we don't have permission.
        for (String path : paths) {
            File file = new File(path);
            DocumentFile documentFile = getDocumentFile(Uri.parse(treeUri), file);
            if (documentFile != null) {
                hasDocumentTreePermission = documentFile.canWrite();
            }
            if (hasDocumentTreePermission) {
                documentFiles.add(documentFile);
            } else {
                documentFiles.clear();
                break;
            }
        }
        return hasDocumentTreePermission;
    }

    static DocumentFile getDocumentFile(Uri treeUri, final File file) {
        String baseFolder = getExtSdCardFolder(file);

        if (baseFolder == null) {
            return null;
        }

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

        // start with root of SD card and then parse through document tree.
        DocumentFile document = DocumentFile.fromTreeUri(ShuttleApplication.getInstance(), treeUri);

        String[] parts = relativePath.split("\\/");
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

    @TargetApi(Build.VERSION_CODES.KITKAT)
    static String getExtSdCardFolder(final File file) {
        String[] extSdPaths = getExtSdCardPaths();
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

    static boolean requiresPermission(List<String> paths) {
        boolean requiresPermission = false;
        for (String path : paths) {
            File file = new File(path);
            requiresPermission = getExtSdCardFolder(file) != null;
            if (requiresPermission) {
                break;
            }
        }

        return requiresPermission;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    static String[] getExtSdCardPaths() {
        List<String> paths = new ArrayList<>();
        try {
            File[] externalFilesDirs = ShuttleApplication.getInstance().getExternalFilesDirs("external");
            if (externalFilesDirs != null && externalFilesDirs.length > 0) {
                for (File file : externalFilesDirs) {
                    if (file != null && !file.equals(ShuttleApplication.getInstance().getExternalFilesDir("external"))) {
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
        return paths.toArray(new String[paths.size()]);
    }

    static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.getParentFile().exists())
            destFile.getParentFile().mkdirs();

        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    static void copyFile(File sourceFile, FileOutputStream outputStream) throws IOException {

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = outputStream.getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    static void showChooseDocumentDialog(Context context, MaterialDialog.SingleButtonCallback listener, boolean hasChecked) {
        MaterialDialog.Builder builder = DialogUtils.getBuilder(context)
                .title(R.string.edit_tags)
                .content(hasChecked ? R.string.tag_editor_document_tree_permission_failed : R.string.tag_editor_document_tree_message)
                .positiveText(R.string.button_ok)
                .onPositive(listener);
        if (hasChecked) {
            builder.negativeText(R.string.cancel);
        }
        builder.show();
    }
}
