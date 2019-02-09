package com.simplecity.amp_library.ui.screens.drawer;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.bignerdranch.expandablerecyclerview.ExpandableRecyclerAdapter;
import com.bignerdranch.expandablerecyclerview.ParentViewHolder;
import com.bignerdranch.expandablerecyclerview.model.Parent;
import com.simplecity.amp_library.R;
import java.util.List;

public class DrawerAdapter extends ExpandableRecyclerAdapter<Parent<DrawerChild>, DrawerChild, ParentViewHolder<Parent<DrawerChild>, DrawerChild>, DrawerChild.ChildHolder> {

    public DrawerAdapter(@NonNull List<Parent<DrawerChild>> parentList) {
        super(parentList);
    }

    static final int TYPE_DIVIDER = 3;

    @Override
    public int getParentViewType(int parentPosition) {

        if (getParentList().get(parentPosition) instanceof DrawerDivider) {
            return TYPE_DIVIDER;
        }

        return super.getParentViewType(parentPosition);
    }

    @Override
    public boolean isParentViewType(int viewType) {
        return super.isParentViewType(viewType) || viewType == TYPE_DIVIDER;
    }

    @NonNull
    @Override
    public ParentViewHolder onCreateParentViewHolder(@NonNull ViewGroup parentViewGroup, int viewType) {
        switch (viewType) {
            case TYPE_DIVIDER:
                return new DrawerDivider.DividerHolder(LayoutInflater.from(parentViewGroup.getContext()).inflate(R.layout.list_item_drawer_divider, parentViewGroup, false));
            case TYPE_PARENT:
                return new DrawerParent.ParentHolder(LayoutInflater.from(parentViewGroup.getContext()).inflate(R.layout.list_item_drawer, parentViewGroup, false));
        }
        throw new IllegalStateException("onCreateParentViewHolder failed to return holder for type: " + viewType);
    }

    @NonNull
    @Override
    public DrawerChild.ChildHolder onCreateChildViewHolder(@NonNull ViewGroup childViewGroup, int viewType) {
        return new DrawerChild.ChildHolder(LayoutInflater.from(childViewGroup.getContext()).inflate(R.layout.list_item_drawer, childViewGroup, false));
    }

    @Override
    public void onBindParentViewHolder(@NonNull ParentViewHolder<Parent<DrawerChild>, DrawerChild> parentViewHolder, int parentPosition, @NonNull Parent<DrawerChild> parent) {
        switch (getParentViewType(parentPosition)) {
            case TYPE_DIVIDER:
                ((DrawerDivider) getParentList().get(parentPosition)).bindView();
                break;
            case TYPE_PARENT:
                ((DrawerParent) getParentList().get(parentPosition)).bindView((DrawerParent.ParentHolder) parentViewHolder);
                break;
        }
    }

    @Override
    public void onBindChildViewHolder(@NonNull DrawerChild.ChildHolder childViewHolder, int parentPosition, int childPosition, @NonNull DrawerChild drawerChild) {
        List<Parent<DrawerChild>> parentList = getParentList();
        if (parentPosition >= 0 && !parentList.isEmpty() && parentPosition < parentList.size()) {
            List<DrawerChild> childList = parentList.get(parentPosition).getChildList();
            if (childPosition >= 0 && !childList.isEmpty() && childPosition < childList.size()) {
                childList.get(childPosition).bindView(childViewHolder);
            }
        }
    }
}