package com.simplecity.amp_library.ui.screens.tagger;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;
import com.afollestad.materialdialogs.MaterialDialog;
import com.annimon.stream.Stream;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.utils.CustomMediaScanner;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import dagger.android.support.AndroidSupportInjection;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

public class TaggerDialog extends DialogFragment {

    public static final String TAG = "TaggerDialog";

    public static final int DOCUMENT_TREE_REQUEST_CODE = 901;

    public static final String ARG_MODEL = "model";

    private MaterialDialog materialDialog;

    private boolean hasCheckedPermissions;

    private AlbumArtist albumArtist;
    private Album album;
    private Song song;

    List<String> originalSongPaths = new ArrayList<>();
    private List<DocumentFile> documentFiles = new ArrayList<>();

    private boolean showAlbum = true;
    private boolean showTrack = true;

    private EditText albumArtistEditText;
    private EditText artistEditText;
    private EditText albumEditText;
    private EditText titleEditText;
    private EditText genreEditText;
    private EditText yearEditText;
    private EditText trackEditText;
    private EditText trackTotalEditText;
    private EditText discEditText;
    private EditText discTotalEditText;
    private EditText lyricsEditText;
    private EditText commentEditText;

    private TextInputLayout albumInputLayout;
    private TextInputLayout titleInputLayout;
    private TextInputLayout trackInputLayout;
    private TextInputLayout discInputLayout;
    private TextInputLayout lyricsInputLayout;
    private TextInputLayout commentInputLayout;

    private String artistName;
    private String albumName;
    private String albumArtistName;
    private String title;
    private String genre;
    private String year;
    private String track;
    private String trackTotal;
    private String disc;
    private String discTotal;
    private String lyrics;
    private String comment;

    @Inject
    SettingsManager settingsManager;

    public static TaggerDialog newInstance(Serializable model) {

        Bundle args = new Bundle();
        args.putSerializable(ARG_MODEL, model);
        TaggerDialog fragment = new TaggerDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Serializable model = getArguments().getSerializable(ARG_MODEL);
        if (model instanceof AlbumArtist) {
            albumArtist = (AlbumArtist) model;

            originalSongPaths = Stream.of(albumArtist.albums)
                    .flatMap(value -> Stream.of(value.paths))
                    .toList();
            showAlbum = false;
            showTrack = false;
        } else if (model instanceof Album) {
            album = (Album) model;
            originalSongPaths = album.paths;
            showTrack = false;
        } else if (model instanceof Song) {
            song = (Song) model;
            originalSongPaths.add(song.path);
        }

        if (originalSongPaths == null || originalSongPaths.isEmpty()) {
            dismiss();

            //Todo: refine & extract
            Toast.makeText(getContext(), R.string.tag_retrieve_error, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        @SuppressLint("InflateParams")
        View customView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_tagger, null, false);

        setupViews(customView);

        populateViews();

        materialDialog = new MaterialDialog.Builder(getContext())
                .title(R.string.edit_tags)
                .customView(customView, false)
                .positiveText(R.string.save)
                .onPositive((dialog, which) -> saveTags())
                .negativeText(R.string.close)
                .onNegative((dialog, which) -> dismiss())
                .autoDismiss(false)
                .build();

        return materialDialog;
    }

    private void setupViews(View rootView) {

        titleEditText = rootView.findViewById(R.id.new_track_name);
        titleInputLayout = getParent(titleEditText);

        albumEditText = rootView.findViewById(R.id.new_album_name);
        albumInputLayout = getParent(albumEditText);

        artistEditText = rootView.findViewById(R.id.new_artist_name);

        albumArtistEditText = rootView.findViewById(R.id.new_album_artist_name);

        genreEditText = rootView.findViewById(R.id.new_genre_name);

        yearEditText = rootView.findViewById(R.id.new_year_number);

        trackEditText = rootView.findViewById(R.id.new_track_number);
        trackInputLayout = getParent(trackEditText);

        trackTotalEditText = rootView.findViewById(R.id.new_track_total);

        discEditText = rootView.findViewById(R.id.new_disc_number);
        discInputLayout = getParent(discEditText);

        discTotalEditText = rootView.findViewById(R.id.new_disc_total);

        lyricsEditText = rootView.findViewById(R.id.new_lyrics);
        lyricsInputLayout = getParent(lyricsEditText);

        commentEditText = rootView.findViewById(R.id.new_comment);
        commentInputLayout = getParent(commentEditText);

        if (albumArtist != null || album != null) {
            titleInputLayout.setVisibility(View.GONE);
            titleEditText.setVisibility(View.GONE);
            trackInputLayout.setVisibility(View.GONE);
            trackEditText.setVisibility(View.GONE);
            trackTotalEditText.setVisibility(View.GONE);
            discInputLayout.setVisibility(View.GONE);
            discEditText.setVisibility(View.GONE);
            lyricsInputLayout.setVisibility(View.GONE);
            lyricsEditText.setVisibility(View.GONE);
            commentInputLayout.setVisibility(View.GONE);
            commentEditText.setVisibility(View.GONE);
        }

        if (albumArtist != null) {
            albumInputLayout.setVisibility(View.GONE);
            albumEditText.setVisibility(View.GONE);
        }
    }

    void populateViews() {

        if (originalSongPaths == null || originalSongPaths.isEmpty()) {
            return;
        }

        try {
            AudioFile mAudioFile = AudioFileIO.read(new File(originalSongPaths.get(0)));
            Tag tag = mAudioFile.getTag();

            if (tag == null) {
                return;
            }

            title = tag.getFirst(FieldKey.TITLE);
            albumName = tag.getFirst(FieldKey.ALBUM);
            artistName = tag.getFirst(FieldKey.ARTIST);
            try {
                albumArtistName = tag.getFirst(FieldKey.ALBUM_ARTIST);
            } catch (UnsupportedOperationException ignored) {

            }
            genre = tag.getFirst(FieldKey.GENRE);
            year = tag.getFirst(FieldKey.YEAR);
            track = tag.getFirst(FieldKey.TRACK);
            try {
                trackTotal = tag.getFirst(FieldKey.TRACK_TOTAL);
            } catch (UnsupportedOperationException ignored) {

            }
            try {
                disc = tag.getFirst(FieldKey.DISC_NO);
            } catch (UnsupportedOperationException ignored) {

            }
            try {
                discTotal = tag.getFirst(FieldKey.DISC_TOTAL);
            } catch (UnsupportedOperationException ignored) {

            }
            try {
                lyrics = tag.getFirst(FieldKey.LYRICS);
            } catch (UnsupportedOperationException ignored) {

            }
            try {
                comment = tag.getFirst(FieldKey.COMMENT);
            } catch (UnsupportedOperationException ignored) {

            }
        } catch (IOException | InvalidAudioFrameException | TagException | ReadOnlyFileException | CannotReadException e) {
            Log.e(TAG, "Failed to read tags. " + e.toString());
        }

        titleEditText.setText(title);
        titleEditText.setSelection(titleEditText.getText().length());

        albumEditText.setText(albumName);
        albumEditText.setSelection(albumEditText.getText().length());

        artistEditText.setText(artistName);
        artistEditText.setSelection(artistEditText.getText().length());

        albumArtistEditText.setText(albumArtistName);
        albumArtistEditText.setSelection(albumArtistEditText.getText().length());

        genreEditText.setText(genre);
        genreEditText.setSelection(genreEditText.getText().length());

        yearEditText.setText(String.valueOf(year));
        yearEditText.setSelection(yearEditText.getText().length());

        trackEditText.setText(String.valueOf(track));
        trackEditText.setSelection(trackEditText.getText().length());

        trackTotalEditText.setText(String.valueOf(trackTotal));
        trackTotalEditText.setSelection(trackTotalEditText.getText().length());

        discEditText.setText(String.valueOf(disc));
        discEditText.setSelection(discEditText.getText().length());

        discTotalEditText.setText(String.valueOf(discTotal));
        discTotalEditText.setSelection(discTotalEditText.getText().length());

        lyricsEditText.setText(lyrics);
        lyricsEditText.setSelection(lyricsEditText.getText().length());

        commentEditText.setText(comment);
        commentEditText.setSelection(commentEditText.getText().length());
    }

    private void saveTags() {

        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage(getString(R.string.tag_editor_check_permission));
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();

        CheckDocumentPermissionsTask task = new CheckDocumentPermissionsTask(getContext(), settingsManager, originalSongPaths, documentFiles, hasPermission -> {

            if (isResumed() && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            if (!isResumed() || getContext() == null) {
                LogUtils.logException(TAG, "Save tags returning early.. Context null or dialog not resumed.", null);
                return;
            }

            if (hasPermission) {

                final ProgressDialog saveProgressDialog = new ProgressDialog(getContext());
                saveProgressDialog.setMessage(getResources().getString(R.string.saving_tags));
                saveProgressDialog.setMax(originalSongPaths.size());
                saveProgressDialog.setIndeterminate(false);
                saveProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                saveProgressDialog.setCancelable(false);
                saveProgressDialog.show();

                TaggerTask.TagCompletionListener listener = new TaggerTask.TagCompletionListener() {
                    @Override
                    public void onSuccess() {

                        CustomMediaScanner.scanFiles(getContext(), originalSongPaths, null);

                        if (getContext() != null && isResumed()) {
                            saveProgressDialog.dismiss();

                            dismiss();
                        }
                    }

                    @Override
                    public void onFailure() {

                        if (getContext() != null && isResumed()) {
                            saveProgressDialog.dismiss();
                            Toast.makeText(getContext(), R.string.tag_error, Toast.LENGTH_LONG).show();
                            dismiss();
                        }
                    }

                    @Override
                    public void onProgress(int progress) {
                        saveProgressDialog.setProgress(progress);
                    }
                };

                TaggerTask taggerTask = new TaggerTask(getContext())
                        .showAlbum(showAlbum)
                        .showTrack(showTrack)
                        .setPaths(originalSongPaths)
                        .setDocumentfiles(documentFiles)
                        .title(titleEditText.getText().toString())
                        .album(albumEditText.getText().toString())
                        .artist(artistEditText.getText().toString())
                        .albumArtist(albumArtistEditText.getText().toString())
                        .year(yearEditText.getText().toString())
                        .track(trackEditText.getText().toString())
                        .trackTotal(trackTotalEditText.getText().toString())
                        .disc(discEditText.getText().toString())
                        .discTotal(discTotalEditText.getText().toString())
                        .lyrics(lyricsEditText.getText().toString())
                        .comment(commentEditText.getText().toString())
                        .genre(genreEditText.getText().toString())
                        .listener(listener)
                        .build();
                taggerTask.execute();
            } else {
                TaggerUtils.showChooseDocumentDialog(getContext(), (dialog1, which1) -> {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                        this.startActivityForResult(intent, DOCUMENT_TREE_REQUEST_CODE);
                    } else {
                        Toast.makeText(getContext(), R.string.R_string_toast_no_document_provider, Toast.LENGTH_LONG).show();
                    }
                }, hasCheckedPermissions);
                hasCheckedPermissions = true;
            }
        });
        task.execute();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case DOCUMENT_TREE_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    Uri treeUri = data.getData();
                    getContext().getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    settingsManager.setDocumentTreeUri(data.getData().toString());
                    saveTags();
                }
                break;
        }
    }

    public void show(FragmentManager fragmentManager) {
        show(fragmentManager, TAG);
    }

    private TextInputLayout getParent(EditText editText) {
        if (editText.getParent() instanceof TextInputLayout) {
            return (TextInputLayout) editText.getParent();
        } else if (editText.getParent() instanceof FrameLayout) {
            return (TextInputLayout) editText.getParent().getParent();
        }
        return null;
    }
}