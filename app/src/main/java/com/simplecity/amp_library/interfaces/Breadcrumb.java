package com.simplecity.amp_library.interfaces;

/**
 * An interface that defines the breadcrumb operations
 */
public interface Breadcrumb {

    /**
     * Changes the path of the breadcrumb
     *
     * @param newPath The new path
     */
    void changeBreadcrumbPath(final String newPath);

    /**
     * Adds a new breadcrumb listener.
     *
     * @param listener The breadcrumb listener to add
     */
    void addBreadcrumbListener(BreadcrumbListener listener);

    /**
     * Sdds an active breadcrumb listener.
     *
     * @param listener The breadcrumb listener to remove
     */
    void removeBreadcrumbListener(BreadcrumbListener listener);

    void setTextColor(int textColor);

}
