package com.simplecity.amp_library.sql;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.simplecity.amp_library.BuildConfig;
import com.simplecity.amp_library.model.Query;
import com.simplecity.amp_library.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.List;

import rx.functions.Action1;
import rx.functions.Func1;

public class SqlUtils {

    private static final boolean ENABLE_LOGGING = false;

    private static final String TAG = "SqlUtils";

    private SqlUtils() {

    }

    @WorkerThread
    public static Cursor createQuery(Context context, Query query) {

        long time = System.currentTimeMillis();

        Cursor cursor = context.getContentResolver()
                .query(query.uri,
                        query.projection,
                        query.selection,
                        query.args,
                        query.sort);

        if (ENABLE_LOGGING && BuildConfig.DEBUG) {
            Log.d(TAG, String.format("Query took %sms. %s", (System.currentTimeMillis() - time), query));
        }

        ThreadUtils.ensureNotOnMainThread();

        return cursor;
    }

    public static <T> List<T> createQuery(Context context, Func1<Cursor, T> mapper, Query query) {

        List<T> items = new ArrayList<>();

        Cursor cursor = createQuery(context, query);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    do {
                        T item = mapper.call(cursor);
                        if (item == null) {
                            throw new NullPointerException("Mapper returned null for row " + cursor.getPosition());
                        }
                        items.add(item);
                    } while (cursor.moveToNext());
                }
            } finally {
                cursor.close();
            }
        }
        return items;
    }

    public static void createActionableQuery(Context context, Action1<Cursor> action, Query query) {

        Cursor cursor = createQuery(context, query);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    do {
                        action.call(cursor);
                    } while (cursor.moveToNext());
                }
            } finally {
                cursor.close();
            }
        }
    }

    public static <T> T createSingleQuery(Context context, Func1<Cursor, T> mapper, Query query) {
        return createSingleQuery(context, mapper, null, query);
    }

    public static <T> T createSingleQuery(Context context, Func1<Cursor, T> mapper, T defaultValue, Query query) {

        T item = defaultValue;

        Cursor cursor = createQuery(context, query);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    item = mapper.call(cursor);
                    if (cursor.moveToNext()) {
                        Log.e(TAG, "Cursor returned more than 1 row. Query: " + query);
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return item;
    }
}