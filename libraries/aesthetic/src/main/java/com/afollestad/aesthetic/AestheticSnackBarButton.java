package com.afollestad.aesthetic;

import android.content.Context;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;
import io.reactivex.disposables.Disposable;

/** @author Aidan Follestad (afollestad) */
final class AestheticSnackBarButton extends AppCompatButton {

  private Disposable subscription;

  public AestheticSnackBarButton(Context context) {
    super(context);
  }

  public AestheticSnackBarButton(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public AestheticSnackBarButton(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    subscription =
        Aesthetic.get(getContext())
            .snackbarActionTextColor()
            .compose(Rx.<Integer>distinctToMainThread())
            .subscribe(ViewTextColorAction.create(this));
  }

  @Override
  protected void onDetachedFromWindow() {
    subscription.dispose();
    super.onDetachedFromWindow();
  }
}
