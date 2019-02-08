package com.simplecity.amp_library.sql.sqlbrite;

import android.database.Cursor;
import android.support.annotation.Nullable;
import com.squareup.sqlbrite2.SqlBrite;
import io.reactivex.ObservableOperator;
import io.reactivex.Observer;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.plugins.RxJavaPlugins;

final class QueryToOneOperator<T> implements ObservableOperator<T, SqlBrite.Query> {
    private final Function<Cursor, T> mapper;
    private final T defaultValue;

    /**
     * A null {@code defaultValue} means nothing will be emitted when empty.
     */
    QueryToOneOperator(Function<Cursor, T> mapper, @Nullable T defaultValue) {
        this.mapper = mapper;
        this.defaultValue = defaultValue;
    }

    @Override
    public Observer<? super SqlBrite.Query> apply(Observer<? super T> observer) {
        return new MappingObserver<>(observer, mapper, defaultValue);
    }

    static final class MappingObserver<T> extends DisposableObserver<SqlBrite.Query> {
        private final Observer<? super T> downstream;
        private final Function<Cursor, T> mapper;
        private final T defaultValue;

        MappingObserver(Observer<? super T> downstream, Function<Cursor, T> mapper, T defaultValue) {
            this.downstream = downstream;
            this.mapper = mapper;
            this.defaultValue = defaultValue;
        }

        @Override
        protected void onStart() {
            downstream.onSubscribe(this);
        }

        @Override
        public void onNext(SqlBrite.Query query) {
            try {
                T item = null;
                Cursor cursor = query.run();
                if (cursor != null) {
                    try {
                        if (cursor.moveToNext()) {
                            item = mapper.apply(cursor);
                            if (item == null) {
                                downstream.onError(new NullPointerException("QueryToOne mapper returned null"));
                                return;
                            }
                            if (cursor.moveToNext()) {
                                throw new IllegalStateException("Cursor returned more than 1 row");
                            }
                        }
                    } finally {
                        cursor.close();
                    }
                }
                if (!isDisposed()) {
                    if (item != null) {
                        downstream.onNext(item);
                    } else if (defaultValue != null) {
                        downstream.onNext(defaultValue);
                    }
                }
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                onError(e);
            }
        }

        @Override
        public void onComplete() {
            if (!isDisposed()) {
                downstream.onComplete();
            }
        }

        @Override
        public void onError(Throwable e) {
            if (isDisposed()) {
                RxJavaPlugins.onError(e);
            } else {
                downstream.onError(e);
            }
        }
    }
}