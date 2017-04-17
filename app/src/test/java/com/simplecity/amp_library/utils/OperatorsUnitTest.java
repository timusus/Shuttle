package com.simplecity.amp_library.utils;


import android.database.Cursor;

import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.Song;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * This is a separate from {@link ShuttleUtilsPowerMockTest} for the time being as PowerMock and Robolectric
 * can't work together until Robolectric 3.3 is released:
 * https://github.com/robolectric/robolectric/wiki/Using-PowerMock
 * <p>
 * Use the devDebug build variant to run.
 */
@Config(sdk = 23, manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)

public class OperatorsUnitTest {

    @Test
    public void testSongsToAlbums() throws Exception {

        // Test to ensure that only one album is generated under the following conditions:

        // 1. The album ID is the same for all songs.
        // 2. The album artist is the same for all songs.
        // 3. The artist differs for some songs.

        List<Song> songs = new ArrayList<>();

        for (int i = 0; i < 15; i++) {
            Song song = new Song(mock(Cursor.class));
            song.id = i;
            song.name = "Song " + i;
            song.albumId = 0;
            song.artistId = 0;
            song.artistName = "Artist 1";
            song.albumArtistName = "Album Artist 1";
            songs.add(song);
        }

        for (int i = 0; i < 15; i++) {
            Song song = new Song(mock(Cursor.class));
            song.id = i + 14;
            song.name = "Song " + i;
            song.albumId = 0;
            song.artistId = 1;
            song.artistName = "Artist 2";
            song.albumArtistName = "Album Artist 1";
            songs.add(song);
        }

        assertThat(Operators.songsToAlbums(songs).size()).isEqualTo(1);
    }

    @Test
    public void testAlbumsToAlbumArtists() throws Exception {

        // Test to ensure that only 2 album artists are generated under the following conditions:

        // 1. There are 3 albums in total.
        // 1. Two albums share the same album artist.

        List<Album> albums = new ArrayList<>();

        albums.add(new Album.Builder()
                .id(0)
                .name("Album 1")
                .albumArtist("Album Artist 1")
                .build());

        albums.add(new Album.Builder()
                .id(1)
                .name("Album 2")
                .albumArtist("Album Artist 1")
                .build());

        albums.add(new Album.Builder()
                .id(2)
                .name("Album 3")
                .albumArtist("Album Artist 2")
                .build());

        assertThat(Operators.albumsToAlbumArtists(albums).size()).isEqualTo(2);
    }
}