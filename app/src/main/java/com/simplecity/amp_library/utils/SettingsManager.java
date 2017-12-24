package com.simplecity.amp_library.utils;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
    public static String KEY_DISPLAY_REMAINING_TIME = "pref_display_remaining_time";

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

    // Playback
    public static String KEY_PREF_REMEMBER_SHUFFLE = "pref_remember_shuffle";

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

    @Nullable
    private String getString(@NonNull String key) {
        return getString(key, null);
    }

    @NonNull
    private String getString(@NonNull String key, @NonNull String defaultValue) {
        return getSharedPreferences().getString(key, defaultValue);
    }

    private void setString(@NonNull String key, @Nullable String value) {
        final SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putString(key, value);
        editor.apply();
    }

    private boolean getBool(@NonNull String key, boolean defaultValue) {
        return getSharedPreferences().getBoolean(key, defaultValue);
    }

    private void setBool(@NonNull String key, boolean value) {
        final SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    private int getInt(@NonNull String key, int defaultValue) {
        return getSharedPreferences().getInt(key, defaultValue);
    }

    private void setInt(@NonNull String key, int value) {
        final SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public static final String KEY_SHOW_LOCKSCREEN_ARTWORK = "pref_show_lockscreen_artwork";

    public boolean showLockscreenArtwork() {
        return getBool(KEY_SHOW_LOCKSCREEN_ARTWORK, true);
    }

    private static final String KEY_KEEP_SCREEN_ON = "pref_screen_on";

    public boolean keepScreenOn() {
        return getBool(KEY_KEEP_SCREEN_ON, false);
    }

    public boolean displayRemainingTime() {
        return getBool(KEY_DISPLAY_REMAINING_TIME, true);
    }

    private static final String KEY_ALBUM_DISPLAY_TYPE = "album_display_type_new";

    public void setAlbumDisplayType(int type) {
        setInt(KEY_ALBUM_DISPLAY_TYPE, type);
    }

    @ViewType
    public int getAlbumDisplayType() {
        return getInt(KEY_ALBUM_DISPLAY_TYPE, ShuttleUtils.isTablet() ? ViewType.ALBUM_PALETTE : ViewType.ALBUM_LIST);
    }

    private static final String KEY_ARTIST_DISPLAY_TYPE = "artist_display_type_new";

    public void setArtistDisplayType(int type) {
        setInt(KEY_ARTIST_DISPLAY_TYPE, type);
    }

    @ViewType
    public int getArtistDisplayType() {
        return getInt(KEY_ARTIST_DISPLAY_TYPE, ViewType.ARTIST_PALETTE);
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
        setInt(getArtistColumnCountKey(), count);
    }

    public int getArtistColumnCount(Resources res) {
        int artistDisplayType = getArtistDisplayType();
        int defaultSpanCount = artistDisplayType == ViewType.ARTIST_LIST ? res.getInteger(R.integer.list_num_columns) : res.getInteger(R.integer.grid_num_columns);
        if (artistDisplayType == ViewType.ARTIST_LIST && defaultSpanCount == 1) {
            return 1;
        }
        return getInt(getArtistColumnCountKey(), defaultSpanCount);
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
        setInt(getAlbumColumnCountKey(), count);
    }

    public int getAlbumColumnCount(Resources res) {
        int albumDisplayType = getAlbumDisplayType();
        int defaultSpanCount = albumDisplayType == ViewType.ALBUM_LIST ? res.getInteger(R.integer.list_num_columns) : res.getInteger(R.integer.grid_num_columns);
        if (albumDisplayType == ViewType.ALBUM_LIST && defaultSpanCount == 1) {
            return 1;
        }
        return getInt(getAlbumColumnCountKey(), defaultSpanCount);
    }

    public boolean getEqualizerEnabled() {
        return getBool("audiofx.global.enable", false);
    }


    private static final String DOCUMENT_TREE_URI = "document_tree_uri";

    public void setDocumentTreeUri(String documentTreeUri) {
        setString(DOCUMENT_TREE_URI, documentTreeUri);
    }

    public String getDocumentTreeUri() {
        return getString(DOCUMENT_TREE_URI);
    }

    private static final String KEY_FOLDER_BROWSER_INITIAL_DIR = "folder_browser_initial_dir";

    public void setFolderBrowserInitialDir(String dir) {
        setString(KEY_FOLDER_BROWSER_INITIAL_DIR, dir);
    }

    public String getFolderBrowserInitialDir() {
        return getString(KEY_FOLDER_BROWSER_INITIAL_DIR);
    }

    private static final String KEY_FOLDER_BROWSER_FILES_SORT_ORDER = "folder_browser_files_sort_order";

    public void setFolderBrowserFilesSortOrder(String sortOrder) {
        setString(KEY_FOLDER_BROWSER_FILES_SORT_ORDER, sortOrder);
    }

    public String getFolderBrowserFilesSortOrder() {
        return getString(KEY_FOLDER_BROWSER_FILES_SORT_ORDER, SortManager.SortFiles.DEFAULT);
    }

    private static final String KEY_FOLDER_BROWSER_FILES_ASCENDING = "folder_browser_files_ascending";

    public void setFolderBrowserFilesAscending(boolean ascending) {
        setBool(KEY_FOLDER_BROWSER_FILES_ASCENDING, ascending);
    }

    public boolean getFolderBrowserFilesAscending() {
        return getBool(KEY_FOLDER_BROWSER_FILES_ASCENDING, true);
    }

    private static final String KEY_FOLDER_BROWSER_FOLDERS_SORT_ORDER = "folder_browser_folders_sort_order";

    public void setFolderBrowserFoldersSortOrder(String sortOrder) {
        setString(KEY_FOLDER_BROWSER_FOLDERS_SORT_ORDER, sortOrder);
    }

    public String getFolderBrowserFoldersSortOrder() {
        return getString(KEY_FOLDER_BROWSER_FOLDERS_SORT_ORDER, SortManager.SortFolders.DEFAULT);
    }

    private static final String KEY_FOLDER_BROWSER_FOLDERS_ASCENDING = "folder_browser_folders_ascending";

    public void setFolderBrowserFoldersAscending(boolean ascending) {
        setBool(KEY_FOLDER_BROWSER_FOLDERS_ASCENDING, ascending);
    }

    public boolean getFolderBrowserFoldersAscending() {
        return getBool(KEY_FOLDER_BROWSER_FOLDERS_ASCENDING, true);
    }

    private static final String KEY_FOLDER_BROWSER_SHOW_FILENAMES = "folder_browser_show_file_names";

    public void setFolderBrowserShowFileNames(boolean showFileNames) {
        setBool(KEY_FOLDER_BROWSER_SHOW_FILENAMES, showFileNames);
    }

    public boolean getFolderBrowserShowFileNames() {
        return getBool(KEY_FOLDER_BROWSER_SHOW_FILENAMES, false);
    }

    private static final String KEY_LAUNCH_COUNT = "launch_count";

    public void incrementLaunchCount() {
        setInt(KEY_LAUNCH_COUNT, getLaunchCount() + 1);
    }

    public int getLaunchCount() {
        return getInt(KEY_LAUNCH_COUNT, 0);
    }

    private static final String KEY_NAG_MESSAGE_READ = "nag_message_read";

    public void setNagMessageRead() {
        setBool(KEY_NAG_MESSAGE_READ, true);
    }

    public boolean getNagMessageRead() {
        return getBool(KEY_NAG_MESSAGE_READ, false);
    }

    private static final String KEY_HAS_RATED = "has_rated";

    public void setHasRated() {
        setBool(KEY_HAS_RATED, true);
    }

    public boolean getHasRated() {
        return getBool(KEY_HAS_RATED, false);
    }

    private static final String KEY_BLUETOOTH_PAUSE_DISCONNECT = "pref_bluetooth_disconnect";
    private static final String KEY_BLUETOOTH_RESUME_CONNECT = "pref_bluetooth_connect";

    public boolean getBluetoothPauseDisconnect() {
        return getBool(KEY_BLUETOOTH_PAUSE_DISCONNECT, true);
    }

    public boolean getBluetoothResumeConnect() {
        return getBool(KEY_BLUETOOTH_RESUME_CONNECT, false);
    }

    // Themes

    public boolean getUsePalette() {
        return getBool(KEY_PREF_PALETTE, true);
    }

    public boolean getUsePaletteNowPlayingOnly() {
        return getBool(KEY_PREF_PALETTE_NOW_PLAYING_ONLY, false);
    }

    public boolean getTintNavBar() {
        return getBool(KEY_PREF_NAV_BAR, false);
    }

    public void storePrimaryColor(int color) {
        setInt(KEY_PREF_PRIMARY_COLOR, color);
    }

    public int getPrimaryColor() {
        return getInt(KEY_PREF_PRIMARY_COLOR, -1);
    }

    public void storeAccentColor(int color) {
        setInt(KEY_PREF_ACCENT_COLOR, color);
    }

    public int getAccentColor() {
        return getInt(KEY_PREF_ACCENT_COLOR, -1);
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
        return getBool(KEY_DOWNLOAD_AUTOMATICALLY, false);
    }

    public boolean preferLastFM() {
        return getBool(KEY_PREFER_LAST_FM, true);
    }

    public boolean preferEmbeddedArtwork() {
        return getBool(KEY_PREFER_EMBEDDED_ARTWORK, false);
    }

    public boolean useGmailPlaceholders() {
        return getBool(KEY_USE_GMAIL_PLACEHOLDERS, false);
    }

    public boolean showArtworkInQueue() {
        return getBool(KEY_QUEUE_ARTWORK, true);
    }

    public boolean cropArtwork() {
        return getBool(KEY_CROP_ARTWORK, false);
    }

    public boolean ignoreMediaStoreArtwork() {
        return getBool(KEY_IGNORE_MEDIASTORE_ART, false);
    }

    public boolean ignoreFolderArtwork() {
        return getBool(KEY_IGNORE_FOLDER_ARTWORK, false);
    }

    public boolean ignoreEmbeddedArtwork() {
        return getBool(KEY_IGNORE_EMBEDDED_ARTWORK, false);
    }

    private static final String KEY_PLAYLIST_IGNORE_DUPLICATES = "pref_ignore_duplicates";

    public boolean ignoreDuplicates() {
        return getBool(KEY_PLAYLIST_IGNORE_DUPLICATES, false);
    }

    public void setIgnoreDuplicates(boolean ignoreDuplicates) {
        setBool(KEY_PLAYLIST_IGNORE_DUPLICATES, ignoreDuplicates);
    }

    // Search settings

    private static final String KEY_SEARCH_FUZZY = "search_fuzzy";

    public void setSearchFuzzy(boolean fuzzy) {
        setBool(KEY_SEARCH_FUZZY, fuzzy);
    }

    public boolean getSearchFuzzy() {
        return getBool(KEY_SEARCH_FUZZY, true);
    }

    private static final String KEY_SEARCH_ARTISTS = "search_artists";

    public void setSearchArtists(boolean searchArtists) {
        setBool(KEY_SEARCH_ARTISTS, searchArtists);
    }

    public boolean getSearchArtists() {
        return getBool(KEY_SEARCH_ARTISTS, true);
    }

    private static final String KEY_SEARCH_ALBUMS = "search_albums";

    public void setSearchAlbums(boolean searchAlbums) {
        setBool(KEY_SEARCH_ALBUMS, searchAlbums);
    }

    public boolean getSearchAlbums() {
        return getBool(KEY_SEARCH_ALBUMS, true);
    }


    // Changelog

    private static final String KEY_VERSION_CODE = "version_code";

    public void setVersionCode() {
        setInt(KEY_VERSION_CODE, BuildConfig.VERSION_CODE);
    }

    public int getStoredVersionCode() {
        return getInt(KEY_VERSION_CODE, -1);
    }

    private static final String KEY_CHANGELOG_SHOW_ON_LAUNCH = "show_on_launch";

    public void setShowChangelogOnLaunch(boolean showOnLaunch) {
        setBool(KEY_CHANGELOG_SHOW_ON_LAUNCH, showOnLaunch);
    }

    public boolean getShowChangelogOnLaunch() {
        return getBool(KEY_CHANGELOG_SHOW_ON_LAUNCH, true);
    }

    // Playback

    public boolean getRememberShuffle() {
        return getBool(KEY_PREF_REMEMBER_SHUFFLE, false);
    }

    public void setRememberShuffle(boolean rememberShuffle) {
        setBool(KEY_PREF_REMEMBER_SHUFFLE, rememberShuffle);
    }

    // Library Controller

    private static final String KEY_DEFAULT_PAGE = "default_page";

    @CategoryItem.Type
    public int getDefaultPageType() {
        return getInt(KEY_DEFAULT_PAGE, CategoryItem.Type.ARTISTS);
    }

    public void setDefaultPageType(@CategoryItem.Type int type) {
        setInt(KEY_DEFAULT_PAGE, type);
    }
}
