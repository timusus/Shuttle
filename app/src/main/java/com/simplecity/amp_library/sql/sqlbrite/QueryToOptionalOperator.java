package com.simplecity.amp_library.sql.sqlbrite;

import android.database.Cursor;
import com.annimon.stream.Optional;
import com.squareup.sqlbrite2.SqlBrite;
import io.reactivex.ObservableOperator;
import io.reactivex.Observer;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.plugins.RxJavaPlugins;

public final class QueryToOptionalOperator<T> implements ObservableOperator<Optional<T>, SqlBrite.Query> {
    private final Function<Cursor, T> mapper;

    QueryToOptionalOperator(Function<Cursor, T> mapper) {
        this.mapper = mapper;
    }

    @Override
    public Observer<? super SqlBrite.Query> apply(Observer<? super Optional<T>> observer) {
        return new MappingObserver<>(observer, mapper);
    }

    static final class MappingObserver<T> extends DisposableObserver<SqlBrite.Query> {
        private final Observer<? super Optional<T>> downstream;
        private final Function<Cursor, T> mapper;

        MappingObserver(Observer<? super Optional<T>> downstream, Function<Cursor, T> mapper) {
            this.downstream = downstream;
            this.mapper = mapper;
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
                    downstream.onNext(Optional.ofNullable(item));
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
