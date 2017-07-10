package com.simplecity.amp_library.utils;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import com.simplecity.amp_library.BuildConfig;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.model.CategoryItem;
import com.simplecity.amp_library.ui.adapters.ViewType;

public class SettingsManager {

    private static final String TAG = "SettingsManager";

    // Support
    public static String KEY_PREF_CHANGELOG = "pref_changelog";
    public static String KEY_PREF_FAQ = "pref_faq";
    public static String KEY_PREF_HELP = "pref_help";
    public static String KEY_PREF_RATE = "pref_rate";
    public static String KEY_PREF_RESTORE_PURCHASES = "pref_restore_purchases";

    // Display
    public static String KEY_PREF_TAB_CHOOSER = "pref_tab_chooser";
    public static String KEY_PREF_DEFAULT_PAGE = "pref_default_page";

    // Themes
    public static String KEY_PREF_THEME_BASE = "pref_theme_base";
    public static String KEY_PREF_PRIMARY_COLOR = "pref_theme_primary_color";
    public static String KEY_PREF_ACCENT_COLOR = "pref_theme_accent_color";
    public static String KEY_PREF_NAV_BAR = "pref_nav_bar";
    public static String KEY_PREF_PALETTE = "pref_theme_use_palette";
    public static String KEY_PREF_PALETTE_NOW_PLAYING_ONLY = "pref_theme_use_palette_now_playing";

    // Artwork
    public static String KEY_PREF_DOWNLOAD_ARTWORK = "pref_download_artwork";
    public static String KEY_PREF_DELETE_ARTWORK = "pref_delete_artwork";

    // Scrobbler
    public static String KEY_PREF_DOWNLOAD_SCROBBLER = "pref_download_simple_lastfm_scrobbler";

    // Blacklist/whitelist
    public static String KEY_PREF_BLACKLIST = "pref_blacklist_view";
    public static String KEY_PREF_WHITELIST = "pref_whitelist_view";

    // Upgrade
    public static String KEY_PREF_UPGRADE = "pref_upgrade";

    private static SettingsManager sInstance;

    public static SettingsManager getInstance() {
        if (sInstance == null) {
            sInstance = new SettingsManager();
        }
        return sInstance;
    }

    // Whether the 'rate' snackbar has been seen during this session
    public boolean hasSeenRateSnackbar = false;

    private SettingsManager() {

    }

    private SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(ShuttleApplication.getInstance());
    }

    private String getStringValue(String key) {
        return getSharedPreferences().getString(key, null);
    }

    private String getStringValue(String key, String defaultValue) {
        return getSharedPreferences().getString(key, defaultValue);
    }

    private boolean getBooleanValue(String key, boolean defaultValue) {
        return getSharedPreferences().getBoolean(key, defaultValue);
    }

    private int getIntValue(String key, int defaultValue) {
        return getSharedPreferences().getInt(key, defaultValue);
    }

    private void setStringValue(String key, String value) {
        final SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putString(key, value);
        editor.apply();
    }

    private void setBooleanValue(String key, boolean value) {
        final SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    private void setIntValue(String key, int value) {
        final SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putInt(key, value);
        editor.apply();
    }

    private static final String KEY_KEEP_SCREEN_ON = "pref_screen_on";

    public boolean keepScreenOn() {
        return getBooleanValue(KEY_KEEP_SCREEN_ON, false);
    }

    private static final String KEY_ALBUM_DISPLAY_TYPE = "album_display_type_new";

    public void setAlbumDisplayType(int type) {
        setIntValue(KEY_ALBUM_DISPLAY_TYPE, type);
    }

    @ViewType
    public int getAlbumDisplayType() {
        return getIntValue(KEY_ALBUM_DISPLAY_TYPE, ViewType.ALBUM_LIST);
    }

    private static final String KEY_ARTIST_DISPLAY_TYPE = "artist_display_type_new";

    public void setArtistDisplayType(int type) {
        setIntValue(KEY_ARTIST_DISPLAY_TYPE, type);
    }

    @ViewType
    public int getArtistDisplayType() {
        return getIntValue(KEY_ARTIST_DISPLAY_TYPE, ViewType.ARTIST_PALETTE);
    }

    private static final String KEY_ARTIST_COLUMN_COUNT = "artist_column_count";
    private static final String KEY_ARTIST_COLUMN_COUNT_LAND = "artist_column_count_land";
    private static final String KEY_ARTIST_COLUMN_COUNT_TABLET = "artist_column_count_tablet";
    private static final String KEY_ARTIST_COLUMN_COUNT_TABLET_LAND = "artist_column_count_tablet_land";

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
        int artistDisplayType = getArtistDisplayType();
        int defaultSpanCount = artistDisplayType == ViewType.ARTIST_LIST ? res.getInteger(R.integer.list_num_columns) : res.getInteger(R.integer.grid_num_columns);
        if (artistDisplayType == ViewType.ARTIST_LIST && defaultSpanCount == 1) {
            return 1;
        }
        return getIntValue(getArtistColumnCountKey(), defaultSpanCount);
    }

    private static final String KEY_ALBUM_COLUMN_COUNT = "album_column_count";
    private static final String KEY_ALBUM_COLUMN_COUNT_LAND = "album_column_count_land";
    private static final String KEY_ALBUM_COLUMN_COUNT_TABLET = "album_column_count_tablet";
    private static final String KEY_ALBUM_COLUMN_COUNT_TABLET_LAND = "album_column_count_tablet_land";

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
        int albumDisplayType = getAlbumDisplayType();
        int defaultSpanCount = albumDisplayType == ViewType.ALBUM_LIST ? res.getInteger(R.integer.list_num_columns) : res.getInteger(R.integer.grid_num_columns);
        if (albumDisplayType == ViewType.ALBUM_LIST && defaultSpanCount == 1) {
            return 1;
        }
        return getIntValue(getAlbumColumnCountKey(), defaultSpanCount);
    }

    public boolean getEqualizerEnabled() {
        return getBooleanValue("audiofx.global.enable", false);
    }

    private static final String DOCUMENT_TREE_URI = "document_tree_uri";

    public void setDocumentTreeUri(String documentTreeUri) {
        setStringValue(DOCUMENT_TREE_URI, documentTreeUri);
    }

    public String getDocumentTreeUri() {
        return getStringValue(DOCUMENT_TREE_URI);
    }

    private static final String KEY_FOLDER_BROWSER_INITIAL_DIR = "folder_browser_initial_dir";

    public void setFolderBrowserInitialDir(String dir) {
        setStringValue(KEY_FOLDER_BROWSER_INITIAL_DIR, dir);
    }

    public String getFolderBrowserInitialDir() {
        return getStringValue(KEY_FOLDER_BROWSER_INITIAL_DIR);
    }

    private static final String KEY_FOLDER_BROWSER_FILES_SORT_ORDER = "folder_browser_files_sort_order";

    public void setFolderBrowserFilesSortOrder(String sortOrder) {
        setStringValue(KEY_FOLDER_BROWSER_FILES_SORT_ORDER, sortOrder);
    }

    public String getFolderBrowserFilesSortOrder() {
        return getStringValue(KEY_FOLDER_BROWSER_FILES_SORT_ORDER, SortManager.SortFiles.DEFAULT);
    }

    private static final String KEY_FOLDER_BROWSER_FILES_ASCENDING = "folder_browser_files_ascending";

    public void setFolderBrowserFilesAscending(boolean ascending) {
        setBooleanValue(KEY_FOLDER_BROWSER_FILES_ASCENDING, ascending);
    }

    public boolean getFolderBrowserFilesAscending() {
        return getBooleanValue(KEY_FOLDER_BROWSER_FILES_ASCENDING, true);
    }

    private static final String KEY_FOLDER_BROWSER_FOLDERS_SORT_ORDER = "folder_browser_folders_sort_order";

    public void setFolderBrowserFoldersSortOrder(String sortOrder) {
        setStringValue(KEY_FOLDER_BROWSER_FOLDERS_SORT_ORDER, sortOrder);
    }

    public String getFolderBrowserFoldersSortOrder() {
        return getStringValue(KEY_FOLDER_BROWSER_FOLDERS_SORT_ORDER, SortManager.SortFolders.DEFAULT);
    }

    private static final String KEY_FOLDER_BROWSER_FOLDERS_ASCENDING = "folder_browser_folders_ascending";

    public void setFolderBrowserFoldersAscending(boolean ascending) {
        setBooleanValue(KEY_FOLDER_BROWSER_FOLDERS_ASCENDING, ascending);
    }

    public boolean getFolderBrowserFoldersAscending() {
        return getBooleanValue(KEY_FOLDER_BROWSER_FOLDERS_ASCENDING, true);
    }

    private static final String KEY_FOLDER_BROWSER_SHOW_FILENAMES = "folder_browser_show_file_names";

    public void setFolderBrowserShowFileNames(boolean showFileNames) {
        setBooleanValue(KEY_FOLDER_BROWSER_SHOW_FILENAMES, showFileNames);
    }

    public boolean getFolderBrowserShowFileNames() {
        return getBooleanValue(KEY_FOLDER_BROWSER_SHOW_FILENAMES, false);
    }

    private static final String KEY_LAUNCH_COUNT = "launch_count";

    public void incrementLaunchCount() {
        setIntValue(KEY_LAUNCH_COUNT, getLaunchCount() + 1);
    }

    public int getLaunchCount() {
        return getIntValue(KEY_LAUNCH_COUNT, 0);
    }

    private static final String KEY_NAG_MESSAGE_READ = "nag_message_read";

    public void setNagMessageRead() {
        setBooleanValue(KEY_NAG_MESSAGE_READ, true);
    }

    public boolean getNagMessageRead() {
        return getBooleanValue(KEY_NAG_MESSAGE_READ, false);
    }

    private static final String KEY_HAS_RATED = "has_rated";

    public void setHasRated() {
        setBooleanValue(KEY_HAS_RATED, true);
    }

    public boolean getHasRated() {
        return getBooleanValue(KEY_HAS_RATED, false);
    }

    private static final String KEY_BLUETOOTH_PAUSE_DISCONNECT = "pref_bluetooth_disconnect";
    private static final String KEY_BLUETOOTH_RESUME_CONNECT = "pref_bluetooth_connect";

    public boolean getBluetoothPauseDisconnect() {
        return getBooleanValue(KEY_BLUETOOTH_PAUSE_DISCONNECT, true);
    }

    public boolean getBluetoothResumeConnect() {
        return getBooleanValue(KEY_BLUETOOTH_RESUME_CONNECT, false);
    }

    // Themes

    public boolean getUsePalette() {
        return getBooleanValue(KEY_PREF_PALETTE, true);
    }

    public boolean getUsePaletteNowPlayingOnly() {
        return getBooleanValue(KEY_PREF_PALETTE_NOW_PLAYING_ONLY, false);
    }

    // Artwork

    public static final String KEY_PREFER_LAST_FM = "pref_prefer_lastfm";
    private static final String KEY_DOWNLOAD_AUTOMATICALLY = "pref_download_artwork_auto";
    private static final String KEY_USE_GMAIL_PLACEHOLDERS = "pref_placeholders";
    private static final String KEY_QUEUE_ARTWORK = "pref_artwork_queue";
    private static final String KEY_CROP_ARTWORK = "pref_crop_artwork";
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

    private static final String KEY_PLAYLIST_IGNORE_DUPLICATES = "pref_ignore_duplicates";

    public boolean ignoreDuplicates() {
        return getBooleanValue(KEY_PLAYLIST_IGNORE_DUPLICATES, false);
    }

    public void setIgnoreDuplicates(boolean ignoreDuplicates) {
        setBooleanValue(KEY_PLAYLIST_IGNORE_DUPLICATES, ignoreDuplicates);
    }

    private static final String KEY_INVERT_NOTIFICATION_ICONS = "pref_invert_notif_icons";

    public boolean invertNotificationIcons() {
        return getBooleanValue(KEY_INVERT_NOTIFICATION_ICONS, false);
    }

    // Search settings

    private static final String KEY_SEARCH_FUZZY = "search_fuzzy";

    public void setSearchFuzzy(boolean fuzzy) {
        setBooleanValue(KEY_SEARCH_FUZZY, fuzzy);
    }

    public boolean getSearchFuzzy() {
        return getBooleanValue(KEY_SEARCH_FUZZY, true);
    }

    private static final String KEY_SEARCH_ARTISTS = "search_artists";

    public void setSearchArtists(boolean searchArtists) {
        setBooleanValue(KEY_SEARCH_ARTISTS, searchArtists);
    }

    public boolean getSearchArtists() {
        return getBooleanValue(KEY_SEARCH_ARTISTS, true);
    }

    private static final String KEY_SEARCH_ALBUMS = "search_albums";

    public void setSearchAlbums(boolean searchAlbums) {
        setBooleanValue(KEY_SEARCH_ALBUMS, searchAlbums);
    }

    public boolean getSearchAlbums() {
        return getBooleanValue(KEY_SEARCH_ALBUMS, true);
    }


    // Changelog

    private static final String KEY_VERSION_CODE = "version_code";

    public void setVersionCode() {
        setIntValue(KEY_VERSION_CODE, BuildConfig.VERSION_CODE);
    }

    public int getStoredVersionCode() {
        return getIntValue(KEY_VERSION_CODE, -1);
    }

    private static final String KEY_CHANGELOG_SHOW_ON_LAUNCH = "show_on_launch";

    public void setShowChangelogOnLaunch(boolean showOnLaunch) {
        setBooleanValue(KEY_CHANGELOG_SHOW_ON_LAUNCH, showOnLaunch);
    }

    public boolean getShowChangelogOnLaunch() {
        return getBooleanValue(KEY_CHANGELOG_SHOW_ON_LAUNCH, true);
    }

    // Library Controller

    private static final String KEY_DEFAULT_PAGE = "default_page";

    @CategoryItem.Type
    public int getDefaultPageType() {
        return getIntValue(KEY_DEFAULT_PAGE, CategoryItem.Type.ARTISTS);
    }

    public void setDefaultPageType(@CategoryItem.Type int type) {
        setIntValue(KEY_DEFAULT_PAGE, type);
    }
}