package com.afollestad.aesthetic;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.widget.TextView;
import io.reactivex.functions.Consumer;

/** @author Aidan Follestad (afollestad) */
@RestrictTo(LIBRARY_GROUP)
class ViewHintTextColorAction implements Consumer<Integer> {

  private final TextView view;

  private ViewHintTextColorAction(TextView view) {
    this.view = view;
  }

  public static ViewHintTextColorAction create(@NonNull TextView view) {
    return new ViewHintTextColorAction(view);
  }

  @Override
  public void accept(@io.reactivex.annotations.NonNull Integer color) {
    if (view != null) {
      view.setHintTextColor(color);
    }
  }
}
