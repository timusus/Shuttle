package com.simplecity.amp_library.model;

import java.io.Serializable;

public class Header implements Serializable {

    public String title;

    public Header(String title) {
        this.title = title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Header header = (Header) o;

        return title != null ? title.equals(header.title) : header.title == null;
    }

    @Override
    public int hashCode() {
        return title != null ? title.hashCode() : 0;
    }
}
