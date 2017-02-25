package com.simplecity.amp_library.sql.databases;

import android.content.ContentValues;

import com.annimon.stream.Stream;
import com.simplecity.amp_library.model.BlacklistedSong;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.utils.DataManager;
import com.squareup.sqlbrite.BriteDatabase;

import java.util.List;

import rx.Observable;

public class BlacklistHelper {

    private BlacklistHelper() {

    }

    public static void addToBlacklist(Song song) {
        ContentValues values = new ContentValues();
        values.put(BlacklistDbOpenHelper.COLUMN_SONG_ID, song.id);

        DataManager.getInstance().getBlacklistDatabase().insert(BlacklistDbOpenHelper.TABLE_SONGS, values);
    }

    public static void addToBlacklist(List<Song> songs) {
        BriteDatabase db = DataManager.getInstance().getBlacklistDatabase();
        BriteDatabase.Transaction transaction = db.newTransaction();
        try {
            Stream.of(songs).map(song -> {
                ContentValues contentValues = new ContentValues();
                contentValues.put(BlacklistDbOpenHelper.COLUMN_SONG_ID, song.id);
                return contentValues;
            }).forEach(contentValues -> db.insert(BlacklistDbOpenHelper.TABLE_SONGS, contentValues));
            transaction.markSuccessful();
        } finally {
            transaction.end();
        }
    }

    public static Observable<List<BlacklistedSong>> getBlacklistSongsObservable() {
        return DataManager.getInstance().getBlacklistDatabase()
                .createQuery(BlacklistDbOpenHelper.TABLE_SONGS, "SELECT * FROM " + BlacklistDbOpenHelper.TABLE_SONGS)
                .mapToList(BlacklistedSong::new);
    }

    public static void deleteSong(long songId) {
        DataManager.getInstance().getBlacklistDatabase().delete(BlacklistDbOpenHelper.TABLE_SONGS, BlacklistDbOpenHelper.COLUMN_SONG_ID + " = " + songId);
    }

    public static void deleteAllSongs() {
        DataManager.getInstance().getBlacklistDatabase().delete(BlacklistDbOpenHelper.TABLE_SONGS, null);
    }
}