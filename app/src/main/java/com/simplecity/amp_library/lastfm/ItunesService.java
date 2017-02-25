package com.simplecity.amp_library.lastfm;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ItunesService {

    @GET("?entity=album&limit=1")
    Call<ItunesResult> getItunesAlbumResult(@Query("term") String term);
}
