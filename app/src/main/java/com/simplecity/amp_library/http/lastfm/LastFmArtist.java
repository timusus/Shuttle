package com.simplecity.amp_library.http.lastfm;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class LastFmArtist implements LastFmResult {

    @SerializedName("artist")
    public Artist artist;

    public static class Artist {
        public String name;
        @SerializedName("image")
        public List<LastFmImage> images = new ArrayList<>();
        public Bio bio;
    }

    @Override
    public String getImageUrl() {
        if (artist == null || artist.images == null || artist.images.isEmpty()) {
            return null;
        }
        return LastFmUtils.getBestImageUrl(artist.images);
    }

    public static class Bio {
        public String summary;
    }
}