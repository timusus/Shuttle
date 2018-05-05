package com.simplecity.amp_library.sql;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.WorkerThread;
import android.util.Log;
import com.simplecity.amp_library.BuildConfig;
import com.simplecity.amp_library.model.Query;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.ThreadUtils;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import java.util.ArrayList;
import java.util.List;

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

    public static <T> List<T> createQuery(Context context, Function<Cursor, T> mapper, Query query) {

        List<T> items = new ArrayList<>();

        Cursor cursor = createQuery(context, query);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    do {
                        T item = mapper.apply(cursor);
                        if (item == null) {
                            throw new NullPointerException("Mapper returned null for row " + cursor.getPosition());
                        }
                        items.add(item);
                    } while (cursor.moveToNext());
                }
            } catch (Exception e) {
                LogUtils.logException(TAG, "createSingle threw an error", e);
            } finally {
                cursor.close();
            }
        }
        return items;
    }

    public static void createActionableQuery(Context context, Consumer<Cursor> action, Query query) {

        Cursor cursor = createQuery(context, query);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    do {
                        action.accept(cursor);
                    } while (cursor.moveToNext());
                }
            } catch (Exception e) {
                LogUtils.logException(TAG, "createActionableQuery threw an error", e);
            } finally {
                cursor.close();
            }
        }
    }

    public static <T> T createSingleQuery(Context context, Function<Cursor, T> mapper, Query query) {
        return createSingleQuery(context, mapper, null, query);
    }

    public static <T> T createSingleQuery(Context context, Function<Cursor, T> mapper, T defaultValue, Query query) {

        T item = defaultValue;

        Cursor cursor = createQuery(context, query);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    item = mapper.apply(cursor);
                    if (cursor.moveToNext()) {
                        Log.e(TAG, "Cursor returned more than 1 row. Query: " + query);
                    }
                }
            } catch (Exception e) {
                LogUtils.logException(TAG, "createSingleQuery threw an error", e);
                e.printStackTrace();
            } finally {
                cursor.close();
            }
        }
        return item;
    }
}