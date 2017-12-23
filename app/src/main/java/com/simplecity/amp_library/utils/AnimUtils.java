package com.simplecity.amp_library.utils;

public final class AnimUtils {

    public static float lerp(float a, float b, float v) {
        return a + (b - a) * v;
    }

    public static int lerp(int a, int b, int v) {
        return a + (b - a) * v;
    }

    public static double lerp(double a, double b, double v) {
        return a + (b - a) * v;
    }

    private AnimUtils() {
        throw new IllegalStateException("no instances");
    }

}
