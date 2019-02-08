package com.afollestad.aesthetic;

import android.content.Context;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

import static com.afollestad.aesthetic.Rx.onErrorLogAndRethrow;
import static com.afollestad.aesthetic.Util.resolveResId;

/** @author Aidan Follestad (afollestad) */
public class AestheticSeekBar extends AppCompatSeekBar {

  @Nullable
  private Disposable subscription;

  private int backgroundResId;

  public AestheticSeekBar(Context context) {
    super(context);
  }

  public AestheticSeekBar(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context, attrs);
  }

  public AestheticSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context, attrs);
  }

  private void init(Context context, AttributeSet attrs) {
    if (attrs != null) {
      backgroundResId = resolveResId(context, attrs, android.R.attr.background);
    }
  }

  public void invalidateColors(ColorIsDarkState state) {
    TintHelper.setTint(this, state.color(), state.isDark());
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();

    if(isInEditMode()) {
      return;
    }

    //noinspection ConstantConditions
    if(!":aesthetic_ignore".equals(getTag())){
      subscription =
              Observable.combineLatest(
                      ViewUtil.getObservableForResId(getContext(), backgroundResId, Aesthetic.get(getContext()).colorAccent()),
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
  }

  @Override
  protected void onDetachedFromWindow() {
    if (subscription!=null) {
        subscription.dispose();
    }
    super.onDetachedFromWindow();
  }
}
