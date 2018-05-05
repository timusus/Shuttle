package com.simplecity.amp_library.sql.sqlbrite;

import android.database.Cursor;
import com.squareup.sqlbrite2.SqlBrite;
import io.reactivex.ObservableOperator;
import io.reactivex.Observer;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.plugins.RxJavaPlugins;
import java.util.ArrayList;
import java.util.List;

public final class QueryToListOperator<T> implements ObservableOperator<List<T>, SqlBrite.Query> {
    private final Function<Cursor, T> mapper;

    QueryToListOperator(Function<Cursor, T> mapper) {
        this.mapper = mapper;
    }

    @Override
    public Observer<? super SqlBrite.Query> apply(Observer<? super List<T>> observer) {
        return new MappingObserver<>(observer, mapper);
    }

    static final class MappingObserver<T> extends DisposableObserver<SqlBrite.Query> {
        private final Observer<? super List<T>> downstream;
        private final Function<Cursor, T> mapper;

        MappingObserver(Observer<? super List<T>> downstream, Function<Cursor, T> mapper) {
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
                Cursor cursor = query.run();
                if (cursor == null || isDisposed()) {
                    return;
                }
                List<T> items = new ArrayList<>(cursor.getCount());
                try {
                    while (cursor.moveToNext()) {
                        items.add(mapper.apply(cursor));
                    }
                } finally {
                    cursor.close();
                }
                if (!isDisposed()) {
                    downstream.onNext(items);
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