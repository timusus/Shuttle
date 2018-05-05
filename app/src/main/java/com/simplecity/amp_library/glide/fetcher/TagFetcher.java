package com.simplecity.amp_library.glide.fetcher;

import com.simplecity.amp_library.model.ArtworkProvider;
import java.io.InputStream;

public class TagFetcher extends BaseFetcher {

    String TAG = "TagFetcher";

    public TagFetcher(ArtworkProvider artworkProvider) {
        super(artworkProvider);
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    @Override
    protected InputStream getStream() {
        return artworkProvider.getTagArtwork();
    }
}