package com.afollestad.aesthetic;

import static com.afollestad.aesthetic.Rx.onErrorLogAndRethrow;
import static com.afollestad.aesthetic.Util.resolveResId;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;
import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/** @author Aidan Follestad (afollestad) */
public class AestheticButton extends AppCompatButton {

  private Disposable subscription;
  private int backgroundResId;

  public AestheticButton(Context context) {
    super(context);
  }

  public AestheticButton(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context, attrs);
  }

  public AestheticButton(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context, attrs);
  }

  private void init(Context context, AttributeSet attrs) {
    if (attrs != null) {
      backgroundResId = resolveResId(context, attrs, android.R.attr.background);
    }
  }

  private void invalidateColors(ColorIsDarkState state) {
    TintHelper.setTintAuto(this, state.color(), true, state.isDark());
    ColorStateList textColorSl =
        new ColorStateList(
            new int[][] {
              new int[] {android.R.attr.state_enabled}, new int[] {-android.R.attr.state_enabled}
            },
            new int[] {
              Util.isColorLight(state.color()) ? Color.BLACK : Color.WHITE,
              state.isDark() ? Color.WHITE : Color.BLACK
            });
    setTextColor(textColorSl);
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    //noinspection ConstantConditions
    subscription =
        Observable.combineLatest(
                ViewUtil.getObservableForResId(
                    getContext(), backgroundResId, Aesthetic.get(getContext()).colorAccent()),
                Aesthetic.get(getContext()).isDark(),
                ColorIsDarkState.creator())
            .compose(Rx.<ColorIsDarkState>distinctToMainThread())
            .subscribe(
                new Consumer<ColorIsDarkState>() {
                  @Override
                  public void accept(@NonNull ColorIsDarkState colorIsDarkState) {
                    invalidateColors(colorIsDarkState);
                  }
                },
                onErrorLogAndRethrow());
  }

  @Override
  protected void onDetachedFromWindow() {
    subscription.dispose();
    super.onDetachedFromWindow();
  }
}
