package com.simplecity.amp_library.sql.databases;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.annimon.stream.Stream;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.model.InclExclItem;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.sql.legacy.BlacklistDbOpenHelper;
import com.simplecity.amp_library.sql.legacy.BlacklistedSong;
import com.simplecity.amp_library.sql.legacy.LegacyDatabaseUtils;
import com.simplecity.amp_library.sql.legacy.WhitelistDbOpenHelper;
import com.simplecity.amp_library.sql.legacy.WhitelistFolder;
import com.simplecity.amp_library.utils.DataManager;

import java.util.Collections;
import java.util.List;

public class InclExclDbOpenHelper extends SQLiteOpenHelper {

    private static final String TAG = "InclExclDbOpenHelper";

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_PATH = "path";
    public static final String COLUMN_TYPE = "type";

    private static final String DATABASE_NAME = "inclexcl.db";
    public static final String TABLE_NAME = "inclexcl";

    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_CREATE_WHITELIST = "CREATE TABLE IF NOT EXISTS "
            + TABLE_NAME + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_PATH + " TEXT NOT NULL, "
            + COLUMN_TYPE + " INTEGER DEFAULT 0"
            + ");";

    public InclExclDbOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE_WHITELIST);

        // It's OK to block here, we're already on a background thread. Not sure if this is a given
        // with SQLiteOpenHelper, but certainly since we're using SqlBrite, we're safe. We need to block
        // so we can complete the SQL transaction in the onCreate() block, allowing the framework to commit
        // the transaction (which happens after this method is called).

        // Blacklist
        List<BlacklistedSong> blacklistedSongs = LegacyDatabaseUtils.getBlacklistSongsObservable().blockingGet();
        if (!blacklistedSongs.isEmpty()) {
            List<Song> allSongs = DataManager.getInstance().getAllSongsRelay().first(Collections.emptyList()).blockingGet();
            Stream.of(allSongs)
                    .filter(song -> Stream.of(blacklistedSongs)
                            .anyMatch(blacklistedSong -> blacklistedSong.songId == song.id))
                    .forEach(song -> {
                        ContentValues contentValues = new ContentValues(2);
                        contentValues.put(InclExclDbOpenHelper.COLUMN_PATH, song.path);
                        contentValues.put(InclExclDbOpenHelper.COLUMN_TYPE, InclExclItem.Type.EXCLUDE);
                        database.insert(TABLE_NAME, null, contentValues);
                    });
        }

        // Whitelist
        List<WhitelistFolder> whitelistFolders = LegacyDatabaseUtils.getWhitelistFolders().blockingGet();
        Stream.of(whitelistFolders)
                .forEach(whitelistFolder -> {
                    ContentValues contentValues = new ContentValues(2);
                    contentValues.put(InclExclDbOpenHelper.COLUMN_PATH, whitelistFolder.folder);
                    contentValues.put(InclExclDbOpenHelper.COLUMN_TYPE, InclExclItem.Type.INCLUDE);
                    database.insert(TABLE_NAME, null, contentValues);
                });

        ShuttleApplication.getInstance().deleteDatabase(BlacklistDbOpenHelper.DATABASE_NAME);
        ShuttleApplication.getInstance().deleteDatabase(WhitelistDbOpenHelper.DATABASE_NAME);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}