package com.simplecity.amp_library.model;

public class SuggestedHeader {

    public String title;
    public String subtitle;
    public Playlist playlist;

    public SuggestedHeader(String title, String subtitle, Playlist playlist) {
        this.title = title;
        this.subtitle = subtitle;
        this.playlist = playlist;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SuggestedHeader that = (SuggestedHeader) o;

        if (title != null ? !title.equals(that.title) : that.title != null) return false;
        if (subtitle != null ? !subtitle.equals(that.subtitle) : that.subtitle != null)
            return false;
        return playlist != null ? playlist.equals(that.playlist) : that.playlist == null;

    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (subtitle != null ? subtitle.hashCode() : 0);
        result = 31 * result + (playlist != null ? playlist.hashCode() : 0);
        return result;
    }
}
