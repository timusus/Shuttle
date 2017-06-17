package test.com.multisheetview.ui.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import com.sothree.slidinguppanel.ScrollableViewHelper;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import test.com.multisheetview.R;

public class MultiSheetView extends FrameLayout {

    public interface SheetStateChangeListener {
        void onSheetStateChanged(@Sheet int sheet, SlidingUpPanelLayout.PanelState state);
    }

    public @interface Sheet {
        int NONE = 0;
        int FIRST = 1;
        int SECOND = 2;
    }

    private static final String TAG = "MultiSheetView";

    private SlidingUpPanelLayout panel1Layout;
    private SlidingUpPanelLayout panel2Layout;

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

        View.inflate(context, R.layout.multi_sheet, this);

        panel1Layout = (SlidingUpPanelLayout) findViewById(R.id.sheet1);
        panel1Layout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelStateChanged(View view, SlidingUpPanelLayout.PanelState oldState, SlidingUpPanelLayout.PanelState newState) {
                if (sheetStateChangeListener != null && oldState != newState) {
                    sheetStateChangeListener.onSheetStateChanged(Sheet.FIRST, newState);
                }
            }

            @Override
            public void onPanelSlide(View view, float offset) {
                fadeView(findViewById(getSheetPeekViewResId(Sheet.FIRST)), offset);
            }
        });

        panel2Layout = (SlidingUpPanelLayout) findViewById(R.id.sheet2);
        panel2Layout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelStateChanged(View view, SlidingUpPanelLayout.PanelState oldState, SlidingUpPanelLayout.PanelState newState) {
                if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                    panel1Layout.setTouchEnabled(true);
                }
                if (sheetStateChangeListener != null && oldState != newState) {
                    sheetStateChangeListener.onSheetStateChanged(Sheet.SECOND, newState);
                }
            }

            @Override
            public void onPanelSlide(View view, float offset) {
                panel1Layout.setTouchEnabled(false);
                fadeView(findViewById(getSheetPeekViewResId(Sheet.SECOND)), offset);
            }
        });

        findViewById(getSheetPeekViewResId(Sheet.FIRST)).setOnClickListener(v -> expandSheet(Sheet.FIRST));
    }

    public void setSheetStateChangeListener(@Nullable SheetStateChangeListener sheetStateChangeListener) {
        this.sheetStateChangeListener = sheetStateChangeListener;
    }

    public void expandSheet(@Sheet int sheet) {
        switch (sheet) {
            case Sheet.FIRST:
                panel1Layout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                break;
            case Sheet.SECOND:
                panel2Layout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                break;
        }
    }

    public void collapseSheet(@Sheet int sheet) {
        switch (sheet) {
            case Sheet.FIRST:
                panel1Layout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                break;
            case Sheet.SECOND:
                panel2Layout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                break;
        }
    }

    public boolean isHidden(@Sheet int sheet) {
        switch (sheet) {
            case Sheet.FIRST:
                return panel1Layout.getPanelState() == SlidingUpPanelLayout.PanelState.HIDDEN;
            case Sheet.SECOND:
                return panel2Layout.getPanelState() == SlidingUpPanelLayout.PanelState.HIDDEN;
        }
        return true;
    }

    public void hideSheet(@Sheet int sheet) {
        switch (sheet) {
            case Sheet.FIRST:
                panel1Layout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
                break;
            case Sheet.SECOND:
                panel2Layout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
                break;
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
        if (panel2Layout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
            return Sheet.SECOND;
        } else if (panel1Layout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
            return Sheet.FIRST;
        } else {
            return Sheet.NONE;
        }
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

    void fadeView(View v, float offset) {
        float alpha = 1 - offset;
        v.setAlpha(alpha);
        v.setVisibility(alpha == 0 ? View.GONE : View.VISIBLE);
    }

    /**
     * A helper method to return the first SlidingUpPanelLayout parent of the passed in View,
     * or null if none can be found.
     *
     * @param v the view whose hierarchy will be traversed.
     * @return the first SlidingUpPanelLayout of the passed in view, or null if none can be found.
     */
    @Nullable
    private static SlidingUpPanelLayout getParentSlidingUpPanelLayout(@Nullable View v) {
        if (v == null) return null;

        if (v instanceof SlidingUpPanelLayout) {
            return (SlidingUpPanelLayout) v;
        }

        if (v.getParent() instanceof View) {
            return getParentSlidingUpPanelLayout((View) v.getParent());
        }

        return null;
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

    /**
     * A helper method to set the passed in View as the 'scrollable view' for the parent SlidingUpPanelLayout.
     *
     * @param rootView       the View whose hierarchy will be traversed to find the SlidingUpPanelLayout
     * @param scrollableView the View to be set as the scrollable view.
     */
    public static void setScrollableView(@NonNull View rootView, @Nullable View scrollableView) {
        SlidingUpPanelLayout slidingUpPanelLayout = getParentSlidingUpPanelLayout(rootView);
        if (slidingUpPanelLayout != null) {
            slidingUpPanelLayout.setScrollableView(scrollableView);
        } else {
            Log.e(TAG, "setScrollableView failed. No parent SlidingUpPanelLayout found");
        }
    }

    /**
     * A helper method to set the passed in View as the 'scrollable view' for the parent SlidingUpPanelLayout.
     *
     * @param rootView             the View whose hierarchy will be traversed to find the SlidingUpPanelLayout
     * @param scrollableViewHelper the ScrollableViewHelper to be set
     */
    public static void setScrollableViewHelper(View rootView, @Nullable ScrollableViewHelper scrollableViewHelper) {
        SlidingUpPanelLayout slidingUpPanelLayout = getParentSlidingUpPanelLayout(rootView);
        if (slidingUpPanelLayout != null) {
            slidingUpPanelLayout.setScrollableViewHelper(scrollableViewHelper);
        } else {
            Log.e(TAG, "setScrollableViewHelper failed. No parent SlidingUpPanelLayout found");
        }
    }

}