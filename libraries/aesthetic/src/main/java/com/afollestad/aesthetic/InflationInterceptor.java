package com.afollestad.aesthetic;

import android.content.Context;
import android.support.annotation.RestrictTo;
import android.support.v4.view.LayoutInflaterFactory;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static com.afollestad.aesthetic.Util.resolveResId;

/** @author Aidan Follestad (afollestad) */
@RestrictTo(LIBRARY_GROUP)
final class InflationInterceptor implements LayoutInflaterFactory {

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
          view = null;
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

    if (view != null && view.getTag() != null && ":aesthetic_ignore".equals(view.getTag())) {
      // Set view back to null so we can let AppCompat handle this view instead.
      view = null;
    }

    return view;
  }
}
