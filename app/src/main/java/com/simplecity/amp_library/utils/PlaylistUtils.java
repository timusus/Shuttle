package com.simplecity.amp_library.utils;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SubMenu;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.crashlytics.android.Crashlytics;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.interfaces.FileType;
import com.simplecity.amp_library.model.BaseFileObject;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Query;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.MusicService;
import com.simplecity.amp_library.sql.SqlUtils;
import com.simplecity.amp_library.sql.providers.PlayCountTable;
import com.simplecity.amp_library.sql.sqlbrite.SqlBriteUtils;
import com.simplecity.amp_library.ui.views.CustomEditText;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class PlaylistUtils {

    private static final String TAG = "PlaylistUtils";

    private PlaylistUtils() {

    }

    @WorkerThread
    public static String makePlaylistName(Context context) {

        String template = context.getString(R.string.new_playlist_name_template);
        int num = 1;

        Query query = new Query.Builder()
                .uri(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI)
                .projection(new String[]{MediaStore.Audio.Playlists.NAME})
                .sort(MediaStore.Audio.Playlists.NAME)
                .build();

        Cursor cursor = SqlUtils.createQuery(context, query);

        if (cursor == null) {
            return null;
        }

        String suggestedName = String.format(template, num++);

        // Need to loop until we've made 1 full pass through without finding a match.
        // Looping more than once shouldn't happen very often, but will happen
        // if you have playlists named "New Playlist 1"/10/2/3/4/5/6/7/8/9, where
        // making only one pass would result in "New Playlist 10" being erroneously
        // picked for the new name.
        boolean done = false;
        while (!done) {
            done = true;
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                String playlistName = cursor.getString(0);
                if (playlistName.compareToIgnoreCase(suggestedName) == 0) {
                    suggestedName = String.format(template, num++);
                    done = false;
                }
                cursor.moveToNext();
            }
        }
        cursor.close();
        return suggestedName;
    }

    public static Observable<Integer> idForPlaylistObservable(Context context, String name) {
        Query query = new Query.Builder()
                .uri(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI)
                .projection(new String[]{MediaStore.Audio.Playlists._ID})
                .selection(MediaStore.Audio.Playlists.NAME + "='" + name + "'")
                .sort(MediaStore.Audio.Playlists.NAME)
                .build();

        return SqlBriteUtils.createSingleQuery(context, cursor -> cursor.getInt(0), -1, query);
    }

    public static void createM3uPlaylist(final Context context, final Playlist playlist) {

        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setIndeterminate(true);
        progressDialog.setTitle(R.string.saving_playlist);
        progressDialog.show();

        playlist.getSongsObservable(context)
                .flatMap(songs -> {
                    if (!songs.isEmpty()) {

                        File playlistFile = null;

                        if (Environment.getExternalStorageDirectory().canWrite()) {
                            File root = new File(Environment.getExternalStorageDirectory(), "Playlists/Export/");
                            if (!root.exists()) {
                                root.mkdirs();
                            }

                            File noMedia = new File(root, ".nomedia");
                            if (!noMedia.exists()) {
                                try {
                                    noMedia.createNewFile();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            String name = playlist.name.replaceAll("[^a-zA-Z0-9.-]", "_");

                            playlistFile = new File(root, name + ".m3u");

                            int i = 0;
                            while (playlistFile.exists()) {
                                i++;
                                playlistFile = new File(root, name + i + ".m3u");
                            }

                            try {
                                FileWriter fileWriter = new FileWriter(playlistFile);
                                StringBuilder body = new StringBuilder();
                                body.append("#EXTM3U\n");

                                for (Song song : songs) {
                                    body.append("#EXTINF:")
                                            .append(song.duration / 1000)
                                            .append(",")
                                            .append(song.name)
                                            .append(" - ")
                                            .append(song.artistName)
                                            .append("\n")
                                            //Todo: Use relative paths instead of absolute
                                            .append(song.path)
                                            .append("\n");
                                }
                                fileWriter.append(body);
                                fileWriter.flush();
                                fileWriter.close();

                            } catch (IOException e) {
                                Log.e(TAG, "Failed to write file: " + e);
                            }
                        }
                        return Observable.just(playlistFile);
                    } else {
                        return Observable.empty();
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnCompleted(progressDialog::dismiss)
                .subscribe(file -> {
                    if (file != null) {
                        Toast.makeText(context, String.format(context.getString(R.string.playlist_saved), file.getPath()), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(context, R.string.playlist_save_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Clears the 'most played' databse
     *
     * @param context Context
     */
    public static void clearMostPlayed(Context context) {
        context.getContentResolver().delete(PlayCountTable.URI, null, null);
    }

    interface OnSavePlaylistListener {
        void onSave(Playlist playlist);
    }

    public static void makePlaylistMenu(Context context, SubMenu sub, int fragmentGroupId) {

        Query query = new Query.Builder()
                .uri(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI)
                .projection(new String[]{
                        BaseColumns._ID,
                        MediaStore.Audio.PlaylistsColumns.NAME
                })
                .build();

        SqlBriteUtils.createQuery(context, Playlist::new, query)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(playlists -> {
                    sub.clear();
                    sub.add(fragmentGroupId, MusicUtils.Defs.NEW_PLAYLIST, 0, R.string.new_playlist);
                    for (Playlist playlist : playlists) {
                        final Intent intent = new Intent();
                        intent.putExtra(ShuttleUtils.ARG_PLAYLIST, playlist);
                        sub.add(fragmentGroupId, MusicUtils.Defs.PLAYLIST_SELECTED, 0, playlist.name).setIntent(intent);
                    }
                });
    }

    /**
     * @return true if this item is a favorite
     */
    public static Observable<Boolean> isFavorite(Context context, Song song) {
        return Observable.fromCallable(Playlist::favoritesPlaylist)
                .flatMap(playlist -> playlist.getSongsObservable(context))
                .flatMap(songs -> Observable.just(songs.contains(song)))
                .onErrorReturn(throwable -> {
                    Log.e(TAG, "isFavorite() called,  playlist null. Returning false");
                    return false;
                });
    }

    public static void addFileObjectsToPlaylist(Context context, Playlist playlist, List<BaseFileObject> fileObjects) {

        ProgressDialog progressDialog = ProgressDialog.show(context, "", context.getString(R.string.gathering_songs), false);

        long folderCount = Stream.of(fileObjects)
                .filter(value -> value.fileType == FileType.FOLDER).count();

        if (folderCount > 0) {
            progressDialog.show();
        }

        ShuttleUtils.getSongsForFileObjects(fileObjects)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    addToPlaylist(context, playlist, songs);
                });
    }

    /**
     * Method addToPlaylist.
     *
     * @param playlist Playlist
     * @param songs    List<Song>
     * @return boolean true if the playlist addition was successful
     */
    public static void addToPlaylist(Context context, Playlist playlist, List<Song> songs) {

        if (playlist == null || songs == null || songs.isEmpty()) {
            return;
        }

        playlist.getSongsObservable(context)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(existingSongs -> {

                    if (!SettingsManager.getInstance().ignoreDuplicates()) {

                        List<Song> duplicates = Stream.of(existingSongs)
                                .filter(songs::contains)
                                .distinct()
                                .collect(Collectors.toList());

                        if (!duplicates.isEmpty()) {

                            View customView = LayoutInflater.from(context).inflate(R.layout.dialog_playlist_duplicates, null);
                            TextView messageText = (TextView) customView.findViewById(R.id.textView);
                            CheckBox applyToAll = (CheckBox) customView.findViewById(R.id.applyToAll);
                            CheckBox alwaysAdd = (CheckBox) customView.findViewById(R.id.alwaysAdd);

                            if (duplicates.size() <= 1) {
                                applyToAll.setVisibility(View.GONE);
                                applyToAll.setChecked(false);
                            }

                            messageText.setText(getPlaylistRemoveString(context, duplicates.get(0)));
                            applyToAll.setText(getApplyCheckboxString(context, duplicates.size()));

                            DialogUtils.getBuilder(context)
                                    .title(R.string.dialog_title_playlist_duplicates)
                                    .customView(customView, false)
                                    .positiveText(R.string.dialog_button_playlist_duplicate_add)
                                    .autoDismiss(false)
                                    .onPositive((dialog, which) -> {
                                        //If we've only got one item, or we're applying it to all items
                                        if (duplicates.size() != 1 && !applyToAll.isChecked()) {
                                            //If we're 'adding' this song, we remove it from the 'duplicates' list
                                            duplicates.remove(0);
                                            messageText.setText(getPlaylistRemoveString(context, duplicates.get(0)));
                                            applyToAll.setText(getApplyCheckboxString(context, duplicates.size()));
                                        } else {
                                            //Add all songs to the playlist
                                            insertPlaylistItems(context, playlist, songs, existingSongs.size());
                                            SettingsManager.getInstance().setIgnoreDuplicates(alwaysAdd.isChecked());
                                            dialog.dismiss();
                                        }
                                    })
                                    .negativeText(R.string.dialog_button_playlist_duplicate_skip)
                                    .onNegative((dialog, which) -> {
                                        //If we've only got one item, or we're applying it to all items
                                        if (duplicates.size() != 1 && !applyToAll.isChecked()) {
                                            //If we're 'skipping' this song, we remove it from the 'duplicates' list,
                                            // and from the ids to be added
                                            songs.remove(duplicates.remove(0));
                                            messageText.setText(getPlaylistRemoveString(context, duplicates.get(0)));
                                            applyToAll.setText(getApplyCheckboxString(context, duplicates.size()));
                                        } else {
                                            //Remove duplicates from our set of ids
                                            Stream.of(duplicates)
                                                    .filter(songs::contains)
                                                    .forEach(songs::remove);
                                            insertPlaylistItems(context, playlist, songs, existingSongs.size());
                                            SettingsManager.getInstance().setIgnoreDuplicates(alwaysAdd.isChecked());
                                            dialog.dismiss();
                                        }
                                    })
                                    .show();
                        } else {
                            insertPlaylistItems(context, playlist, songs, existingSongs.size());
                        }
                    } else {
                        insertPlaylistItems(context, playlist, songs, existingSongs.size());
                    }
                });
    }

    private static void insertPlaylistItems(@NonNull Context context, @NonNull Playlist playlist, @NonNull List<Song> songs, int songCount) {

        if (songs.isEmpty()) {
            return;
        }

        ContentValues[] contentValues = new ContentValues[songs.size()];
        for (int i = 0, length = songs.size(); i < length; i++) {
            contentValues[i] = new ContentValues();
            contentValues[i].put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, songCount + i);
            contentValues[i].put(MediaStore.Audio.Playlists.Members.AUDIO_ID, songs.get(i).id);
        }

        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlist.id);
        if (uri != null) {
            ShuttleApplication.getInstance().getContentResolver().bulkInsert(uri, contentValues);
            PlaylistUtils.showPlaylistToast(context, songs.size());
        }
    }

    private static String getApplyCheckboxString(Context context, int count) {
        return String.format(context.getString(R.string.dialog_checkbox_playlist_duplicate_apply_all), count);
    }

    private static SpannableStringBuilder getPlaylistRemoveString(Context context, Song song) {
        SpannableStringBuilder spannableString = new SpannableStringBuilder(String.format(context.getString(R.string.dialog_message_playlist_add_duplicate), song.artistName, song.name));
        final StyleSpan boldSpan = new StyleSpan(android.graphics.Typeface.BOLD);
        spannableString.setSpan(boldSpan, 0, song.artistName.length() + song.name.length() + 3, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        return spannableString;
    }

    /**
     * Method clearPlaylist.
     *
     * @param context    Context
     * @param playlistId int
     */
    public static void clearPlaylist(Context context, int playlistId) {
        final Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
        context.getContentResolver().delete(uri, null, null);
    }

    public static Playlist createPlaylist(Context context, String name) {

        Playlist playlist = null;
        long id = -1;

        if (!TextUtils.isEmpty(name)) {
            Query query = new Query.Builder()
                    .uri(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI)
                    .projection(new String[]{MediaStore.Audio.PlaylistsColumns.NAME})
                    .selection(MediaStore.Audio.PlaylistsColumns.NAME + " = '" + name + "'")
                    .build();

            final Cursor cursor = SqlUtils.createQuery(context, query);

            if (cursor != null) {
                int count = cursor.getCount();
                cursor.close();
                if (count <= 0) {
                    final ContentValues values = new ContentValues(1);
                    values.put(MediaStore.Audio.PlaylistsColumns.NAME, name);
                    //Catch NPE occurring on Amazon devices.
                    try {
                        final Uri uri = context.getContentResolver().insert(
                                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                                values);
                        if (uri != null) {
                            id = Long.parseLong(uri.getLastPathSegment());
                        }
                    } catch (NullPointerException e) {
                        Crashlytics.log("Failed to create playlist: " + e.getMessage());
                    }
                }
            }
        }

        if (id != -1) {
            playlist = new Playlist(Playlist.Type.USER_CREATED, id, name, true, false, true, true, true);
        } else {
            Crashlytics.log("Failed to create playlist. Id:" + id);
        }

        return playlist;
    }

    /**
     * Removes all entries from the 'favorites' playlist
     */
    public static void clearFavorites(Context context) {
        Playlist favoritesPlaylist = Playlist.favoritesPlaylist();
        if (favoritesPlaylist.id >= 0) {
            final Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", favoritesPlaylist.id);
            context.getContentResolver().delete(uri, null, null);
        }
    }

    public static void toggleFavorite(Context context) {
        MusicUtils.isFavorite()
                .subscribeOn(Schedulers.io())
                .subscribe(isFavorite -> {
                    if (!isFavorite) {
                        addToFavorites(context);
                    } else {
                        removeFromFavorites(context);
                    }
                });
    }

    /**
     * Add a song to the favourites playlist
     */
    public static void addToFavorites(final Context context) {

        Song song = MusicUtils.getSong();

        if (song == null) {
            return;
        }

        Observable.fromCallable(Playlist::favoritesPlaylist)
                .flatMap(playlist -> playlist.getSongsObservable(context)
                        .flatMap(new Func1<List<Song>, Observable<Playlist>>() {
                            @Override
                            public Observable<Playlist> call(List<Song> songs) {
                                Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlist.id);
                                ContentValues values = new ContentValues();
                                values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, song.id);
                                values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, songs.size() + 1);
                                context.getContentResolver().insert(uri, values);
                                return Observable.just(playlist);
                            }
                        }))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(playlist -> {
                    Toast.makeText(context,
                            context.getResources().getString(R.string.song_to_favourites, song.name),
                            Toast.LENGTH_SHORT).show();

                    LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(MusicService.InternalIntents.FAVORITE_CHANGED));
                });
    }

    public static void removeFromFavorites(Context context) {

        Song song = MusicUtils.getSong();

        if (song == null) {
            return;
        }

        Observable.fromCallable(
                () -> {
                    Playlist favoritesPlaylist = Playlist.favoritesPlaylist();
                    if (favoritesPlaylist.id >= 0) {
                        final Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", favoritesPlaylist.id);
                        return context.getContentResolver().delete(uri, MediaStore.Audio.Playlists.Members.AUDIO_ID + "=" + song.id, null);
                    }
                    return 0;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(numTracksAdded -> {
                    if (numTracksAdded > 0) {
                        Toast.makeText(context,
                                context.getResources().getString(R.string.song_removed_from_favourites, song.name),
                                Toast.LENGTH_SHORT).show();
                        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(MusicService.InternalIntents.FAVORITE_CHANGED));
                    }
                });
    }

    public static void showPlaylistToast(Context context, int numTracksAdded) {
        final String message = context.getResources().getQuantityString(R.plurals.NNNtrackstoplaylist, numTracksAdded, numTracksAdded);
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void createPlaylistDialog(final Context context, List<Song> songs) {
        createPlaylistDialog(context, playlistId ->
                addToPlaylist(context, playlistId, songs));
    }

    public static void createFileObjectPlaylistDialog(final Context context, List<BaseFileObject> fileObjects) {
        createPlaylistDialog(context, playlistId ->
                addFileObjectsToPlaylist(context, playlistId, fileObjects));
    }

    private static void createPlaylistDialog(final Context context, final OnSavePlaylistListener listener) {

        View customView = LayoutInflater.from(context).inflate(R.layout.dialog_playlist, null);
        final EditText editText = (EditText) customView.findViewById(R.id.editText);

        Observable.fromCallable(() -> makePlaylistName(context))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(name -> {
                    editText.setText(name);
                    if (!TextUtils.isEmpty(name)) {
                        editText.setSelection(name.length());
                    }
                });

        MaterialDialog.Builder builder = DialogUtils.getBuilder(context)
                .customView(customView, false)
                .title(R.string.add_to_playlist)
                .positiveText(R.string.create_playlist_create_text)
                .onPositive((materialDialog, dialogAction) -> {
                    String name = editText.getText().toString();
                    if (!name.isEmpty()) {
                        idForPlaylistObservable(context, name)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(id -> {
                                    Uri uri;
                                    if (id >= 0) {
                                        uri = ContentUris.withAppendedId(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, id);
                                        clearPlaylist(context, id);
                                    } else {
                                        ContentValues values = new ContentValues(1);
                                        values.put(MediaStore.Audio.Playlists.NAME, name);
                                        try {
                                            uri = context.getContentResolver().insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, values);
                                        } catch (IllegalArgumentException | NullPointerException e) {
                                            Toast.makeText(context, R.string.dialog_create_playlist_error, Toast.LENGTH_LONG).show();
                                            uri = null;
                                        }
                                    }

                                    if (uri != null) {
                                        listener.onSave(new Playlist(Playlist.Type.USER_CREATED, Long.valueOf(uri.getLastPathSegment()), name, true, false, true, true, true));
                                    }
                                });
                    }
                })
                .negativeText(R.string.cancel);

        final Dialog dialog = builder.build();
        dialog.show();

        TextWatcher textWatcher = new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // don't care about this one
            }

            //Fixme:
            // It's probably best to just query all playlist names first, and then check against
            //that list, rather than requerying for each char change.
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String newText = editText.getText().toString();
                if (newText.trim().length() == 0) {
                    ((MaterialDialog) dialog).getActionButton(DialogAction.POSITIVE).setEnabled(false);
                } else {
                    ((MaterialDialog) dialog).getActionButton(DialogAction.POSITIVE).setEnabled(true);
                    // check if playlist with current name exists already, and warn the user if so.
                    idForPlaylistObservable(context, newText)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(id -> {
                                if (id >= 0) {
                                    ((MaterialDialog) dialog).getActionButton(DialogAction.POSITIVE).setText(R.string.create_playlist_overwrite_text);
                                } else {
                                    ((MaterialDialog) dialog).getActionButton(DialogAction.POSITIVE).setText(R.string.create_playlist_create_text);
                                }
                            });
                }
            }

            public void afterTextChanged(Editable s) {
                // don't care about this one
            }
        };

        editText.addTextChangedListener(textWatcher);
    }

    public static void renamePlaylistDialog(final Context context, final Playlist playlist, final MaterialDialog.SingleButtonCallback listener) {

        View customView = LayoutInflater.from(context).inflate(R.layout.dialog_playlist, null);
        final CustomEditText editText = (CustomEditText) customView.findViewById(R.id.editText);
        editText.setText(playlist.name);

        MaterialDialog.Builder builder = DialogUtils.getBuilder(context)
                .title(R.string.create_playlist_create_text_prompt)
                .customView(customView, false)
                .positiveText(R.string.save)
                .onPositive((materialDialog, dialogAction) -> {

                    String name = editText.getText().toString();
                    if (name.length() > 0) {
                        ContentResolver resolver = context.getContentResolver();
                        ContentValues values = new ContentValues(1);
                        values.put(MediaStore.Audio.Playlists.NAME, name);
                        resolver.update(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                                values,
                                MediaStore.Audio.Playlists._ID + "=?",
                                new String[]{Long.valueOf(playlist.id).toString()}
                        );
                        playlist.name = name;
                        Toast.makeText(context, R.string.playlist_renamed_message, Toast.LENGTH_SHORT).show();
                    }
                    if (listener != null) {
                        listener.onClick(materialDialog, dialogAction);
                    }
                })
                .negativeText(R.string.cancel);

        final MaterialDialog dialog = builder.build();

        TextWatcher textWatcher = new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // check if playlist with current name exists already, and warn the user if so.
                setSaveButton(dialog, playlist, editText.getText().toString());
            }

            public void afterTextChanged(Editable s) {
            }
        };

        editText.addTextChangedListener(textWatcher);

        dialog.show();
    }

    static void setSaveButton(MaterialDialog dialog, Playlist playlist, String typedName) {
        if (typedName.trim().length() == 0) {
            TextView button = dialog.getActionButton(DialogAction.POSITIVE);
            if (button != null) {
                button.setEnabled(false);
            }
        } else {
            TextView button = dialog.getActionButton(DialogAction.POSITIVE);
            if (button != null) {
                button.setEnabled(true);
            }
            if (playlist.id >= 0 && !playlist.name.equals(typedName)) {
                if (button != null) {
                    button.setText(R.string.create_playlist_overwrite_text);
                }
            } else {
                if (button != null) {
                    button.setText(R.string.create_playlist_create_text);
                }
            }
        }
    }
}
