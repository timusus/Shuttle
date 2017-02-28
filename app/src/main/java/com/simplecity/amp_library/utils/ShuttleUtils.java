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
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.simplecity.amp_library.BuildConfig;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.constants.Config;
import com.simplecity.amp_library.model.BaseFileObject;
import com.simplecity.amp_library.model.Query;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.sql.SqlUtils;
import com.simplecity.amp_library.sql.providers.PlayCountTable;

import java.io.File;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * General helpers
 */
public final class ShuttleUtils {

    //Arguments supplied to various bundles

    public static final String ARG_ALBUM_ARTIST = "album_artist";
    public static final String ARG_ARTIST = "artist";
    public static final String ARG_ALBUM = "album";
    public static final String ARG_SONG = "song";
    public static final String ARG_PLAYLIST = "playlist";
    public static final String ARG_GENRE = "genre";

    private final static String TAG = "ShuttleUtils";
    public static final int NEW_ALBUM_PHOTO = 100;
    public static final int NEW_ARTIST_PHOTO = 200;

    public static void openShuttleLink(Activity activity, String appPackageName) {
        if (activity == null || appPackageName == null) {
            return;
        }
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(ShuttleUtils.getShuttleMarketUri(appPackageName))));
        } catch (android.content.ActivityNotFoundException ignored) {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(ShuttleUtils.getShuttleWebUri(appPackageName))));
        }
    }

    public static boolean isAmazonBuild() {
        return BuildConfig.FLAVOR.equals("amazonFree") || BuildConfig.FLAVOR.equals("amazonPaid");
    }

    public static String getShuttleMarketUri(String packageName) {
        String uri;
        if (isAmazonBuild()) {
            uri = "amzn://apps/android?p=" + packageName;
        } else {
            uri = "market://details?id=" + packageName;
        }
        return uri;
    }

    public static String getShuttleWebUri(String packageName) {
        String uri;
        if (isAmazonBuild()) {
            uri = "http://www.amazon.com/gp/mas/dl/android?p=" + packageName;
        } else {
            uri = "https://play.google.com/store/apps/details?id=" + packageName;
        }
        return uri;
    }

    /**
     * Execute an {@link AsyncTask} on a thread pool
     *
     * @param task Task to execute
     * @param args Optional arguments to pass to{@link  AsyncTask#execute(Object[])}
     * @param <T>  Task argument type
     */
    @SuppressLint("NewApi")
    public static <T> void execute(AsyncTask<T, ?, ?> task, T... args) {
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, args);
    }

    /**
     * Method setRingtone.
     *
     * @param context context
     * @param song    Song
     */
    public static void setRingtone(final Context context, final Song song) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(context)) {

                new AlertDialog.Builder(context)
                        .setTitle(R.string.dialog_title_set_ringtone)
                        .setMessage(R.string.dialog_message_set_ringtone)
                        .setPositiveButton(R.string.button_ok, (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                            intent.setData(Uri.parse("package:" + ShuttleApplication.getInstance().getPackageName()));
                            context.startActivity(intent);
                        }).setNegativeButton(R.string.cancel, null)
                        .show();
                return;
            }
        }

        Observable.fromCallable(() -> {

                    boolean success = false;

                    final ContentResolver resolver = context.getContentResolver();
                    // Set the flag in the database to mark this as a ringtone
                    final Uri ringUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id);
                    try {
                        final ContentValues values = new ContentValues(2);
                        values.put(MediaStore.Audio.AudioColumns.IS_RINGTONE, "1");
                        values.put(MediaStore.Audio.AudioColumns.IS_ALARM, "1");
                        if (ringUri != null) {
                            resolver.update(ringUri, values, null, null);
                        }
                    } catch (final UnsupportedOperationException ex) {
                        // most likely the card just got unmounted
                        Log.e(TAG, "couldn't set ringtone flag for song " + song);
                        return false;
                    }

                    Query query = new Query.Builder()
                            .uri(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
                            .projection(new String[]{
                                    BaseColumns._ID,
                                    MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.TITLE
                            })
                            .selection(BaseColumns._ID + "=" + song.id)
                            .build();

                    final Cursor cursor = SqlUtils.createQuery(context, query);

                    try {
                        if (cursor != null && cursor.getCount() == 1) {
                            // Set the system setting to make this the current ringtone
                            cursor.moveToFirst();
                            if (ringUri != null) {
                                Settings.System.putString(resolver, Settings.System.RINGTONE, ringUri.toString());
                            }
                            success = true;
                        }
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }

                    return success;
                }
        )
                .map(success -> success ? context.getString(R.string.ringtone_set, song.name) : context.getString(R.string.ringtone_set_failed))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());

    }

    /**
     * Check whether we have an internet connection
     *
     * @param careAboutWifiOnly whether we care if the preference 'download via wifi only' is checked
     * @return true if we have a connection, false otherwise
     */

    public static boolean isOnline(boolean careAboutWifiOnly) {

        Context context = ShuttleApplication.getInstance();

        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        //Check if we are restricted to download over wifi only
        boolean wifiOnly = mPrefs.getBoolean("pref_download_wifi_only", true);

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

    public static boolean isUpgraded() {

        if (ShuttleApplication.getInstance().getIsUpgraded()) {
            return true;
        }

        try {
            return ShuttleApplication.getInstance().getPackageName().equals(Config.PACKAGE_NAME_PRO);
        } catch (Exception ignored) {
        }

        //If something goes wrong, assume the user has the pro version
        return true;
    }

    /**
     * @return true if device is running API >= 17
     */
    public static boolean hasJellyBeanMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
    }

    /**
     * @return true if device is running API >= 18
     */
    public static boolean hasJellyBeanMR2() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }

    /**
     * @return true if device is running API >= 19
     */
    public static boolean hasKitKat() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    /**
     * @return true if device is running API >= 20
     */
    public static boolean hasAndroidLPreview() {
        return Build.VERSION.SDK_INT >= 20;
    }

    /**
     * @return true if device is running API >= 21
     */
    public static boolean hasLollipop() {
        return Build.VERSION.SDK_INT >= 21;
    }

    /**
     * @return true if device is running API >= 22
     */
    public static boolean hasLollipopMR1() {
        return Build.VERSION.SDK_INT >= 22;
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
     * @return true if device is running API >= 24
     */
    public static boolean hasNougatMR1() {
        return Build.VERSION.SDK_INT >= 25;
    }

    public static boolean isLandscape() {
        final int orientation = ShuttleApplication.getInstance().getResources().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    public static boolean isTablet() {
        return ShuttleApplication.getInstance().getResources().getBoolean(R.bool.isTablet);
    }

    static Observable<List<Song>> getSongsForFileObjects(List<BaseFileObject> fileObjects) {
        List<Observable<List<Song>>> observables = Stream.of(fileObjects)
                .map(fileObject -> FileHelper.getSongList(new File(fileObject.path), true, false))
                .collect(Collectors.toList());

        return Observable.concat(observables).reduce((songs, songs2) -> {
            songs.addAll(songs2);
            return songs;
        });
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

    public static String getIpAddr() {
        @SuppressLint("WifiManagerLeak")
        int i = ((WifiManager) ShuttleApplication.getInstance().getSystemService(Context.WIFI_SERVICE)).getConnectionInfo().getIpAddress();
        Object[] arrayOfObject = new Object[4];
        arrayOfObject[0] = i & 0xFF;
        arrayOfObject[1] = 0xFF & i >> 8;
        arrayOfObject[2] = 0xFF & i >> 16;
        arrayOfObject[3] = 0xFF & i >> 24;
        return String.format("%d.%d.%d.%d", arrayOfObject);
    }
}
