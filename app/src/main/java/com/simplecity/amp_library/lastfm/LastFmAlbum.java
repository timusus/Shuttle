package com.simplecity.amp_library.lastfm;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class LastFmAlbum implements LastFmResult {

    @SerializedName("album")
    public Album album;

    public static class Album {
        public String name;
        @SerializedName("image")
        public List<LastFmImage> images = new ArrayList<>();
        public Wiki wiki;
    }

    @Override
    public String getImageUrl() {
        if (album != null) {
            return LastFmUtils.getBestImageUrl(album.images);
        } else {
            return null;
        }
    }

    public static class Wiki {
        public String summary;
    }
}