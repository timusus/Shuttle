package com.afollestad.aesthetic;

import static com.afollestad.aesthetic.Rx.onErrorLogAndRethrow;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/** @author Aidan Follestad (afollestad) */
public class AestheticRecyclerView extends RecyclerView {

  private Disposable subscription;

  public AestheticRecyclerView(Context context) {
    super(context);
  }

  public AestheticRecyclerView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public AestheticRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  private void invalidateColors(int color) {
    EdgeGlowUtil.setEdgeGlowColor(this, color, null);
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
