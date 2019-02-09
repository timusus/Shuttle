package com.simplecity.amp_library.utils;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;
import com.annimon.stream.Stream;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.Song;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

public class ArtworkUtils {

    private static final String TAG = "ArtworkUtils";

    //This class is never instantiated
    private ArtworkUtils() {

    }

    /**
     * Searches the parent directory of the passed in path for [cover/album/artwork].[png/jpg/jpeg]
     * using regex and returns a {@link InputStream} representing the artwork
     */
    @WorkerThread
    public static InputStream getFolderArtwork(@Nullable final String path) {

        InputStream fileInputStream = null;

        if (path != null) {
            File[] files;
            File file = new File(path);
            File parent = file.getParentFile();
            if (parent.exists() && parent.isDirectory()) {
                final Pattern pattern = Pattern.compile("(folder|cover|album).*\\.(jpg|jpeg|png)", Pattern.CASE_INSENSITIVE);
                files = parent.listFiles(file1 -> pattern.matcher(file1.getName()).matches());

                if (files.length > 0) {
                    try {
                        File artworkFile = Stream.of(files)
                                .filter(aFile -> aFile.exists() && aFile.length() > 1024)
                                .max((a, b) -> (int) (a.length() / 1024 - b.length() / 1024))
                                .get();

                        fileInputStream = getFileArtwork(artworkFile);
                    } catch (NoSuchElementException e) {
                        Log.e(TAG, "getFolderArtwork failed: " + e.toString());
                    }
                }
            }
        }
        return fileInputStream;
    }

    /**
     * Returns a FileInputStream for the given file, or null if the file is invalid
     */
    @WorkerThread
    public static InputStream getFileArtwork(@Nullable File file) {

        if (file == null || !file.exists() || file.length() < 10 * 1024) {
            return null;
        }

        FileInputStream fileInputStream = null;

        try {
            fileInputStream = new FileInputStream(file);
        } catch (FileNotFoundException | NoSuchElementException e) {
            Log.e(TAG, "getFileArtwork failed: " + e.toString());
        }

        return fileInputStream;
    }

    /**
     * Retrieves the Artwork for the given album id from the MediaStore as an {@link InputStream}
     */
    @WorkerThread
    public static InputStream getMediaStoreArtwork(Context context, long albumId) {

        Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId);

        FileInputStream fileInputStream = null;

        Cursor cursor = context
                .getContentResolver()
                .query(contentUri, new String[] { MediaStore.Audio.Albums.ALBUM_ART }, null, null, null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    File file = new File(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART)));
                    if (file.exists()) {
                        try {
                            fileInputStream = new FileInputStream(file);
                        } catch (FileNotFoundException ignored) {

                        }
                    }
                }
            } catch (NullPointerException ignored) {

            } finally {
                cursor.close();
            }
        }

        return fileInputStream;
    }

    /**
     * Retrieves the Artwork for the given {@link Song} from the MediaStore as an {@link InputStream}
     */
    @WorkerThread
    public static InputStream getMediaStoreArtwork(Context context, @NonNull Song song) {
        return getMediaStoreArtwork(context, song.albumId);
    }

    /**
     * Retrieves the Artwork for the given {@link Album} from the MediaStore as an {@link InputStream}
     */
    @WorkerThread
    public static InputStream getMediaStoreArtwork(Context context, @NonNull Album album) {
        return getMediaStoreArtwork(context, album.id);
    }

    /**
     * Retrieves the Artwork from the id3 tags of the file at the given path.
     */
    @WorkerThread
    public static InputStream getTagArtwork(@Nullable String filePath) {

        InputStream inputStream = null;

        if (filePath != null) {
            try {
                AudioFile audioFIle = AudioFileIO.read(new File(filePath));
                if (audioFIle != null) {
                    Tag tag = audioFIle.getTag();
                    if (tag != null) {
                        org.jaudiotagger.tag.datatype.Artwork artwork = tag.getFirstArtwork();
                        if (artwork != null) {
                            inputStream = new ByteArrayInputStream(artwork.getBinaryData());
                        }
                    }
                }
            } catch (CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException ignored) {

            }
        }

        return inputStream;
    }

    /**
     * Searches the parent directory of the passed in path for [cover/album/artwork].[png/jpg/jpeg]
     * using regex and returns a {@link List<File>} representing the artwork
     */
    @WorkerThread
    public static List<File> getAllFolderArtwork(@Nullable final String path) {
        List<File> fileArray = new ArrayList<>();

        if (path != null) {
            File[] files;
            File parent = new File(path).getParentFile();
            if (parent.exists() && parent.isDirectory()) {
                final Pattern pattern = Pattern.compile("(folder|cover|album).*\\.(jpg|jpeg|png)", Pattern.CASE_INSENSITIVE);
                files = parent.listFiles(file1 -> pattern.matcher(file1.getName()).matches());

                if (files.length != 0) {
                    for (File file : files) {
                        if (file.exists()) {
                            fileArray.add(file);
                        }
                    }
                }
            }
        }
        return fileArray;
    }
}