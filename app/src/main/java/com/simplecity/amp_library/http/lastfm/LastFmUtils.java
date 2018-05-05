package com.simplecity.amp_library.http.lastfm;

import java.util.List;

@SuppressWarnings("WeakerAccess")
public class LastFmUtils {

    private static final String TAG = "LastFmUtils";

    public static String getBestImageUrl(List<LastFmImage> images) {
        String[] sizes = new String[] {
                "mega", "extralarge", "large", "medium"
        };

        for (String size : sizes) {
            LastFmImage image = findSize(images, size);
            if (image != null) {
                if (image.url != null) {
                    // Last.fm are now returning 300x300 images for most image sizes. Thanks for documenting
                    // your API changes \s
                    // It looks like they've also implemented an image resizer. Replace any '123x456', or '123s'
                    // with 1080s
                    image.url = image.url.replaceFirst("/\\d*s(/|$)|/\\d*x\\d*(/|$)", "/1080s/");
                }
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