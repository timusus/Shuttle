package com.simplecity.amp_library.model;

import java.io.Serializable;

public class Artist implements Serializable {

    public long id;
    public String name;
    public int numAlbums;
    public int numSongs;

    public Artist(long id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Artist artist = (Artist) o;

        if (id != artist.id) return false;
        if (numAlbums != artist.numAlbums) return false;
        if (numSongs != artist.numSongs) return false;
        return name != null ? name.equals(artist.name) : artist.name == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + numAlbums;
        result = 31 * result + numSongs;
        return result;
    }
}