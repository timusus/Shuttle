package com.afollestad.aesthetic;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Consumer;

/** @author Aidan Follestad (afollestad) */
public final class Rx {

  public static Consumer<Throwable> onErrorLogAndRethrow() {
    return new Consumer<Throwable>() {
      @Override
      public void accept(@NonNull Throwable throwable) throws Exception {
        throwable.printStackTrace();
        throw Exceptions.propagate(throwable);
      }
    };
  }

  public static <T> ObservableTransformer<T, T> distinctToMainThread() {
    return new ObservableTransformer<T, T>() {
      @Override
      public ObservableSource<T> apply(@NonNull Observable<T> obs) {
        return obs.observeOn(AndroidSchedulers.mainThread()).distinctUntilChanged();
      }
    };
  }
}
