package com.afollestad.aesthetic;

import static com.afollestad.aesthetic.Rx.onErrorLogAndRethrow;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;
import android.util.AttributeSet;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/** @author Aidan Follestad (afollestad) */
public class AestheticDrawerLayout extends DrawerLayout {

  private ActiveInactiveColors lastState;
  private DrawerArrowDrawable arrowDrawable;
  private Disposable subscription;

  public AestheticDrawerLayout(Context context) {
    super(context);
  }

  public AestheticDrawerLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public AestheticDrawerLayout(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  private void invalidateColor(ActiveInactiveColors colors) {
    if (colors == null) {
      return;
    }
    this.lastState = colors;
    if (this.arrowDrawable != null) {
      this.arrowDrawable.setColor(lastState.activeColor());
    }
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    subscription =
        Aesthetic.get(getContext())
            .colorIconTitle(null)
            .compose(Rx.<ActiveInactiveColors>distinctToMainThread())
            .subscribe(
                new Consumer<ActiveInactiveColors>() {
                  @Override
                  public void accept(
                      @io.reactivex.annotations.NonNull ActiveInactiveColors colors) {
                    invalidateColor(colors);
                  }
                },
                onErrorLogAndRethrow());
  }

  @Override
  protected void onDetachedFromWindow() {
    subscription.dispose();
    super.onDetachedFromWindow();
  }

  @Override
  public void addDrawerListener(@NonNull DrawerListener listener) {
    super.addDrawerListener(listener);
    if (listener instanceof ActionBarDrawerToggle) {
      this.arrowDrawable = ((ActionBarDrawerToggle) listener).getDrawerArrowDrawable();
    }
    invalidateColor(lastState);
  }

  @SuppressWarnings("deprecation")
  @Override
  public void setDrawerListener(DrawerListener listener) {
    super.setDrawerListener(listener);
    if (listener instanceof ActionBarDrawerToggle) {
      this.arrowDrawable = ((ActionBarDrawerToggle) listener).getDrawerArrowDrawable();
    }
    invalidateColor(lastState);
  }
}
