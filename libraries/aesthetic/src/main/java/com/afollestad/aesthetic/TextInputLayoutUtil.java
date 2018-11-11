package com.afollestad.aesthetic;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.res.ColorStateList;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.design.widget.TextInputLayout;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** @author Aidan Follestad (afollestad) */
@RestrictTo(LIBRARY_GROUP)
final class TextInputLayoutUtil {

  static void setHint(@NonNull TextInputLayout view, @ColorInt int hintColor) {
    try {
      final Field mDefaultTextColorField =
         Util.findField(TextInputLayout.class, "defaultHintTextColor", "mDefaultTextColor");
      mDefaultTextColorField.setAccessible(true);
      mDefaultTextColorField.set(view, ColorStateList.valueOf(hintColor));
      final Method updateLabelStateMethod =
          TextInputLayout.class.getDeclaredMethod("updateLabelState", boolean.class, boolean.class);
      updateLabelStateMethod.setAccessible(true);
      updateLabelStateMethod.invoke(view, false, true);
    } catch (Throwable t) {
      throw new IllegalStateException(
          "Failed to set TextInputLayout hint (collapsed) color: " + t.getLocalizedMessage(), t);
    }
  }

  static void setAccent(@NonNull TextInputLayout view, @ColorInt int accentColor) {
    try {
      final Field mFocusedTextColorField = Util.findField(TextInputLayout.class, "focusedTextColor", "mFocusedTextColor");
      mFocusedTextColorField.setAccessible(true);
      mFocusedTextColorField.set(view, ColorStateList.valueOf(accentColor));
      final Method updateLabelStateMethod =
          TextInputLayout.class.getDeclaredMethod("updateLabelState", boolean.class, boolean.class);
      updateLabelStateMethod.setAccessible(true);
      updateLabelStateMethod.invoke(view, false, true);
    } catch (Throwable t) {
      throw new IllegalStateException(
          "Failed to set TextInputLayout accent (expanded) color: " + t.getLocalizedMessage(), t);
    }
  }
}
