package com.afollestad.aesthetic;

import android.content.Context;
import androidx.annotation.RestrictTo;
import androidx.core.view.LayoutInflaterFactory;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
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
      case "androidx.appcompat.widget.AppCompatImageView":
        view = new AestheticImageView(context, attrs);
        break;
      case "ImageButton":
      case "androidx.appcompat.widget.AppCompatImageButton":
        view = new AestheticImageButton(context, attrs);
        break;
      case "androidx.drawerlayout.widget.DrawerLayout":
        view = new AestheticDrawerLayout(context, attrs);
        break;
      case "Toolbar":
      case "androidx.appcompat.widget.Toolbar":
        view = new AestheticToolbar(context, attrs);
        break;
      case "androidx.appcompat.widget.AppCompatTextView":
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
      case "androidx.appcompat.widget.AppCompatButton":
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
      case "androidx.appcompat.widget.AppCompatCheckBox":
      case "CheckBox":
        view = new AestheticCheckBox(context, attrs);
        break;
      case "androidx.appcompat.widget.AppCompatRadioButton":
      case "RadioButton":
        view = new AestheticRadioButton(context, attrs);
        break;
      case "androidx.appcompat.widget.AppCompatEditText":
      case "EditText":
        view = new AestheticEditText(context, attrs);
        break;
      case "Switch":
        view = new AestheticSwitch(context, attrs);
        break;
      case "androidx.appcompat.widget.SwitchCompat":
        view = new AestheticSwitchCompat(context, attrs);
        break;
      case "androidx.appcompat.widget.AppCompatSeekBar":
      case "SeekBar":
        view = new AestheticSeekBar(context, attrs);
        break;
      case "ProgressBar":
      case "me.zhanghai.android.materialprogressbar.MaterialProgressBar":
        view = new AestheticProgressBar(context, attrs);
        break;
      case "androidx.appcompat.view.menu.ActionMenuItemView":
        view = new AestheticActionMenuItemView(context, attrs);
        break;

      case "androidx.recyclerview.widget.RecyclerView":
        view = new AestheticRecyclerView(context, attrs);
        break;
      case "androidx.core.widget.NestedScrollView":
        view = new AestheticNestedScrollView(context, attrs);
        break;
      case "ListView":
        view = new AestheticListView(context, attrs);
        break;
      case "ScrollView":
        view = new AestheticScrollView(context, attrs);
        break;
      case "androidx.viewpager.widget.ViewPager":
        view = new AestheticViewPager(context, attrs);
        break;

      case "Spinner":
      case "androidx.appcompat.widget.AppCompatSpinner":
        view = new AestheticSpinner(context, attrs);
        break;

      case "com.google.android.material.textfield.TextInputLayout":
        view = new AestheticTextInputLayout(context, attrs);
        break;
      case "com.google.android.material.textfield.TextInputEditText":
        view = new AestheticTextInputEditText(context, attrs);
        break;

      case "androidx.cardview.widget.CardView":
        view = new AestheticCardView(context, attrs);
        break;
      case "com.google.android.material.tabs.TabLayout":
        view = new AestheticTabLayout(context, attrs);
        break;
      case "com.google.android.material.navigation.NavigationView":
        view = new AestheticNavigationView(context, attrs);
        break;
      case "com.google.android.material.bottomnavigation.BottomNavigationView":
        view = new AestheticBottomNavigationView(context, attrs);
        break;
      case "com.google.android.material.floatingactionbutton.FloatingActionButton":
        view = new AestheticFab(context, attrs);
        break;
      case "androidx.coordinatorlayout.widget.CoordinatorLayout":
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
