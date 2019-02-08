package com.simplecity.amp_library.search;

import android.support.annotation.NonNull;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.Song;
import java.util.List;

public class SearchResult {

    @NonNull
    List<AlbumArtist> albumArtists;

    @NonNull
    List<Album> albums;

    @NonNull
    List<Song> songs;

    public SearchResult(@NonNull List<AlbumArtist> albumArtists, @NonNull List<Album> albums, @NonNull List<Song> songs) {
        this.albumArtists = albumArtists;
        this.albums = albums;
        this.songs = songs;
    }
}
