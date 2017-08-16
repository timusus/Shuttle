package com.afollestad.aesthetic;

import android.support.annotation.NonNull;
import android.view.View;
import io.reactivex.functions.Consumer;

/** @author Aidan Follestad (afollestad) */
@SuppressWarnings("WeakerAccess")
public final class ViewBackgroundAction implements Consumer<Integer> {

  private final View view;

  private ViewBackgroundAction(View view) {
    this.view = view;
  }

  public static ViewBackgroundAction create(@NonNull View view) {
    return new ViewBackgroundAction(view);
  }

  @Override
  public void accept(@io.reactivex.annotations.NonNull Integer color) {
    if (view != null) {
      view.setBackgroundColor(color);
    }
  }
}
