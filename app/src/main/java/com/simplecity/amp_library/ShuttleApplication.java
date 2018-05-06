package com.simplecity.amp_library;

import android.Manifest;
import android.app.Application;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.bumptech.glide.Glide;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.core.CrashlyticsCore;
import com.google.android.libraries.cast.companionlibrary.cast.CastConfiguration;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.simplecity.amp_library.constants.Config;
import com.simplecity.amp_library.dagger.component.AppComponent;
import com.simplecity.amp_library.dagger.component.DaggerAppComponent;
import com.simplecity.amp_library.dagger.module.AppModule;
import com.simplecity.amp_library.model.Genre;
import com.simplecity.amp_library.model.Query;
import com.simplecity.amp_library.model.UserSelectedArtwork;
import com.simplecity.amp_library.sql.SqlUtils;
import com.simplecity.amp_library.sql.databases.CustomArtworkTable;
import com.simplecity.amp_library.sql.providers.PlayCountTable;
import com.simplecity.amp_library.sql.sqlbrite.SqlBriteUtils;
import com.simplecity.amp_library.utils.AnalyticsManager;
import com.simplecity.amp_library.utils.DataManager;
import com.simplecity.amp_library.utils.InputMethodManagerLeaks;
import com.simplecity.amp_library.utils.LegacyUtils;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.StringUtils;
import com.simplecity.amp_library.utils.sorting.SortManager;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;
import io.fabric.sdk.android.Fabric;
import io.reactivex.Completable;
import io.reactivex.CompletableTransformer;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.TagOptionSingleton;

public class ShuttleApplication extends Application {

    private static final String TAG = "ShuttleApplication";

    private static ShuttleApplication instance;

    public static synchronized ShuttleApplication getInstance() {
        return instance;
    }

    private boolean isUpgraded;

    public static final double VOLUME_INCREMENT = 0.05;

    private RefWatcher refWatcher;

    public HashMap<String, UserSelectedArtwork> userSelectedArtwork = new HashMap<>();

    private static Logger jaudioTaggerLogger1 = Logger.getLogger("org.jaudiotagger.audio");
    private static Logger jaudioTaggerLogger2 = Logger.getLogger("org.jaudiotagger");

    private AppComponent appComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }

        instance = this;

        if (BuildConfig.DEBUG) {
            // Traceur.enableLogging();

            // enableStrictMode();
        }

        // Todo: Remove after 2.0.5-beta2
        if (BuildConfig.VERSION_NAME.equals("2.0.5-beta2")) {
            int genreDetailSongsSortOrder = SortManager.getInstance().getGenreDetailSongsSortOrder();
            if (genreDetailSongsSortOrder == SortManager.SongSort.DEFAULT) {
                SortManager.getInstance().setGenreDetailSongsSortOrder(SortManager.SongSort.DETAIL_DEFAULT);
            }
        }

        appComponent = initDagger(this);

        refWatcher = LeakCanary.install(this);
        // workaround to fix InputMethodManager leak as suggested by LeakCanary lib
        InputMethodManagerLeaks.fixFocusedViewLeak(this);

        //Crashlytics
        CrashlyticsCore crashlyticsCore = new CrashlyticsCore.Builder()
                .disabled(BuildConfig.DEBUG)
                .build();

        Fabric.with(this,
                new Crashlytics.Builder()
                        .core(crashlyticsCore)
                        .answers(new Answers())
                        .build());

        // Firebase
        FirebaseApp.initializeApp(this);
        FirebaseAnalytics.getInstance(this);

        VideoCastManager.initialize(this,
                new CastConfiguration.Builder(Config.CHROMECAST_APP_ID)
                        .enableLockScreen()
                        .enableNotification()
                        .build()
        );

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // we cannot call setDefaultValues for multiple fragment based XML preference
        // files with readAgain flag set to false, so always check KEY_HAS_SET_DEFAULT_VALUES
        if (!prefs.getBoolean(PreferenceManager.KEY_HAS_SET_DEFAULT_VALUES, false)) {
            PreferenceManager.setDefaultValues(this, R.xml.settings_headers, true);
            PreferenceManager.setDefaultValues(this, R.xml.settings_artwork, true);
            PreferenceManager.setDefaultValues(this, R.xml.settings_blacklist, true);
            PreferenceManager.setDefaultValues(this, R.xml.settings_display, true);
            PreferenceManager.setDefaultValues(this, R.xml.settings_headset, true);
            PreferenceManager.setDefaultValues(this, R.xml.settings_scrobbling, true);
            PreferenceManager.setDefaultValues(this, R.xml.settings_themes, true);
        }

        // Turn off logging for jaudiotagger.
        jaudioTaggerLogger1.setLevel(Level.OFF);
        jaudioTaggerLogger2.setLevel(Level.OFF);

        TagOptionSingleton.getInstance().setPadNumbers(true);

        SettingsManager.getInstance().incrementLaunchCount();

        Completable.fromAction(() -> {
            Query query = new Query.Builder()
                    .uri(CustomArtworkTable.URI)
                    .projection(new String[] { CustomArtworkTable.COLUMN_ID, CustomArtworkTable.COLUMN_KEY, CustomArtworkTable.COLUMN_TYPE, CustomArtworkTable.COLUMN_PATH })
                    .build();

            SqlUtils.createActionableQuery(ShuttleApplication.this, cursor ->
                            userSelectedArtwork.put(
                                    cursor.getString(cursor.getColumnIndexOrThrow(CustomArtworkTable.COLUMN_KEY)),
                                    new UserSelectedArtwork(
                                            cursor.getInt(cursor.getColumnIndexOrThrow(CustomArtworkTable.COLUMN_TYPE)),
                                            cursor.getString(cursor.getColumnIndexOrThrow(CustomArtworkTable.COLUMN_PATH)))
                            ),
                    query);
        })
                .subscribeOn(Schedulers.io())
                .subscribe();

        Completable.timer(5, TimeUnit.SECONDS)
                .andThen(Completable.defer(this::repairMediaStoreYearFromTags))
                .doOnError(throwable -> LogUtils.logException(TAG, "Failed to update year from tags", throwable))
                .onErrorComplete()
                .subscribeOn(Schedulers.io())
                .subscribe();

        Completable.timer(10, TimeUnit.SECONDS)
                .andThen(Completable.defer(this::cleanGenres))
                .doOnError(throwable -> LogUtils.logException(TAG, "Failed to clean genres", throwable))
                .onErrorComplete()
                .subscribeOn(Schedulers.io())
                .subscribe();

        Completable.timer(15, TimeUnit.SECONDS)
                .andThen(Completable.defer(this::cleanMostPlayedPlaylist))
                .doOnError(throwable -> LogUtils.logException(TAG, "Failed to clean most played", throwable))
                .onErrorComplete()
                .subscribeOn(Schedulers.io())
                .subscribe();

        Completable.timer(20, TimeUnit.SECONDS)
                .andThen(Completable.defer(LegacyUtils::deleteOldResources))
                .doOnError(throwable -> LogUtils.logException(TAG, "Failed to delete old resources", throwable))
                .onErrorComplete()
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    CompletableTransformer doOnDelay(long delay, TimeUnit timeUnit) {
        return upstream -> Completable.timer(delay, timeUnit)
                .andThen(Completable.defer(() -> upstream))
                .doOnError(throwable -> LogUtils.logException(TAG, "Failed to delete old resources", throwable))
                .onErrorComplete()
                .subscribeOn(Schedulers.io());
    }

    public RefWatcher getRefWatcher() {
        return this.refWatcher;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        Glide.get(this).clearMemory();
    }

    public AppComponent getAppComponent() {
        return appComponent;
    }

    protected AppComponent initDagger(ShuttleApplication application) {
        return DaggerAppComponent.builder()
                .appModule(new AppModule(application))
                .build();
    }

    public static String getVersion() {
        try {
            return instance.getPackageManager().getPackageInfo(instance.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException | NullPointerException ignored) {

        }
        return "unknown";
    }

    public void setIsUpgraded(boolean isUpgraded) {
        this.isUpgraded = isUpgraded;
        AnalyticsManager.setIsUpgraded();
    }

    public boolean getIsUpgraded() {
        return isUpgraded || BuildConfig.DEBUG;
    }

    public static File getDiskCacheDir(String uniqueName) {
        try {
            // Check if media is mounted or storage is built-in, if so, try and use external cache dir
            // otherwise use internal cache dir
            String cachePath = null;
            File externalCacheDir = getInstance().getExternalCacheDir();
            if ((Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || !Environment.isExternalStorageRemovable()) && externalCacheDir != null) {
                cachePath = externalCacheDir.getPath();
            } else if (getInstance().getCacheDir() != null) {
                cachePath = getInstance().getCacheDir().getPath();
            }
            if (cachePath != null) {
                return new File(cachePath + File.separator + uniqueName);
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "getDiskCacheDir() failed. " + e.toString());
        }
        return null;
    }

    /**
     * Check items in the Most Played playlist and ensure their ids exist in the MediaStore.
     * <p>
     * If they don't, remove them from the playlist.
     */
    @NonNull
    private Completable cleanMostPlayedPlaylist() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return Completable.complete();
        }

        return Completable.fromAction(() -> {
            List<Integer> playCountIds = new ArrayList<>();

            Query query = new Query.Builder()
                    .uri(PlayCountTable.URI)
                    .projection(new String[] { PlayCountTable.COLUMN_ID })
                    .build();

            SqlUtils.createActionableQuery(this, cursor ->
                    playCountIds.add(cursor.getInt(cursor.getColumnIndex(PlayCountTable.COLUMN_ID))), query);

            List<Integer> songIds = new ArrayList<>();

            query = new Query.Builder()
                    .uri(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
                    .projection(new String[] { MediaStore.Audio.Media._ID })
                    .build();

            SqlUtils.createActionableQuery(this, cursor ->
                    songIds.add(cursor.getInt(cursor.getColumnIndex(PlayCountTable.COLUMN_ID))), query);

            StringBuilder selection = new StringBuilder(PlayCountTable.COLUMN_ID + " IN (");

            selection.append(TextUtils.join(",", Stream.of(playCountIds)
                    .filter(playCountId -> !songIds.contains(playCountId))
                    .toList()));

            selection.append(")");

            try {
                getContentResolver().delete(PlayCountTable.URI, selection.toString(), null);
            } catch (IllegalArgumentException ignored) {
            }
        });
    }

    @NonNull
    private Completable cleanGenres() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return Completable.complete();
        }

        // This observable emits a genre every 50ms. We then make a query against the genre database to populate the song count.
        // If the count is zero, then the genre can be deleted.
        // The reason for the delay is, on some slower devices, if the user has tons of genres, a ton of cursors get created.
        // If the maximum number of cursors is created (based on memory/processor speed or god knows what else), then the device
        // will start throwing CursorWindow exceptions, and the queries will slow down massively. This ends up making all queries slow.
        // This task isn't time critical, so we can afford to let it just casually do its job.
        return SqlBriteUtils.createSingleList(ShuttleApplication.getInstance(), Genre::new, Genre.getQuery())
                .flatMapObservable(Observable::fromIterable)
                .concatMap(genre -> Observable.just(genre).delay(50, TimeUnit.MILLISECONDS))
                .flatMapSingle(genre -> genre.getSongsObservable()
                        .doOnSuccess(songs -> {
                            if (songs.isEmpty()) {
                                try {
                                    getContentResolver().delete(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI, MediaStore.Audio.Genres._ID + " == " + genre.id, null);
                                } catch (IllegalArgumentException | UnsupportedOperationException ignored) {
                                    //Don't care if we couldn't delete this uri.
                                }
                            }
                        })
                ).flatMapCompletable(songs -> Completable.complete());
    }

    @NonNull
    private Completable repairMediaStoreYearFromTags() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return Completable.complete();
        }

        return DataManager.getInstance()
                .getSongsObservable(value -> value.year < 1)
                .first(Collections.emptyList())
                .map(songs -> Stream.of(songs)
                        .flatMap(song -> {
                            if (!TextUtils.isEmpty(song.path)) {
                                File file = new File(song.path);
                                if (file.exists()) {
                                    try {
                                        AudioFile audioFile = AudioFileIO.read(file);
                                        Tag tag = audioFile.getTag();
                                        if (tag != null) {
                                            String year = tag.getFirst(FieldKey.YEAR);
                                            int yearInt = StringUtils.parseInt(year);
                                            if (yearInt > 0) {
                                                song.year = yearInt;
                                                ContentValues contentValues = new ContentValues();
                                                contentValues.put(MediaStore.Audio.Media.YEAR, yearInt);

                                                return Stream.of(ContentProviderOperation
                                                        .newUpdate(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id))
                                                        .withValues(contentValues)
                                                        .build());
                                            }
                                        }
                                    } catch (CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            return Stream.empty();
                        })
                        .collect(Collectors.toCollection(ArrayList::new))
                )
                .doOnSuccess(contentProviderOperations -> getContentResolver().applyBatch(MediaStore.AUTHORITY, contentProviderOperations))
                .flatMapCompletable(songs -> Completable.complete());
    }

    private void enableStrictMode() {
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .penaltyFlashScreen()
                .build());
    }
}
