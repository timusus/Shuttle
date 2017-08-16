package com.afollestad.aesthetic;

import android.content.res.ColorStateList;
import android.support.annotation.RestrictTo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function4;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

/**
 * Handles auto theming of dialogs from my Material Dialogs library, using the ThemeSingleton class.
 * Uses reflection so that Material Dialogs isn't a needed dependency if you depend on this library.
 *
 * @author Aidan Follestad (afollestad)
 */
@RestrictTo(LIBRARY_GROUP)
final class MaterialDialogsUtil {

  static boolean shouldSupport() {
    try {
      Class.forName("com.afollestad.materialdialogs.internal.ThemeSingleton");
    } catch (ClassNotFoundException e) {
      return false;
    }
    return true;
  }

  static class Params {

    final int primaryTextColor;
    final int secondaryTextColor;
    final int accentColor;
    final boolean darkTheme;

    private Params(
        int primaryTextColor, int secondaryTextColor, int accentColor, boolean darkTheme) {
      this.primaryTextColor = primaryTextColor;
      this.secondaryTextColor = secondaryTextColor;
      this.accentColor = accentColor;
      this.darkTheme = darkTheme;
    }

    public static Params create(
        int primaryTextColor, int secondaryTextColor, int accentColor, boolean darkTheme) {
      return new Params(primaryTextColor, secondaryTextColor, accentColor, darkTheme);
    }
  }

  @SuppressWarnings("TryWithIdenticalCatches")
  static void theme(Params params) {
    try {
      Class<?> cls = Class.forName("com.afollestad.materialdialogs.internal.ThemeSingleton");
      Method getMethod = cls.getMethod("get");
      Object instance = getMethod.invoke(null);

      Field fieldDarkTheme = cls.getField("darkTheme");
      fieldDarkTheme.set(instance, params.darkTheme);

      Field fieldTitleColor = cls.getField("titleColor");
      fieldTitleColor.set(instance, params.primaryTextColor);

      Field fieldContentColor = cls.getField("contentColor");
      fieldContentColor.set(instance, params.secondaryTextColor);

      Field fieldItemColor = cls.getField("itemColor");
      fieldItemColor.set(instance, params.secondaryTextColor);

      Field fieldPosColor = cls.getField("positiveColor");
      fieldPosColor.set(instance, ColorStateList.valueOf(params.accentColor));

      Field fieldNeuColor = cls.getField("neutralColor");
      fieldNeuColor.set(instance, ColorStateList.valueOf(params.accentColor));

      Field fieldNegColor = cls.getField("negativeColor");
      fieldNegColor.set(instance, ColorStateList.valueOf(params.accentColor));

      Field fieldWidgetColor = cls.getField("widgetColor");
      fieldWidgetColor.set(instance, ColorStateList.valueOf(params.accentColor));

      Field fieldLinkColor = cls.getField("linkColor");
      fieldLinkColor.set(instance, ColorStateList.valueOf(params.accentColor));

    } catch (Throwable t) {
//      t.printStackTrace();
    }
  }

  static Disposable observe(Aesthetic instance) {
    return Observable.combineLatest(
            instance.textColorPrimary(),
            instance.textColorSecondary(),
            instance.colorAccent(),
            instance.isDark(),
            new Function4<Integer, Integer, Integer, Boolean, Params>() {
              @Override
              public MaterialDialogsUtil.Params apply(
                  @io.reactivex.annotations.NonNull Integer primaryText,
                  @io.reactivex.annotations.NonNull Integer secondaryText,
                  @io.reactivex.annotations.NonNull Integer accent,
                  @io.reactivex.annotations.NonNull Boolean isDark)
                  throws Exception {
                return MaterialDialogsUtil.Params.create(
                    primaryText, secondaryText, accent, isDark);
              }
            })
        .distinctUntilChanged()
        .subscribe(
            new Consumer<Params>() {
              @Override
              public void accept(
                  @io.reactivex.annotations.NonNull MaterialDialogsUtil.Params params)
                  throws Exception {
                MaterialDialogsUtil.theme(params);
              }
            });
  }
}
