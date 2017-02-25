package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * A class that represents a breadcrumb item
 */
public class BreadcrumbItem extends TextView {

    private String mItemPath;

    /**
     * Constructor of <code>BreadcrumbItem</code>
     *
     * @param context
     */
    public BreadcrumbItem(Context context) {
        super(context);
    }

    /**
     * Constructor of <code>BreadcrumbItem</code>
     *
     * @param context
     * @param attrs   The attributes of the XML tag that is inflating the view
     */
    public BreadcrumbItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Constructor of <code>BreadcrumbItem</code>
     *
     * @param context  The current context
     * @param attrs    The attributes of the XML tag that is inflating the view.
     * @param defStyle The default style to apply to this view. If 0, no style
     *                 will be applied (beyond what is included in the theme). This may
     *                 either be an attribute resource, whose value will be retrieved
     *                 from the current theme, or an explicit style resource.
     */
    public BreadcrumbItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Returns the item path associated with with this breadcrumb item
     *
     * @return String The item path associated
     */
    public String getItemPath() {
        return this.mItemPath;
    }

    /**
     * Sets the item path associated with with this breadcrumb item
     *
     * @param itemPath The item path
     */
    protected void setItemPath(String itemPath) {
        this.mItemPath = itemPath;
    }

}
