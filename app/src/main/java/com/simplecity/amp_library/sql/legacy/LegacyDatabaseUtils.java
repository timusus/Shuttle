package com.simplecity.amp_library.sql.legacy;

import com.simplecity.amp_library.ShuttleApplication;
import com.squareup.sqlbrite2.BriteDatabase;
import com.squareup.sqlbrite2.SqlBrite;

import java.util.Collections;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class LegacyDatabaseUtils {

    private static final String TAG = "LegacyDatabaseUtils";

    private LegacyDatabaseUtils() {

    }

    /**
     * @return a {@link BriteDatabase} wrapping the blacklist SqliteOpenHelper.
     */
    public static BriteDatabase getBlacklistDatabase() {
        return new SqlBrite.Builder().build()
                .wrapDatabaseHelper(new BlacklistDbOpenHelper(ShuttleApplication.getInstance()), Schedulers.io());
    }

    /**
     * @return a <b>continuous</b> stream of {@link List<BlacklistedSong>>}, backed by a behavior relay for caching query results.
     */
    private static Single<List<BlacklistedSong>> getBlacklistRelay() {
        return getBlacklistDatabase()
                .createQuery(BlacklistDbOpenHelper.TABLE_SONGS, "SELECT * FROM " + BlacklistDbOpenHelper.TABLE_SONGS)
                .mapToList(BlacklistedSong::new)
                .first(Collections.emptyList())
                .subscribeOn(Schedulers.io());
    }

    /**
     * @return a {@link BriteDatabase} wrapping the whitelist SqliteOpenHelper.
     */
    public static BriteDatabase getWhitelistDatabase() {
        return new SqlBrite.Builder().build()
                .wrapDatabaseHelper(new WhitelistDbOpenHelper(ShuttleApplication.getInstance()), Schedulers.io());
    }

    /**
     * @return a <b>continuous</b> stream of {@link List<WhitelistFolder>>}
     */
    private static Single<List<WhitelistFolder>> getWhitelistRelay() {
        return getWhitelistDatabase()
                .createQuery(WhitelistDbOpenHelper.TABLE_FOLDERS, "SELECT * FROM " + WhitelistDbOpenHelper.TABLE_FOLDERS)
                .mapToList(WhitelistFolder::new)
                .first(Collections.emptyList())
                .subscribeOn(Schedulers.io());
    }

    public static Single<List<WhitelistFolder>> getWhitelistFolders() {
        return getWhitelistDatabase()
                .createQuery(WhitelistDbOpenHelper.TABLE_FOLDERS, "SELECT * FROM " + WhitelistDbOpenHelper.TABLE_FOLDERS)
                .mapToList(WhitelistFolder::new)
                .first(Collections.emptyList());
    }

    public static Single<List<BlacklistedSong>> getBlacklistSongsObservable() {
        return getBlacklistDatabase()
                .createQuery(BlacklistDbOpenHelper.TABLE_SONGS, "SELECT * FROM " + BlacklistDbOpenHelper.TABLE_SONGS)
                .mapToList(BlacklistedSong::new)
                .first(Collections.emptyList());
    }
}