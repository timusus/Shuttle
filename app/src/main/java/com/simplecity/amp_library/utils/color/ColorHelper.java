package com.simplecity.amp_library.utils.color;

/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

import android.app.Notification;
import android.content.Context;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.util.Log;

/**
 * Helper class to process legacy (Holo) notifications to make them look like material notifications.
 *
 * @hide
 */
public class ColorHelper {

    private static final String TAG = "ColorHelper";

    private static final Object sLock = new Object();

    private static ColorHelper sInstance;

    public static ColorHelper getInstance() {
        synchronized (sLock) {
            if (sInstance == null) {
                sInstance = new ColorHelper();
            }
            return sInstance;
        }
    }

    /**
     * Finds a suitable color such that there's enough contrast.
     *
     * @param color the color to start searching from.
     * @param other the color to ensure contrast against. Assumed to be lighter than {@param color}
     * @param findFg if true, we assume {@param color} is a foreground, otherwise a background.
     * @param minRatio the minimum contrast ratio required.
     * @return a color with the same hue as {@param color}, potentially darkened to meet the
     *          contrast ratio.
     */
    private static int findContrastColor(int color, int other, boolean findFg, double minRatio) {
        int fg = findFg ? color : other;
        int bg = findFg ? other : color;
        if (ColorUtilsFromCompat.calculateContrast(fg, bg) >= minRatio) {
            return color;
        }

        double[] lab = new double[3];
        ColorUtilsFromCompat.colorToLAB(findFg ? fg : bg, lab);

        double low = 0, high = lab[0];
        final double a = lab[1], b = lab[2];
        for (int i = 0; i < 15 && high - low > 0.00001; i++) {
            final double l = (low + high) / 2;
            if (findFg) {
                fg = ColorUtilsFromCompat.LABToColor(l, a, b);
            } else {
                bg = ColorUtilsFromCompat.LABToColor(l, a, b);
            }
            if (ColorUtilsFromCompat.calculateContrast(fg, bg) > minRatio) {
                low = l;
            } else {
                high = l;
            }
        }
        return ColorUtilsFromCompat.LABToColor(low, a, b);
    }

    /**
     * Finds a suitable alpha such that there's enough contrast.
     *
     * @param color the color to start searching from.
     * @param backgroundColor the color to ensure contrast against.
     * @param minRatio the minimum contrast ratio required.
     * @return the same color as {@param color} with potentially modified alpha to meet contrast
     */
    private static int findAlphaToMeetContrast(int color, int backgroundColor, double minRatio) {
        int fg = color;
        int bg = backgroundColor;
        if (ColorUtilsFromCompat.calculateContrast(fg, bg) >= minRatio) {
            return color;
        }
        int startAlpha = Color.alpha(color);
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        int low = startAlpha, high = 255;
        for (int i = 0; i < 15 && high - low > 0; i++) {
            final int alpha = (low + high) / 2;
            fg = Color.argb(alpha, r, g, b);
            if (ColorUtilsFromCompat.calculateContrast(fg, bg) > minRatio) {
                high = alpha;
            } else {
                low = alpha;
            }
        }
        return Color.argb(high, r, g, b);
    }

    /**
     * Finds a suitable color such that there's enough contrast.
     *
     * @param color the color to start searching from.
     * @param other the color to ensure contrast against. Assumed to be darker than {@param color}
     * @param findFg if true, we assume {@param color} is a foreground, otherwise a background.
     * @param minRatio the minimum contrast ratio required.
     * @return a color with the same hue as {@param color}, potentially darkened to meet the
     *          contrast ratio.
     */
    private static int findContrastColorAgainstDark(int color, int other, boolean findFg,
            double minRatio) {
        int fg = findFg ? color : other;
        int bg = findFg ? other : color;
        if (ColorUtilsFromCompat.calculateContrast(fg, bg) >= minRatio) {
            return color;
        }

        float[] hsl = new float[3];
        ColorUtilsFromCompat.colorToHSL(findFg ? fg : bg, hsl);

        float low = hsl[2], high = 1;
        for (int i = 0; i < 15 && high - low > 0.00001; i++) {
            final float l = (low + high) / 2;
            hsl[2] = l;
            if (findFg) {
                fg = ColorUtilsFromCompat.HSLToColor(hsl);
            } else {
                bg = ColorUtilsFromCompat.HSLToColor(hsl);
            }
            if (ColorUtilsFromCompat.calculateContrast(fg, bg) > minRatio) {
                high = l;
            } else {
                low = l;
            }
        }
        return findFg ? fg : bg;
    }

    /**
     * Change a color by a specified value
     * @param baseColor the base color to lighten
     * @param amount the amount to lighten the color from 0 to 100. This corresponds to the L
     *               increase in the LAB color space. A negative value will darken the color and
     *               a positive will lighten it.
     * @return the changed color
     */
    private static int changeColorLightness(int baseColor, int amount) {
        final double[] result = ColorUtilsFromCompat.getTempDouble3Array();
        ColorUtilsFromCompat.colorToLAB(baseColor, result);
        result[0] = Math.max(Math.min(100, result[0] + amount), 0);
        return ColorUtilsFromCompat.LABToColor(result[0], result[1], result[2]);
    }

    public static int resolvePrimaryColor(Context context, int backgroundColor) {
        boolean useDark = shouldUseDark(backgroundColor);
        if (useDark) {
            return context.getResources().getColor(
                    android.R.color.primary_text_light);
        } else {
            return context.getResources().getColor(
                   android.R.color.primary_text_dark);
        }
    }

    public static int resolveSecondaryColor(Context context, int backgroundColor) {
        boolean useDark = shouldUseDark(backgroundColor);
        if (useDark) {
            return context.getResources().getColor(
                    android.R.color.secondary_text_light);
        } else {
            return context.getResources().getColor(
                    android.R.color.secondary_text_dark);
        }
    }

    private static boolean shouldUseDark(int backgroundColor) {
        boolean useDark = backgroundColor == Notification.COLOR_DEFAULT;
        if (!useDark) {
            useDark = ColorUtilsFromCompat.calculateLuminance(backgroundColor) > 0.5;
        }
        return useDark;
    }

    private static double calculateLuminance(int backgroundColor) {
        return ColorUtilsFromCompat.calculateLuminance(backgroundColor);
    }

    private static double calculateContrast(int foregroundColor, int backgroundColor) {
        return ColorUtilsFromCompat.calculateContrast(foregroundColor, backgroundColor);
    }

    private static boolean satisfiesTextContrast(int backgroundColor, int foregroundColor) {
        return ColorHelper.calculateContrast(foregroundColor, backgroundColor) >= 4.5;
    }

    static boolean isColorLight(int backgroundColor) {
        return calculateLuminance(backgroundColor) > 0.5f;
    }

    /**
     * Framework copy of functions needed from android.support.v4.graphics.ColorUtils.
     */
    private static class ColorUtilsFromCompat {
        private static final double XYZ_WHITE_REFERENCE_X = 95.047;
        private static final double XYZ_WHITE_REFERENCE_Y = 100;
        private static final double XYZ_WHITE_REFERENCE_Z = 108.883;
        private static final double XYZ_EPSILON = 0.008856;
        private static final double XYZ_KAPPA = 903.3;

        private static final ThreadLocal<double[]> TEMP_ARRAY = new ThreadLocal<>();

        private ColorUtilsFromCompat() {}

        /**
         * Composite two potentially translucent colors over each other and returns the result.
         */
        static int compositeColors(@ColorInt int foreground, @ColorInt int background) {
            int bgAlpha = Color.alpha(background);
            int fgAlpha = Color.alpha(foreground);
            int a = compositeAlpha(fgAlpha, bgAlpha);

            int r = compositeComponent(Color.red(foreground), fgAlpha,
                    Color.red(background), bgAlpha, a);
            int g = compositeComponent(Color.green(foreground), fgAlpha,
                    Color.green(background), bgAlpha, a);
            int b = compositeComponent(Color.blue(foreground), fgAlpha,
                    Color.blue(background), bgAlpha, a);

            return Color.argb(a, r, g, b);
        }

        private static int compositeAlpha(int foregroundAlpha, int backgroundAlpha) {
            return 0xFF - (((0xFF - backgroundAlpha) * (0xFF - foregroundAlpha)) / 0xFF);
        }

        private static int compositeComponent(int fgC, int fgA, int bgC, int bgA, int a) {
            if (a == 0) return 0;
            return ((0xFF * fgC * fgA) + (bgC * bgA * (0xFF - fgA))) / (a * 0xFF);
        }

        /**
         * Returns the luminance of a color as a float between {@code 0.0} and {@code 1.0}.
         * <p>Defined as the Y component in the XYZ representation of {@code color}.</p>
         */
        @FloatRange(from = 0.0, to = 1.0)
        public static double calculateLuminance(@ColorInt int color) {
            final double[] result = getTempDouble3Array();
            colorToXYZ(color, result);
            // Luminance is the Y component
            return result[1] / 100;
        }

        /**
         * Returns the contrast ratio between {@code foreground} and {@code background}.
         * {@code background} must be opaque.
         * <p>
         * Formula defined
         * <a href="http://www.w3.org/TR/2008/REC-WCAG20-20081211/#contrast-ratiodef">here</a>.
         */
        static double calculateContrast(@ColorInt int foreground, @ColorInt int background) {
            if (Color.alpha(background) != 255) {
                Log.wtf(TAG, "background can not be translucent: #"
                        + Integer.toHexString(background));
            }
            if (Color.alpha(foreground) < 255) {
                // If the foreground is translucent, composite the foreground over the background
                foreground = compositeColors(foreground, background);
            }

            final double luminance1 = calculateLuminance(foreground) + 0.05;
            final double luminance2 = calculateLuminance(background) + 0.05;

            // Now return the lighter luminance divided by the darker luminance
            return Math.max(luminance1, luminance2) / Math.min(luminance1, luminance2);
        }

        /**
         * Convert the ARGB color to its CIE Lab representative components.
         *
         * @param color  the ARGB color to convert. The alpha component is ignored
         * @param outLab 3-element array which holds the resulting LAB components
         */
        static void colorToLAB(@ColorInt int color, @NonNull double[] outLab) {
            RGBToLAB(Color.red(color), Color.green(color), Color.blue(color), outLab);
        }

        /**
         * Convert RGB components to its CIE Lab representative components.
         *
         * <ul>
         * <li>outLab[0] is L [0 ...100)</li>
         * <li>outLab[1] is a [-128...127)</li>
         * <li>outLab[2] is b [-128...127)</li>
         * </ul>
         *
         * @param r      red component value [0..255]
         * @param g      green component value [0..255]
         * @param b      blue component value [0..255]
         * @param outLab 3-element array which holds the resulting LAB components
         */
        static void RGBToLAB(@IntRange(from = 0x0, to = 0xFF) int r,
                @IntRange(from = 0x0, to = 0xFF) int g, @IntRange(from = 0x0, to = 0xFF) int b,
                @NonNull double[] outLab) {
            // First we convert RGB to XYZ
            RGBToXYZ(r, g, b, outLab);
            // outLab now contains XYZ
            XYZToLAB(outLab[0], outLab[1], outLab[2], outLab);
            // outLab now contains LAB representation
        }

        /**
         * Convert the ARGB color to it's CIE XYZ representative components.
         *
         * <p>The resulting XYZ representation will use the D65 illuminant and the CIE
         * 2° Standard Observer (1931).</p>
         *
         * <ul>
         * <li>outXyz[0] is X [0 ...95.047)</li>
         * <li>outXyz[1] is Y [0...100)</li>
         * <li>outXyz[2] is Z [0...108.883)</li>
         * </ul>
         *
         * @param color  the ARGB color to convert. The alpha component is ignored
         * @param outXyz 3-element array which holds the resulting LAB components
         */
        static void colorToXYZ(@ColorInt int color, @NonNull double[] outXyz) {
            RGBToXYZ(Color.red(color), Color.green(color), Color.blue(color), outXyz);
        }

        /**
         * Convert RGB components to it's CIE XYZ representative components.
         *
         * <p>The resulting XYZ representation will use the D65 illuminant and the CIE
         * 2° Standard Observer (1931).</p>
         *
         * <ul>
         * <li>outXyz[0] is X [0 ...95.047)</li>
         * <li>outXyz[1] is Y [0...100)</li>
         * <li>outXyz[2] is Z [0...108.883)</li>
         * </ul>
         *
         * @param r      red component value [0..255]
         * @param g      green component value [0..255]
         * @param b      blue component value [0..255]
         * @param outXyz 3-element array which holds the resulting XYZ components
         */
        static void RGBToXYZ(@IntRange(from = 0x0, to = 0xFF) int r,
                @IntRange(from = 0x0, to = 0xFF) int g, @IntRange(from = 0x0, to = 0xFF) int b,
                @NonNull double[] outXyz) {
            if (outXyz.length != 3) {
                throw new IllegalArgumentException("outXyz must have a length of 3.");
            }

            double sr = r / 255.0;
            sr = sr < 0.04045 ? sr / 12.92 : Math.pow((sr + 0.055) / 1.055, 2.4);
            double sg = g / 255.0;
            sg = sg < 0.04045 ? sg / 12.92 : Math.pow((sg + 0.055) / 1.055, 2.4);
            double sb = b / 255.0;
            sb = sb < 0.04045 ? sb / 12.92 : Math.pow((sb + 0.055) / 1.055, 2.4);

            outXyz[0] = 100 * (sr * 0.4124 + sg * 0.3576 + sb * 0.1805);
            outXyz[1] = 100 * (sr * 0.2126 + sg * 0.7152 + sb * 0.0722);
            outXyz[2] = 100 * (sr * 0.0193 + sg * 0.1192 + sb * 0.9505);
        }

        /**
         * Converts a color from CIE XYZ to CIE Lab representation.
         *
         * <p>This method expects the XYZ representation to use the D65 illuminant and the CIE
         * 2° Standard Observer (1931).</p>
         *
         * <ul>
         * <li>outLab[0] is L [0 ...100)</li>
         * <li>outLab[1] is a [-128...127)</li>
         * <li>outLab[2] is b [-128...127)</li>
         * </ul>
         *
         * @param x      X component value [0...95.047)
         * @param y      Y component value [0...100)
         * @param z      Z component value [0...108.883)
         * @param outLab 3-element array which holds the resulting Lab components
         */
        static void XYZToLAB(@FloatRange(from = 0f, to = XYZ_WHITE_REFERENCE_X) double x,
                @FloatRange(from = 0f, to = XYZ_WHITE_REFERENCE_Y) double y,
                @FloatRange(from = 0f, to = XYZ_WHITE_REFERENCE_Z) double z,
                @NonNull double[] outLab) {
            if (outLab.length != 3) {
                throw new IllegalArgumentException("outLab must have a length of 3.");
            }
            x = pivotXyzComponent(x / XYZ_WHITE_REFERENCE_X);
            y = pivotXyzComponent(y / XYZ_WHITE_REFERENCE_Y);
            z = pivotXyzComponent(z / XYZ_WHITE_REFERENCE_Z);
            outLab[0] = Math.max(0, 116 * y - 16);
            outLab[1] = 500 * (x - y);
            outLab[2] = 200 * (y - z);
        }

        /**
         * Converts a color from CIE Lab to CIE XYZ representation.
         *
         * <p>The resulting XYZ representation will use the D65 illuminant and the CIE
         * 2° Standard Observer (1931).</p>
         *
         * <ul>
         * <li>outXyz[0] is X [0 ...95.047)</li>
         * <li>outXyz[1] is Y [0...100)</li>
         * <li>outXyz[2] is Z [0...108.883)</li>
         * </ul>
         *
         * @param l      L component value [0...100)
         * @param a      A component value [-128...127)
         * @param b      B component value [-128...127)
         * @param outXyz 3-element array which holds the resulting XYZ components
         */
        static void LABToXYZ(@FloatRange(from = 0f, to = 100) final double l,
                @FloatRange(from = -128, to = 127) final double a,
                @FloatRange(from = -128, to = 127) final double b,
                @NonNull double[] outXyz) {
            final double fy = (l + 16) / 116;
            final double fx = a / 500 + fy;
            final double fz = fy - b / 200;

            double tmp = Math.pow(fx, 3);
            final double xr = tmp > XYZ_EPSILON ? tmp : (116 * fx - 16) / XYZ_KAPPA;
            final double yr = l > XYZ_KAPPA * XYZ_EPSILON ? Math.pow(fy, 3) : l / XYZ_KAPPA;

            tmp = Math.pow(fz, 3);
            final double zr = tmp > XYZ_EPSILON ? tmp : (116 * fz - 16) / XYZ_KAPPA;

            outXyz[0] = xr * XYZ_WHITE_REFERENCE_X;
            outXyz[1] = yr * XYZ_WHITE_REFERENCE_Y;
            outXyz[2] = zr * XYZ_WHITE_REFERENCE_Z;
        }

        /**
         * Converts a color from CIE XYZ to its RGB representation.
         *
         * <p>This method expects the XYZ representation to use the D65 illuminant and the CIE
         * 2° Standard Observer (1931).</p>
         *
         * @param x X component value [0...95.047)
         * @param y Y component value [0...100)
         * @param z Z component value [0...108.883)
         * @return int containing the RGB representation
         */
        @ColorInt
        static int XYZToColor(@FloatRange(from = 0f, to = XYZ_WHITE_REFERENCE_X) double x,
                @FloatRange(from = 0f, to = XYZ_WHITE_REFERENCE_Y) double y,
                @FloatRange(from = 0f, to = XYZ_WHITE_REFERENCE_Z) double z) {
            double r = (x * 3.2406 + y * -1.5372 + z * -0.4986) / 100;
            double g = (x * -0.9689 + y * 1.8758 + z * 0.0415) / 100;
            double b = (x * 0.0557 + y * -0.2040 + z * 1.0570) / 100;

            r = r > 0.0031308 ? 1.055 * Math.pow(r, 1 / 2.4) - 0.055 : 12.92 * r;
            g = g > 0.0031308 ? 1.055 * Math.pow(g, 1 / 2.4) - 0.055 : 12.92 * g;
            b = b > 0.0031308 ? 1.055 * Math.pow(b, 1 / 2.4) - 0.055 : 12.92 * b;

            return Color.rgb(
                    constrain((int) Math.round(r * 255), 0, 255),
                    constrain((int) Math.round(g * 255), 0, 255),
                    constrain((int) Math.round(b * 255), 0, 255));
        }

        /**
         * Converts a color from CIE Lab to its RGB representation.
         *
         * @param l L component value [0...100]
         * @param a A component value [-128...127]
         * @param b B component value [-128...127]
         * @return int containing the RGB representation
         */
        @ColorInt
        static int LABToColor(@FloatRange(from = 0f, to = 100) final double l,
                @FloatRange(from = -128, to = 127) final double a,
                @FloatRange(from = -128, to = 127) final double b) {
            final double[] result = getTempDouble3Array();
            LABToXYZ(l, a, b, result);
            return XYZToColor(result[0], result[1], result[2]);
        }

        private static int constrain(int amount, int low, int high) {
            return amount < low ? low : (amount > high ? high : amount);
        }

        private static float constrain(float amount, float low, float high) {
            return amount < low ? low : (amount > high ? high : amount);
        }

        private static double pivotXyzComponent(double component) {
            return component > XYZ_EPSILON
                    ? Math.pow(component, 1 / 3.0)
                    : (XYZ_KAPPA * component + 16) / 116;
        }

        static double[] getTempDouble3Array() {
            double[] result = TEMP_ARRAY.get();
            if (result == null) {
                result = new double[3];
                TEMP_ARRAY.set(result);
            }
            return result;
        }

        /**
         * Convert HSL (hue-saturation-lightness) components to a RGB color.
         * <ul>
         * <li>hsl[0] is Hue [0 .. 360)</li>
         * <li>hsl[1] is Saturation [0...1]</li>
         * <li>hsl[2] is Lightness [0...1]</li>
         * </ul>
         * If hsv values are out of range, they are pinned.
         *
         * @param hsl 3-element array which holds the input HSL components
         * @return the resulting RGB color
         */
        @ColorInt
        static int HSLToColor(@NonNull float[] hsl) {
            final float h = hsl[0];
            final float s = hsl[1];
            final float l = hsl[2];

            final float c = (1f - Math.abs(2 * l - 1f)) * s;
            final float m = l - 0.5f * c;
            final float x = c * (1f - Math.abs((h / 60f % 2f) - 1f));

            final int hueSegment = (int) h / 60;

            int r = 0, g = 0, b = 0;

            switch (hueSegment) {
                case 0:
                    r = Math.round(255 * (c + m));
                    g = Math.round(255 * (x + m));
                    b = Math.round(255 * m);
                    break;
                case 1:
                    r = Math.round(255 * (x + m));
                    g = Math.round(255 * (c + m));
                    b = Math.round(255 * m);
                    break;
                case 2:
                    r = Math.round(255 * m);
                    g = Math.round(255 * (c + m));
                    b = Math.round(255 * (x + m));
                    break;
                case 3:
                    r = Math.round(255 * m);
                    g = Math.round(255 * (x + m));
                    b = Math.round(255 * (c + m));
                    break;
                case 4:
                    r = Math.round(255 * (x + m));
                    g = Math.round(255 * m);
                    b = Math.round(255 * (c + m));
                    break;
                case 5:
                case 6:
                    r = Math.round(255 * (c + m));
                    g = Math.round(255 * m);
                    b = Math.round(255 * (x + m));
                    break;
            }

            r = constrain(r, 0, 255);
            g = constrain(g, 0, 255);
            b = constrain(b, 0, 255);

            return Color.rgb(r, g, b);
        }

        /**
         * Convert the ARGB color to its HSL (hue-saturation-lightness) components.
         * <ul>
         * <li>outHsl[0] is Hue [0 .. 360)</li>
         * <li>outHsl[1] is Saturation [0...1]</li>
         * <li>outHsl[2] is Lightness [0...1]</li>
         * </ul>
         *
         * @param color  the ARGB color to convert. The alpha component is ignored
         * @param outHsl 3-element array which holds the resulting HSL components
         */
        static void colorToHSL(@ColorInt int color, @NonNull float[] outHsl) {
            RGBToHSL(Color.red(color), Color.green(color), Color.blue(color), outHsl);
        }

        /**
         * Convert RGB components to HSL (hue-saturation-lightness).
         * <ul>
         * <li>outHsl[0] is Hue [0 .. 360)</li>
         * <li>outHsl[1] is Saturation [0...1]</li>
         * <li>outHsl[2] is Lightness [0...1]</li>
         * </ul>
         *
         * @param r      red component value [0..255]
         * @param g      green component value [0..255]
         * @param b      blue component value [0..255]
         * @param outHsl 3-element array which holds the resulting HSL components
         */
        static void RGBToHSL(@IntRange(from = 0x0, to = 0xFF) int r,
                @IntRange(from = 0x0, to = 0xFF) int g, @IntRange(from = 0x0, to = 0xFF) int b,
                @NonNull float[] outHsl) {
            final float rf = r / 255f;
            final float gf = g / 255f;
            final float bf = b / 255f;

            final float max = Math.max(rf, Math.max(gf, bf));
            final float min = Math.min(rf, Math.min(gf, bf));
            final float deltaMaxMin = max - min;

            float h, s;
            float l = (max + min) / 2f;

            if (max == min) {
                // Monochromatic
                h = s = 0f;
            } else {
                if (max == rf) {
                    h = ((gf - bf) / deltaMaxMin) % 6f;
                } else if (max == gf) {
                    h = ((bf - rf) / deltaMaxMin) + 2f;
                } else {
                    h = ((rf - gf) / deltaMaxMin) + 4f;
                }

                s = deltaMaxMin / (1f - Math.abs(2f * l - 1f));
            }

            h = (h * 60f) % 360f;
            if (h < 0) {
                h += 360f;
            }

            outHsl[0] = constrain(h, 0f, 360f);
            outHsl[1] = constrain(s, 0f, 1f);
            outHsl[2] = constrain(l, 0f, 1f);
        }

    }


    /**
     * The lightness difference that has to be added to the primary text color to obtain the
     * secondary text color when the background is light.
     */
    private static final int LIGHTNESS_TEXT_DIFFERENCE_LIGHT = 20;

    /**
     * The lightness difference that has to be added to the primary text color to obtain the
     * secondary text color when the background is dark.
     * A bit less then the above value, since it looks better on dark backgrounds.
     */
    private static final int LIGHTNESS_TEXT_DIFFERENCE_DARK = -10;

    public Pair<Integer, Integer> ensureColors(Context context, boolean hasForegroundColor, int backgroundColor, int foregroundColor) {
        int primaryTextColor;
        int secondaryTextColor;
        if (!hasForegroundColor) {
            primaryTextColor = ColorHelper.resolvePrimaryColor(context, backgroundColor);
            secondaryTextColor = ColorHelper.resolveSecondaryColor(context, backgroundColor);
            int COLOR_DEFAULT = 0;
            if (backgroundColor != COLOR_DEFAULT) {
                primaryTextColor = ColorHelper.findAlphaToMeetContrast(primaryTextColor, backgroundColor, 4.5);
                secondaryTextColor = ColorHelper.findAlphaToMeetContrast(secondaryTextColor, backgroundColor, 4.5);
            }
        } else {
            double backLum = ColorHelper.calculateLuminance(backgroundColor);
            double textLum = ColorHelper.calculateLuminance(foregroundColor);
            double contrast = ColorHelper.calculateContrast(foregroundColor,
                    backgroundColor);
            // We only respect the given colors if worst case Black or White still has
            // contrast
            boolean backgroundLight = backLum > textLum && ColorHelper.satisfiesTextContrast(backgroundColor, Color.BLACK)
                    || backLum <= textLum && !ColorHelper.satisfiesTextContrast(backgroundColor, Color.WHITE);
            if (contrast < 4.5f) {
                if (backgroundLight) {
                    secondaryTextColor = ColorHelper.findContrastColor(
                            foregroundColor,
                            backgroundColor,
                            true /* findFG */,
                            4.5f);
                    primaryTextColor = ColorHelper.changeColorLightness(
                            secondaryTextColor, -LIGHTNESS_TEXT_DIFFERENCE_LIGHT);
                } else {
                    secondaryTextColor =
                            ColorHelper.findContrastColorAgainstDark(
                                    foregroundColor,
                                    backgroundColor,
                                    true /* findFG */,
                                    4.5f);
                    primaryTextColor = ColorHelper.changeColorLightness(
                            secondaryTextColor, -LIGHTNESS_TEXT_DIFFERENCE_DARK);
                }
            } else {
                primaryTextColor = foregroundColor;
                secondaryTextColor = ColorHelper.changeColorLightness(
                        primaryTextColor, backgroundLight ? LIGHTNESS_TEXT_DIFFERENCE_LIGHT
                                : LIGHTNESS_TEXT_DIFFERENCE_DARK);
                if (ColorHelper.calculateContrast(secondaryTextColor,
                        backgroundColor) < 4.5f) {
                    // oh well the secondary is not good enough
                    if (backgroundLight) {
                        secondaryTextColor = ColorHelper.findContrastColor(
                                secondaryTextColor,
                                backgroundColor,
                                true /* findFG */,
                                4.5f);
                    } else {
                        secondaryTextColor
                                = ColorHelper.findContrastColorAgainstDark(
                                secondaryTextColor,
                                backgroundColor,
                                true /* findFG */,
                                4.5f);
                    }
                    primaryTextColor = ColorHelper.changeColorLightness(
                            secondaryTextColor, backgroundLight
                                    ? -LIGHTNESS_TEXT_DIFFERENCE_LIGHT
                                    : -LIGHTNESS_TEXT_DIFFERENCE_DARK);
                }
            }
        }
        return new Pair<>(primaryTextColor, secondaryTextColor);
    }
}