package com.simplecity.amp_library.tagger;

import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.support.v4.provider.DocumentFile;

import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.model.TagUpdate;
import com.simplecity.amp_library.utils.ShuttleUtils;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TaggerTask extends AsyncTask<Object, Integer, Boolean> {

    public interface TagCompletionListener {
        void onSuccess();

        void onFailure();

        void onProgress(int progress);
    }

    private TagCompletionListener tagCompletionListener;

    private boolean showAlbum;
    private boolean showTrack;

    private List<String> paths;
    private List<DocumentFile> documentFiles;
    private List<File> tempFiles = new ArrayList<>();

    private String titleText;
    private String albumText;
    private String artistText;
    private String albumArtistText;
    private String yearText;
    private String trackText;
    private String trackTotalText;
    private String discText;
    private String discTotalText;
    private String lyricsText;
    private String commentText;
    private String genreText;

    public TaggerTask() {

    }

    public TaggerTask(boolean showAlbum, boolean showTrack, List<String> paths,
                      List<DocumentFile> documentFiles, String titleText, String albumText,
                      String artistText, String albumArtistText, String yearText, String trackText,
                      String trackTotalText, String discText, String discTotalText, String lyricsText,
                      String commentText, String genreText,
                      TagCompletionListener listener) {

        this.showAlbum = showAlbum;
        this.showTrack = showTrack;
        this.paths = paths;
        this.documentFiles = documentFiles;
        this.titleText = titleText;
        this.albumText = albumText;
        this.artistText = artistText;
        this.albumArtistText = albumArtistText;
        this.yearText = yearText;
        this.trackText = trackText;
        this.trackTotalText = trackTotalText;
        this.discText = discText;
        this.discTotalText = discTotalText;
        this.lyricsText = lyricsText;
        this.commentText = commentText;
        this.genreText = genreText;
        this.tagCompletionListener = listener;
    }

    @Override
    protected Boolean doInBackground(Object... params) {

        boolean success = false;

        boolean requiresPermission = TaggerUtils.requiresPermission(paths);

        for (int i = 0; i < paths.size(); i++) {
            final String path = paths.get(i);
            try {

                File orig = new File(path);
                AudioFile audioFile = AudioFileIO.read(orig);
                Tag tag = audioFile.getTag();
                if (tag == null) {
                    break;
                }

                TagUpdate tagUpdate = new TagUpdate(tag);

                tagUpdate.softSetArtist(artistText);
                tagUpdate.softSetAlbumArtist(albumArtistText);
                tagUpdate.softSetGenre(genreText);
                tagUpdate.softSetYear(yearText);

                if (showAlbum) {
                    tagUpdate.softSetAlbum(albumText);
                    tagUpdate.softSetDiscTotal(discTotalText);
                }

                if (showTrack) {
                    tagUpdate.softSetTitle(titleText);
                    tagUpdate.softSetTrack(trackText);
                    tagUpdate.softSetTrackTotal(trackTotalText);
                    tagUpdate.softSetDisc(discText);
                    tagUpdate.softSetLyrics(lyricsText);
                    tagUpdate.softSetComment(commentText);
                }

                File temp = null;
                if (tagUpdate.hasChanged()) {

                    if (ShuttleUtils.hasLollipop() && TaggerUtils.requiresPermission(paths)) {
                        temp = new File(ShuttleApplication.getInstance().getFilesDir(), orig.getName());
                        tempFiles.add(temp);
                        TaggerUtils.copyFile(orig, temp);

                        audioFile = AudioFileIO.read(temp);
                        tag = audioFile.getTag();
                        if (tag == null) {
                            break;
                        }
                    }

                    tagUpdate.updateTag(tag);
                    AudioFileIO.write(audioFile);

                    if (ShuttleUtils.hasLollipop() && requiresPermission && temp != null) {
                        DocumentFile documentFile = documentFiles.get(i);
                        if (documentFile != null) {
                            ParcelFileDescriptor pfd = ShuttleApplication.getInstance().getContentResolver().openFileDescriptor(documentFile.getUri(), "w");
                            if (pfd != null) {
                                FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());
                                TaggerUtils.copyFile(temp, fileOutputStream);
                                pfd.close();
                            }
                            if (temp.delete()) {
                                if (tempFiles.contains(temp)) {
                                    tempFiles.remove(temp);
                                }
                            }
                        }
                    }
                }

                publishProgress(i);
                success = true;

            } catch (CannotWriteException | IOException | CannotReadException | InvalidAudioFrameException | TagException | ReadOnlyFileException e) {
                e.printStackTrace();
            } finally {
                //Try to clean up our temp files
                if (tempFiles != null && tempFiles.size() != 0) {
                    for (int j = tempFiles.size() - 1; j >= 0; j--) {
                        File file = tempFiles.get(j);
                        file.delete();
                        tempFiles.remove(j);
                    }
                }
            }
        }

        return success;
    }

    @Override
    protected void onPostExecute(Boolean success) {

        if (tagCompletionListener != null) {
            if (success) {
                tagCompletionListener.onSuccess();
            } else {
                tagCompletionListener.onFailure();
            }
        }
    }

    @Override
    protected void onProgressUpdate(Integer... object) {

        if (tagCompletionListener != null) {
            tagCompletionListener.onProgress(object[0] + 1);
        }
    }

    //Builders

    public TaggerTask setPaths(List<String> paths) {
        this.paths = paths;
        return this;
    }

    public TaggerTask setDocumentfiles(List<DocumentFile> documentFiles) {
        this.documentFiles = documentFiles;
        return this;
    }

    public TaggerTask showAlbum(boolean showAlbum) {
        this.showAlbum = showAlbum;
        return this;
    }

    public TaggerTask showTrack(boolean showTrack) {
        this.showTrack = showTrack;
        return this;
    }

    public TaggerTask title(String titleText) {
        this.titleText = titleText;
        return this;
    }

    public TaggerTask album(String albumText) {
        this.albumText = albumText;
        return this;
    }

    public TaggerTask artist(String artistText) {
        this.artistText = artistText;
        return this;
    }

    public TaggerTask albumArtist(String albumArtistText) {
        this.albumArtistText = albumArtistText;
        return this;
    }

    public TaggerTask year(String yearText) {
        this.yearText = yearText;
        return this;
    }

    public TaggerTask track(String trackText) {
        this.trackText = trackText;
        return this;
    }

    public TaggerTask trackTotal(String trackTotalText) {
        this.trackTotalText = trackTotalText;
        return this;
    }

    public TaggerTask disc(String discText) {
        this.discText = discText;
        return this;
    }

    public TaggerTask discTotal(String discTotalText) {
        this.discTotalText = discTotalText;
        return this;
    }

    public TaggerTask lyrics(String lyricsText) {
        this.lyricsText = lyricsText;
        return this;
    }

    public TaggerTask comment(String commentText) {
        this.commentText = commentText;
        return this;
    }

    public TaggerTask genre(String genreText) {
        this.genreText = genreText;
        return this;
    }

    public TaggerTask listener(TagCompletionListener listener) {
        this.tagCompletionListener = listener;
        return this;
    }

    public TaggerTask build() {
        return new TaggerTask(showAlbum, showTrack, paths, documentFiles, titleText,
                albumText, artistText, albumArtistText, yearText, trackText, trackTotalText,
                discText, discTotalText, lyricsText, commentText, genreText,
                tagCompletionListener);
    }
}