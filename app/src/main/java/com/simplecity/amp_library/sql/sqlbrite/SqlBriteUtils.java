package com.simplecity.amp_library.sql.sqlbrite;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import com.simplecity.amp_library.BuildConfig;
import com.simplecity.amp_library.model.Query;
import com.squareup.sqlbrite2.BriteContentResolver;
import com.squareup.sqlbrite2.SqlBrite;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import java.util.Collections;
import java.util.List;

public final class SqlBriteUtils {

    static final boolean LOGGING_ENABLED = false;

    private SqlBriteUtils() {

    }

    private static final String TAG = "SqlBriteUtils";

    private static BriteContentResolver wrapContentProvider(@NonNull Context context) {
        final SqlBrite sqlBrite = new SqlBrite.Builder().build();
        BriteContentResolver briteContentResolver = sqlBrite.wrapContentProvider(context.getContentResolver(), Schedulers.io());
        briteContentResolver.setLoggingEnabled(LOGGING_ENABLED && BuildConfig.DEBUG);
        return briteContentResolver;
    }

    private static Observable<SqlBrite.Query> createObservable(@NonNull Context context, @NonNull Query query) {
        return wrapContentProvider(context)
                .createQuery(query.uri, query.projection, query.selection, query.args, query.sort, false)
                .subscribeOn(Schedulers.io())
                .doOnError(error -> Log.e(TAG, "Query failed.\nError:" + error.toString() + "\nQuery: " + query.toString()));
    }

    /**
     * Creates an {@link Observable} that emits new items when subscribed and when the content provider notifies of a change.
     */
    public static <T> Observable<T> createObservable(@NonNull Context context, @NonNull Function<Cursor, T> mapper, @NonNull Query query, T defaultValue) {
        return createObservable(context, query)
                .lift(new QueryToOneOperator<>(mapper, defaultValue));
    }

    /**
     * Creates a {@link Single} that emits an item.
     */
    public static <T> Single<T> createSingle(@NonNull Context context, @NonNull Function<Cursor, T> mapper, @NonNull Query query, T defaultValue) {
        return createObservable(context, mapper, query, defaultValue)
                .firstOrError();
    }

    /**
     * Creates an {@link Observable} that emits new lists when subscribed and when the content provider notifies of a change.
     */
    public static <T> Observable<List<T>> createObservableList(@NonNull Context context, @NonNull Function<Cursor, T> mapper, @NonNull Query query) {
        return createObservable(context, query)
                .lift(new QueryToListOperator<>(mapper));
    }

    /**
     * Creates a {@link Single} that emits a list.
     */
    public static <T> Single<List<T>> createSingleList(@NonNull Context context, @NonNull Function<Cursor, T> mapper, @NonNull Query query) {
        return createObservableList(context, mapper, query)
                .first(Collections.emptyList());
    }
}