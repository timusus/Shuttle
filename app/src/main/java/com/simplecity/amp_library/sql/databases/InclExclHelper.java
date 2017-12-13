package com.simplecity.amp_library.sql.databases;

import android.content.ContentValues;

import com.annimon.stream.Stream;
import com.simplecity.amp_library.model.InclExclItem;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.utils.DataManager;
import com.squareup.sqlbrite2.BriteDatabase;

import java.util.List;

public class InclExclHelper {

    private static final String TAG = "InclExclHelper";

    private InclExclHelper() {

    }

    public static void addToInclExcl(InclExclItem inclExclItem) {
        ContentValues values = new ContentValues(2);
        values.put(InclExclDbOpenHelper.COLUMN_PATH, inclExclItem.path);
        values.put(InclExclDbOpenHelper.COLUMN_TYPE, inclExclItem.type);
        DataManager.getInstance().getInclExclDatabase().insert(InclExclDbOpenHelper.TABLE_NAME, values);
    }

    public static void addToInclExcl(List<InclExclItem> inclExclItems) {
        BriteDatabase db = DataManager.getInstance().getInclExclDatabase();
        BriteDatabase.Transaction transaction = db.newTransaction();
        try {
            Stream.of(inclExclItems).map(inclExclItem -> {
                ContentValues contentValues = new ContentValues(2);
                contentValues.put(InclExclDbOpenHelper.COLUMN_PATH, inclExclItem.path);
                contentValues.put(InclExclDbOpenHelper.COLUMN_TYPE, inclExclItem.type);
                return contentValues;
            }).forEach(contentValues -> db.insert(InclExclDbOpenHelper.TABLE_NAME, contentValues));
            transaction.markSuccessful();
        } finally {
            transaction.end();
        }
    }

    public static void deleteInclExclItem(InclExclItem inclExclItem) {
        DataManager.getInstance().getInclExclDatabase().delete(InclExclDbOpenHelper.TABLE_NAME,
                InclExclDbOpenHelper.COLUMN_PATH + " = '" + inclExclItem.path.replaceAll("'", "\''") + "'" +
                        " AND " + InclExclDbOpenHelper.COLUMN_TYPE + " = " + inclExclItem.type);
    }

    public static void deleteAllItems(@InclExclItem.Type int type) {
        DataManager.getInstance().getInclExclDatabase().delete(InclExclDbOpenHelper.TABLE_NAME, InclExclDbOpenHelper.COLUMN_TYPE + " = " + type);
    }

    public static void addToInclExcl(Song song, @InclExclItem.Type int type) {
        addToInclExcl(new InclExclItem(song.path, type));
    }

    public static void addToInclExcl(List<Song> songs, @InclExclItem.Type int type) {
        addToInclExcl(Stream.of(songs).map(song -> new InclExclItem(song.path, type)).toList());
    }
}