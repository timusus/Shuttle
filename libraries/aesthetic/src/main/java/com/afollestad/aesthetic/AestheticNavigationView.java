package com.afollestad.aesthetic;

import static com.afollestad.aesthetic.Rx.onErrorLogAndRethrow;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/** @author Aidan Follestad (afollestad) */
@SuppressWarnings("RestrictedApi")
public class AestheticNavigationView extends NavigationView {

  private Disposable modeSubscription;
  private Disposable colorSubscription;

  public AestheticNavigationView(Context context) {
    super(context);
  }

  public AestheticNavigationView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public AestheticNavigationView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  private void invalidateColors(ColorIsDarkState state) {
    int selectedColor = state.color();
    boolean isDark = state.isDark();
    int baseColor = isDark ? Color.WHITE : Color.BLACK;
    int unselectedIconColor = Util.adjustAlpha(baseColor, .54f);
    int unselectedTextColor = Util.adjustAlpha(baseColor, .87f);
    int selectedItemBgColor =
        ContextCompat.getColor(
            getContext(),
            isDark
                ? R.color.ate_navigation_drawer_selected_dark
                : R.color.ate_navigation_drawer_selected_light);

    final ColorStateList iconSl =
        new ColorStateList(
            new int[][] {
              new int[] {-android.R.attr.state_checked}, new int[] {android.R.attr.state_checked}
            },
            new int[] {unselectedIconColor, selectedColor});
    final ColorStateList textSl =
        new ColorStateList(
            new int[][] {
              new int[] {-android.R.attr.state_checked}, new int[] {android.R.attr.state_checked}
            },
            new int[] {unselectedTextColor, selectedColor});
    setItemTextColor(textSl);
    setItemIconTintList(iconSl);

    StateListDrawable bgDrawable = new StateListDrawable();
    bgDrawable.addState(
        new int[] {android.R.attr.state_checked}, new ColorDrawable(selectedItemBgColor));
    setItemBackground(bgDrawable);
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    modeSubscription =
        Aesthetic.get(getContext())
            .navigationViewMode()
            .compose(Rx.<Integer>distinctToMainThread())
            .subscribe(
                new Consumer<Integer>() {
                  @Override
                  public void accept(@NonNull Integer mode) {
                    switch (mode) {
                      case NavigationViewMode.SELECTED_PRIMARY:
                        colorSubscription =
                            Observable.combineLatest(
                                    Aesthetic.get(getContext()).colorPrimary(),
                                    Aesthetic.get(getContext()).isDark(),
                                    ColorIsDarkState.creator())
                                .compose(Rx.<ColorIsDarkState>distinctToMainThread())
                                .subscribe(
                                    new Consumer<ColorIsDarkState>() {
                                      @Override
                                      public void accept(
                                          @NonNull ColorIsDarkState colorIsDarkState) {
                                        invalidateColors(colorIsDarkState);
                                      }
                                    },
                                    onErrorLogAndRethrow());
                        break;
                      case NavigationViewMode.SELECTED_ACCENT:
                        colorSubscription =
                            Observable.combineLatest(
                                    Aesthetic.get(getContext()).colorAccent(),
                                    Aesthetic.get(getContext()).isDark(),
                                    ColorIsDarkState.creator())
                                .compose(Rx.<ColorIsDarkState>distinctToMainThread())
                                .subscribe(
                                    new Consumer<ColorIsDarkState>() {
                                      @Override
                                      public void accept(
                                          @NonNull ColorIsDarkState colorIsDarkState) {
                                        invalidateColors(colorIsDarkState);
                                      }
                                    },
                                    onErrorLogAndRethrow());
                        break;
                      default:
                        throw new IllegalStateException("Unknown nav view mode: " + mode);
                    }
                  }
                },
                onErrorLogAndRethrow());
  }

  @Override
  protected void onDetachedFromWindow() {
    if (modeSubscription != null) {
      modeSubscription.dispose();
    }
    if (colorSubscription != null) {
      colorSubscription.dispose();
    }
    super.onDetachedFromWindow();
  }
}
