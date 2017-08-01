package com.afollestad.aesthetic;

import static com.afollestad.aesthetic.Rx.onErrorLogAndRethrow;
import static com.afollestad.aesthetic.Util.resolveResId;

import android.content.Context;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

/** @author Aidan Follestad (afollestad) */
public class AestheticTextView extends AppCompatTextView {

  private Disposable subscription;
  private int textColorResId;

  public AestheticTextView(Context context) {
    super(context);
  }

  public AestheticTextView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context, attrs);
  }

  public AestheticTextView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context, attrs);
  }

  private void init(Context context, AttributeSet attrs) {
    if (attrs != null) {
      textColorResId = resolveResId(context, attrs, android.R.attr.textColor);
    }
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    Observable<Integer> obs =
        ViewUtil.getObservableForResId(
            getContext(), textColorResId, Aesthetic.get(getContext()).textColorSecondary());
    //noinspection ConstantConditions
    subscription =
        obs.compose(Rx.<Integer>distinctToMainThread())
            .subscribe(ViewTextColorAction.create(this), onErrorLogAndRethrow());
  }

  @Override
  protected void onDetachedFromWindow() {
    subscription.dispose();
    super.onDetachedFromWindow();
  }
}
