package com.simplecity.amp_library.lastfm;

import java.util.List;

@SuppressWarnings("WeakerAccess")
public class LastFmUtils {

    private static final String TAG = "LastFmUtils";

    public static String getBestImageUrl(List<LastFmImage> images) {
        String[] sizes = new String[]{
                "mega", "extralarge", "large", "medium"
        };

        for (String size : sizes) {
            LastFmImage image = findSize(images, size);
            if (image != null) {
                return image.url;
            }
        }
        return null;
    }

    private static LastFmImage findSize(List<LastFmImage> images, String size) {

        for (int i = 0, imagesSize = images.size(); i < imagesSize; i++) {
            LastFmImage image = images.get(i);
            if (image.size.equals(size)) {
                return image;
            }
        }
        return null;
    }
}