package com.afollestad.aesthetic;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import io.reactivex.Observable;
import java.lang.reflect.Field;

import static com.afollestad.aesthetic.TintHelper.createTintedDrawable;
import static com.afollestad.aesthetic.Util.isColorLight;
import static com.afollestad.aesthetic.Util.resolveResId;

/** @author Aidan Follestad (afollestad) */
public final class ViewUtil {

  @Nullable
  public static Observable<Integer> getObservableForResId(
      @NonNull Context context, @IdRes int resId, @Nullable Observable<Integer> fallback) {
    if (resId == 0) {
      return fallback;
    } else if (resId == resolveResId(context, R.attr.colorPrimary, 0)) {
      return Aesthetic.get(context).colorPrimary();
    } else if (resId == resolveResId(context, R.attr.colorPrimaryDark, 0)) {
      return Aesthetic.get(context).colorPrimaryDark();
    } else if (resId == resolveResId(context, android.R.attr.statusBarColor, 0)) {
      return Aesthetic.get(context).colorStatusBar();
    } else if (resId == resolveResId(context, R.attr.colorAccent, 0)) {
      return Aesthetic.get(context).colorAccent();
    } else if (resId == resolveResId(context, android.R.attr.windowBackground, 0)) {
      return Aesthetic.get(context).colorWindowBackground();
    } else if (resId == resolveResId(context, android.R.attr.textColorPrimary, 0)) {
      return Aesthetic.get(context).textColorPrimary();
    } else if (resId == resolveResId(context, android.R.attr.textColorPrimaryInverse, 0)) {
      return Aesthetic.get(context).textColorPrimaryInverse();
    } else if (resId == resolveResId(context, android.R.attr.textColorSecondary, 0)) {
      return Aesthetic.get(context).textColorSecondary();
    } else if (resId == resolveResId(context, android.R.attr.textColorSecondaryInverse, 0)) {
      return Aesthetic.get(context).textColorSecondaryInverse();
    }
    return fallback;
  }

  static void tintToolbarMenu(
      @NonNull Toolbar toolbar, @NonNull Menu menu, ActiveInactiveColors titleIconColors) {
    // The collapse icon displays when action views are expanded (e.g. SearchView)
    try {
      final Field field = Toolbar.class.getDeclaredField("mCollapseIcon");
      field.setAccessible(true);
      Drawable collapseIcon = (Drawable) field.get(toolbar);
      if (collapseIcon != null)
        field.set(toolbar, createTintedDrawable(collapseIcon, titleIconColors.toEnabledSl()));
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Theme menu action views
    for (int i = 0; i < menu.size(); i++) {
      MenuItem item = menu.getItem(i);
      if (item.getActionView() instanceof SearchView) {
        themeSearchView(titleIconColors, (SearchView) item.getActionView());
      }
    }
  }

  private static void themeSearchView(ActiveInactiveColors tintColors, SearchView view) {
    final Class<?> cls = view.getClass();
    try {
      final Field mSearchSrcTextViewField = cls.getDeclaredField("mSearchSrcTextView");
      mSearchSrcTextViewField.setAccessible(true);
      final EditText mSearchSrcTextView = (EditText) mSearchSrcTextViewField.get(view);
      mSearchSrcTextView.setTextColor(tintColors.activeColor());
      mSearchSrcTextView.setHintTextColor(tintColors.inactiveColor());
      TintHelper.setCursorTint(mSearchSrcTextView, tintColors.activeColor());

      Field field = cls.getDeclaredField("mSearchButton");
      tintImageView(view, field, tintColors);
      field = cls.getDeclaredField("mGoButton");
      tintImageView(view, field, tintColors);
      field = cls.getDeclaredField("mCloseButton");
      tintImageView(view, field, tintColors);
      field = cls.getDeclaredField("mVoiceButton");
      tintImageView(view, field, tintColors);

      field = cls.getDeclaredField("mSearchPlate");
      field.setAccessible(true);
      TintHelper.setTintAuto(
          (View) field.get(view),
          tintColors.activeColor(),
          true,
          !isColorLight(tintColors.activeColor()));

      field = cls.getDeclaredField("mSearchHintIcon");
      field.setAccessible(true);
      field.set(view, createTintedDrawable((Drawable) field.get(view), tintColors.toEnabledSl()));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void tintImageView(Object target, Field field, ActiveInactiveColors tintColors)
      throws Exception {
    field.setAccessible(true);
    final ImageView imageView = (ImageView) field.get(target);
    if (imageView.getDrawable() != null) {
      imageView.setImageDrawable(
          createTintedDrawable(imageView.getDrawable(), tintColors.toEnabledSl()));
    }
  }
}
