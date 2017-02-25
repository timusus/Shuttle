package com.simplecity.amp_library.glide.fetcher;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import com.simplecity.amp_library.model.ArtworkProvider;

import java.io.File;
import java.io.InputStream;

public class TypeFetcher implements DataFetcher<InputStream> {

    private static final String TAG = "MultiFetcher";

    private DataFetcher<InputStream> dataFetcher;

    private ArtworkProvider artworkProvider;

    @ArtworkProvider.Type
    private int type;

    private File file;

    public TypeFetcher(ArtworkProvider artworkProvider, @ArtworkProvider.Type int type, File file) {
        this.artworkProvider = artworkProvider;
        this.type = type;
        this.file = file;
    }

    private InputStream loadData(DataFetcher<InputStream> dataFetcher, Priority priority) {
        InputStream inputStream;
        try {
            inputStream = dataFetcher.loadData(priority);
        } catch (Exception e) {
            if (dataFetcher != null) {
                dataFetcher.cleanup();
            }
            inputStream = null;
        }
        return inputStream;
    }

    @Override
    public InputStream loadData(Priority priority) throws Exception {
        switch (type) {
            case ArtworkProvider.Type.MEDIA_STORE:
                dataFetcher = new MediaStoreFetcher(artworkProvider);
                break;
            case ArtworkProvider.Type.FOLDER:
                dataFetcher = new FolderFetcher(artworkProvider, file);
                break;
            case ArtworkProvider.Type.TAG:
                dataFetcher = new TagFetcher(artworkProvider);
                break;
            case ArtworkProvider.Type.LAST_FM:
                dataFetcher = new LastFmFetcher(artworkProvider);
                break;
            case ArtworkProvider.Type.ITUNES:
                dataFetcher = new ItunesFetcher(artworkProvider);
                break;
        }
        return loadData(dataFetcher, priority);
    }

    @Override
    public void cleanup() {
        if (dataFetcher != null) {
            dataFetcher.cleanup();
        }
    }

    @Override
    public void cancel() {
        if (dataFetcher != null) {
            dataFetcher.cancel();
        }
    }

    @Override
    public String getId() {
        String id = artworkProvider.getArtworkKey() + "_" + type;
        if (file != null) {
            id += "_" + file.hashCode();
        }
        return id;
    }
}
