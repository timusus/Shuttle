package com.simplecity.amp_library.lastfm;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class ItunesResult {

    public List<Result> results = new ArrayList<>();

    public String getImageUrl() {
        if (results.isEmpty()) {
            return null;
        } else {
            return results.get(0).getUrl();
        }
    }

    public static class Result {
        @SerializedName("artworkUrl100")
        public String url;

        String getUrl() {
            if (url.contains("100x100")) {
                url = url.replace("100x100", "600x600");
            }
            return url;
        }
    }
}