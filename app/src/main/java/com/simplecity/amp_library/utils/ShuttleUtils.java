package com.simplecity.amp_library.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;
import com.annimon.stream.Stream;
import com.simplecity.amp_library.BuildConfig;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.constants.Config;
import com.simplecity.amp_library.data.Repository;
import com.simplecity.amp_library.model.BaseFileObject;
import com.simplecity.amp_library.model.Query;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.sql.SqlUtils;
import com.simplecity.amp_library.sql.providers.PlayCountTable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class ShuttleUtils {

    //Arguments supplied to various bundles

    private final static String TAG = "ShuttleUtils";

    @NonNull
    public static Intent getShuttleStoreIntent(@NonNull String packageName) {
        String uri;
        if (isAmazonBuild()) {
            uri = "amzn://apps/android?p=" + packageName;
        } else {
            uri = "market://details?id=" + packageName;
        }
        return new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
    }

    @NonNull
    public static Intent getShuttleWebIntent(@NonNull String packageName) {
        String uri;
        if (isAmazonBuild()) {
            uri = "http://www.amazon.com/gp/mas/dl/android?p=" + packageName;
        } else {
            uri = "https://play.google.com/store/apps/details?id=" + packageName;
        }
        return new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
    }

    public static void openShuttleLink(@NonNull Activity activity, @NonNull String packageName, PackageManager packageManager) {
        Intent intent = getShuttleStoreIntent(packageName);
        if (packageManager.resolveActivity(intent, 0) == null) {
            intent = getShuttleWebIntent(packageName);
        }
        activity.startActivity(intent);
    }

    public static boolean isAmazonBuild() {
        return BuildConfig.FLAVOR.equals("amazonFree") || BuildConfig.FLAVOR.equals("amazonPaid");
    }

    /**
     * Check whether we have an internet connection
     *
     * @param careAboutWifiOnly whether we care if the preference 'download via wifi only' is checked
     * @return true if we have a connection, false otherwise
     */
    public static boolean isOnline(Context context, boolean careAboutWifiOnly) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        //Check if we are restricted to download over wifi only
        boolean wifiOnly = prefs.getBoolean("pref_download_wifi_only", true);

        //If we don't care whether wifi is allowed or not, set wifiOnly to false
        if (!careAboutWifiOnly) {
            wifiOnly = false;
        }

        final ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        //Check the state of the wifi network
        final NetworkInfo wifiNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiNetwork != null && wifiNetwork.isConnectedOrConnecting()) {
            return true;
        }

        //Check other networks
        final NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting() && !wifiOnly;
    }

    public static boolean isUpgraded(ShuttleApplication application, SettingsManager settingsManager) {

        if (application.getIsUpgraded()) {
            return true;
        }

        if (settingsManager.getIsLegacyUpgraded()) {
            return true;
        }

        try {
            return application.getPackageName().equals(Config.PACKAGE_NAME_PRO);
        } catch (Exception ignored) {
        }

        //If something goes wrong, assume the user has the pro version
        return true;
    }

    /**
     * @return true if device is running API >= 23
     */
    public static boolean hasMarshmallow() {
        return Build.VERSION.SDK_INT >= 23;
    }

    /**
     * @return true if device is running API >= 24
     */
    public static boolean hasNougat() {
        return Build.VERSION.SDK_INT >= 24;
    }

    /**
     * @return true if device is running API >= 26
     */
    public static boolean hasOreo() {
        return Build.VERSION.SDK_INT >= 26;
    }

    public static boolean isLandscape(Context context) {
        final int orientation = context.getResources().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    public static boolean isTablet(Context context) {
        return context.getResources().getBoolean(R.bool.isTablet);
    }

    public static Single<List<Song>> getSongsForFileObjects(Repository.SongsRepository songsRepository, List<BaseFileObject> fileObjects) {

        List<Single<List<Song>>> observables = Stream.of(fileObjects)
                .map(fileObject -> FileHelper.getSongList(songsRepository, new File(fileObject.path), true, false))
                .toList();

        return Single.concat(observables)
                .reduce((songs, songs2) -> {
                    List<Song> allSongs = new ArrayList<>();
                    allSongs.addAll(songs);
                    allSongs.addAll(songs2);
                    return allSongs;
                }).toSingle();
    }

    public static void incrementPlayCount(Context context, Song song) {

        if (song == null) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put(PlayCountTable.COLUMN_ID, song.id);
        values.put(PlayCountTable.COLUMN_PLAY_COUNT, song.getPlayCount(context) + 1);
        values.put(PlayCountTable.COLUMN_TIME_PLAYED, System.currentTimeMillis());

        try {
            if (context.getContentResolver().update(PlayCountTable.URI, values, PlayCountTable.COLUMN_ID + " ='" + song.id + "'", null) < 1) {
                context.getContentResolver().insert(PlayCountTable.URI, values);
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to increment play count: " + e.toString());
        }
    }

    public static String getIpAddr(Context context) {
        @SuppressLint("WifiManagerLeak")
        int i = ((WifiManager) context.getSystemService(Context.WIFI_SERVICE)).getConnectionInfo().getIpAddress();
        Object[] arrayOfObject = new Object[4];
        arrayOfObject[0] = i & 0xFF;
        arrayOfObject[1] = 0xFF & i >> 8;
        arrayOfObject[2] = 0xFF & i >> 16;
        arrayOfObject[3] = 0xFF & i >> 24;
        return String.format("%d.%d.%d.%d", arrayOfObject);
    }

    public static boolean canDrawBehindStatusBar() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH);
    }
}
