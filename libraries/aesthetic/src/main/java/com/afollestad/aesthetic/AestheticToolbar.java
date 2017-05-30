package com.afollestad.aesthetic;

import android.content.Context;
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

  public AestheticToolbar(Context context) {
    super(context);
  }

  public AestheticToolbar(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public AestheticToolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  private void invalidateColors(BgIconColorState state) {
    lastState = state;
    setBackgroundColor(state.bgColor());
    setTitleTextColor(state.iconTitleColor().activeColor());
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

  @SuppressWarnings("ConstantConditions")
  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    onColorUpdated = PublishSubject.create();
    subscription =
        Observable.combineLatest(
                Aesthetic.get().colorPrimary(),
                Aesthetic.get().colorIconTitle(null),
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
