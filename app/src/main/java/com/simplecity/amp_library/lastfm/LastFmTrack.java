package com.simplecity.amp_library.lastfm;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("WeakerAccess")
class LastFmTrack implements LastFmResult {

    @SerializedName("track")
    public Track track;

    public static class Track {
        @SerializedName("album")
        public TrackAlbum album;

        public static class TrackAlbum {

            @SerializedName("album")
            public TrackAlbum album;

            @SerializedName("image")
            public List<LastFmImage> images = new ArrayList<>();
        }
    }

    @Override
    public String getImageUrl() {
        return LastFmUtils.getBestImageUrl(track.album.images);
    }

}