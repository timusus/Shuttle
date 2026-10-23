package com.afollestad.aesthetic;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.res.ColorStateList;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import com.google.android.material.textfield.TextInputLayout;

/** @author Aidan Follestad (afollestad) */
@RestrictTo(LIBRARY_GROUP)
final class TextInputLayoutUtil {

  static void setHint(@NonNull TextInputLayout view, @ColorInt int hintColor) {
    view.setDefaultHintTextColor(ColorStateList.valueOf(hintColor));
  }

  static void setAccent(@NonNull TextInputLayout view, @ColorInt int accentColor) {
    view.setHintTextColor(ColorStateList.valueOf(accentColor));
  }
}
