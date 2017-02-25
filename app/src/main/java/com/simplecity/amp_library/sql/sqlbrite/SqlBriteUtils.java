package com.simplecity.amp_library.sql.sqlbrite;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.simplecity.amp_library.model.Query;
import com.squareup.sqlbrite.BriteContentResolver;
import com.squareup.sqlbrite.SqlBrite;

import java.util.List;

import rx.Observable;
import rx.Single;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public final class SqlBriteUtils {

    private SqlBriteUtils() {

    }

    private static final String TAG = "SqlBriteUtils";

    private static BriteContentResolver wrapContentProvider(Context context) {
        final SqlBrite sqlBrite = SqlBrite.create();
        BriteContentResolver briteContentResolver = sqlBrite.wrapContentProvider(context.getContentResolver(), Schedulers.io());
//        briteContentResolver.setLoggingEnabled(BuildConfig.DEBUG);
        return briteContentResolver;
    }

    /**
     * Creates an {@link Observable} that <b>continuously</b> emits new Lists when subscribed and when the content provider notifies of a change.
     */
    public static <T> Observable<List<T>> createContinuousQuery(Context context, Func1<Cursor, T> mapper, Query query) {
        return wrapContentProvider(context)
                .createQuery(query.uri, query.projection, query.selection, query.args, query.sort, false)
                .subscribeOn(Schedulers.io())
                .lift(new QueryToListOperator<>(mapper))
                .doOnError(error -> Log.e(TAG, "Query failed.\nError:" + error.toString() + "\nQuery: " + query.toString()));
    }

    /**
     * Creates a {@link Observable} that emits a List.
     */
    public static <T> Observable<List<T>> createQuery(Context context, Func1<Cursor, T> mapper, Query query) {
        return createContinuousQuery(context, mapper, query).first();
    }

    /**
     * Creates an {@link Observable} that <b>continuously</b> emits a single item when subscribed and when the content provider notifies of a change.
     * <p>
     * Note: No default item is emitted. If the cursor is empty, downstream subscribers will not
     * receive a result.
     */
    public static <T> Observable<T> createSingleContinuousQuery(Context context, Func1<Cursor, T> mapper, Query query) {
        return createSingleContinuousQuery(context, mapper, null, query);
    }

    /**
     * Creates a {@link Observable} that emits a single item.
     */
    public static <T> Observable<T> createSingleQuery(Context context, Func1<Cursor, T> mapper, Query query) {
        return createSingleContinuousQuery(context, mapper, null, query).first();
    }

    /**
     * Creates an {@link Observable} that <b>continuously</b> emits a single item when subscribed and when the content provider notifies of a change.
     * <p>
     * Note: If no default item is supplied, nothing is emitted. Downstream subscribers will not receive a result.
     *
     * @param defaultValue the default value to emit (or null if no default value should be emitted)
     */
    public static <T> Observable<T> createSingleContinuousQuery(Context context, Func1<Cursor, T> mapper, T defaultValue, Query query) {
        return wrapContentProvider(context)
                .createQuery(query.uri, query.projection, query.selection, query.args, query.sort, false)
                .subscribeOn(Schedulers.io())
                .lift(new QueryToOneOperator<>(mapper, defaultValue != null, defaultValue))
                .doOnError(error -> Log.e(TAG, "Query failed.\nError:" + error.toString() + "\nQuery: " + query.toString()));
    }

    /**
     * Creates a {@link Single} that emits a single item.
     * <p>
     * Note: If no default item is supplied, nothing is emitted. Downstream subscribers will not receive a result.
     */
    public static <T> Observable<T> createSingleQuery(Context context, Func1<Cursor, T> mapper, T defaultValue, Query query) {
        return createSingleContinuousQuery(context, mapper, defaultValue, query).first();
    }
}