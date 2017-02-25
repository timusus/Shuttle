package com.simplecity.amp_library.model;

public class UserSelectedArtwork {

    @ArtworkProvider.Type
    public int type;
    public String path;

    public UserSelectedArtwork(@ArtworkProvider.Type int type, String path) {
        this.type = type;
        this.path = path;
    }

    @Override
    public String toString() {
        return "UserSelectedArtwork{" +
                "type=" + type +
                ", path='" + path + '\'' +
                '}';
    }
}
