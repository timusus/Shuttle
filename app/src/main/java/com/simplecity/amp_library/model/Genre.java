package com.simplecity.amp_library.model;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import com.simplecity.amp_library.sql.sqlbrite.SqlBriteUtils;

import java.io.Serializable;
import java.util.List;

import rx.Observable;

public class Genre implements Serializable {

    public long id;
    public String name;
    public int numSongs;

    public static String[] getProjection() {
        return new String[]{
                MediaStore.Audio.Genres._ID,
                MediaStore.Audio.Genres.NAME
        };
    }

    public static Query getQuery() {
        return new Query.Builder()
                .uri(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI)
                .projection(Genre.getProjection())
                .selection(null)
                .args(null)
                .sort(MediaStore.Audio.Genres.DEFAULT_SORT_ORDER)
                .build();
    }

    public Genre(Cursor cursor) {
        this.id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Genres._ID));
        this.name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Genres.NAME));
    }

    public Genre(long genreId, String name) {
        this.id = genreId;
        this.name = name;
    }

    public Observable<List<Song>> getSongsObservable(Context context) {
        Query query = Song.getQuery();
        query.uri = MediaStore.Audio.Genres.Members.getContentUri("external", id);
        return SqlBriteUtils.createQuery(context, Song::new, query);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Genre genre = (Genre) o;

        if (id != genre.id) return false;
        return name != null ? name.equals(genre.name) : genre.name == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}
