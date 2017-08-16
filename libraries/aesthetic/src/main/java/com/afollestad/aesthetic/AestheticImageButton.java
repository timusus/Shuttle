package com.afollestad.aesthetic;

import static com.afollestad.aesthetic.Rx.onErrorLogAndRethrow;
import static com.afollestad.aesthetic.Util.resolveResId;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageButton;
import android.util.AttributeSet;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

/** @author Aidan Follestad (afollestad) */
public class AestheticImageButton extends AppCompatImageButton {

  private Disposable bgSubscription;
  private int backgroundResId;

  public AestheticImageButton(Context context) {
    super(context);
  }

  public AestheticImageButton(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init(context, attrs);
  }

  public AestheticImageButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context, attrs);
  }

  private void init(Context context, AttributeSet attrs) {
    if (attrs != null) {
      backgroundResId = resolveResId(context, attrs, android.R.attr.background);
    }
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    Observable<Integer> obs = ViewUtil.getObservableForResId(getContext(), backgroundResId, null);
    if (obs != null) {
      bgSubscription =
          obs.compose(Rx.<Integer>distinctToMainThread())
              .subscribe(ViewBackgroundAction.create(this), onErrorLogAndRethrow());
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    if (bgSubscription != null) {
      bgSubscription.dispose();
    }
    super.onDetachedFromWindow();
  }
}
