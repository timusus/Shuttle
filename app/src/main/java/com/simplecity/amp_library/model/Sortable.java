package com.simplecity.amp_library.model;

/**
 * An interface used to define sorting keys used for object comparison
 */
public interface Sortable {

    String getSortKey();

    void setSortKey();
}