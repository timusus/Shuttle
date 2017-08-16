package com.afollestad.aesthetic;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.v7.widget.CardView;
import android.view.View;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.observers.DisposableObserver;

/** @author Aidan Follestad (afollestad) */
@RestrictTo(LIBRARY_GROUP)
final class ViewBackgroundSubscriber extends DisposableObserver<Integer> {

  private final View view;

  private ViewBackgroundSubscriber(@NonNull View view) {
    this.view = view;
  }

  public static ViewBackgroundSubscriber create(@NonNull View view) {
    return new ViewBackgroundSubscriber(view);
  }

  @Override
  public void onError(Throwable e) {
    throw Exceptions.propagate(e);
  }

  @Override
  public void onComplete() {}

  @Override
  public void onNext(Integer color) {
    if (view instanceof CardView) {
      ((CardView) view).setCardBackgroundColor(color);
    } else {
      view.setBackgroundColor(color);
    }
  }
}
