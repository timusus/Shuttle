package com.simplecity.amp_library.model;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.http.HttpClient;
import com.simplecity.amp_library.lastfm.ItunesResult;
import com.simplecity.amp_library.lastfm.LastFmResult;
import com.simplecity.amp_library.sql.SqlUtils;
import com.simplecity.amp_library.sql.providers.PlayCountTable;
import com.simplecity.amp_library.sql.sqlbrite.SqlBriteUtils;
import com.simplecity.amp_library.utils.ArtworkUtils;
import com.simplecity.amp_library.utils.ComparisonUtils;
import com.simplecity.amp_library.utils.FileHelper;
import com.simplecity.amp_library.utils.StringUtils;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;

import retrofit2.Call;
import rx.Observable;

public class Song implements
        Serializable,
        Comparable<Song>,
        ArtworkProvider,
        Sortable {

    private static final String TAG = "Song";

    public long id;
    public String name;
    public String artistName;
    public long artistId;
    public String albumName;
    public long albumId;
    public long duration;
    public int year;
    public int dateAdded;
    public long playlistSongId;
    public long playlistSongPlayOrder;
    public int playCount;
    public long lastPlayed;
    public long startTime;
    private long elapsedTime = 0;
    private boolean isPaused;
    public int track;
    public int discNumber;
    public boolean isPodcast;
    public String path;
    public int bookMark;

    public String albumArtistName;

    private TagInfo tagInfo;

    private String durationLabel;
    private String bitrateLabel;
    private String sampleRateLabel;
    private String formatLabel;
    private String trackNumberLabel;
    private String discNumberLabel;
    private String fileSizeLabel;

    private String artworkKey;
    private String sortKey;

    public static String[] getProjection() {
        return new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.IS_PODCAST,
                MediaStore.Audio.Media.BOOKMARK,
                "album_artist"
        };
    }

    public static Query getQuery() {
        return new Query.Builder()
                .uri(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
                .projection(Song.getProjection())
                .selection(MediaStore.Audio.Media.IS_MUSIC + "=1 OR " + MediaStore.Audio.Media.IS_PODCAST + "=1")
                .args(null)
                .sort(MediaStore.Audio.Media.TRACK)
                .build();
    }

    public Song(Cursor cursor) {

        id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));

        name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));

        artistId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID));

        artistName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));

        albumId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));

        albumName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));

        duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));

        year = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR));

        track = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK));

        if (track >= 1000) {
            discNumber = track / 1000;
            track = track % 1000;
        }

        dateAdded = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED));

        path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));

        albumArtistName = artistName;
        if (cursor.getColumnIndex("album_artist") != -1) {
            String albumArtist = cursor.getString(cursor.getColumnIndex("album_artist"));
            if (albumArtist != null) {
                albumArtistName = albumArtist;
            }
        }

        isPodcast = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_PODCAST)) == 1;

        bookMark = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.BOOKMARK));

        //Populate the artwork key & sort key properties if null.
        setSortKey();
        setArtworkKey();
    }

    public Genre getGenre() {
        Query query = Genre.getQuery();
        query.uri = MediaStore.Audio.Genres.getContentUriForAudioId("external", (int) id);
        return SqlUtils.createSingleQuery(ShuttleApplication.getInstance(), Genre::new, query);
    }

    public int getPlayCount(Context context) {

        int playCount = 0;

        Uri playCountUri = PlayCountTable.URI;
        Uri appendedUri = ContentUris.withAppendedId(playCountUri, id);

        if (appendedUri != null) {

            Query query = new Query.Builder()
                    .uri(appendedUri)
                    .projection(new String[]{PlayCountTable.COLUMN_ID, PlayCountTable.COLUMN_PLAY_COUNT})
                    .build();

            playCount = SqlUtils.createSingleQuery(context, cursor ->
                    cursor.getInt(cursor.getColumnIndex(PlayCountTable.COLUMN_PLAY_COUNT)), 0, query);
        }

        return playCount;
    }

    public void setStartTime() {
        startTime = System.currentTimeMillis();
    }

    /**
     * Checks whether this track has been played for at least 75% of it's duration
     *
     * @return true if the elapsed time is > 75% of the duration false otherwise
     */
    public boolean hasPlayed() {
        return getElapsedTime() != 0 && ((float) getElapsedTime() / (float) duration) > 0.75f;
    }

    /**
     * Sets this track as 'paused' to make sure the elapsed time doesn't continue to increase
     */
    public void setPaused() {
        elapsedTime = elapsedTime + System.currentTimeMillis() - startTime;
        isPaused = true;
    }

    /**
     * Sets this track as 'resumed' to resume incrementing the elapsed time
     */
    public void setResumed() {
        startTime = System.currentTimeMillis();
        isPaused = false;
    }

    /**
     * Gets the elapsed time of this track (in millis)
     *
     * @return the elapsed time of this track (in millis)
     */
    private long getElapsedTime() {
        if (isPaused) {
            return elapsedTime;
        } else {
            return elapsedTime + System.currentTimeMillis() - startTime;
        }
    }

    public String getDurationLabel() {
        if (durationLabel == null) {
            durationLabel = StringUtils.makeTimeString(ShuttleApplication.getInstance(), duration / 1000);
        }
        return durationLabel;
    }

    public TagInfo getTagInfo() {
        if (tagInfo == null) {
            tagInfo = new TagInfo(path);
        }
        return tagInfo;
    }

    public String getBitrateLabel() {
        if (bitrateLabel == null) {
            bitrateLabel = getTagInfo().bitrate + ShuttleApplication.getInstance().getString(R.string.song_info_bitrate_suffix);
        }
        return bitrateLabel;
    }

    public String getSampleRateLabel() {
        if (sampleRateLabel == null) {
            sampleRateLabel = ((float) getTagInfo().sampleRate) / 1000 + ShuttleApplication.getInstance().getString(R.string.song_info_sample_rate_suffix);
        }
        return sampleRateLabel;
    }

    public String getFormatLabel() {
        if (formatLabel == null) {
            formatLabel = getTagInfo().format;
        }
        return formatLabel;
    }

    public String getTrackNumberLabel() {
        if (trackNumberLabel == null) {
            if (track == -1) {
                trackNumberLabel = String.valueOf(getTagInfo().trackNumber);
            } else {
                trackNumberLabel = String.valueOf(track);
            }
        }
        return trackNumberLabel;
    }

    public String getDiscNumberLabel() {
        if (discNumberLabel == null) {
            if (discNumber == -1) {
                discNumberLabel = String.valueOf(getTagInfo().discNumber);
            } else {
                discNumberLabel = String.valueOf(discNumber);
            }
        }
        return discNumberLabel;
    }

    public String getFileSizeLabel() {
        if (fileSizeLabel == null) {
            if (!TextUtils.isEmpty(path)) {
                File file = new File(path);
                fileSizeLabel = FileHelper.getHumanReadableSize(file.length());
            }
        }
        return fileSizeLabel;
    }

    public Album getAlbum() {
        return new Album.Builder()
                .id(albumId)
                .name(albumName)
                .addArtist(new Artist(artistId, artistName))
                .albumArtist(albumArtistName)
                .year(year)
                .numSongs(1)
                .numDiscs(discNumber)
                .lastPlayed(lastPlayed)
                .dateAdded(dateAdded)
                .path(path)
                .songPlayCount(playCount)
                .build();
    }

    public AlbumArtist getAlbumArtist() {
        return new AlbumArtist.Builder()
                .name(albumArtistName)
                .album(getAlbum())
                .build();
    }

    public Observable<Genre> getGenreObservable(Context context) {
        Query query = Genre.getQuery();
        query.uri = MediaStore.Audio.Genres.getContentUriForAudioId("external", (int) id);
        return SqlBriteUtils.createSingleQuery(context, Genre::new, query);
    }

    public Genre getGenre(Context context) {
        Query query = Genre.getQuery();
        query.uri = MediaStore.Audio.Genres.getContentUriForAudioId("external", (int) id);
        return SqlUtils.createSingleQuery(context, Genre::new, query);
    }

    public void share(Context context) {
        final Intent intent = new Intent(Intent.ACTION_SEND).setType("audio/*");
        intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file:///" + path));
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_via)));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Song song = (Song) o;

        return id == song.id && artistId == song.artistId && albumId == song.albumId;

    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (artistId ^ (artistId >>> 32));
        result = 31 * result + (int) (albumId ^ (albumId >>> 32));
        return result;
    }

    @Override
    public String getSortKey() {
        if (sortKey == null) {
            setSortKey();
        }
        return sortKey;
    }

    @Override
    public void setSortKey() {
        sortKey = StringUtils.keyFor(name);
    }

    @Override
    public String getArtworkKey() {
        if (artworkKey == null) setArtworkKey();
        return artworkKey;
    }

    private void setArtworkKey() {
        artworkKey = String.format("%s_%s", albumArtistName, albumName);
    }

    @Override
    public Call<? extends LastFmResult> getLastFmArtwork() {
        return HttpClient.getInstance().lastFmService.getLastFmAlbumResult(artistName, albumName);
    }

    @Override
    public Call<ItunesResult> getItunesArtwork() {
        return HttpClient.getInstance().itunesService.getItunesAlbumResult(String.format("%s %s", artistName, albumName));
    }

    @Override
    public InputStream getMediaStoreArtwork() {
        return ArtworkUtils.getMediaStoreArtwork(this);
    }

    @Override
    public InputStream getFolderArtwork() {
        return ArtworkUtils.getFolderArtwork(path);
    }

    @Override
    public InputStream getTagArtwork() {
        return ArtworkUtils.getTagArtwork(path);
    }

    @Override
    public List<File> getFolderArtworkFiles() {
        return ArtworkUtils.getAllFolderArtwork(path);
    }

    @Override
    public String toString() {
        return "\nSong{" +
                "\nid='" + id +
                "\nname='" + name +
                "\nalbumArtistName='" + albumArtistName +
                '}';
    }

    @Nullable
    @Override
    public int compareTo(@NonNull Song song) {
        return ComparisonUtils.compare(getSortKey(), song.getSortKey());
    }

    public boolean delete() {

        if (path == null) return false;

        boolean success = false;

        File file = new File(path);
        if (file.exists()) {
            success = file.delete();
        }

        return success;
    }
}