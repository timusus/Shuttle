package com.simplecity.amp_library.ui.screens.drawer;

import android.support.annotation.NonNull;
import android.view.View;
import com.bignerdranch.expandablerecyclerview.ParentViewHolder;
import com.bignerdranch.expandablerecyclerview.model.Parent;
import java.util.Collections;
import java.util.List;

public class DrawerDivider implements Parent<DrawerChild> {

    @Override
    public List<DrawerChild> getChildList() {
        return Collections.emptyList();
    }

    @Override
    public boolean isInitiallyExpanded() {
        return false;
    }

    public void bindView() {

    }

    static class DividerHolder extends ParentViewHolder {

        DividerHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}