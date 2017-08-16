package com.afollestad.aesthetic;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.support.annotation.RestrictTo;
import android.view.View;
import io.reactivex.Observable;

/** @author Aidan Follestad (afollestad) */
@RestrictTo(LIBRARY_GROUP)
final class ViewObservablePair {

  private final View view;
  private final Observable<Integer> observable;

  private ViewObservablePair(View view, Observable<Integer> observable) {
    this.view = view;
    this.observable = observable;
  }

  static ViewObservablePair create(View view, Observable<Integer> observable) {
    return new ViewObservablePair(view, observable);
  }

  View view() {
    return view;
  }

  Observable<Integer> observable() {
    return observable;
  }
}
