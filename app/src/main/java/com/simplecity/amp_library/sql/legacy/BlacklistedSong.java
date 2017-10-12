package com.simplecity.amp_library.sql.legacy;

import android.database.Cursor;

public class BlacklistedSong {

    public long id;

    public long songId;

    public BlacklistedSong(Cursor cursor) {
        id = cursor.getLong(0);
        songId = cursor.getLong(1);
    }

    @Override
    public String toString() {
        return String.valueOf(songId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BlacklistedSong that = (BlacklistedSong) o;

        return id == that.id && songId == that.songId;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (songId ^ (songId >>> 32));
        return result;
    }
}