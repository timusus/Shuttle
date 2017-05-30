package com.afollestad.aesthetic;

import android.content.Context;
import android.content.res.ColorStateList;
import android.support.annotation.ColorInt;
import android.support.design.widget.TabLayout;
import android.util.AttributeSet;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

import static com.afollestad.aesthetic.Rx.onErrorLogAndRethrow;
import static com.afollestad.aesthetic.TintHelper.createTintedDrawable;
import static com.afollestad.aesthetic.Util.adjustAlpha;

/** @author Aidan Follestad (afollestad) */
public class AestheticTabLayout extends TabLayout {

  private static final float UNFOCUSED_ALPHA = 0.5f;
  private Disposable indicatorModeSubscription;
  private Disposable bgModeSubscription;
  private Disposable indicatorColorSubscription;
  private Disposable bgColorSubscription;

  public AestheticTabLayout(Context context) {
    super(context);
  }

  public AestheticTabLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public AestheticTabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  private void setIconsColor(int color) {
    final ColorStateList sl =
        new ColorStateList(
            new int[][] {
              new int[] {-android.R.attr.state_selected}, new int[] {android.R.attr.state_selected}
            },
            new int[] {adjustAlpha(color, UNFOCUSED_ALPHA), color});
    for (int i = 0; i < getTabCount(); i++) {
      final TabLayout.Tab tab = getTabAt(i);
      if (tab != null && tab.getIcon() != null) {
        tab.setIcon(createTintedDrawable(tab.getIcon(), sl));
      }
    }
  }

  @Override
  public void setBackgroundColor(@ColorInt int color) {
    super.setBackgroundColor(color);
    Aesthetic.get()
        .colorIconTitle(Observable.just(color))
        .take(1)
        .subscribe(
            new Consumer<ActiveInactiveColors>() {
              @Override
              public void accept(@NonNull ActiveInactiveColors activeInactiveColors) {
                setIconsColor(activeInactiveColors.activeColor());
                setTabTextColors(
                    adjustAlpha(activeInactiveColors.inactiveColor(), UNFOCUSED_ALPHA),
                    activeInactiveColors.activeColor());
              }
            });
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();

    bgModeSubscription =
        Aesthetic.get()
            .tabLayoutBackgroundMode()
            .compose(Rx.<Integer>distinctToMainThread())
            .subscribe(
                new Consumer<Integer>() {
                  @Override
                  public void accept(@NonNull Integer mode) {
                    if (bgColorSubscription != null) {
                      bgColorSubscription.dispose();
                    }
                    switch (mode) {
                      case TabLayoutIndicatorMode.PRIMARY:
                        bgColorSubscription =
                            Aesthetic.get()
                                .colorPrimary()
                                .compose(Rx.<Integer>distinctToMainThread())
                                .subscribe(
                                    ViewBackgroundAction.create(AestheticTabLayout.this),
                                    onErrorLogAndRethrow());
                        break;
                      case TabLayoutIndicatorMode.ACCENT:
                        bgColorSubscription =
                            Aesthetic.get()
                                .colorAccent()
                                .compose(Rx.<Integer>distinctToMainThread())
                                .subscribe(
                                    ViewBackgroundAction.create(AestheticTabLayout.this),
                                    onErrorLogAndRethrow());
                        break;
                      default:
                        throw new IllegalStateException("Unimplemented bg mode: " + mode);
                    }
                  }
                },
                onErrorLogAndRethrow());

    indicatorModeSubscription =
        Aesthetic.get()
            .tabLayoutIndicatorMode()
            .compose(Rx.<Integer>distinctToMainThread())
            .subscribe(
                new Consumer<Integer>() {
                  @Override
                  public void accept(@NonNull Integer mode) {
                    if (indicatorColorSubscription != null) {
                      indicatorColorSubscription.dispose();
                    }
                    switch (mode) {
                      case TabLayoutIndicatorMode.PRIMARY:
                        indicatorColorSubscription =
                            Aesthetic.get()
                                .colorPrimary()
                                .compose(Rx.<Integer>distinctToMainThread())
                                .subscribe(
                                    new Consumer<Integer>() {
                                      @Override
                                      public void accept(@NonNull Integer color) {
                                        setSelectedTabIndicatorColor(color);
                                      }
                                    },
                                    onErrorLogAndRethrow());
                        break;
                      case TabLayoutIndicatorMode.ACCENT:
                        indicatorColorSubscription =
                            Aesthetic.get()
                                .colorAccent()
                                .compose(Rx.<Integer>distinctToMainThread())
                                .subscribe(
                                    new Consumer<Integer>() {
                                      @Override
                                      public void accept(@NonNull Integer color) {
                                        setSelectedTabIndicatorColor(color);
                                      }
                                    },
                                    onErrorLogAndRethrow());
                        break;
                      default:
                        throw new IllegalStateException("Unimplemented bg mode: " + mode);
                    }
                  }
                },
                onErrorLogAndRethrow());
  }

  @Override
  protected void onDetachedFromWindow() {
    if (bgModeSubscription != null) {
      bgModeSubscription.dispose();
    }
    if (indicatorModeSubscription != null) {
      indicatorModeSubscription.dispose();
    }
    if (bgColorSubscription != null) {
      bgColorSubscription.dispose();
    }
    if (indicatorColorSubscription != null) {
      indicatorColorSubscription.dispose();
    }
    super.onDetachedFromWindow();
  }
}
