package com.afollestad.aesthetic;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;

import static com.afollestad.aesthetic.Rx.onErrorLogAndRethrow;
import static com.afollestad.aesthetic.TintHelper.createTintedDrawable;
import static com.afollestad.aesthetic.Util.setOverflowButtonColor;

/** @author Aidan Follestad (afollestad) */
public class AestheticToolbar extends Toolbar {

  private BgIconColorState lastState;
  private Disposable subscription;
  private PublishSubject<Integer> onColorUpdated;

  private boolean transparentBackground = false;

  public AestheticToolbar(Context context) {
    super(context);
  }

  public AestheticToolbar(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);

    init(context, attrs);
  }

  public AestheticToolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    init(context, attrs);
  }

  private void init(Context context, @Nullable AttributeSet attrs){
    TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AestheticToolbar);
    transparentBackground = a.getBoolean(R.styleable.AestheticToolbar_transparentBackground, false);
    a.recycle();
  }

  private void invalidateColors(BgIconColorState state) {
    lastState = state;
    if (!transparentBackground) {
      setBackgroundColor(state.bgColor());
    }
    setTitleTextColor(state.iconTitleColor().activeColor());
    setSubtitleTextColor(state.iconTitleColor().activeColor());
    setOverflowButtonColor(this, state.iconTitleColor().activeColor());
    if (getNavigationIcon() != null) {
      setNavigationIcon(getNavigationIcon());
    }
    onColorUpdated.onNext(state.bgColor());
    ViewUtil.tintToolbarMenu(this, getMenu(), state.iconTitleColor());
  }

  public Observable<Integer> colorUpdated() {
    return onColorUpdated;
  }

  @Override
  public void setNavigationIcon(@Nullable Drawable icon) {
    if (lastState == null) {
      super.setNavigationIcon(icon);
      return;
    }
    super.setNavigationIcon(createTintedDrawable(icon, lastState.iconTitleColor().toEnabledSl()));
  }

  public void setNavigationIcon(@Nullable Drawable icon, @ColorInt int color) {
    if (lastState == null) {
      super.setNavigationIcon(icon);
      return;
    }
    super.setNavigationIcon(createTintedDrawable(icon, color));
  }

  public void setTransparentBackground(boolean transparentBackground) {
    this.transparentBackground = transparentBackground;
    setBackgroundColor(Color.TRANSPARENT);
  }

  @SuppressWarnings("ConstantConditions")
  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    onColorUpdated = PublishSubject.create();

    // Need to invalidate the colors as early as possible. When subscribing to the continuous observable
    // below (subscription = ...), we're using distinctToMainThread(), which introduces a slight delay. During
    // this delay, we see the original colors, which are then swapped once the emission is consumed.
    // So, we'll just do a take(1), and since we're calling from the main thread, we don't need to worry
    // about distinctToMainThread() for this call. This prevents the 'flickering' of colors.

    Observable.combineLatest(
            Aesthetic.get(getContext()).colorPrimary(),
            Aesthetic.get(getContext()).colorIconTitle(null),
            BgIconColorState.creator())
            .take(1)
            .subscribe(new Consumer<BgIconColorState>() {
              @Override
              public void accept(BgIconColorState bgIconColorState) throws Exception {
                invalidateColors(bgIconColorState);
              }
            });

    subscription =
            Observable.combineLatest(
                    Aesthetic.get(getContext()).colorPrimary(),
                    Aesthetic.get(getContext()).colorIconTitle(null),
                    BgIconColorState.creator())
                    .compose(Rx.<BgIconColorState>distinctToMainThread())
                    .subscribe(
                            new Consumer<BgIconColorState>() {
                              @Override
                              public void accept(@NonNull BgIconColorState bgIconColorState) {
                                invalidateColors(bgIconColorState);
                              }
                            },
                            onErrorLogAndRethrow());
  }

  @Override
  protected void onDetachedFromWindow() {
    lastState = null;
    onColorUpdated = null;
    subscription.dispose();
    super.onDetachedFromWindow();
  }
}
