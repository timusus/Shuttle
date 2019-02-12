package com.simplecity.amp_library.ui.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ContentProviderOperation;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.provider.DocumentFile;
import android.widget.Toast;
import com.afollestad.materialdialogs.MaterialDialog;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Supplier;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.data.Repository;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.MediaManager;
import com.simplecity.amp_library.saf.SafManager;
import com.simplecity.amp_library.sql.providers.PlayCountTable;
import com.simplecity.amp_library.utils.CustomMediaScanner;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.extensions.AlbumExtKt;
import com.simplecity.amp_library.utils.extensions.SongExtKt;
import dagger.android.support.AndroidSupportInjection;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

public class DeleteDialog extends DialogFragment implements SafManager.SafDialog.SafResultListener {

    public @interface Type {
        int ARTISTS = 0;
        int ALBUMS = 1;
        int SONGS = 2;
    }

    private static final String TAG = "DeleteDialog";

    private static final String ARG_TYPE = "type";
    private static final String ARG_DELETE_MESSAGE_ID = "delete_message_id";

    private static final String ARG_ARTISTS = "artists";
    private static final String ARG_ALBUMS = "artists";
    private static final String ARG_SONGS = "songs";

    private static final String BUNDLE_SONGS_FOR_NORMAL_DELETION = "songs_for_normal_deletion";
    private static final String BUNDLE_SONGS_FOR_SAF_DELETION = "songs_for_saf_deletion";

    @Type
    private int type;

    @StringRes
    private int deleteMessageId;

    @Inject
    MediaManager mediaManager;

    @Inject
    Repository.SongsRepository songsRepository;

    @Inject
    SettingsManager settingsManager;

    private List<AlbumArtist> artists;
    private List<Album> albums;
    private List<Song> songs;

    private List<Song> songsForNormalDeletion = new ArrayList<>();
    private List<DocumentFile> documentFilesForDeletion = new ArrayList<>();
    private List<Song> songsForSafDeletion = new ArrayList<>();

    private CompositeDisposable disposables = new CompositeDisposable();

    public interface ListArtistsRef extends Supplier<List<AlbumArtist>> {
    }

    public static DeleteDialog newInstance(@NonNull ListArtistsRef artists) {
        Bundle args = new Bundle();
        args.putInt(ARG_TYPE, Type.ARTISTS);
        args.putInt(ARG_DELETE_MESSAGE_ID, artists.get().size() == 1 ? R.string.delete_album_artist_desc : R.string.delete_album_artist_desc_multiple);
        args.putSerializable(ARG_ARTISTS, (Serializable) artists.get());
        DeleteDialog fragment = new DeleteDialog();
        fragment.setArguments(args);
        return fragment;
    }

    public interface ListAlbumsRef extends Supplier<List<Album>> {
    }

    public static DeleteDialog newInstance(@NonNull ListAlbumsRef albums) {
        Bundle args = new Bundle();
        args.putInt(ARG_TYPE, Type.ALBUMS);
        args.putInt(ARG_DELETE_MESSAGE_ID, albums.get().size() == 1 ? R.string.delete_album_desc : R.string.delete_album_desc_multiple);
        args.putSerializable(ARG_ALBUMS, (Serializable) albums.get());
        DeleteDialog fragment = new DeleteDialog();
        fragment.setArguments(args);
        return fragment;
    }

    public interface ListSongsRef extends Supplier<List<Song>> {
    }

    public static DeleteDialog newInstance(@NonNull ListSongsRef songs) {
        Bundle args = new Bundle();
        args.putInt(ARG_TYPE, Type.SONGS);
        args.putInt(ARG_DELETE_MESSAGE_ID, songs.get().size() == 1 ? R.string.delete_song_desc : R.string.delete_song_desc_multiple);
        args.putSerializable(ARG_SONGS, (Serializable) songs.get());
        DeleteDialog fragment = new DeleteDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        AndroidSupportInjection.inject(this);
        super.onCreate(savedInstanceState);

        deleteMessageId = getArguments().getInt(ARG_DELETE_MESSAGE_ID);

        type = getArguments().getInt(ARG_TYPE);
        switch (type) {
            case Type.ARTISTS:
                artists = (List<AlbumArtist>) getArguments().getSerializable(ARG_ARTISTS);
                break;
            case Type.ALBUMS:
                albums = (List<Album>) getArguments().getSerializable(ARG_ALBUMS);
                break;
            case Type.SONGS:
                songs = (List<Song>) getArguments().getSerializable(ARG_SONGS);
                break;
        }

        if (savedInstanceState != null) {
            songsForNormalDeletion = (List<Song>) savedInstanceState.getSerializable(BUNDLE_SONGS_FOR_NORMAL_DELETION);
            songsForSafDeletion = (List<Song>) savedInstanceState.getSerializable(BUNDLE_SONGS_FOR_SAF_DELETION);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        List<String> names = new ArrayList<>();
        switch (type) {
            case Type.ARTISTS:
                names = Stream.of(artists).map(albumArtist -> albumArtist.name).toList();
                break;
            case Type.ALBUMS:
                names = Stream.of(albums).map(album -> album.name).toList();
                break;
            case Type.SONGS:
                names = Stream.of(songs).map(song -> song.name).toList();
                break;
        }

        String message;
        if (names.isEmpty()) {
            message = getString(R.string.delete_songs_unknown);
        } else {
            if (names.size() > 1) {
                message = String.format(getString(deleteMessageId), Stream.of(names)
                        .map(itemName -> "\n\u2022 " + itemName)
                        .collect(Collectors.joining()) + "\n");
            } else {
                message = String.format(getString(deleteMessageId), names.get(0));
            }
        }

        return new MaterialDialog.Builder(getContext())
                .iconRes(R.drawable.ic_warning_24dp)
                .title(R.string.delete_item)
                .content(message)
                .positiveText(R.string.button_ok)
                .onPositive((materialDialog, dialogAction) -> deleteSongsOrShowSafDialog())
                .negativeText(R.string.cancel)
                .onNegative((materialDialog, dialogAction) -> dismiss())
                .autoDismiss(false)
                .build();
    }

    @Override
    public void onPause() {
        super.onPause();

        disposables.clear();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(BUNDLE_SONGS_FOR_NORMAL_DELETION, (Serializable) songsForNormalDeletion);
        outState.putSerializable(BUNDLE_SONGS_FOR_SAF_DELETION, (Serializable) songsForSafDeletion);
        super.onSaveInstanceState(outState);
    }

    @NonNull
    Single<List<Song>> getSongs() {
        switch (type) {
            case Type.ARTISTS:
                return Observable.fromIterable(artists)
                        .flatMapSingle(albumArtist -> albumArtist.getSongsSingle(songsRepository))
                        .reduce(Collections.<Song>emptyList(), (songs, songs2) -> Stream.concat(Stream.of(songs), Stream.of(songs2)).toList())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread());
            case Type.ALBUMS:
                return Observable.fromIterable(albums)
                        .flatMapSingle(album -> AlbumExtKt.getSongsSingle(album, songsRepository))
                        .reduce(Collections.<Song>emptyList(), (songs, songs2) -> Stream.concat(Stream.of(songs), Stream.of(songs2)).toList())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread());
            case Type.SONGS:
                return Single.just(songs);
        }
        return Single.just(Collections.emptyList());
    }

    public void show(FragmentManager fragmentManager) {
        show(fragmentManager, TAG);
    }

    @SuppressLint("CheckResult")
    void deleteSongsOrShowSafDialog() {
        disposables.add(getSongs().map(songs -> {
            // Keep track of the songs we want to delete, for later.
            Stream.of(songs).forEach(song -> {
                if (SafManager.getInstance(getContext(), settingsManager).requiresPermission(new File(song.path))) {
                    songsForSafDeletion.add(song);
                } else {
                    songsForNormalDeletion.add(song);
                }
            });

            boolean requiresSafDialog = false;
            if (!songsForSafDeletion.isEmpty()) {
                // We're gonna need SAF access to delete some songs.
                // We may be able to build a list of document files if the user has been here before..
                List<DocumentFile> documentFiles = SafManager.getInstance(getContext(), settingsManager).getWriteableDocumentFiles(Stream.of(songsForSafDeletion)
                        .map(song -> new File(song.path))
                        .toList());

                if (documentFiles.size() == songsForSafDeletion.size()) {
                    // We have all the document files we need. No need to show SAF dialog.
                    this.documentFilesForDeletion.addAll(documentFiles);
                } else {
                    // We'll have to show the SAF dialog
                    requiresSafDialog = true;
                }
            }
            return requiresSafDialog;
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(requiresSafDialog -> {
                    if (requiresSafDialog) {
                        if (DeleteDialog.this.isAdded()) {
                            SafManager.SafDialog.show(DeleteDialog.this);
                        } else {
                            LogUtils.logException(TAG, "Failed to delete songs.. Couldn't show SAFDialog", null);
                            Toast.makeText(getContext(), getString(R.string.delete_songs_failure_toast), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        disposables.add(deleteSongs()
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeOn(Schedulers.io())
                                .subscribe(deletedSongs -> {
                                    if (DeleteDialog.this.isAdded()) {
                                        if (deletedSongs > 0) {
                                            Toast.makeText(getContext(), getString(R.string.delete_songs_success_toast, deletedSongs), Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(getContext(), getString(R.string.delete_songs_failure_toast), Toast.LENGTH_SHORT).show();
                                        }
                                        dismiss();
                                    }
                                }, error -> {
                                    LogUtils.logException(TAG, "Failed to delete songs", error);
                                    if (DeleteDialog.this.isAdded()) {
                                        Toast.makeText(getContext(), getString(R.string.delete_songs_failure_toast), Toast.LENGTH_SHORT).show();
                                    }
                                }));
                    }
                }, error -> LogUtils.logException(TAG, "Failed to delete songs", error)));
    }

    @SuppressLint("CheckResult")
    Single<Integer> deleteSongs() {

        return Single.fromCallable(() -> {
            int deletedSongs = 0;
            if (!documentFilesForDeletion.isEmpty()) {
                deletedSongs += Stream.of(documentFilesForDeletion).filter(DocumentFile::delete).count();
                tidyUp(songsForSafDeletion);
                documentFilesForDeletion.clear();
                songsForSafDeletion.clear();
            }

            if (!songsForNormalDeletion.isEmpty()) {
                deletedSongs += Stream.of(songsForNormalDeletion).filter(SongExtKt::delete).count();
                tidyUp(songsForNormalDeletion);
                songsForNormalDeletion.clear();
            }
            return deletedSongs;
        });
    }

    void tidyUp(@NonNull List<Song> deletedSongs) {
        if (deletedSongs.isEmpty()) {
            return;
        }

        // Remove songs from current play queue
        mediaManager.removeSongsFromQueue(deletedSongs);

        // Remove songs from play count table
        ArrayList<ContentProviderOperation> operations = Stream.of(deletedSongs).map(song -> ContentProviderOperation
                .newDelete(PlayCountTable.URI)
                .withSelection(PlayCountTable.COLUMN_ID + "=" + song.id, null)
                .build())
                .collect(Collectors.toCollection(ArrayList::new));
        try {
            getContext().getContentResolver().applyBatch(PlayCountTable.AUTHORITY, operations);
        } catch (RemoteException | OperationApplicationException e) {
            e.printStackTrace();
        }

        CustomMediaScanner.scanFiles(getContext(), Stream.of(deletedSongs)
                .map(song -> song.path)
                .toList(), null);
    }

    @SuppressLint("CheckResult")
    @Override
    public void onResult(@Nullable Uri treeUri) {
        if (treeUri != null) {
            disposables.add(Completable.fromAction(() -> documentFilesForDeletion = SafManager.getInstance(getContext(), settingsManager).getWriteableDocumentFiles(Stream.of(songsForSafDeletion)
                    .map(song -> new File(song.path))
                    .toList()))
                    .andThen(deleteSongs())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(deletedSongs -> {
                        if (deletedSongs > 0) {
                            Toast.makeText(getContext(), getString(R.string.delete_songs_success_toast, deletedSongs), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), getString(R.string.delete_songs_failure_toast), Toast.LENGTH_SHORT).show();
                        }
                        dismiss();
                    }, error -> LogUtils.logException(TAG, "Failed to delete songs", error)));
        } else {
            Toast.makeText(getContext(), R.string.delete_songs_failure_toast, Toast.LENGTH_LONG).show();
            dismiss();
        }
    }
}