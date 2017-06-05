package com.simplecity.amp_library.ui.modelviews;

public interface SelectableViewModel<T> {

    void setSelected(boolean selected);

    boolean isSelected();

    T getItem();
}