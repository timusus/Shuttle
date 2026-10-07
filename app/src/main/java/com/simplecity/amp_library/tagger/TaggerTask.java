package com.simplecity.amp_library.tagger;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.model.TagUpdate;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

public class TaggerTask extends AsyncTask<Object, Integer, Boolean> {

    private static final String TAG = "TaggerTask";

    public interface TagCompletionListener {
        void onSuccess();

        /**
         * @param writtenPaths the files that were changed before the failure, so they can still be rescanned.
         */
        void onFailure(List<String> writtenPaths);

        void onProgress(int progress);
    }

    private static final String BACKUP_PREFIX = "tagger-backup-";

    private TagCompletionListener tagCompletionListener;

    private boolean showAlbum;
    private boolean showTrack;

    private List<String> paths;
    private List<Uri> writeUris;
    private List<File> tempFiles = new ArrayList<>();
    private List<String> writtenPaths = new ArrayList<>();

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
            List<Uri> writeUris, String titleText, String albumText,
            String artistText, String albumArtistText, String yearText, String trackText,
            String trackTotalText, String discText, String discTotalText, String lyricsText,
            String commentText, String genreText,
            TagCompletionListener listener) {

        this.showAlbum = showAlbum;
        this.showTrack = showTrack;
        this.paths = paths;
        this.writeUris = writeUris;
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
        boolean failed = false;

        sweepBackups();

        for (int i = 0; i < paths.size(); i++) {
            final String path = paths.get(i);
            // Files we can't write directly (SD card via SAF, or anything on API 30+ via MediaStore) are tagged as a
            // temp copy, then written back through this uri.
            final Uri writeUri = writeUris != null && i < writeUris.size() ? writeUris.get(i) : null;
            try {

                File orig = new File(path);
                if (writeUri != null && restoreKeptBackup(path, writeUri)) {
                    writtenPaths.add(path);
                }
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

                    if (writeUri != null) {
                        temp = new File(ShuttleApplication.getInstance().getFilesDir(),
                                BACKUP_PREFIX + System.currentTimeMillis() + "-" + i + "-" + orig.getName());
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

                    if (temp != null) {
                        // From here the temp copy may be the only good one: the cleanup in finally must not touch it.
                        tempFiles.remove(temp);
                        // Remember it before the original is truncated, so a kill mid-write still gets it restored.
                        rememberBackup(path, temp);
                        try {
                            writeBack(temp, writeUri);
                        } catch (IOException e) {
                            Log.e(TAG, "Failed to write tagged file back, temp copy kept at " + temp, e);
                            throw e;
                        }
                        forgetBackup(path);
                        temp.delete();
                    }
                }

                publishProgress(i);
                if (!writtenPaths.contains(path)) {
                    writtenPaths.add(path);
                }
                success = true;
            } catch (CannotWriteException | IOException | CannotReadException | InvalidAudioFrameException | TagException | ReadOnlyFileException | RuntimeException e) {
                // jaudiotagger can also throw RuntimeExceptions on malformed files
                e.printStackTrace();
                failed = true;
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

        return success && !failed;
    }

    private static SharedPreferences backupIndex() {
        return ShuttleApplication.getInstance().getSharedPreferences("tagger_backups", Context.MODE_PRIVATE);
    }

    private static void rememberBackup(String path, File backup) throws IOException {
        if (!backupIndex().edit().putString(path, backup.getAbsolutePath()).commit()) {
            throw new IOException("Couldn't record backup " + backup);
        }
    }

    private static void forgetBackup(String path) {
        backupIndex().edit().remove(path).commit();
    }

    /**
     * Drops index entries whose backup is gone, and deletes backups no entry refers to (left by earlier versions).
     */
    private static void sweepBackups() {
        SharedPreferences index = backupIndex();
        Set<String> referenced = new HashSet<>();
        SharedPreferences.Editor editor = index.edit();
        for (Map.Entry<String, ?> entry : index.getAll().entrySet()) {
            Object backupPath = entry.getValue();
            if (backupPath instanceof String && new File((String) backupPath).exists()) {
                referenced.add((String) backupPath);
            } else {
                editor.remove(entry.getKey());
            }
        }
        editor.commit();

        File[] files = ShuttleApplication.getInstance().getFilesDir().listFiles((dir, name) -> name.startsWith(BACKUP_PREFIX));
        if (files != null) {
            for (File file : files) {
                if (!referenced.contains(file.getAbsolutePath())) {
                    file.delete();
                }
            }
        }
    }

    /**
     * If an earlier write-back for this file failed, the original may be damaged: write the kept good copy back first.
     * Throws if that fails, so we don't edit the damaged file.
     *
     * @return true if the file was restored from the backup.
     */
    private static boolean restoreKeptBackup(String path, Uri writeUri) throws IOException {
        String backupPath = backupIndex().getString(path, null);
        if (backupPath == null) {
            return false;
        }
        File backup = new File(backupPath);
        if (!backup.exists()) {
            forgetBackup(path);
            return false;
        }
        if (!isDamaged(new File(path), backup)) {
            // The file is intact (the write-back finished, or something else has replaced it since): keep it.
            forgetBackup(path);
            backup.delete();
            return false;
        }
        writeBack(backup, writeUri);
        forgetBackup(path);
        backup.delete();
        return true;
    }

    private static boolean isDamaged(File file, File backup) {
        if (file.length() < backup.length()) {
            return true;
        }
        try {
            AudioFileIO.read(file);
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    private static void writeBack(File temp, Uri writeUri) throws IOException {
        IOException failure = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                // "wt" truncates, so a shorter file doesn't keep the old file's trailing bytes
                ParcelFileDescriptor pfd = ShuttleApplication.getInstance().getContentResolver().openFileDescriptor(writeUri, "wt");
                if (pfd == null) {
                    throw new IOException("Couldn't open " + writeUri);
                }
                try (ParcelFileDescriptor fd = pfd) {
                    TaggerUtils.copyFile(temp, new FileOutputStream(fd.getFileDescriptor()));
                }
                return;
            } catch (Exception e) {
                // Includes RuntimeExceptions such as SecurityException from the content provider
                failure = e instanceof IOException ? (IOException) e : new IOException(e);
            }
        }
        throw failure;
    }

    @Override
    protected void onPostExecute(Boolean success) {

        if (tagCompletionListener != null) {
            if (success) {
                tagCompletionListener.onSuccess();
            } else {
                tagCompletionListener.onFailure(writtenPaths);
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

    /**
     * @param writeUris for each path, the uri to write the tagged file back through, or null to write the file directly.
     */
    public TaggerTask setWriteUris(List<Uri> writeUris) {
        this.writeUris = writeUris;
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
        return new TaggerTask(showAlbum, showTrack, paths, writeUris, titleText,
                albumText, artistText, albumArtistText, yearText, trackText, trackTotalText,
                discText, discTotalText, lyricsText, commentText, genreText,
                tagCompletionListener);
    }
}