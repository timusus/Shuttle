//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.simplecity.amp_library.ui.recyclerview;

import android.support.v7.widget.RebindReportingHolder;
import android.view.View;

import com.bignerdranch.android.multiselector.MultiSelector;
import com.bignerdranch.android.multiselector.SelectableHolder;

/**
 * A copy of {@link com.bignerdranch.android.multiselector.MultiSelectorBindingHolder}
 * which allows for a null multiselector.
 */
public abstract class CustomMultiSelectorBindingHolder extends RebindReportingHolder implements SelectableHolder {

    private final MultiSelector mMultiSelector;

    public CustomMultiSelectorBindingHolder(View itemView, MultiSelector multiSelector) {
        super(itemView);
        this.mMultiSelector = multiSelector;
    }

    protected void onRebind() {
        if (mMultiSelector != null) {
            this.mMultiSelector.bindHolder(this, this.getAdapterPosition(), this.getItemId());
        }
    }
}
