package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.view.View;
import com.afollestad.aesthetic.AestheticToolbar;

public class ContextualToolbar extends AestheticToolbar {

    public ContextualToolbar(Context context) {
        super(context);
    }

    public ContextualToolbar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ContextualToolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void show() {
        setVisibility(View.VISIBLE);
    }

    public void hide() {
        setVisibility(View.GONE);
    }

    /**
     * Traverses the fragment hierarchy, searching for an instance of {@link ContextualToolbarHost}
     *
     * @return {@link ContextualToolbar} or null if none can be found in the fragment hierarchy.
     */
    @Nullable
    public static ContextualToolbar findContextualToolbar(Fragment fragment) {
        if (fragment instanceof ContextualToolbarHost) {
            return ((ContextualToolbarHost) fragment).getContextualToolbar();
        } else {
            Fragment parentFragment = fragment.getParentFragment();
            if (parentFragment != null) {
                return findContextualToolbar(parentFragment);
            }
        }
        return null;
    }
}