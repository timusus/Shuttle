package com.simplecity.amp_library.tagger;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.provider.DocumentFile;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.utils.CustomMediaScanner;
import com.simplecity.amp_library.utils.DialogUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.ThemeUtils;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TaggerDialog extends DialogFragment {

    interface TextChangeListener {
        void onTextChanged(boolean isDifferent);
    }

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

    private TextInputLayout albumArtistInputLayout;
    private TextInputLayout artistInputLayout;
    private TextInputLayout albumInputLayout;
    private TextInputLayout titleInputLayout;
    private TextInputLayout genreInputLayout;
    private TextInputLayout yearInputLayout;
    private TextInputLayout trackInputLayout;
    private TextInputLayout trackTotalInputLayout;
    private TextInputLayout discInputLayout;
    private TextInputLayout discTotalInputLayout;
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

    private TextChangeListener textChangeListener = isDifferent ->
            materialDialog.getActionButton(DialogAction.POSITIVE).setEnabled(isDifferent);

    public static TaggerDialog newInstance(Serializable model) {

        Bundle args = new Bundle();
        args.putSerializable(ARG_MODEL, model);
        TaggerDialog fragment = new TaggerDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Serializable model = getArguments().getSerializable(ARG_MODEL);
        if (model instanceof AlbumArtist) {
            albumArtist = (AlbumArtist) model;

            originalSongPaths = Stream.of(albumArtist.albums)
                    .flatMap(value -> Stream.of(value.paths))
                    .collect(Collectors.toList());
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

        View customView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_tagger, null, false);

        setupViews(customView);

        populateViews();

        materialDialog = DialogUtils.getBuilder(getContext())
                .title(R.string.edit_tags)
                .customView(customView, false)
                .positiveText(R.string.save)
                .onPositive((dialog, which) ->
                        saveTags())
                .negativeText(R.string.close)
                .onNegative((dialog, which) ->
                        dismiss())
                .autoDismiss(false)
                .build();

        materialDialog.getActionButton(DialogAction.POSITIVE).setEnabled(false);

        return materialDialog;
    }

    private void setupViews(View rootView) {

        ScrollView scrollView = (ScrollView) rootView.findViewById(R.id.scrollview);
        ThemeUtils.themeScrollView(scrollView);

        titleEditText = (EditText) rootView.findViewById(R.id.new_track_name);
        titleInputLayout = getParent(titleEditText);
        ThemeUtils.themeEditText(titleEditText);

        albumEditText = (EditText) rootView.findViewById(R.id.new_album_name);
        albumInputLayout = getParent(albumEditText);
        ThemeUtils.themeEditText(albumEditText);

        artistEditText = (EditText) rootView.findViewById(R.id.new_artist_name);
        artistInputLayout = getParent(artistEditText);
        ThemeUtils.themeEditText(artistEditText);

        albumArtistEditText = (EditText) rootView.findViewById(R.id.new_album_artist_name);
        albumArtistInputLayout = getParent(albumArtistEditText);
        ThemeUtils.themeEditText(albumArtistEditText);

        genreEditText = (EditText) rootView.findViewById(R.id.new_genre_name);
        genreInputLayout = getParent(genreEditText);
        ThemeUtils.themeEditText(genreEditText);

        yearEditText = (EditText) rootView.findViewById(R.id.new_year_number);
        yearInputLayout = getParent(yearEditText);
        ThemeUtils.themeEditText(yearEditText);

        trackEditText = (EditText) rootView.findViewById(R.id.new_track_number);
        trackInputLayout = getParent(trackEditText);
        ThemeUtils.themeEditText(trackEditText);

        trackTotalEditText = (EditText) rootView.findViewById(R.id.new_track_total);
        trackTotalInputLayout = getParent(trackTotalEditText);
        ThemeUtils.themeEditText(trackTotalEditText);

        discEditText = (EditText) rootView.findViewById(R.id.new_disc_number);
        discInputLayout = getParent(discEditText);
        ThemeUtils.themeEditText(discEditText);

        discTotalEditText = (EditText) rootView.findViewById(R.id.new_disc_total);
        discTotalInputLayout = getParent(discTotalEditText);
        ThemeUtils.themeEditText(discTotalEditText);

        lyricsEditText = (EditText) rootView.findViewById(R.id.new_lyrics);
        lyricsInputLayout = getParent(lyricsEditText);
        ThemeUtils.themeEditText(lyricsEditText);

        commentEditText = (EditText) rootView.findViewById(R.id.new_comment);
        commentInputLayout = getParent(commentEditText);
        ThemeUtils.themeEditText(commentEditText);

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
            disc = tag.getFirst(FieldKey.DISC_NO);
            discTotal = tag.getFirst(FieldKey.DISC_TOTAL);
            lyrics = tag.getFirst(FieldKey.LYRICS);
            comment = tag.getFirst(FieldKey.COMMENT);

        } catch (IOException | InvalidAudioFrameException | TagException | ReadOnlyFileException | CannotReadException e) {
            Log.e(TAG, "Failed to read tags. " + e.toString());
        }

        titleEditText.setText(title);
        titleEditText.setSelection(titleEditText.getText().length());
        titleEditText.addTextChangedListener(new CustomTextWatcher(titleEditText, textChangeListener));

        albumEditText.setText(albumName);
        albumEditText.setSelection(albumEditText.getText().length());
        albumEditText.addTextChangedListener(new CustomTextWatcher(titleEditText, textChangeListener));

        artistEditText.setText(artistName);
        artistEditText.setSelection(artistEditText.getText().length());
        artistEditText.addTextChangedListener(new CustomTextWatcher(artistEditText, textChangeListener));

        albumArtistEditText.setText(albumArtistName);
        albumArtistEditText.setSelection(albumArtistEditText.getText().length());
        albumArtistEditText.addTextChangedListener(new CustomTextWatcher(albumArtistEditText, textChangeListener));

        genreEditText.setText(genre);
        genreEditText.setSelection(genreEditText.getText().length());
        genreEditText.addTextChangedListener(new CustomTextWatcher(genreEditText, textChangeListener));

        yearEditText.setText(String.valueOf(year));
        yearEditText.setSelection(yearEditText.getText().length());
        yearEditText.addTextChangedListener(new CustomTextWatcher(yearEditText, textChangeListener));

        trackEditText.setText(String.valueOf(track));
        trackEditText.setSelection(trackEditText.getText().length());
        trackEditText.addTextChangedListener(new CustomTextWatcher(trackEditText, textChangeListener));

        trackTotalEditText.setText(String.valueOf(trackTotal));
        trackTotalEditText.setSelection(trackTotalEditText.getText().length());
        trackTotalEditText.addTextChangedListener(new CustomTextWatcher(trackTotalEditText, textChangeListener));

        discEditText.setText(String.valueOf(disc));
        discEditText.setSelection(discEditText.getText().length());
        discEditText.addTextChangedListener(new CustomTextWatcher(discEditText, textChangeListener));

        discTotalEditText.setText(String.valueOf(discTotal));
        discTotalEditText.setSelection(discTotalEditText.getText().length());
        discTotalEditText.addTextChangedListener(new CustomTextWatcher(discTotalEditText, textChangeListener));

        lyricsEditText.setText(lyrics);
        lyricsEditText.setSelection(lyricsEditText.getText().length());
        lyricsEditText.addTextChangedListener(new CustomTextWatcher(lyricsEditText, textChangeListener));

        commentEditText.setText(comment);
        commentEditText.setSelection(commentEditText.getText().length());
        commentEditText.addTextChangedListener(new CustomTextWatcher(commentEditText, textChangeListener));
    }

    private void saveTags() {

        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage(getString(R.string.tag_editor_check_permission));
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();

        CheckDocumentPermissionsTask task = new CheckDocumentPermissionsTask(
                originalSongPaths, documentFiles, hasPermission -> {

            progressDialog.dismiss();

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

                        CustomMediaScanner.scanFiles(originalSongPaths, null);

                        if (getContext() != null && isResumed()) {
                            saveProgressDialog.dismiss();

                            dismiss();
                        }
                    }

                    @Override
                    public void onFailure() {

                        if (getContext() != null && isResumed()) {
                            saveProgressDialog.dismiss();

                            if (ShuttleUtils.hasKitKat() && !ShuttleUtils.hasLollipop()) {
                                Toast.makeText(getContext(), R.string.tag_error_kitkat, Toast.LENGTH_LONG).show();
                            } else if (ShuttleUtils.hasLollipop()) {
                                Toast.makeText(getContext(), R.string.tag_error_lollipop, Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getContext(), R.string.tag_edit_error, Toast.LENGTH_LONG).show();
                            }
                            dismiss();
                        }
                    }

                    @Override
                    public void onProgress(int progress) {
                        saveProgressDialog.setProgress(progress);
                    }
                };

                TaggerTask taggerTask = new TaggerTask()
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
                    if (ShuttleUtils.hasLollipop()) {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                        if (intent.resolveActivity(ShuttleApplication.getInstance().getPackageManager()) != null) {
                            getActivity().startActivityForResult(intent, DOCUMENT_TREE_REQUEST_CODE);
                        } else {
                            Toast.makeText(getContext(), R.string.R_string_toast_no_document_provider, Toast.LENGTH_LONG).show();
                        }
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
        if (ShuttleUtils.hasKitKat()) {
            switch (requestCode) {
                case DOCUMENT_TREE_REQUEST_CODE:
                    if (resultCode == Activity.RESULT_OK) {
                        Uri treeUri = data.getData();
                        ShuttleApplication.getInstance().getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        SettingsManager.getInstance().setDocumentTreeUri(data.getData().toString());
                        saveTags();
                    }
                    break;
            }
        }
    }

    public void show(FragmentManager fragmentManager) {
        show(fragmentManager, TAG);
    }

    private static class CustomTextWatcher implements TextWatcher {

        private String originalText;
        private TextChangeListener textChangeListener;

        public CustomTextWatcher(EditText editText, TextChangeListener textChangeListener) {
            this.originalText = editText.getText().toString();
            this.textChangeListener = textChangeListener;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            textChangeListener.onTextChanged(!s.toString().equals(originalText));
        }
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