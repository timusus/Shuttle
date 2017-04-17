package com.simplecity.amp_library.utils;

import com.annimon.stream.Stream;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.Song;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Operators {

    private static final String TAG = "Operators";

    public static List<Album> songsToAlbums(List<Song> songs) {

        HashMap<Long, Album> albumMap = new HashMap<>();

        for (Song song : songs) {

            //Create an album representing the album this song belongs to
            Album album = song.getAlbum();

            //Now check if there's already an equivalent album in our albumMap
            Album oldAlbum = albumMap.get(album.id);

            if (oldAlbum != null) {

                //Increment the number of songs.
                oldAlbum.numSongs++;

                //The number of discs is just the largest disc number for songs
                oldAlbum.numDiscs = Math.max(song.discNumber, oldAlbum.numDiscs);

                oldAlbum.songPlayCount += song.playCount;

                //Add any new artists
                Stream.of(album.artists)
                        .filter(artist -> !oldAlbum.artists.contains(artist))
                        .forEach(artist -> oldAlbum.artists.add(artist));

                //Add new paths
                Stream.of(album.paths)
                        .filter(path -> !oldAlbum.paths.contains(path))
                        .forEach(path -> oldAlbum.paths.add(path));

            } else {
                //Couldn't find an existing entry for this album. Add a new one.
                albumMap.put(album.id, album);
            }
        }

        return new ArrayList<>(albumMap.values());
    }

    public static List<AlbumArtist> albumsToAlbumArtists(List<Album> albums) {

        HashMap<String, AlbumArtist> albumArtistMap = new HashMap<>();

        for (Album album : albums) {

            //Create an album-artist representing the album-artist this album belongs to
            AlbumArtist albumArtist = album.getAlbumArtist();

            //Check if there's already an equivalent album-artist in our albumArtistMap
            AlbumArtist oldAlbumArtist = albumArtistMap.get(albumArtist.name);
            if (oldAlbumArtist != null) {

                //Add this album to the album artist's albums
                if (!oldAlbumArtist.albums.contains(album)) {
                    oldAlbumArtist.albums.add(album);
                }

            } else {
                albumArtistMap.put(albumArtist.name, albumArtist);
            }
        }

        return new ArrayList<>(albumArtistMap.values());
    }
}