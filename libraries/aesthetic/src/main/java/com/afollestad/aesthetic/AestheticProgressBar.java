package com.afollestad.aesthetic;

import static com.afollestad.aesthetic.Rx.onErrorLogAndRethrow;

import android.content.Context;
import android.util.AttributeSet;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

/** @author Aidan Follestad (afollestad) */
public class AestheticProgressBar extends MaterialProgressBar {

  private Disposable subscription;

  public AestheticProgressBar(Context context) {
    super(context);
  }

  public AestheticProgressBar(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public AestheticProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  private void invalidateColors(int color) {
    TintHelper.setTint(this, color);
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    subscription =
        Aesthetic.get(getContext())
            .colorAccent()
            .compose(Rx.<Integer>distinctToMainThread())
            .subscribe(
                new Consumer<Integer>() {
                  @Override
                  public void accept(@NonNull Integer color) {
                    invalidateColors(color);
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
