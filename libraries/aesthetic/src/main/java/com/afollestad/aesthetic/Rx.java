package com.afollestad.aesthetic;

import android.util.Log;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;

/** @author Aidan Follestad (afollestad) */
public final class Rx {

  public static Consumer<Throwable> onErrorLogAndRethrow() {
    return new Consumer<Throwable>() {
      @Override
      public void accept(@NonNull Throwable throwable) throws Exception {
        // A theming failure must not take the app down; the subscription just stops updating.
        Log.e("Aesthetic", "Theme subscription failed", throwable);
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
