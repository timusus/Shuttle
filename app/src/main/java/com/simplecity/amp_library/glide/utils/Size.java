package com.simplecity.amp_library.glide.utils;

public class Size {

    public int width;
    public int height;

    public Size(int width, int height) {
        {
            this.width = width;
            this.height = height;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Size size = (Size) o;

        return width == size.width && height == size.height;

    }

    @Override
    public int hashCode() {
        int result = width;
        result = 31 * result + height;
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s, %s", width, height);
    }
}
