package com.afollestad.aesthetic;

import android.content.Context;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.AttributeSet;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;

import static com.afollestad.aesthetic.Rx.onErrorLogAndRethrow;
import static com.afollestad.aesthetic.Util.resolveResId;

/** @author Aidan Follestad (afollestad) */
public class AestheticCheckBox extends AppCompatCheckBox {

  private CompositeDisposable subscriptions;
  protected int backgroundResId;

  public AestheticCheckBox(Context context) {
    super(context);
  }

  public AestheticCheckBox(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context, attrs);
  }

  public AestheticCheckBox(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context, attrs);
  }

  private void init(Context context, AttributeSet attrs) {
    if (attrs != null) {
      backgroundResId = resolveResId(context, attrs, android.R.attr.background);
    }
  }

  protected void invalidateColors(ColorIsDarkState state) {
    TintHelper.setTint(this, state.color(), state.isDark());
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    subscriptions = new CompositeDisposable();
    //noinspection ConstantConditions
    subscriptions.add(
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
                onErrorLogAndRethrow()));
    subscriptions.add(
        Aesthetic.get(getContext())
            .textColorPrimary()
            .compose(Rx.<Integer>distinctToMainThread())
            .subscribe(ViewTextColorAction.create(this)));
  }

  @Override
  protected void onDetachedFromWindow() {
    subscriptions.clear();
    super.onDetachedFromWindow();
  }
}
