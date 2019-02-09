package com.simplecity.amp_library.utils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.utils.sorting.SortManager;
import io.reactivex.Single;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static List<Song> albumShuffleSongs(List<Song> songs, SortManager sortManager) {

        sortManager.sortSongs(songs, SortManager.SongSort.ALBUM_NAME);

        List<Map.Entry<Long, List<Song>>> albumSongMap = Stream.of(songs)
                .groupBy(song -> song.albumId)
                .collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
                    Collections.shuffle(list);
                    return list;
                }));

        return Stream.of(albumSongMap)
                .flatMap(stringListEntry -> Stream.of(stringListEntry.getValue()))
                .toList();
    }

    public static Single<List<Song>> reduceSongSingles(List<Single<List<Song>>> singles) {
        return Single.zip(singles,
                lists -> Stream.of(lists)
                        .map(o -> (List<Song>) o)
                        .reduce((value1, value2) -> {
                            List<Song> allSongs = new ArrayList<>();
                            allSongs.addAll(value1);
                            allSongs.addAll(value2);
                            return allSongs;
                        }).orElse(Collections.emptyList()));
    }
}