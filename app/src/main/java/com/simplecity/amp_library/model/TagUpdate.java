package com.simplecity.amp_library.model;

import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

public class TagUpdate {

    String title;
    String album;
    String artist;
    String albumArtist;
    String genre;
    String year;
    String track;
    String trackTotal;
    String disc;
    String discTotal;
    String lyrics;
    String comment;

    boolean titleHasChanged;
    boolean albumHasChanged;
    boolean artistHasChanged;
    boolean albumArtistHasChanged;
    boolean genreHasChanged;
    boolean yearHasChanged;
    boolean trackHasChanged;
    boolean trackTotalHasChanged;
    boolean discHasChanged;
    boolean discTotalHasChanged;
    boolean lyricsHasChanged;
    boolean commentHasChanged;

    public TagUpdate(Tag tag) {
        try {
            this.title = tag.getFirst(FieldKey.TITLE);
        } catch (UnsupportedOperationException ignored) {
        }
        try {
            this.album = tag.getFirst(FieldKey.ALBUM);
        } catch (UnsupportedOperationException ignored) {
        }
        try {
            this.artist = tag.getFirst(FieldKey.ARTIST);
        } catch (UnsupportedOperationException ignored) {
        }
        try {
            this.albumArtist = tag.getFirst(FieldKey.ALBUM_ARTIST);
        } catch (UnsupportedOperationException ignored) {
        }
        try {
            this.genre = tag.getFirst(FieldKey.GENRE);
        } catch (UnsupportedOperationException ignored) {
        }
        try {
            this.year = tag.getFirst(FieldKey.YEAR);
        } catch (UnsupportedOperationException ignored) {
        }
        try {
            this.track = tag.getFirst(FieldKey.TRACK);
        } catch (UnsupportedOperationException ignored) {
        }
        try {
            this.trackTotal = tag.getFirst(FieldKey.TRACK_TOTAL);
        } catch (UnsupportedOperationException ignored) {
        }
        try {
            this.disc = tag.getFirst(FieldKey.DISC_NO);
        } catch (UnsupportedOperationException ignored) {
        }
        try {
            this.discTotal = tag.getFirst(FieldKey.DISC_TOTAL);
        } catch (UnsupportedOperationException ignored) {
        }
        try {
            this.lyrics = tag.getFirst(FieldKey.LYRICS);
        } catch (UnsupportedOperationException ignored) {
        }
        try {
            this.comment = tag.getFirst(FieldKey.COMMENT);
        } catch (UnsupportedOperationException ignored) {
        }
    }

    public void softSetTitle(String title) {
        if (title == null) {
            return;
        }
        if (this.title == null || !this.title.equals(title)) {
            this.title = title;
            titleHasChanged = true;
        }
    }

    public void softSetAlbum(String album) {
        if (album == null) {
            return;
        }
        if (this.album == null || !this.album.equals(album)) {
            this.album = album;
            albumHasChanged = true;
        }
    }

    public void softSetArtist(String artist) {
        if (artist == null) {
            return;
        }
        if (this.artist == null || !this.artist.equals(artist)) {
            this.artist = artist;
            artistHasChanged = true;
        }
    }

    public void softSetAlbumArtist(String albumArtist) {
        if (albumArtist == null) {
            return;
        }
        if (this.albumArtist == null || !this.albumArtist.equals(albumArtist)) {
            this.albumArtist = albumArtist;
            albumArtistHasChanged = true;
        }
    }

    public void softSetGenre(String genre) {
        if (genre == null) {
            return;
        }
        if (this.genre == null || !this.genre.equals(genre)) {
            this.genre = genre;
            genreHasChanged = true;
        }
    }

    public void softSetYear(String year) {
        if (year == null) {
            return;
        }
        if (this.year == null || !this.year.equals(year)) {
            this.year = year;
            yearHasChanged = true;
        }
    }

    public void softSetTrack(String track) {
        if (track == null) {
            return;
        }
        if (this.track == null || !this.track.equals(track)) {
            this.track = track;
            trackHasChanged = true;
        }
    }

    public void softSetTrackTotal(String trackTotal) {
        if (trackTotal == null) {
            return;
        }
        if (this.trackTotal == null || !this.trackTotal.equals(trackTotal)) {
            this.trackTotal = trackTotal;
            trackTotalHasChanged = true;
        }
    }

    public void softSetDisc(String disc) {
        if (disc == null) {
            return;
        }
        if (this.disc == null || !this.disc.equals(disc)) {
            this.disc = disc;
            discHasChanged = true;
        }
    }

    public void softSetDiscTotal(String discTotal) {
        if (discTotal == null) {
            return;
        }
        if (this.discTotal == null || !this.discTotal.equals(discTotal)) {
            this.discTotal = discTotal;
            discTotalHasChanged = true;
        }
    }

    public void softSetLyrics(String lyrics) {
        if (lyrics == null) {
            return;
        }
        if (this.lyrics == null || !this.lyrics.equals(lyrics)) {
            this.lyrics = lyrics;
            lyricsHasChanged = true;
        }
    }

    public void softSetComment(String comment) {
        if (comment == null) {
            return;
        }
        if (this.comment == null || !this.comment.equals(comment)) {
            this.comment = comment;
            commentHasChanged = true;
        }
    }

    public boolean hasChanged() {
        return titleHasChanged || albumHasChanged || artistHasChanged ||
                albumArtistHasChanged || genreHasChanged || yearHasChanged
                || trackHasChanged || trackTotalHasChanged || discHasChanged
                || discTotalHasChanged || lyricsHasChanged || commentHasChanged;
    }

    public void updateTag(Tag tag) {
        if (tag == null) {
            return;
        }
        if (titleHasChanged) {
            try {
                tag.setField(FieldKey.TITLE, title);
            } catch (Exception ignored) {
            }
        }
        if (albumHasChanged) {
            try {
                tag.setField(FieldKey.ALBUM, album);
            } catch (Exception ignored) {
            }
        }
        if (artistHasChanged) {
            try {
                tag.setField(FieldKey.ARTIST, artist);
            } catch (Exception ignored) {
            }
        }
        if (albumArtistHasChanged) {
            try {
                tag.setField(FieldKey.ALBUM_ARTIST, albumArtist);
            } catch (Exception ignored) {
            }
        }
        if (genreHasChanged) {
            try {
                tag.setField(FieldKey.GENRE, genre);
            } catch (Exception ignored) {
            }
        }
        if (yearHasChanged) {
            try {
                tag.setField(FieldKey.YEAR, year);
            } catch (Exception ignored) {
            }
        }
        if (trackHasChanged) {
            try {
                tag.setField(FieldKey.TRACK, track);
            } catch (Exception ignored) {
            }
        }
        if (trackTotalHasChanged) {
            try {
                tag.setField(FieldKey.TRACK_TOTAL, trackTotal);
            } catch (Exception ignored) {
            }
        }
        if (discHasChanged) {
            try {
                tag.setField(FieldKey.DISC_NO, disc);
            } catch (Exception ignored) {
            }
        }
        if (discTotalHasChanged) {
            try {
                tag.setField(FieldKey.DISC_TOTAL, discTotal);
            } catch (Exception ignored) {
            }
        }
        if (lyricsHasChanged) {
            try {
                tag.setField(FieldKey.LYRICS, lyrics);
            } catch (Exception ignored) {
            }
        }
        if (commentHasChanged) {
            try {
                tag.setField(FieldKey.COMMENT, comment);
            } catch (Exception ignored) {
            }
        }
    }
}