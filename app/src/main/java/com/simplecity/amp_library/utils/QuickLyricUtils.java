package com.simplecity.amp_library.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

/**
 * QuickLyric helpers
 */
public class QuickLyricUtils {

    public static boolean isQLInstalled(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo("com.geecko.QuickLyric", PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }

    public static void getLyricsFor(Context context, String artist, String track) {
        Intent intent = new Intent("com.geecko.QuickLyric.getLyrics");
        intent.putExtra("TAGS", new String[]{artist, track});
        context.startActivity(intent);
    }
}
