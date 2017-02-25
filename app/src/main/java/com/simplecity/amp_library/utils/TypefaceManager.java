package com.simplecity.amp_library.utils;

import android.graphics.Typeface;
import android.support.v4.util.ArrayMap;

import com.simplecity.amp_library.ShuttleApplication;

import java.util.Map;

public class TypefaceManager {

    public static final String SANS_SERIF = "sans-serif";

    public static final String SANS_SERIF_MEDIUM = "sans-serif-medium";

    public static final String SANS_SERIF_LIGHT = "sans-serif-light";

    public static final String ANDROID_CLOCK_MONO_THIN = "AndroidClockMono-Thin.ttf";

    private final String ROBOTO_MEDIUM = "Roboto-Medium.ttf";

    private final Map<String, Typeface> mCache = new ArrayMap<>();
    private static TypefaceManager sInstance = null;

    /**
     * Only initialize through {@link #getInstance()}
     */
    private TypefaceManager() {
    }

    public static TypefaceManager getInstance() {
        if (sInstance == null) {
            sInstance = new TypefaceManager();
        }
        return sInstance;
    }

    /**
     * @param typeface The name of the type face asset
     * @return The {@link android.graphics.Typeface} that matches
     * <code>typeface</code>
     */
    public Typeface getTypeface(String typeface) {
        Typeface result = mCache.get(typeface);
        if (result == null) {

            switch (typeface) {
                case SANS_SERIF:
                    result = Typeface.create("sans-serif", Typeface.NORMAL);
                    break;
                case SANS_SERIF_MEDIUM:
                    if (ShuttleUtils.hasLollipop()) {
                        result = Typeface.create("sans-serif-medium", Typeface.NORMAL);
                    } else {
                        result = Typeface.createFromAsset(ShuttleApplication.getInstance().getAssets(), "fonts/" + ROBOTO_MEDIUM);
                    }
                    break;
                case SANS_SERIF_LIGHT:
                    result = Typeface.create("sans-serif-light", Typeface.NORMAL);
                    break;
                default:
                    result = Typeface.createFromAsset(ShuttleApplication.getInstance().getAssets(), "fonts/" + typeface);
                    break;
            }
            mCache.put(typeface, result);
        }
        return result;
    }

}

