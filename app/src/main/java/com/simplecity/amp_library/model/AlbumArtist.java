package com.simplecity.amp_library.model;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.annimon.stream.Stream;
import com.simplecity.amp_library.data.Repository;
import com.simplecity.amp_library.utils.ComparisonUtils;
import com.simplecity.amp_library.utils.StringUtils;
import io.reactivex.Single;
import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AlbumArtist implements
        Serializable,
        Comparable<AlbumArtist>,
        ArtworkProvider,
        Sortable {

    public String name;

    public List<Album> albums = new ArrayList<>();

    private String sortKey;

    public AlbumArtist(String name, List<Album> albums) {
        this.name = name;
        this.albums = albums;
    }

    public Single<List<Song>> getSongsSingle(Repository.SongsRepository songsRepository) {
        return songsRepository.getSongs(song -> Stream.of(albums)
                .map(album -> album.id)
                .anyMatch(albumId -> albumId == song.albumId))
                .first(Collections.emptyList());
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
    @NonNull
    public String getArtworkKey() {
        return name;
    }

    public static class Builder {
        private String name;
        private List<Album> albums = new ArrayList<>();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder albums(List<Album> albums) {
            this.albums = albums;
            return this;
        }

        public Builder album(Album album) {
            this.albums.add(album);
            return this;
        }

        public AlbumArtist build() {
            return new AlbumArtist(this.name, this.albums);
        }
    }

    public int getNumAlbums() {
        return albums.size();
    }

    public int getNumSongs() {
        int numSongs = 0;
        for (Album album : albums) {
            numSongs += album.numSongs;
        }
        return numSongs;
    }

    @Override
    public String toString() {
        return "AlbumArtist{" +
                "name='" + name + '\'' +
                ", albums=" + albums +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AlbumArtist that = (AlbumArtist) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return albums != null ? albums.equals(that.albums) : that.albums == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (albums != null ? albums.hashCode() : 0);
        return result;
    }

    @Nullable
    @Override
    public String getRemoteArtworkUrl() {
        try {
            return "https://artwork.shuttlemusicplayer.app/api/v1/artwork?artist=" + URLEncoder.encode(name, Charset.forName("UTF-8").name());
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    @Nullable
    @Override
    public InputStream getMediaStoreArtwork(Context context) {
        return null;
    }

    @Nullable
    @Override
    public InputStream getFolderArtwork() {
        return null;
    }

    @Nullable
    @Override
    public InputStream getTagArtwork() {
        return null;
    }

    @Nullable
    @Override
    public List<File> getFolderArtworkFiles() {
        return Collections.emptyList();
    }

    @Override
    public int compareTo(@NonNull AlbumArtist albumArtist) {
        return ComparisonUtils.compare(getSortKey(), albumArtist.getSortKey());
    }
}