package com.simplecity.amp_library.sql.providers;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.simplecity.amp_library.BuildConfig;
import com.simplecity.amp_library.sql.databases.CustomArtworkTable;

import java.util.Arrays;
import java.util.HashSet;

public class CustomArtworkContentProvider extends ContentProvider {

    private CustomArtworkTable database;

    // Used for the Uri Matcher
    private static final int CUSTOM_ARTWORK = 10;

    private static final int CUSTOM_ARTWORK_ID = 20;

    private static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".custom_artwork.contentprovider";

    private static final String BASE_PATH = "custom_artwork";

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sURIMatcher.addURI(AUTHORITY, BASE_PATH, CUSTOM_ARTWORK);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", CUSTOM_ARTWORK_ID);
    }

    @Override
    public boolean onCreate() {
        database = new CustomArtworkTable(getContext());
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
        queryBuilder.setTables(CustomArtworkTable.TABLE_ARTIST_ART);

        int uriType = sURIMatcher.match(uri);
        switch (uriType) {
            case CUSTOM_ARTWORK:
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
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        long id;
        switch (uriType) {
            case CUSTOM_ARTWORK:
                id = sqlDB.insert(CustomArtworkTable.TABLE_ARTIST_ART, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(BASE_PATH + "/" + id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        int rowsDeleted;
        switch (uriType) {
            case CUSTOM_ARTWORK:
                rowsDeleted = sqlDB.delete(CustomArtworkTable.TABLE_ARTIST_ART, selection,
                        selectionArgs);
                break;
            case CUSTOM_ARTWORK_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(CustomArtworkTable.TABLE_ARTIST_ART,
                            CustomArtworkTable.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsDeleted = sqlDB.delete(CustomArtworkTable.TABLE_ARTIST_ART,
                            CustomArtworkTable.COLUMN_ID + "=" + id
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
            case CUSTOM_ARTWORK:
                rowsUpdated = sqlDB.update(CustomArtworkTable.TABLE_ARTIST_ART,
                        values,
                        selection,
                        selectionArgs);
                break;
            case CUSTOM_ARTWORK_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(CustomArtworkTable.TABLE_ARTIST_ART,
                            values,
                            CustomArtworkTable.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsUpdated = sqlDB.update(CustomArtworkTable.TABLE_ARTIST_ART,
                            values,
                            CustomArtworkTable.COLUMN_ID + "=" + id
                                    + " and "
                                    + selection,
                            selectionArgs
                    );
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }

    private void checkColumns(String[] projection) {
        String[] available = {
                CustomArtworkTable.COLUMN_ID,
                CustomArtworkTable.COLUMN_KEY,
                CustomArtworkTable.COLUMN_PATH,
                CustomArtworkTable.COLUMN_TYPE
        };
        if (projection != null) {
            HashSet<String> requestedColumns = new HashSet<>(Arrays.asList(projection));
            HashSet<String> availableColumns = new HashSet<>(Arrays.asList(available));
            // check if all columns which are requested are available
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown columns in PROJECTION");
            }
        }
    }

}
