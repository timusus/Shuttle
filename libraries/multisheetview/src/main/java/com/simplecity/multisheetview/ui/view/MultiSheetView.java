package com.simplecity.multisheetview.ui.view;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.View;

import com.simplecity.multisheetview.R;
import com.simplecity.multisheetview.ui.behavior.CustomBottomSheetBehavior;

public class MultiSheetView extends CoordinatorLayout {

    private static final String TAG = "MultiSheetView";

    public interface SheetStateChangeListener {
        void onSheetStateChanged(@Sheet int sheet, @BottomSheetBehavior.State int state);

        void onSlide(@Sheet int sheet, float slideOffset);
    }

    public @interface Sheet {
        int NONE = 0;
        int FIRST = 1;
        int SECOND = 2;
    }

    private CustomBottomSheetBehavior bottomSheetBehavior1;
    private CustomBottomSheetBehavior bottomSheetBehavior2;

    @Nullable
    private SheetStateChangeListener sheetStateChangeListener;

    public MultiSheetView(Context context) {
        this(context, null);
    }

    public MultiSheetView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MultiSheetView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        inflate(getContext(), R.layout.multi_sheet, this);

        View sheet1 = findViewById(R.id.sheet1);
        bottomSheetBehavior1 = (CustomBottomSheetBehavior) BottomSheetBehavior.from(sheet1);
        bottomSheetBehavior1.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                fadeView(Sheet.FIRST, newState);

                if (sheetStateChangeListener != null) {
                    sheetStateChangeListener.onSheetStateChanged(Sheet.FIRST, newState);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                fadeView(findViewById(getSheetPeekViewResId(Sheet.FIRST)), slideOffset);

                if (sheetStateChangeListener != null) {
                    sheetStateChangeListener.onSlide(Sheet.FIRST, slideOffset);
                }
            }
        });

        View sheet2 = findViewById(R.id.sheet2);
        bottomSheetBehavior2 = (CustomBottomSheetBehavior) BottomSheetBehavior.from(sheet2);
        bottomSheetBehavior2.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED || newState == BottomSheetBehavior.STATE_DRAGGING) {
                    bottomSheetBehavior1.setAllowDragging(false);
                } else {
                    bottomSheetBehavior1.setAllowDragging(true);
                }

                fadeView(Sheet.SECOND, newState);

                if (sheetStateChangeListener != null) {
                    sheetStateChangeListener.onSheetStateChanged(Sheet.SECOND, newState);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                bottomSheetBehavior1.setAllowDragging(false);
                fadeView(findViewById(getSheetPeekViewResId(Sheet.SECOND)), slideOffset);

                if (sheetStateChangeListener != null) {
                    sheetStateChangeListener.onSlide(Sheet.SECOND, slideOffset);
                }
            }
        });

        //First sheet view click listener
        findViewById(getSheetPeekViewResId(Sheet.FIRST)).setOnClickListener(v ->
                expandSheet(Sheet.FIRST));

        //Second sheet view click listener
        findViewById(getSheetPeekViewResId(Sheet.SECOND)).setOnClickListener(v ->
                expandSheet(Sheet.SECOND));

        findViewById(getSheetPeekViewResId(Sheet.SECOND)).setOnTouchListener((v, event) -> {
            bottomSheetBehavior1.setAllowDragging(false);
            bottomSheetBehavior2.setAllowDragging(true);
            return false;
        });
    }

    public void setSheetStateChangeListener(@Nullable SheetStateChangeListener sheetStateChangeListener) {
        this.sheetStateChangeListener = sheetStateChangeListener;
    }

    public void expandSheet(@Sheet int sheet) {
        switch (sheet) {
            case Sheet.FIRST:
                bottomSheetBehavior1.setState(BottomSheetBehavior.STATE_EXPANDED);
                break;
            case Sheet.SECOND:
                bottomSheetBehavior2.setState(BottomSheetBehavior.STATE_EXPANDED);
                break;
        }
    }

    public void collapseSheet(@Sheet int sheet) {
        switch (sheet) {
            case Sheet.FIRST:
                bottomSheetBehavior1.setState(BottomSheetBehavior.STATE_COLLAPSED);
                break;
            case Sheet.SECOND:
                bottomSheetBehavior2.setState(BottomSheetBehavior.STATE_COLLAPSED);
                break;
        }
    }

    public boolean isHidden() {
        int peekHeight = getContext().getResources().getDimensionPixelSize(R.dimen.bottom_sheet_peek_1_height);
        return bottomSheetBehavior1.getPeekHeight() < peekHeight;
    }

    /**
     * Sets the peek height of sheet one to 0.
     *
     * @param collapse true if all expanded sheets should be collapsed.
     * @param animate  true if the change in peek height should be animated
     */
    public void hide(boolean collapse, boolean animate) {
        if (!isHidden()) {
            int peekHeight = getContext().getResources().getDimensionPixelSize(R.dimen.bottom_sheet_peek_1_height);
            if (animate) {
                ValueAnimator valueAnimator = ValueAnimator.ofInt(peekHeight, 0);
                valueAnimator.setDuration(200);
                valueAnimator.addUpdateListener(valueAnimator1 -> bottomSheetBehavior1.setPeekHeight((Integer) valueAnimator1.getAnimatedValue()));
                valueAnimator.start();
            } else {
                bottomSheetBehavior1.setPeekHeight(0);
            }
            ((LayoutParams) findViewById(getMainContainerResId()).getLayoutParams()).bottomMargin = 0;
            if (collapse) {
                goToSheet(Sheet.NONE);
            }
        }
    }

    /**
     * Restores the peek height to its default value.
     *
     * @param animate true if the change in peek height should be animated
     */
    public void unhide(boolean animate) {
        if (isHidden()) {
            int peekHeight = getContext().getResources().getDimensionPixelSize(R.dimen.bottom_sheet_peek_1_height);
            int currentHeight = bottomSheetBehavior1.getPeekHeight();
            float ratio = 1 - (currentHeight / peekHeight);
            if (animate) {
                ValueAnimator valueAnimator = ValueAnimator.ofInt(bottomSheetBehavior1.getPeekHeight(), peekHeight);
                valueAnimator.setDuration((long) (200 * ratio));
                valueAnimator.addUpdateListener(valueAnimator1 -> bottomSheetBehavior1.setPeekHeight((Integer) valueAnimator1.getAnimatedValue()));
                valueAnimator.start();
            } else {
                bottomSheetBehavior1.setPeekHeight(peekHeight);
            }
            ((LayoutParams) findViewById(getMainContainerResId()).getLayoutParams()).bottomMargin = peekHeight;
        }
    }

    /**
     * Expand the passed in sheet, collapsing/expanding the other sheet(s) as required.
     */
    public void goToSheet(@Sheet int sheet) {
        switch (sheet) {
            case Sheet.NONE:
                collapseSheet(Sheet.FIRST);
                collapseSheet(Sheet.SECOND);
                break;
            case Sheet.FIRST:
                collapseSheet(Sheet.SECOND);
                expandSheet(Sheet.FIRST);
                break;
            case Sheet.SECOND:
                expandSheet(Sheet.FIRST);
                expandSheet(Sheet.SECOND);
                break;
        }
    }

    /**
     * @return the currently expanded Sheet
     */
    @Sheet
    public int getCurrentSheet() {
        if (bottomSheetBehavior2.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            return Sheet.SECOND;
        } else if (bottomSheetBehavior1.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            return Sheet.FIRST;
        } else {
            return Sheet.NONE;
        }
    }

    public void restoreSheet(@Sheet int sheet) {
        goToSheet(sheet);
        fadeView(Sheet.FIRST, bottomSheetBehavior1.getState());
        fadeView(Sheet.SECOND, bottomSheetBehavior2.getState());
    }

    public boolean consumeBackPress() {
        switch (getCurrentSheet()) {
            case Sheet.SECOND:
                collapseSheet(Sheet.SECOND);
                return true;
            case Sheet.FIRST:
                collapseSheet(Sheet.FIRST);
                return true;
        }
        return false;
    }

    @IdRes
    public int getMainContainerResId() {
        return R.id.mainContainer;
    }

    @SuppressLint("DefaultLocale")
    @IdRes
    public int getSheetContainerViewResId(@Sheet int sheet) {
        switch (sheet) {
            case Sheet.FIRST:
                return R.id.sheet1Container;
            case Sheet.SECOND:
                return R.id.sheet2Container;
        }

        throw new IllegalStateException(String.format("No container view resId found for sheet: %d", sheet));
    }

    @SuppressLint("DefaultLocale")
    @IdRes
    public int getSheetPeekViewResId(@Sheet int sheet) {
        switch (sheet) {
            case Sheet.FIRST:
                return R.id.sheet1PeekView;
            case Sheet.SECOND:
                return R.id.sheet2PeekView;
        }

        throw new IllegalStateException(String.format("No peek view resId found for sheet: %d", sheet));
    }

    private void fadeView(@Sheet int sheet, @BottomSheetBehavior.State int state) {
        if (state == BottomSheetBehavior.STATE_EXPANDED) {
            fadeView(findViewById(getSheetPeekViewResId(sheet)), 1f);
        } else if (state == BottomSheetBehavior.STATE_COLLAPSED) {
            fadeView(findViewById(getSheetPeekViewResId(sheet)), 0f);
        }
    }

    private void fadeView(View v, float offset) {
        float alpha = 1 - offset;
        v.setAlpha(alpha);
        v.setVisibility(alpha == 0 ? View.GONE : View.VISIBLE);
    }

    /**
     * A helper method to return the first MultiSheetView parent of the passed in View,
     * or null if none can be found.
     *
     * @param v the view whose hierarchy will be traversed.
     * @return the first MultiSheetView of the passed in view, or null if none can be found.
     */
    @Nullable
    public static MultiSheetView getParentMultiSheetView(@Nullable View v) {
        if (v == null) return null;

        if (v instanceof MultiSheetView) {
            return (MultiSheetView) v;
        }

        if (v.getParent() instanceof View) {
            return getParentMultiSheetView((View) v.getParent());
        }

        return null;
    }
}