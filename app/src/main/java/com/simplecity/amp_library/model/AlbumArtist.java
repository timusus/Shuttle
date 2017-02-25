package com.simplecity.amp_library.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.annimon.stream.Stream;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.http.HttpClient;
import com.simplecity.amp_library.lastfm.ItunesResult;
import com.simplecity.amp_library.lastfm.LastFmResult;
import com.simplecity.amp_library.utils.ComparisonUtils;
import com.simplecity.amp_library.utils.DataManager;
import com.simplecity.amp_library.utils.StringUtils;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import rx.Observable;

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
    
    public Observable<List<Song>> getSongsObservable() {
        return DataManager.getInstance().getSongsObservable(song -> Stream.of(albums)
                .map(album -> album.id)
                .anyMatch(albumId -> albumId == song.albumId));
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

    public String getNumAlbumsSongsLabel() {
        return StringUtils.makeAlbumAndSongsLabel(ShuttleApplication.getInstance(), getNumAlbums(), getNumSongs());
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
    public Call<? extends LastFmResult> getLastFmArtwork() {
        return HttpClient.getInstance().lastFmService.getLastFmArtistResult(name);
    }

    @Nullable
    @Override
    public Call<ItunesResult> getItunesArtwork() {
        return null;
    }

    @Nullable
    @Override
    public InputStream getMediaStoreArtwork() {
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
        return null;
    }

    @Override
    public int compareTo(@NonNull AlbumArtist albumArtist) {
        return ComparisonUtils.compare(getSortKey(), albumArtist.getSortKey());
    }
}