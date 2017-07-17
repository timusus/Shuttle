package com.simplecity.amp_library.ui.dialog;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import android.support.annotation.StringRes;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.sql.providers.PlayCountTable;
import com.simplecity.amp_library.utils.CustomMediaScanner;
import com.simplecity.amp_library.utils.DialogUtils;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.MusicUtils;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class DeleteDialog {

    private static final String TAG = "DeleteDialog";

    private DeleteDialog() {
    }

    public static class DeleteDialogBuilder {

        private Context context;

        @StringRes
        private int deleteSingleMessageId;

        @StringRes
        private int deleteMultipleMessageId;

        private List<String> itemNames;
        private Single<List<Song>> songsSingle;

        public DeleteDialogBuilder context(Context context) {
            this.context = context;
            return this;
        }

        public DeleteDialogBuilder singleMessageId(@StringRes int deleteSingleMessageId) {
            this.deleteSingleMessageId = deleteSingleMessageId;
            return this;
        }

        public DeleteDialogBuilder multipleMessage(@StringRes int deleteMultipleMessageId) {
            this.deleteMultipleMessageId = deleteMultipleMessageId;
            return this;
        }

        public DeleteDialogBuilder itemNames(List<String> itemNames) {
            this.itemNames = itemNames;
            return this;
        }

        public DeleteDialogBuilder songsToDelete(Single<List<Song>> songsObservable) {
            this.songsSingle = songsObservable;
            return this;
        }

        void deleteSongs() {
            songsSingle
                    .map(lists -> Stream.of(lists)
                            .flatMap(Stream::of)
                            .filter(Song::delete)
                            .toList())
                    .doOnSuccess(songs -> {
                        //Current play queue
                        MusicUtils.removeFromQueue(songs, true);

                        //Play Count Table
                        ArrayList<ContentProviderOperation> operations = Stream.of(songs).map(song -> ContentProviderOperation
                                .newDelete(PlayCountTable.URI)
                                .withSelection(PlayCountTable.COLUMN_ID + "=" + song.id, null)
                                .build())
                                .collect(Collectors.toCollection(ArrayList<ContentProviderOperation>::new));

                        try {
                            context.getContentResolver().applyBatch(PlayCountTable.AUTHORITY, operations);
                        } catch (RemoteException | OperationApplicationException e) {
                            e.printStackTrace();
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(deletedSongs -> {
                        if (deletedSongs.size() > 0) {
                            CustomMediaScanner.scanFiles(Stream.of(deletedSongs)
                                    .map(song -> song.path)
                                    .toList(), null);
                            Toast.makeText(context, String.format(context.getString(R.string.delete_songs_success_toast), deletedSongs.size()), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, R.string.delete_songs_failure_toast, Toast.LENGTH_SHORT).show();
                        }
                    }, error -> LogUtils.logException(TAG, "Error scanning files", error));
        }

        public MaterialDialog build() {

            String stringToFormat = context.getString(deleteSingleMessageId);

            String names;

            if (itemNames.size() > 1) {
                stringToFormat = context.getString(deleteMultipleMessageId);
                names = Stream.of(itemNames)
                        .map(itemName -> "\n\u2022 " + itemName)
                        .collect(Collectors.joining()) + "\n";
            } else {
                names = itemNames.get(0);
            }

            String message = String.format(stringToFormat, names);

            return DialogUtils.getBuilder(context)
                    .iconRes(R.drawable.ic_warning_24dp)
                    .title(R.string.delete_item)
                    .content(message)
                    .positiveText(R.string.button_ok)
                    .onPositive((materialDialog, dialogAction) -> deleteSongs())
                    .negativeText(R.string.cancel)
                    .build();
        }
    }
}
