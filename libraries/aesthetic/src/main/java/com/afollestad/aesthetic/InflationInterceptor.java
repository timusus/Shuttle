package com.afollestad.aesthetic;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static com.afollestad.aesthetic.Util.resolveResId;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.view.LayoutInflaterFactory;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.view.ContextThemeWrapper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import io.reactivex.Observable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** @author Aidan Follestad (afollestad) */
@RestrictTo(LIBRARY_GROUP)
final class InflationInterceptor implements LayoutInflaterFactory {

  private final Method onCreateViewMethod;
  private final Method createViewMethod;
  private final Field constructorArgsField;
  private final AppCompatActivity keyContext;
  @NonNull private final LayoutInflater layoutInflater;
  @Nullable private final AppCompatDelegate delegate;
  private int[] ATTRS_THEME;

  InflationInterceptor(
      @Nullable AppCompatActivity keyContext,
      @NonNull LayoutInflater li,
      @Nullable AppCompatDelegate delegate) {
    this.keyContext = keyContext;
    layoutInflater = li;
    this.delegate = delegate;

    try {
      onCreateViewMethod =
          LayoutInflater.class.getDeclaredMethod(
              "onCreateView", View.class, String.class, AttributeSet.class);
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException("Failed to retrieve the onCreateView method.", e);
    }

    try {
      createViewMethod =
          LayoutInflater.class.getDeclaredMethod(
              "createView", String.class, String.class, AttributeSet.class);
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException("Failed to retrieve the createView method.", e);
    }

    try {
      constructorArgsField = LayoutInflater.class.getDeclaredField("mConstructorArgs");
    } catch (NoSuchFieldException e) {
      throw new IllegalStateException("Failed to retrieve the mConstructorArgs field.", e);
    }

    try {
      final Field attrsThemeField = LayoutInflater.class.getDeclaredField("ATTRS_THEME");
      attrsThemeField.setAccessible(true);
      ATTRS_THEME = (int[]) attrsThemeField.get(null);
    } catch (Throwable t) {
      t.printStackTrace();
      Log.d(
          "InflationInterceptor",
          "Failed to get the value of static field ATTRS_THEME: " + t.getMessage());
    }

    onCreateViewMethod.setAccessible(true);
    createViewMethod.setAccessible(true);
    constructorArgsField.setAccessible(true);
  }

  private static void log(String msg, Object... args) {
    //noinspection PointlessBooleanExpression
    if (args != null) {
      Log.d("InflationInterceptor", String.format(msg, args));
    } else {
      Log.d("InflationInterceptor", msg);
    }
  }

  private boolean isBlackListedForApply(String name) {
    return "android.support.design.internal.NavigationMenuItemView".equals(name)
        || "ViewStub".equals(name)
        || "fragment".equals(name)
        || "include".equals(name);
  }

  @Override
  public View onCreateView(View parent, final String name, Context context, AttributeSet attrs) {
    View view = null;
    final int viewId = resolveResId(context, attrs, android.R.attr.id);

    switch (name) {
      case "ImageView":
      case "android.support.v7.widget.AppCompatImageView":
        view = new AestheticImageView(context, attrs);
        break;
      case "ImageButton":
      case "android.support.v7.widget.AppCompatImageButton":
        view = new AestheticImageButton(context, attrs);
        break;

      case "android.support.v4.widget.DrawerLayout":
        view = new AestheticDrawerLayout(context, attrs);
        break;
      case "Toolbar":
      case "android.support.v7.widget.Toolbar":
        view = new AestheticToolbar(context, attrs);
        break;
      case "android.support.v7.widget.AppCompatTextView":
      case "TextView":
        if (viewId == R.id.snackbar_text) {
          view = new AestheticSnackBarTextView(context, attrs);
        } else {
          view = new AestheticTextView(context, attrs);
          if (parent instanceof LinearLayout && view.getId() == android.R.id.message) {
            // This is for a toast message
            view = null;
          }
        }
        break;
      case "Button":
      case "android.support.v7.widget.AppCompatButton":
        if (viewId == android.R.id.button1
            || viewId == android.R.id.button2
            || viewId == android.R.id.button3) {
          view = new AestheticDialogButton(context, attrs);
        } else if (viewId == R.id.snackbar_action) {
          view = new AestheticSnackBarButton(context, attrs);
        } else {
          view = new AestheticButton(context, attrs);
        }
        break;
      case "android.support.v7.widget.AppCompatCheckBox":
      case "CheckBox":
        view = new AestheticCheckBox(context, attrs);
        break;
      case "android.support.v7.widget.AppCompatRadioButton":
      case "RadioButton":
        view = new AestheticRadioButton(context, attrs);
        break;
      case "android.support.v7.widget.AppCompatEditText":
      case "EditText":
        view = new AestheticEditText(context, attrs);
        break;
      case "Switch":
        view = new AestheticSwitch(context, attrs);
        break;
      case "android.support.v7.widget.SwitchCompat":
        view = new AestheticSwitchCompat(context, attrs);
        break;
      case "android.support.v7.widget.AppCompatSeekBar":
      case "SeekBar":
        view = new AestheticSeekBar(context, attrs);
        break;
      case "ProgressBar":
      case "me.zhanghai.android.materialprogressbar.MaterialProgressBar":
        view = new AestheticProgressBar(context, attrs);
        break;
      case "android.support.v7.view.menu.ActionMenuItemView":
        view = new AestheticActionMenuItemView(context, attrs);
        break;

      case "android.support.v7.widget.RecyclerView":
        view = new AestheticRecyclerView(context, attrs);
        break;
      case "android.support.v4.widget.NestedScrollView":
        view = new AestheticNestedScrollView(context, attrs);
        break;
      case "ListView":
        view = new AestheticListView(context, attrs);
        break;
      case "ScrollView":
        view = new AestheticScrollView(context, attrs);
        break;
      case "android.support.v4.view.ViewPager":
        view = new AestheticViewPager(context, attrs);
        break;

      case "Spinner":
      case "android.support.v7.widget.AppCompatSpinner":
        view = new AestheticSpinner(context, attrs);
        break;

      case "android.support.design.widget.TextInputLayout":
        view = new AestheticTextInputLayout(context, attrs);
        break;
      case "android.support.design.widget.TextInputEditText":
        view = new AestheticTextInputEditText(context, attrs);
        break;

      case "android.support.v7.widget.CardView":
        view = new AestheticCardView(context, attrs);
        break;
      case "android.support.design.widget.TabLayout":
        view = new AestheticTabLayout(context, attrs);
        break;
      case "android.support.design.widget.NavigationView":
        view = new AestheticNavigationView(context, attrs);
        break;
      case "android.support.design.widget.BottomNavigationView":
        view = new AestheticBottomNavigationView(context, attrs);
        break;
      case "android.support.design.widget.FloatingActionButton":
        view = new AestheticFab(context, attrs);
        break;
      case "android.support.design.widget.CoordinatorLayout":
        view = new AestheticCoordinatorLayout(context, attrs);
        break;
    }

    int viewBackgroundRes = 0;

    if (view != null && view.getTag() != null && ":aesthetic_ignore".equals(view.getTag())) {
      // Set view back to null so we can let AppCompat handle this view instead.
      view = null;
    } else if (attrs != null) {
      viewBackgroundRes = resolveResId(context, attrs, android.R.attr.background);
    }

    if (view == null) {
      // First, check if the AppCompatDelegate will give us a view, usually (maybe always) null.
      if (delegate != null) {
        view = delegate.createView(parent, name, context, attrs);
        if (view == null) {
          view = keyContext.onCreateView(parent, name, context, attrs);
        } else {
          view = null;
        }
      } else {
        view = null;
      }

      if (isBlackListedForApply(name)) {
        return view;
      }

      // Mimic code of LayoutInflater using reflection tricks (this would normally be run when this factory returns null).
      // We need to intercept the default behavior rather than allowing the LayoutInflater to handle it after this method returns.
      if (view == null) {
        try {
          Context viewContext = layoutInflater.getContext();
          // Apply a theme wrapper, if requested.
          if (ATTRS_THEME != null) {
            final TypedArray ta = viewContext.obtainStyledAttributes(attrs, ATTRS_THEME);
            final int themeResId = ta.getResourceId(0, 0);
            if (themeResId != 0) {
              //noinspection RestrictedApi
              viewContext = new ContextThemeWrapper(viewContext, themeResId);
            }
            ta.recycle();
          }

          Object[] constructorArgs;
          try {
            constructorArgs = (Object[]) constructorArgsField.get(layoutInflater);
          } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                "Failed to retrieve the mConstructorArgsField field.", e);
          }

          final Object lastContext = constructorArgs[0];
          constructorArgs[0] = viewContext;
          try {
            if (-1 == name.indexOf('.')) {
              view = (View) onCreateViewMethod.invoke(layoutInflater, parent, name, attrs);
            } else {
              view = (View) createViewMethod.invoke(layoutInflater, name, null, attrs);
            }
          } catch (Exception e) {
            log("Failed to inflate %s: %s", name, e.getMessage());
            e.printStackTrace();
          } finally {
            constructorArgs[0] = lastContext;
          }
        } catch (Throwable t) {
          throw new RuntimeException(
              String.format("An error occurred while inflating View %s: %s", name, t.getMessage()),
              t);
        }
      }
    }
    return view;
  }
}
