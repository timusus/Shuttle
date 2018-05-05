package com.simplecity.amp_library.glide.fetcher;

import android.support.annotation.CallSuper;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import com.simplecity.amp_library.model.ArtworkProvider;
import java.io.IOException;
import java.io.InputStream;

abstract class BaseFetcher implements DataFetcher<InputStream> {

    protected ArtworkProvider artworkProvider;

    protected InputStream stream;

    BaseFetcher(ArtworkProvider artworkProvider) {
        this.artworkProvider = artworkProvider;
    }

    protected abstract String getTag();

    protected abstract InputStream getStream() throws IOException;

    @Override
    public InputStream loadData(Priority priority) throws Exception {

        stream = getStream();
        return stream;
    }

    @Override
    @CallSuper
    public void cleanup() {
        try {
            if (stream != null) {
                stream.close();
            }
        } catch (IOException e) {
            // Ignored
        }
    }

    @Override
    public void cancel() {

    }

    @Override
    public String getId() {
        return artworkProvider.getArtworkKey() + "_" + getTag();
    }
}
