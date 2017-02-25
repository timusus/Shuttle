package com.simplecity.amp_library.sql.databases;

import android.content.ContentValues;

import com.annimon.stream.Stream;
import com.simplecity.amp_library.model.WhitelistFolder;
import com.simplecity.amp_library.utils.DataManager;
import com.squareup.sqlbrite.BriteDatabase;

import java.util.List;

import rx.Observable;

public class WhitelistHelper {

    private WhitelistHelper() {

    }

    public static void addToWhitelist(String path) {
        ContentValues values = new ContentValues();
        values.put(WhitelistDbOpenHelper.COLUMN_FOLDER, path);

        DataManager.getInstance().getWhitelistDatabase().insert(WhitelistDbOpenHelper.TABLE_FOLDERS, values);
    }

    public static void addToWhitelist(List<String> paths) {
        BriteDatabase db = DataManager.getInstance().getWhitelistDatabase();
        BriteDatabase.Transaction transaction = db.newTransaction();
        try {
            Stream.of(paths).map(path -> {
                ContentValues contentValues = new ContentValues();
                contentValues.put(WhitelistDbOpenHelper.COLUMN_FOLDER, path);
                return contentValues;
            }).forEach(contentValues -> db.insert(WhitelistDbOpenHelper.TABLE_FOLDERS, contentValues));
            transaction.markSuccessful();
        } finally {
            transaction.end();
        }
    }

    public static void deleteFolder(WhitelistFolder folder) {
        DataManager.getInstance().getWhitelistDatabase().delete(WhitelistDbOpenHelper.TABLE_FOLDERS, WhitelistDbOpenHelper.COLUMN_ID + " = " + folder.id);
    }

    public static Observable<List<WhitelistFolder>> getWhitelistFolders() {
        return DataManager.getInstance().getWhitelistDatabase()
                .createQuery(WhitelistDbOpenHelper.TABLE_FOLDERS, "SELECT * FROM " + WhitelistDbOpenHelper.TABLE_FOLDERS)
                .mapToList(WhitelistFolder::new);
    }

    public static void deleteAllFolders() {
        DataManager.getInstance().getWhitelistDatabase().delete(WhitelistDbOpenHelper.TABLE_FOLDERS, null);
    }
}