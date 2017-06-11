package com.afollestad.aesthetic;

import android.support.annotation.ColorInt;
import android.support.annotation.RestrictTo;

import io.reactivex.functions.Function3;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

/**
 * @author Aidan Follestad (afollestad)
 */
@RestrictTo(LIBRARY_GROUP)
public final class LightDarkColorState {

    private final int lightColor;
    private final int darkColor;
    private final boolean isDark;

    public LightDarkColorState(int lightColor, int darkColor, boolean isDark) {
        this.lightColor = lightColor;
        this.darkColor = darkColor;
        this.isDark = isDark;
    }

    static LightDarkColorState create(int lightColor, int darkColor, boolean isDark) {
        return new LightDarkColorState(lightColor, darkColor, isDark);
    }

    public static Function3<Integer, Integer, Boolean, LightDarkColorState> creator() {
        return new Function3<Integer, Integer, Boolean, LightDarkColorState>() {
            @Override
            public LightDarkColorState apply(Integer lightcolor, Integer darkColor, Boolean aBoolean) {
                return LightDarkColorState.create(lightcolor, darkColor, aBoolean);
            }
        };
    }

    @ColorInt
    public int color() {
        return isDark() ? darkColor : lightColor;
    }

    public boolean isDark() {
        return isDark;
    }
}
