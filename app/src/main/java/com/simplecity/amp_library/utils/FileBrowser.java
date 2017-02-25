package com.simplecity.amp_library.utils;

import android.os.Environment;
import android.text.TextUtils;

import com.simplecity.amp_library.interfaces.FileType;
import com.simplecity.amp_library.model.BaseFileObject;
import com.simplecity.amp_library.model.FileObject;
import com.simplecity.amp_library.model.FolderObject;
import com.simplecity.amp_library.model.TagInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FileBrowser {

    private static final String TAG = "FileBrowser";

    private File mCurrentDir;

    /**
     * Loads the specified folder.
     *
     * @param directory The file object to points to the directory to load.
     * @return An {@link List<BaseFileObject>} object that holds the data of the specified directory.
     */
    public List<BaseFileObject> loadDir(File directory) {
        mCurrentDir = directory;

        List<BaseFileObject> folderObjects = new ArrayList<>();
        List<BaseFileObject> fileObjects = new ArrayList<>();

        //Grab a list of all files/subdirs within the specified directory.
        File[] files = directory.listFiles(FileHelper.getAudioFilter());

        if (files != null) {
            for (File file : files) {
                BaseFileObject baseFileObject;

                if (file.isDirectory()) {
                    baseFileObject = new FolderObject();
                    baseFileObject.path = FileHelper.getPath(file);
                    baseFileObject.name = file.getName();
                    File[] listOfFiles = file.listFiles(FileHelper.getAudioFilter());
                    if (listOfFiles != null && listOfFiles.length > 0) {
                        for (File listOfFile : listOfFiles) {
                            if (listOfFile.isDirectory()) {
                                ((FolderObject) baseFileObject).folderCount++;
                            } else {
                                ((FolderObject) baseFileObject).fileCount++;
                            }
                        }
                    } else {
                        continue;
                    }
                    if (!folderObjects.contains(baseFileObject)) {
                        folderObjects.add(baseFileObject);
                    }

                } else {
                    baseFileObject = new FileObject();
                    baseFileObject.path = FileHelper.getPath(file);
                    baseFileObject.name = FileHelper.getName(file.getName());
                    baseFileObject.size = file.length();
                    ((FileObject) baseFileObject).extension = FileHelper.getExtension(file.getName());
                    if (TextUtils.isEmpty(((FileObject) baseFileObject).extension)) {
                        continue;
                    }
                    ((FileObject) baseFileObject).tagInfo = new TagInfo(baseFileObject.path);

                    if (!fileObjects.contains(baseFileObject)) {
                        fileObjects.add(baseFileObject);
                    }
                }
            }
        }

        sortFileObjects(fileObjects);
        sortFolderObjects(folderObjects);

        if (!SettingsManager.getInstance().getFolderBrowserFilesAscending()) {
            Collections.reverse(fileObjects);
        }

        if (!SettingsManager.getInstance().getFolderBrowserFoldersAscending()) {
            Collections.reverse(folderObjects);
        }

        folderObjects.addAll(fileObjects);

        if (!FileHelper.isRootDirectory(mCurrentDir)) {
            FolderObject parentObject = new FolderObject();
            parentObject.fileType = FileType.PARENT;
            parentObject.name = FileHelper.PARENT_DIRECTORY;
            parentObject.path = FileHelper.getPath(mCurrentDir) + "/" + FileHelper.PARENT_DIRECTORY;
            folderObjects.add(0, parentObject);
        }

        return folderObjects;
    }

    public File getCurrentDir() {
        return mCurrentDir;
    }

    public File getInitialDir() {

        File dir;
        String[] files;

        String settingsDir = SettingsManager.getInstance().getFolderBrowserInitialDir();
        if (settingsDir != null) {
            File file = new File(settingsDir);
            if (file.exists()) {
                return file;
            }
        }

        dir = getRootDir();

        files = dir.list((dir1, filename) -> dir1.isDirectory() && filename.toLowerCase().contains("storage"));

        if (files != null && files.length > 0) {
            dir = new File(dir + "/" + files[0]);
            //If there's an extsdcard path in our base dir, let's navigate to that. External SD cards are cool.
            files = dir.list((dir1, filename) -> dir1.isDirectory() && filename.toLowerCase().contains("extsdcard"));
            if (files != null && files.length > 0) {
                dir = new File(dir + "/" + files[0]);
            } else {
                //If we have external storage, use that as our initial dir
                if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                    dir = Environment.getExternalStorageDirectory();
                }
            }
        } else {
            //If we have external storage, use that as our initial dir
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                dir = Environment.getExternalStorageDirectory();
            }
        }

        //Whether or not there was an sdcard, let's see if there's a 'music' dir for us to navigate to
        if (dir != null) {
            files = dir.list((dir1, filename) -> dir1.isDirectory() && filename.toLowerCase().contains("music"));
        }
        if (files != null && files.length > 0) {
            dir = new File(dir + "/" + files[0]);
        }

        return dir;
    }

    public File getRootDir() {
        return new File("/");
    }

    public void sortFolderObjects(List<BaseFileObject> baseFileObjects) {

        switch (SettingsManager.getInstance().getFolderBrowserFoldersSortOrder()) {
            case SortManager.SortFolders.COUNT:
                Collections.sort(baseFileObjects, fileCountComparator());
                Collections.sort(baseFileObjects, folderCountComparator());
                break;

            case SortManager.SortFolders.DEFAULT:
            default:
                Collections.sort(baseFileObjects, filenameComparator());
                break;
        }
    }

    public void sortFileObjects(List<BaseFileObject> baseFileObjects) {
        switch (SettingsManager.getInstance().getFolderBrowserFilesSortOrder()) {
            case SortManager.SortFiles.SIZE:
                Collections.sort(baseFileObjects, sizeComparator());
                break;
            case SortManager.SortFiles.FILE_NAME:
                Collections.sort(baseFileObjects, filenameComparator());
                break;
            case SortManager.SortFiles.ARTIST_NAME:
                Collections.sort(baseFileObjects, artistNameComparator());
                break;
            case SortManager.SortFiles.ALBUM_NAME:
                Collections.sort(baseFileObjects, albumNameComparator());
                break;
            case SortManager.SortFiles.TRACK_NAME:
                Collections.sort(baseFileObjects, trackNameComparator());
                break;
            case SortManager.SortFiles.DEFAULT:
            default:
                Collections.sort(baseFileObjects, trackNumberComparator());
                Collections.sort(baseFileObjects, albumNameComparator());
                Collections.sort(baseFileObjects, artistNameComparator());
                break;
        }
    }

    private Comparator sizeComparator() {
        return (Comparator<BaseFileObject>) (lhs, rhs) -> (int) (rhs.size - lhs.size);
    }

    private Comparator filenameComparator() {
        return (Comparator<BaseFileObject>) (lhs, rhs) -> lhs.name.compareToIgnoreCase(rhs.name);
    }

//    private Comparator durationComparator() {
//        return (Comparator<FileObject>) (lhs, rhs) -> (int) (rhs.duration - lhs.duration);
//    }

    private Comparator trackNumberComparator() {
        return (Comparator<FileObject>) (lhs, rhs) -> lhs.tagInfo.trackNumber - rhs.tagInfo.trackNumber;
    }

    private Comparator folderCountComparator() {
        return (Comparator<FolderObject>) (lhs, rhs) -> rhs.folderCount - lhs.folderCount;
    }

    private Comparator fileCountComparator() {
        return (Comparator<FolderObject>) (lhs, rhs) -> rhs.fileCount - lhs.fileCount;
    }

    private Comparator artistNameComparator() {
        return (Comparator<FileObject>) (lhs, rhs) -> {
            if (lhs.tagInfo.artistName == null || rhs.tagInfo.artistName == null) {
                return nullCompare(lhs.tagInfo.artistName, rhs.tagInfo.artistName);
            }
            return lhs.tagInfo.artistName.compareToIgnoreCase(rhs.tagInfo.artistName);
        };
    }

    private Comparator albumNameComparator() {
        return (Comparator<FileObject>) (lhs, rhs) -> {
            if (lhs.tagInfo.albumName == null || rhs.tagInfo.albumName == null) {
                return nullCompare(lhs.tagInfo.albumName, rhs.tagInfo.albumName);
            }
            return lhs.tagInfo.albumName.compareToIgnoreCase(rhs.tagInfo.albumName);
        };
    }

    private Comparator trackNameComparator() {
        return (Comparator<FileObject>) (lhs, rhs) -> {
            if (lhs.tagInfo.trackName == null || rhs.tagInfo.trackName == null) {
                return nullCompare(lhs.tagInfo.trackName, rhs.tagInfo.trackName);
            }
            return lhs.tagInfo.trackName.compareToIgnoreCase(rhs.tagInfo.trackName);
        };
    }

    <T extends Comparable<T>> int nullCompare(T a, T b) {
        return a == null ? (b == null ? 0 : Integer.MIN_VALUE) : (b == null ? Integer.MAX_VALUE : a.compareTo(b));
    }

}
