package com.simplecity.amp_library.utils;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.ui.modelviews.ViewType;

public class SettingsManager {

    private static final String TAG = "SettingsManager";

    private static SettingsManager sInstance;

    public static SettingsManager getInstance() {
        if (sInstance == null) {
            sInstance = new SettingsManager();
        }
        return sInstance;
    }

    private SettingsManager() {

    }

    private SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(ShuttleApplication.getInstance());
    }

    public String getStringValue(String key) {
        return getSharedPreferences().getString(key, null);
    }

    public String getStringValue(String key, String defaultValue) {
        return getSharedPreferences().getString(key, defaultValue);
    }

    public boolean getBooleanValue(String key, boolean defaultValue) {
        return getSharedPreferences().getBoolean(key, defaultValue);
    }

    public int getIntValue(String key, int defaultValue) {
        return getSharedPreferences().getInt(key, defaultValue);
    }

    public void setStringValue(String key, String value) {
        final SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putString(key, value);
        editor.apply();
    }

    public void setBooleanValue(String key, boolean value) {
        final SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public void setIntValue(String key, int value) {
        final SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public static final String KEY_SHOW_LOCKSCREEN_ARTWORK = "pref_show_lockscreen_artwork";

    public boolean showLockscreenArtwork() {
        return getBooleanValue(KEY_SHOW_LOCKSCREEN_ARTWORK, true);
    }

    public static final String KEY_CAN_TINT_NAV_BAR = "pref_nav_bar";

    public boolean canTintNavBar() {
        return ShuttleUtils.hasLollipop() && getBooleanValue(KEY_CAN_TINT_NAV_BAR, true);
    }

    public static final String KEY_KEEP_SCREEN_ON = "pref_screen_on";

    public boolean keepScreenOn() {
        return getBooleanValue(KEY_KEEP_SCREEN_ON, false);
    }

    public static final String KEY_ALBUM_DISPLAY_TYPE = "album_display_type_new";

    public void setAlbumDisplayType(int type) {
        setIntValue(KEY_ALBUM_DISPLAY_TYPE, type);
    }

    @ViewType
    public int getAlbumDisplayType() {
        return getIntValue(KEY_ALBUM_DISPLAY_TYPE, ViewType.ALBUM_LIST);
    }

    public static final String KEY_ARTIST_DISPLAY_TYPE = "artist_display_type_new";

    public void setArtistDisplayType(int type) {
        setIntValue(KEY_ARTIST_DISPLAY_TYPE, type);
    }

    @ViewType
    public int getArtistDisplayType() {
        return getIntValue(KEY_ARTIST_DISPLAY_TYPE, ViewType.ARTIST_PALETTE);
    }

    public static final String KEY_ARTIST_COLUMN_COUNT = "artist_column_count";
    public static final String KEY_ARTIST_COLUMN_COUNT_LAND = "artist_column_count_land";
    public static final String KEY_ARTIST_COLUMN_COUNT_TABLET = "artist_column_count_tablet";
    public static final String KEY_ARTIST_COLUMN_COUNT_TABLET_LAND = "artist_column_count_tablet_land";

    private String getArtistColumnCountKey() {
        String key = KEY_ARTIST_COLUMN_COUNT;

        if (ShuttleUtils.isLandscape()) {
            key = ShuttleUtils.isTablet() ? KEY_ARTIST_COLUMN_COUNT_TABLET_LAND : KEY_ARTIST_COLUMN_COUNT_LAND;
        } else {
            if (ShuttleUtils.isTablet()) key = KEY_ARTIST_COLUMN_COUNT_TABLET;
        }

        return key;
    }

    public void setArtistColumnCount(int count) {
        setIntValue(getArtistColumnCountKey(), count);
    }

    public int getArtistColumnCount(Resources res) {
        int defaultSpanCount = getArtistDisplayType() == ViewType.ARTIST_LIST ? res.getInteger(R.integer.list_num_columns) : res.getInteger(R.integer.grid_num_columns);
        return getIntValue(getArtistColumnCountKey(), defaultSpanCount);
    }

    public static final String KEY_ALBUM_COLUMN_COUNT = "album_column_count";
    public static final String KEY_ALBUM_COLUMN_COUNT_LAND = "album_column_count_land";
    public static final String KEY_ALBUM_COLUMN_COUNT_TABLET = "album_column_count_tablet";
    public static final String KEY_ALBUM_COLUMN_COUNT_TABLET_LAND = "album_column_count_tablet_land";

    private String getAlbumColumnCountKey() {
        String key = KEY_ALBUM_COLUMN_COUNT;

        if (ShuttleUtils.isLandscape()) {
            key = ShuttleUtils.isTablet() ? KEY_ALBUM_COLUMN_COUNT_TABLET_LAND : KEY_ALBUM_COLUMN_COUNT_LAND;
        } else {
            if (ShuttleUtils.isTablet()) key = KEY_ALBUM_COLUMN_COUNT_TABLET;
        }

        return key;
    }

    public void setAlbumColumnCount(int count) {
        setIntValue(getAlbumColumnCountKey(), count);
    }

    public int getAlbumColumnCount(Resources res) {
        int defaultSpanCount = getAlbumDisplayType() == ViewType.ALBUM_LIST ? res.getInteger(R.integer.list_num_columns) : res.getInteger(R.integer.grid_num_columns);
        return getIntValue(getAlbumColumnCountKey(), defaultSpanCount);
    }

    public boolean getEqualizerEnabled() {
        return getBooleanValue("audiofx.global.enable", false);
    }

    public static final String DOCUMENT_TREE_URI = "document_tree_uri";

    public void setDocumentTreeUri(String documentTreeUri) {
        setStringValue(DOCUMENT_TREE_URI, documentTreeUri);
    }

    public String getDocumentTreeUri() {
        return getStringValue(DOCUMENT_TREE_URI);
    }

    public static final String KEY_FOLDER_BROWSER_INITIAL_DIR = "folder_browser_initial_dir";

    public void setFolderBrowserInitialDir(String dir) {
        setStringValue(KEY_FOLDER_BROWSER_INITIAL_DIR, dir);
    }

    public String getFolderBrowserInitialDir() {
        return getStringValue(KEY_FOLDER_BROWSER_INITIAL_DIR);
    }

    public static final String KEY_FOLDER_BROWSER_FILES_SORT_ORDER = "folder_browser_files_sort_order";

    public void setFolderBrowserFilesSortOrder(String sortOrder) {
        setStringValue(KEY_FOLDER_BROWSER_FILES_SORT_ORDER, sortOrder);
    }

    public String getFolderBrowserFilesSortOrder() {
        return getStringValue(KEY_FOLDER_BROWSER_FILES_SORT_ORDER, SortManager.SortFiles.DEFAULT);
    }

    public static final String KEY_FOLDER_BROWSER_FILES_ASCENDING = "folder_browser_files_ascending";

    public void setFolderBrowserFilesAscending(boolean ascending) {
        setBooleanValue(KEY_FOLDER_BROWSER_FILES_ASCENDING, ascending);
    }

    public boolean getFolderBrowserFilesAscending() {
        return getBooleanValue(KEY_FOLDER_BROWSER_FILES_ASCENDING, true);
    }

    public static final String KEY_FOLDER_BROWSER_FOLDERS_SORT_ORDER = "folder_browser_folders_sort_order";

    public void setFolderBrowserFoldersSortOrder(String sortOrder) {
        setStringValue(KEY_FOLDER_BROWSER_FOLDERS_SORT_ORDER, sortOrder);
    }

    public String getFolderBrowserFoldersSortOrder() {
        return getStringValue(KEY_FOLDER_BROWSER_FOLDERS_SORT_ORDER, SortManager.SortFolders.DEFAULT);
    }

    public static final String KEY_FOLDER_BROWSER_FOLDERS_ASCENDING = "folder_browser_folders_ascending";

    public void setFolderBrowserFoldersAscending(boolean ascending) {
        setBooleanValue(KEY_FOLDER_BROWSER_FOLDERS_ASCENDING, ascending);
    }

    public boolean getFolderBrowserFoldersAscending() {
        return getBooleanValue(KEY_FOLDER_BROWSER_FOLDERS_ASCENDING, true);
    }

    public static final String KEY_FOLDER_BROWSER_SHOW_FILENAMES = "folder_browser_show_file_names";

    public void setFolderBrowserShowFileNames(boolean showFileNames) {
        setBooleanValue(KEY_FOLDER_BROWSER_SHOW_FILENAMES, showFileNames);
    }

    public boolean getFolderBrowserShowFileNames() {
        return getBooleanValue(KEY_FOLDER_BROWSER_SHOW_FILENAMES, false);
    }

    public static final String KEY_LAUNCH_COUNT = "launch_count";

    public void incrementLaunchCount() {
        setIntValue(KEY_LAUNCH_COUNT, getLaunchCount() + 1);
    }

    public int getLaunchCount() {
        return getIntValue(KEY_LAUNCH_COUNT, 0);
    }

    public static final String KEY_NAG_MESSAGE_READ = "nag_message_read";

    public void setNagMessageRead() {
        setBooleanValue(KEY_NAG_MESSAGE_READ, true);
    }

    public boolean getNagMessageRead() {
        return getBooleanValue(KEY_NAG_MESSAGE_READ, false);
    }

    public static final String KEY_HAS_RATED = "has_rated";

    public void setHasRated() {
        setBooleanValue(KEY_HAS_RATED, true);
    }

    public boolean getHasRated() {
        return getBooleanValue(KEY_HAS_RATED, false);
    }

    public static final String KEY_BLUETOOTH_PAUSE_DISCONNECT = "pref_bluetooth_disconnect";
    public static final String KEY_BLUETOOTH_RESUME_CONNECT = "pref_bluetooth_connect";

    public boolean getBluetoothPauseDisconnect() {
        return getBooleanValue(KEY_BLUETOOTH_PAUSE_DISCONNECT, true);
    }

    public boolean getBluetoothResumeConnect() {
        return getBooleanValue(KEY_BLUETOOTH_RESUME_CONNECT, false);
    }

    public static final String KEY_PRIMARY_COLOR = "pref_theme_highlight_color";
    public static final String KEY_ACCENT_COLOR = "pref_theme_accent_color";
    public static final String KEY_ACCENT_IS_WHITE = "pref_theme_white_accent";

    public int getPrimaryColor(int defaultColor) {
        return getIntValue(KEY_PRIMARY_COLOR, defaultColor);
    }

    public void setPrimaryColor(int primaryColor) {
        setIntValue(KEY_PRIMARY_COLOR, primaryColor);
    }

    public int getAccentColor(int defaultColor) {
        return getIntValue(KEY_ACCENT_COLOR, defaultColor);
    }

    public void setAccentColor(int accentColor) {
        setIntValue(KEY_ACCENT_COLOR, accentColor);
    }

    public boolean isAccentColorWhite() {
        return getBooleanValue(KEY_ACCENT_IS_WHITE, false);
    }


    //ARTWORK

    public static final String KEY_PREFER_LAST_FM = "pref_prefer_lastfm";
    public static final String KEY_DOWNLOAD_AUTOMATICALLY = "pref_download_artwork_auto";
    public static final String KEY_USE_GMAIL_PLACEHOLDERS = "pref_placeholders";
    public static final String KEY_QUEUE_ARTWORK = "pref_artwork_queue";
    public static final String KEY_CROP_ARTWORK = "pref_crop_artwork";
    public static final String KEY_IGNORE_MEDIASTORE_ART = "pref_ignore_mediastore_artwork";
    public static final String KEY_IGNORE_EMBEDDED_ARTWORK = "pref_ignore_embedded_artwork";
    public static final String KEY_IGNORE_FOLDER_ARTWORK = "pref_ignore_folder_artwork";
    public static final String KEY_PREFER_EMBEDDED_ARTWORK = "pref_prefer_embedded";

    public boolean canDownloadArtworkAutomatically() {
        return getBooleanValue(KEY_DOWNLOAD_AUTOMATICALLY, false);
    }

    public boolean preferLastFM() {
        return getBooleanValue(KEY_PREFER_LAST_FM, true);
    }

    public boolean preferEmbeddedArtwork() {
        return getBooleanValue(KEY_PREFER_EMBEDDED_ARTWORK, false);
    }

    public boolean useGmailPlaceholders() {
        return getBooleanValue(KEY_USE_GMAIL_PLACEHOLDERS, false);
    }

    public boolean showArtworkInQueue() {
        return getBooleanValue(KEY_QUEUE_ARTWORK, true);
    }

    public boolean cropArtwork() {
        return getBooleanValue(KEY_CROP_ARTWORK, false);
    }

    public boolean ignoreMediaStoreArtwork() {
        return getBooleanValue(KEY_IGNORE_MEDIASTORE_ART, false);
    }

    public boolean ignoreFolderArtwork() {
        return getBooleanValue(KEY_IGNORE_FOLDER_ARTWORK, false);
    }

    public boolean ignoreEmbeddedArtwork() {
        return getBooleanValue(KEY_IGNORE_EMBEDDED_ARTWORK, false);
    }

    public static final String KEY_PLAYLIST_IGNORE_DUPLICATES = "pref_ignore_duplicates";

    public boolean ignoreDuplicates() {
        return getBooleanValue(KEY_PLAYLIST_IGNORE_DUPLICATES, false);
    }

    public void setIgnoreDuplicates(boolean ignoreDuplicates) {
        setBooleanValue(KEY_PLAYLIST_IGNORE_DUPLICATES, ignoreDuplicates);
    }

    public static final String KEY_INVERT_NOTIFICATION_ICONS = "pref_invert_notif_icons";

    public boolean invertNotificationIcons() {
        return getBooleanValue(KEY_INVERT_NOTIFICATION_ICONS, false);
    }
}