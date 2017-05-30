package com.afollestad.aesthetic;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.view.LayoutInflaterCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

/** @author Aidan Follestad (afollestad) */
@RestrictTo(LIBRARY_GROUP)
public final class Util {

  static void setInflaterFactory(@NonNull LayoutInflater li, @NonNull AppCompatActivity activity) {
    LayoutInflaterCompat.setFactory(
        li, new InflationInterceptor(activity, li, activity.getDelegate()));
  }

  /** Taken from CollapsingToolbarLayout's CollapsingTextHelper class. */
  static int blendColors(int color1, int color2, float ratio) {
    final float inverseRatio = 1f - ratio;
    float a = (Color.alpha(color1) * inverseRatio) + (Color.alpha(color2) * ratio);
    float r = (Color.red(color1) * inverseRatio) + (Color.red(color2) * ratio);
    float g = (Color.green(color1) * inverseRatio) + (Color.green(color2) * ratio);
    float b = (Color.blue(color1) * inverseRatio) + (Color.blue(color2) * ratio);
    return Color.argb((int) a, (int) r, (int) g, (int) b);
  }

  @SuppressWarnings("deprecation")
  static void setBackgroundCompat(@NonNull View view, @Nullable Drawable drawable) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      view.setBackground(drawable);
    } else {
      view.setBackgroundDrawable(drawable);
    }
  }

  static void setStatusBarColorCompat(@NonNull AppCompatActivity activity, @ColorInt int color) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      activity.getWindow().setStatusBarColor(color);
    }
  }

  static void setNavBarColorCompat(@NonNull AppCompatActivity activity, @ColorInt int color) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      activity.getWindow().setNavigationBarColor(color);
    }
  }

  static int stripAlpha(@ColorInt int color) {
    return Color.rgb(Color.red(color), Color.green(color), Color.blue(color));
  }

  @ColorInt
  static int resolveColor(Context context, @AttrRes int attr) {
    return resolveColor(context, attr, 0);
  }

  @ColorInt
  static int resolveColor(Context context, @AttrRes int attr, int fallback) {
    TypedArray a = context.getTheme().obtainStyledAttributes(new int[] {attr});
    try {
      return a.getColor(0, fallback);
    } catch (Throwable ignored) {
      return fallback;
    } finally {
      a.recycle();
    }
  }

  @IdRes
  public static int resolveResId(Context context, @AttrRes int attr, int fallback) {
    TypedArray a = context.getTheme().obtainStyledAttributes(new int[] {attr});
    try {
      return a.getResourceId(0, fallback);
    } finally {
      a.recycle();
    }
  }

  public static int resolveResId(Context context, AttributeSet attrs, @AttrRes int attrId) {
    TypedArray ta = context.obtainStyledAttributes(attrs, new int[] {attrId});
    int result = ta.getResourceId(0, 0);
    ta.recycle();
    return result;
  }

  //  static ColorStateList resolveActionTextColorStateList(
  //      Context context, @AttrRes int colorAttr, ColorStateList fallback) {
  //    TypedArray a = context.getTheme().obtainStyledAttributes(new int[] {colorAttr});
  //    try {
  //      final TypedValue value = a.peekValue(0);
  //      if (value == null) {
  //        return fallback;
  //      }
  //      if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT
  //          && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
  //        return getActionTextStateList(context, value.data);
  //      } else {
  //        final ColorStateList stateList = a.getColorStateList(0);
  //        if (stateList != null) {
  //          return stateList;
  //        } else {
  //          return fallback;
  //        }
  //      }
  //    } finally {
  //      a.recycle();
  //    }
  //  }

  // Get the specified color resource, creating a ColorStateList if the resource
  // points to a color value.
  //  static ColorStateList getActionTextColorStateList(Context context, @ColorRes int colorId) {
  //    final TypedValue value = new TypedValue();
  //    context.getResources().getValue(colorId, value, true);
  //    if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT
  //        && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
  //      return getActionTextStateList(context, value.data);
  //    } else {
  //
  //      if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
  //        //noinspection deprecation
  //        return context.getResources().getColorStateList(colorId);
  //      } else {
  //        return context.getColorStateList(colorId);
  //      }
  //    }
  //  }

  //  static ColorStateList getActionTextStateList(Context context, int newPrimaryColor) {
  //    final int fallBackButtonColor = resolveColor(context, android.R.attr.textColorPrimary);
  //    if (newPrimaryColor == 0) {
  //      newPrimaryColor = fallBackButtonColor;
  //    }
  //    int[][] states =
  //        new int[][] {
  //          new int[] {-android.R.attr.state_enabled}, // disabled
  //          new int[] {} // enabled
  //        };
  //    int[] colors = new int[] {adjustAlpha(newPrimaryColor, 0.4f), newPrimaryColor};
  //    return new ColorStateList(states, colors);
  //  }

  @ColorInt
  static int adjustAlpha(
      @ColorInt int color, @SuppressWarnings("SameParameterValue") float factor) {
    int alpha = Math.round(Color.alpha(color) * factor);
    int red = Color.red(color);
    int green = Color.green(color);
    int blue = Color.blue(color);
    return Color.argb(alpha, red, green, blue);
  }

  static void setOverflowButtonColor(@NonNull final Toolbar toolbar, final @ColorInt int color) {
    @SuppressLint("PrivateResource")
    final String overflowDescription =
        toolbar.getResources().getString(R.string.abc_action_menu_overflow_description);
    final ArrayList<View> outViews = new ArrayList<>();
    toolbar.findViewsWithText(
        outViews, overflowDescription, View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
    if (outViews.isEmpty()) {
      return;
    }
    final AppCompatImageView overflow = (AppCompatImageView) outViews.get(0);
    overflow.setImageDrawable(TintHelper.createTintedDrawable(overflow.getDrawable(), color));
  }

  @ColorInt
  static int shiftColor(@ColorInt int color, @FloatRange(from = 0.0f, to = 2.0f) float by) {
    if (by == 1f) return color;
    float[] hsv = new float[3];
    Color.colorToHSV(color, hsv);
    hsv[2] *= by; // value component
    return Color.HSVToColor(hsv);
  }

  @ColorInt
  static int darkenColor(@ColorInt int color) {
    return shiftColor(color, 0.9f);
  }

  public static boolean isColorLight(@ColorInt int color) {
    if (color == Color.BLACK) {
      return false;
    } else if (color == Color.WHITE || color == Color.TRANSPARENT) {
      return true;
    }
    final double darkness =
        1
            - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color))
                / 255;
    return darkness < 0.4;
  }

  // optional convenience method, this can be called when we have information about the background color and want to consider it
  static boolean isColorLight(@ColorInt int color, @ColorInt int bgColor) {
    if (Color.alpha(color)
        < 128) { // if the color is less than 50% visible rely on the background color
      return isColorLight(
          bgColor); // one could use some kind of color mixing here before passing the color
    }
    return isColorLight(color);
  }

  //  @ColorInt
  //  static int invertColor(@ColorInt int color) {
  //    final int r = 255 - Color.red(color);
  //    final int g = 255 - Color.green(color);
  //    final int b = 255 - Color.blue(color);
  //    return Color.argb(Color.alpha(color), r, g, b);
  //  }

  static void setLightStatusBarCompat(@NonNull AppCompatActivity activity, boolean lightMode) {
    final View view = activity.getWindow().getDecorView();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      int flags = view.getSystemUiVisibility();
      if (lightMode) {
        flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
      } else {
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
      }
      view.setSystemUiVisibility(flags);
    }
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  static void setTaskDescriptionColor(@NonNull Activity activity, @ColorInt int color) {
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
      return;
    }
    // Task description requires fully opaque color
    color = stripAlpha(color);
    // Default is app's launcher icon
    Bitmap icon =
        ((BitmapDrawable) activity.getApplicationInfo().loadIcon(activity.getPackageManager()))
            .getBitmap();
    // Sets color of entry in the system recents page
    ActivityManager.TaskDescription td =
        new ActivityManager.TaskDescription((String) activity.getTitle(), icon, color);
    activity.setTaskDescription(td);
  }

  //  @Nullable
  //  static Toolbar getSupportActionBarView(@Nullable ActionBar ab) {
  //    if (ab == null) return null;
  //    try {
  //      Field field = ab.getClass().getDeclaredField("mDecorToolbar");
  //      field.setAccessible(true);
  //      ToolbarWidgetWrapper wrapper = (ToolbarWidgetWrapper) field.get(ab);
  //      field = ToolbarWidgetWrapper.class.getDeclaredField("mToolbar");
  //      field.setAccessible(true);
  //      return (Toolbar) field.get(wrapper);
  //    } catch (Throwable t) {
  //      Log.d("Aesthetic", "Unable to get Toolbar from " + ab.getClass().getName());
  //      return null;
  //    }
  //  }

  @NonNull
  static ViewGroup getRootView(@NonNull Activity activity) {
    return (ViewGroup) ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
  }
}
