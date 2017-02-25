package com.simplecity.amp_library.model;

public class CategoryItem {

    public String title;
    public boolean checked;

    /**
     * Sets the checked state of this category
     *
     * @param checked true if this category should be checked, false otherwise
     */
    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    /**
     * Checks whether this CategoryItem is checked
     *
     * @return true if this CategoryItem is checked
     */
    public boolean isChecked() {
        return checked;
    }

    /**
     * Returns a new {@link com.simplecity.amp_library.model.CategoryItem}
     *
     * @param title     the title of this category
     * @param isChecked whether this category is checked or not
     */
    public CategoryItem(String title, boolean isChecked) {
        this.title = title;
        this.checked = isChecked;
    }

}
