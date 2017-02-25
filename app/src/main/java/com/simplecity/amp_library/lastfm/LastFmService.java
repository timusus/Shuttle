package com.simplecity.amp_library.lastfm;

import com.simplecity.amp_library.BuildConfig;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface LastFmService {

    String METHOD_TRACK = "track.getInfo";
    String METHOD_ARTIST = "artist.getInfo";
    String METHOD_ALBUM = "album.getInfo";

    @GET("?api_key=" + BuildConfig.LASTFM_API_KEY + "&format=json&autocorrect=1" + "&method=" + METHOD_TRACK)
    Call<LastFmTrack> getLastFmTrackResult(@Query("artist") String artist, @Query("track") String track);

    @GET("?api_key=" + BuildConfig.LASTFM_API_KEY + "&format=json&autocorrect=1" + "&method=" + METHOD_ALBUM)
    Call<LastFmAlbum> getLastFmAlbumResult(@Query("artist") String artist, @Query("album") String album);

    @GET("?api_key=" + BuildConfig.LASTFM_API_KEY + "&format=json&autocorrect=1" + "&method=" + METHOD_ARTIST)
    Call<LastFmArtist> getLastFmArtistResult(@Query("artist") String artist);
}