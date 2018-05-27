package com.simplecity.amp_library.utils.menu.folder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.SubMenu;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.afollestad.materialdialogs.MaterialDialog;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.interfaces.FileType;
import com.simplecity.amp_library.model.BaseFileObject;
import com.simplecity.amp_library.model.FileObject;
import com.simplecity.amp_library.model.FolderObject;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.MediaManager;
import com.simplecity.amp_library.ui.modelviews.FolderView;
import com.simplecity.amp_library.utils.CustomMediaScanner;
import com.simplecity.amp_library.utils.DialogUtils;
import com.simplecity.amp_library.utils.FileHelper;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.menu.MenuUtils;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import java.io.File;
import java.util.Collections;
import java.util.List;

public class FolderMenuUtils {

    private static final String TAG = "FolderMenuUtils";

    public interface Callbacks {

        void showToast(String message);

        void showToast(@StringRes int messageResId);

        void shareSong(Song song);

        void setRingtone(Song song);

        void showBiographyDialog(Song song);

        void onPlaylistItemsInserted();

        void onQueueItemsInserted(String message);

        void showTagEditor(Song song);

        void onFileNameChanged(FolderView folderView);

        void onFileDeleted(FolderView folderView);

        void playNext(Single<List<Song>> songsSingle);
    }

    private FolderMenuUtils() {
        //no instance
    }

    private static Single<Song> getSongForFile(FileObject fileObject) {
        return FileHelper.getSong(new File(fileObject.path))
                .observeOn(AndroidSchedulers.mainThread());
    }

    private static Single<List<Song>> getSongsForFolderObject(FolderObject folderObject) {
        return FileHelper.getSongList(new File(folderObject.path), true, false);
    }

    private static void scanFile(FileObject fileObject, Callbacks callbacks) {
        CustomMediaScanner.scanFile(fileObject.path, callbacks::showToast);
    }

    // Todo: Remove context requirement.
    private static void scanFolder(Context context, FolderObject folderObject) {
        CustomMediaScanner.scanFile(context, folderObject);
    }

    // Todo: Remove context requirement.
    private static void renameFile(Context context, FolderView folderView, BaseFileObject fileObject, Callbacks callbacks) {

        @SuppressLint("InflateParams")
        View customView = LayoutInflater.from(context).inflate(R.layout.dialog_rename, null);

        final EditText editText = customView.findViewById(R.id.editText);
        editText.setText(fileObject.name);

        MaterialDialog.Builder builder = DialogUtils.getBuilder(context);
        if (fileObject.fileType == FileType.FILE) {
            builder.title(R.string.rename_file);
        } else {
            builder.title(R.string.rename_folder);
        }

        builder.customView(customView, false);
        builder.positiveText(R.string.save)
                .onPositive((materialDialog, dialogAction) -> {
                    if (editText.getText() != null) {
                        if (FileHelper.renameFile(context, fileObject, editText.getText().toString())) {
                            callbacks.onFileNameChanged(folderView);
                        } else {
                            callbacks.showToast(fileObject.fileType == FileType.FOLDER ? R.string.rename_folder_failed : R.string.rename_file_failed);
                        }
                    }
                });
        builder.negativeText(R.string.cancel)
                .show();
    }

    // Todo: Remove context requirement.
    private static void deleteFile(Context context, FolderView folderView, BaseFileObject fileObject, Callbacks callbacks) {
        MaterialDialog.Builder builder = DialogUtils.getBuilder(context)
                .title(R.string.delete_item)
                .iconRes(R.drawable.ic_warning_24dp);
        if (fileObject.fileType == FileType.FILE) {
            builder.content(String.format(context.getResources().getString(
                    R.string.delete_file_confirmation_dialog), fileObject.name));
        } else {
            builder.content(String.format(context.getResources().getString(
                    R.string.delete_folder_confirmation_dialog), fileObject.path));
        }
        builder.positiveText(R.string.button_ok)
                .onPositive((materialDialog, dialogAction) -> {
                    if (FileHelper.deleteFile(new File(fileObject.path))) {
                        callbacks.onFileDeleted(folderView);
                        CustomMediaScanner.scanFiles(Collections.singletonList(fileObject.path), null);
                    } else {
                        Toast.makeText(context,
                                fileObject.fileType == FileType.FOLDER ? R.string.delete_folder_failed : R.string.delete_file_failed,
                                Toast.LENGTH_LONG).show();
                    }
                });
        builder.negativeText(R.string.cancel)
                .show();
    }

    public static void setupFolderMenu(PopupMenu menu, BaseFileObject fileObject) {

        menu.inflate(R.menu.menu_file);

        // Add playlist menu
        SubMenu sub = menu.getMenu().findItem(R.id.addToPlaylist).getSubMenu();
        PlaylistUtils.createPlaylistMenu(sub);

        if (!fileObject.canReadWrite()) {
            menu.getMenu().findItem(R.id.rename).setVisible(false);
        }

        switch (fileObject.fileType) {
            case FileType.FILE:
                menu.getMenu().findItem(R.id.play).setVisible(false);
                break;
            case FileType.FOLDER:
                menu.getMenu().findItem(R.id.songInfo).setVisible(false);
                menu.getMenu().findItem(R.id.ringtone).setVisible(false);
                menu.getMenu().findItem(R.id.share).setVisible(false);
                menu.getMenu().findItem(R.id.editTags).setVisible(false);
                break;
            case FileType.PARENT:
                break;
        }
    }

    @Nullable
    public static PopupMenu.OnMenuItemClickListener getFolderMenuClickListener(Context context, MediaManager mediaManager, FolderView folderView, Callbacks callbacks) {
        switch (folderView.baseFileObject.fileType) {
            case FileType.FILE:
                return getFileMenuClickListener(context, mediaManager, folderView, (FileObject) folderView.baseFileObject, callbacks);
            case FileType.FOLDER:
                return getFolderMenuClickListener(context, mediaManager, folderView, (FolderObject) folderView.baseFileObject, callbacks);
        }
        return null;
    }

    private static PopupMenu.OnMenuItemClickListener getFolderMenuClickListener(Context context, MediaManager mediaManager, FolderView folderView, FolderObject folderObject, Callbacks callbacks) {

        return menuItem -> {
            switch (menuItem.getItemId()) {
                case R.id.play:
                    MenuUtils.play(mediaManager, getSongsForFolderObject(folderObject), callbacks::showToast);
                    return true;
                case R.id.playNext:
                    callbacks.playNext(getSongsForFolderObject(folderObject));
                    return true;
                case MediaManager.NEW_PLAYLIST:
                    MenuUtils.newPlaylist(context, getSongsForFolderObject(folderObject), callbacks::onPlaylistItemsInserted);
                    return true;
                case MediaManager.PLAYLIST_SELECTED:
                    MenuUtils.addToPlaylist(context, menuItem, getSongsForFolderObject(folderObject), callbacks::onPlaylistItemsInserted);
                    return true;
                case R.id.addToQueue:
                    MenuUtils.addToQueue(mediaManager, getSongsForFolderObject(folderObject), callbacks::onQueueItemsInserted);
                    return true;
                case R.id.scan:
                    scanFolder(context, folderObject);
                    return true;
                case R.id.whitelist:
                    MenuUtils.whitelist(getSongsForFolderObject(folderObject));
                    return true;
                case R.id.blacklist:
                    MenuUtils.blacklist(getSongsForFolderObject(folderObject));
                    return true;
                case R.id.rename:
                    renameFile(context, folderView, folderObject, callbacks);
                    return true;
                case R.id.delete:
                    deleteFile(context, folderView, folderObject, callbacks);
                    return true;
            }
            return false;
        };
    }

    private static PopupMenu.OnMenuItemClickListener getFileMenuClickListener(Context context, MediaManager mediaManager, FolderView folderView, FileObject fileObject, Callbacks callbacks) {
        return menuItem -> {

            Consumer<Throwable> errorHandler = e -> LogUtils.logException(TAG, "getFileMenuClickListener threw error", e);

            switch (menuItem.getItemId()) {
                case R.id.playNext:
                    getSongForFile(fileObject).subscribe(song ->
                            MenuUtils.playNext(mediaManager, song, callbacks::showToast), errorHandler);
                    return true;
                case MediaManager.NEW_PLAYLIST:
                    getSongForFile(fileObject).subscribe(song ->
                            MenuUtils.newPlaylist(context, Collections.singletonList(song), callbacks::onPlaylistItemsInserted), errorHandler);
                    return true;
                case MediaManager.PLAYLIST_SELECTED:
                    getSongForFile(fileObject).subscribe(song ->
                            MenuUtils.addToPlaylist(context, menuItem, Collections.singletonList(song), callbacks::onPlaylistItemsInserted), errorHandler);
                    return true;
                case R.id.addToQueue:
                    getSongForFile(fileObject).subscribe(song ->
                            MenuUtils.addToQueue(mediaManager, Collections.singletonList(song), callbacks::onQueueItemsInserted), errorHandler);
                    return true;
                case R.id.scan:
                    scanFile(fileObject, callbacks);
                    return true;
                case R.id.editTags:
                    getSongForFile(fileObject).subscribe(callbacks::showTagEditor, errorHandler);
                    return true;
                case R.id.share:
                    getSongForFile(fileObject).subscribe(callbacks::shareSong, errorHandler);
                    return true;
                case R.id.ringtone:
                    getSongForFile(fileObject).subscribe(callbacks::setRingtone, errorHandler);
                    return true;
                case R.id.songInfo:
                    getSongForFile(fileObject).subscribe(callbacks::showBiographyDialog, errorHandler);
                    return true;
                case R.id.blacklist:
                    getSongForFile(fileObject).subscribe(MenuUtils::blacklist, errorHandler);
                    return true;
                case R.id.whitelist:
                    getSongForFile(fileObject).subscribe(MenuUtils::whitelist, errorHandler);
                    return true;
                case R.id.rename:
                    renameFile(context, folderView, fileObject, callbacks);
                    return true;
                case R.id.delete:
                    deleteFile(context, folderView, fileObject, callbacks);
                    return true;
            }
            return false;
        };
    }
}
