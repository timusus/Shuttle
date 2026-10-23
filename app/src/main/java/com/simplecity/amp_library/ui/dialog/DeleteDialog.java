package com.simplecity.amp_library.ui.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.Context;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.MediaStore;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import android.widget.Toast;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Supplier;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.MediaManager;
import com.simplecity.amp_library.saf.SafManager;
import com.simplecity.amp_library.sql.providers.PlayCountTable;
import com.simplecity.amp_library.utils.CustomMediaScanner;
import com.simplecity.amp_library.utils.DialogUtils;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.extensions.SongExtKt;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
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

    /**
     * Holds the items to delete and the deletion in progress. The lists live here rather than in the fragment arguments,
     * which overflowed the binder transaction limit for large selections. They survive configuration changes but not
     * process death; the dialog dismisses itself if they are gone.
     */
    public static class DeleteViewModel extends ViewModel {
        List<AlbumArtist> artists;
        List<Album> albums;
        List<Song> songs;

        final List<Song> songsForNormalDeletion = new ArrayList<>();
        final List<DocumentFile> documentFilesForDeletion = new ArrayList<>();
        final List<Song> songsForSafDeletion = new ArrayList<>();

        // Songs awaiting the user's consent to a MediaStore delete request (API 30+)
        final List<Song> songsForMediaStoreDeletion = new ArrayList<>();

        boolean hasItems() {
            return artists != null || albums != null || songs != null;
        }
    }

    @Type
    private int type;

    @StringRes
    private int deleteMessageId;

    @Inject
    MediaManager mediaManager;

    // Set by newInstance() and handed to the view model in onCreate()
    private List<AlbumArtist> pendingArtists;
    private List<Album> pendingAlbums;
    private List<Song> pendingSongs;

    private DeleteViewModel viewModel;

    private CompositeDisposable disposables = new CompositeDisposable();

    private final ActivityResultLauncher<IntentSenderRequest> deleteRequestLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> onMediaStoreDeleteResult(result.getResultCode() == Activity.RESULT_OK));

    public interface ListArtistsRef extends Supplier<List<AlbumArtist>> {
    }

    public static DeleteDialog newInstance(@NonNull ListArtistsRef artists) {
        Bundle args = new Bundle();
        args.putInt(ARG_TYPE, Type.ARTISTS);
        args.putInt(ARG_DELETE_MESSAGE_ID, artists.get().size() == 1 ? R.string.delete_album_artist_desc : R.string.delete_album_artist_desc_multiple);
        DeleteDialog fragment = new DeleteDialog();
        fragment.pendingArtists = artists.get();
        fragment.setArguments(args);
        return fragment;
    }

    public interface ListAlbumsRef extends Supplier<List<Album>> {
    }

    public static DeleteDialog newInstance(@NonNull ListAlbumsRef albums) {
        Bundle args = new Bundle();
        args.putInt(ARG_TYPE, Type.ALBUMS);
        args.putInt(ARG_DELETE_MESSAGE_ID, albums.get().size() == 1 ? R.string.delete_album_desc : R.string.delete_album_desc_multiple);
        DeleteDialog fragment = new DeleteDialog();
        fragment.pendingAlbums = albums.get();
        fragment.setArguments(args);
        return fragment;
    }

    public interface ListSongsRef extends Supplier<List<Song>> {
    }

    public static DeleteDialog newInstance(@NonNull ListSongsRef songs) {
        Bundle args = new Bundle();
        args.putInt(ARG_TYPE, Type.SONGS);
        args.putInt(ARG_DELETE_MESSAGE_ID, songs.get().size() == 1 ? R.string.delete_song_desc : R.string.delete_song_desc_multiple);
        DeleteDialog fragment = new DeleteDialog();
        fragment.pendingSongs = songs.get();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ShuttleApplication.getInstance().getAppComponent()
                .inject(this);

        deleteMessageId = getArguments().getInt(ARG_DELETE_MESSAGE_ID);
        type = getArguments().getInt(ARG_TYPE);

        viewModel = new ViewModelProvider(this).get(DeleteViewModel.class);
        if (!viewModel.hasItems()) {
            viewModel.artists = pendingArtists;
            viewModel.albums = pendingAlbums;
            viewModel.songs = pendingSongs;
        }

        if (!viewModel.hasItems()) {
            // Restored after process death: the items to delete are gone.
            setShowsDialog(false);
            dismissAllowingStateLoss();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        List<String> names = new ArrayList<>();
        switch (type) {
            case Type.ARTISTS:
                names = Stream.of(viewModel.artists).map(albumArtist -> albumArtist.name).toList();
                break;
            case Type.ALBUMS:
                names = Stream.of(viewModel.albums).map(album -> album.name).toList();
                break;
            case Type.SONGS:
                names = Stream.of(viewModel.songs).map(song -> song.name).toList();
                break;
        }

        String message;
        if (names.isEmpty()) {
            message = getString(R.string.delete_songs_unknown);
        } else {
            if (names.size() > 1) {
                message = String.format(getString(deleteMessageId), Stream.of(names)
                        .map(itemName -> "\n• " + itemName)
                        .collect(Collectors.joining()) + "\n");
            } else {
                message = String.format(getString(deleteMessageId), names.get(0));
            }
        }

        return DialogUtils.getBuilder(getContext())
                .iconRes(R.drawable.ic_warning_24dp)
                .title(R.string.delete_item)
                .content(message)
                .positiveText(R.string.button_ok)
                .onPositive((materialDialog, dialogAction) -> deleteSongsOrShowSafDialog())
                .negativeText(R.string.cancel)
                .onNegative((materialDialog, dialogAction) -> dismissAllowingStateLoss())
                .autoDismiss(false)
                .build();
    }

    @Override
    public void onDestroy() {
        disposables.clear();

        super.onDestroy();
    }

    @NonNull
    Single<List<Song>> getSongs() {
        switch (type) {
            case Type.ARTISTS:
                return Observable.fromIterable(viewModel.artists)
                        .flatMapSingle(AlbumArtist::getSongsSingle)
                        .reduce(Collections.<Song>emptyList(), (songs, songs2) -> Stream.concat(Stream.of(songs), Stream.of(songs2)).toList());
            case Type.ALBUMS:
                return Observable.fromIterable(viewModel.albums)
                        .flatMapSingle(Album::getSongsSingle)
                        .reduce(Collections.<Song>emptyList(), (songs, songs2) -> Stream.concat(Stream.of(songs), Stream.of(songs2)).toList());
            case Type.SONGS:
                return Single.just(viewModel.songs);
        }
        return Single.just(Collections.emptyList());
    }

    public void show(FragmentManager fragmentManager) {
        show(fragmentManager, TAG);
    }

    void deleteSongsOrShowSafDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestMediaStoreDeletion();
            return;
        }

        disposables.add(getSongs()
                .observeOn(Schedulers.io())
                .map(songs -> {
                    // Keep track of the songs we want to delete, for later.
                    Stream.of(songs).forEach(song -> {
                        if (SafManager.getInstance().requiresPermission(new File(song.path))) {
                            viewModel.songsForSafDeletion.add(song);
                        } else {
                            viewModel.songsForNormalDeletion.add(song);
                        }
                    });

                    boolean requiresSafDialog = false;
                    if (!viewModel.songsForSafDeletion.isEmpty()) {
                        // We're gonna need SAF access to delete some songs.
                        // We may be able to build a list of document files if the user has been here before..
                        List<DocumentFile> documentFiles = SafManager.getInstance().getWriteableDocumentFiles(Stream.of(viewModel.songsForSafDeletion)
                                .map(song -> new File(song.path))
                                .toList());

                        if (documentFiles.size() == viewModel.songsForSafDeletion.size()) {
                            // We have all the document files we need. No need to show SAF dialog.
                            viewModel.documentFilesForDeletion.addAll(documentFiles);
                        } else {
                            // We'll have to show the SAF dialog
                            requiresSafDialog = true;
                        }
                    }
                    return requiresSafDialog;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(requiresSafDialog -> {
                    if (requiresSafDialog) {
                        if (isAdded()) {
                            SafManager.SafDialog.show(DeleteDialog.this);
                        } else {
                            LogUtils.logException(TAG, "Failed to delete songs.. Couldn't show SAFDialog", null);
                            showResultToast(0);
                        }
                    } else {
                        deleteSongsAndDismiss(deleteSongs());
                    }
                }, error -> LogUtils.logException(TAG, "Failed to delete songs", error)));
    }

    /**
     * From API 30 the app can't delete other apps' media files directly; ask the user to approve a MediaStore delete request.
     */
    @SuppressLint("NewApi")
    private void requestMediaStoreDeletion() {
        disposables.add(getSongs()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> {
                    if (!isAdded()) {
                        return;
                    }
                    List<Song> deletable = Stream.of(songs).filter(song -> song.id > 0).toList();
                    List<Uri> uris = Stream.of(deletable)
                            .map(song -> song.id)
                            .distinct()
                            .map(id -> ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id))
                            .toList();
                    if (uris.isEmpty()) {
                        showResultToast(0);
                        dismissAllowingStateLoss();
                        return;
                    }
                    PendingIntent pendingIntent;
                    try {
                        pendingIntent = MediaStore.createDeleteRequest(requireContext().getContentResolver(), uris);
                    } catch (IllegalArgumentException | SecurityException e) {
                        // MediaProvider rejects the whole request if any item is missing or isn't one of its audio rows.
                        LogUtils.logException(TAG, "Failed to create MediaStore delete request", e);
                        showResultToast(0);
                        dismissAllowingStateLoss();
                        return;
                    }
                    viewModel.songsForMediaStoreDeletion.clear();
                    viewModel.songsForMediaStoreDeletion.addAll(deletable);
                    deleteRequestLauncher.launch(new IntentSenderRequest.Builder(pendingIntent.getIntentSender()).build());
                }, error -> {
                    LogUtils.logException(TAG, "Failed to request deletion", error);
                    showResultToast(0);
                    dismissAllowingStateLoss();
                }));
    }

    private void onMediaStoreDeleteResult(boolean approved) {
        List<Song> songs = new ArrayList<>(viewModel.songsForMediaStoreDeletion);
        viewModel.songsForMediaStoreDeletion.clear();
        if (!approved || songs.isEmpty()) {
            dismissAllowingStateLoss();
            return;
        }
        // The system has deleted the files; clean up our own references to them.
        deleteSongsAndDismiss(Single.fromCallable(() -> {
            tidyUp(songs);
            return songs.size();
        }));
    }

    private void deleteSongsAndDismiss(Single<Integer> deletion) {
        disposables.add(deletion
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(deletedSongs -> {
                    showResultToast(deletedSongs);
                    dismissAllowingStateLoss();
                }, error -> {
                    LogUtils.logException(TAG, "Failed to delete songs", error);
                    showResultToast(0);
                    dismissAllowingStateLoss();
                }));
    }

    private void showResultToast(int deletedSongs) {
        // Application context: the dialog may already be detached when the deletion completes.
        Context context = ShuttleApplication.getInstance();
        if (deletedSongs > 0) {
            Toast.makeText(context, context.getString(R.string.delete_songs_success_toast, deletedSongs), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, context.getString(R.string.delete_songs_failure_toast), Toast.LENGTH_SHORT).show();
        }
    }

    Single<Integer> deleteSongs() {

        return Single.fromCallable(() -> {
            int deletedSongs = 0;
            if (!viewModel.documentFilesForDeletion.isEmpty()) {
                deletedSongs += Stream.of(viewModel.documentFilesForDeletion).filter(DocumentFile::delete).count();
                tidyUp(viewModel.songsForSafDeletion);
                viewModel.documentFilesForDeletion.clear();
                viewModel.songsForSafDeletion.clear();
            }

            if (!viewModel.songsForNormalDeletion.isEmpty()) {
                deletedSongs += Stream.of(viewModel.songsForNormalDeletion).filter(SongExtKt::delete).count();
                tidyUp(viewModel.songsForNormalDeletion);
                viewModel.songsForNormalDeletion.clear();
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
            // Runs on a background thread, possibly after the dialog has been detached.
            ShuttleApplication.getInstance().getContentResolver().applyBatch(PlayCountTable.AUTHORITY, operations);
        } catch (RemoteException | OperationApplicationException e) {
            e.printStackTrace();
        }

        CustomMediaScanner.scanFiles(Stream.of(deletedSongs)
                .map(song -> song.path)
                .toList(), null);
    }

    @Override
    public void onResult(@Nullable Uri treeUri) {
        if (treeUri != null) {
            deleteSongsAndDismiss(Completable.fromAction(() -> {
                viewModel.documentFilesForDeletion.clear();
                viewModel.documentFilesForDeletion.addAll(SafManager.getInstance().getWriteableDocumentFiles(Stream.of(viewModel.songsForSafDeletion)
                        .map(song -> new File(song.path))
                        .toList()));
            }).andThen(deleteSongs()));
        } else {
            showResultToast(0);
            dismissAllowingStateLoss();
        }
    }
}
