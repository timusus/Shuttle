package com.simplecity.amp_library.sql.providers;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.simplecity.amp_library.BuildConfig;

import java.util.Arrays;
import java.util.HashSet;

public class PlayCountContentProvider extends ContentProvider {

    private static final String TAG = "PlayCountContentProvide";

    private PlayCountTable database;

    // Used for the Uri Matcher
    private static final int PLAY_COUNT = 10;

    private static final int PLAY_COUNT_ID = 20;

    private static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".play_count.contentprovider";

    private static final String BASE_PATH = "play_count";

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sURIMatcher.addURI(AUTHORITY, BASE_PATH, PLAY_COUNT);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", PLAY_COUNT_ID);
    }

    @Override
    public boolean onCreate() {
        database = new PlayCountTable(getContext());
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        // Using SQLiteQueryBuilder instead of query() method
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        // Check if the caller has requested a column which does not exists
        checkColumns(projection);

        // Set the table
        queryBuilder.setTables(PlayCountTable.TABLE_PLAY_COUNT);

        int uriType = sURIMatcher.match(uri);
        switch (uriType) {
            case PLAY_COUNT:
                break;
            case PLAY_COUNT_ID:
                // Adding the ID to the original query
                queryBuilder.appendWhere(PlayCountTable.COLUMN_ID + "=" + uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase db = database.getWritableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection,
                selectionArgs, null, null, sortOrder);
        // Make sure that potential listeners are getting notified
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        long id;
        switch (uriType) {
            case PLAY_COUNT:
                id = sqlDB.insert(PlayCountTable.TABLE_PLAY_COUNT, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        if (id != -1) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return Uri.parse(BASE_PATH + "/" + id);
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        int rowsDeleted;
        switch (uriType) {
            case PLAY_COUNT:
                rowsDeleted = sqlDB.delete(PlayCountTable.TABLE_PLAY_COUNT, selection, selectionArgs);
                break;
            case PLAY_COUNT_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(PlayCountTable.TABLE_PLAY_COUNT,
                            PlayCountTable.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsDeleted = sqlDB.delete(PlayCountTable.TABLE_PLAY_COUNT,
                            PlayCountTable.COLUMN_ID + "=" + id
                                    + " and " + selection,
                            selectionArgs
                    );
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        int rowsUpdated;
        switch (uriType) {
            case PLAY_COUNT:
                rowsUpdated = sqlDB.update(PlayCountTable.TABLE_PLAY_COUNT,
                        values,
                        selection,
                        selectionArgs);
                break;
            case PLAY_COUNT_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(PlayCountTable.TABLE_PLAY_COUNT,
                            values,
                            PlayCountTable.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsUpdated = sqlDB.update(PlayCountTable.TABLE_PLAY_COUNT,
                            values,
                            PlayCountTable.COLUMN_ID + "=" + id
                                    + " and "
                                    + selection,
                            selectionArgs
                    );
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        if (rowsUpdated > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    private void checkColumns(String[] projection) {
        String[] available = {
                PlayCountTable.COLUMN_ID,
                PlayCountTable.COLUMN_PLAY_COUNT,
                PlayCountTable.COLUMN_TIME_PLAYED
        };

        if (projection != null)

        {
            HashSet<String> requestedColumns = new HashSet<>(Arrays.asList(projection));
            HashSet<String> availableColumns = new HashSet<>(Arrays.asList(available));
            // Check if all columns which are requested are available
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown columns in PROJECTION");
            }
        }
    }

}
