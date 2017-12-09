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
    Aesthetic.get(getContext())
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

      /* Begin workaround

      In some certain situations, the time between onAttachedToWindow and the color change observables
      emitting is too much, and the view gets displayed momentarily using the original colors.

      On top of that, Aesthetic seems to strip out or otherwise modify view 'theme' attributes,
      so even if this TabLayout's parent Toolbar is using a 'dark' theme, the text for the TabItems still
      comes out black.

      As a workaround, we setup the colors in a blocking fashion, to ensure the correct colors are applied
      when onAttachedToWindow is called.

      Lastly, for some reason, the 'setTabTextColors' has to be called after a delay, or else it doesn't
      seem to take effect. Hard to tell if that's because Aesthetic isn't producing the right colors immediately,
      or if it's something to do with TabLayout.

      It's really not ideal to have blocking RX calls happening on the main thread, but until a proper solution
      is found, this is the best I can do.

      Note: To reproduce this issue, it seems the TabLayout needs to belong to a nested fragment.
     */
      final Integer primaryColor = Aesthetic.get(getContext())
              .colorPrimary()
              .blockingFirst();
      ViewBackgroundAction.create(AestheticTabLayout.this).accept(primaryColor);

      getHandler().postDelayed(new Runnable() {
          @Override
          public void run() {

              ActiveInactiveColors activeInactiveColors = Aesthetic.get(getContext())
                      .colorIconTitle(Observable.just(primaryColor))
                      .blockingFirst();

              setTabTextColors(
                      adjustAlpha(activeInactiveColors.inactiveColor(), UNFOCUSED_ALPHA),
                      activeInactiveColors.activeColor());
          }
      }, 50);
      // End workaround

    bgModeSubscription =
            Aesthetic.get(getContext())
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
                                        Aesthetic.get(getContext())
                                                .colorPrimary()
                                                .compose(Rx.<Integer>distinctToMainThread())
                                                .subscribe(
                                                        ViewBackgroundAction.create(AestheticTabLayout.this),
                                                        onErrorLogAndRethrow());
                        break;
                      case TabLayoutIndicatorMode.ACCENT:
                        bgColorSubscription =
                            Aesthetic.get(getContext())
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
        Aesthetic.get(getContext())
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
                            Aesthetic.get(getContext())
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
                            Aesthetic.get(getContext())
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
