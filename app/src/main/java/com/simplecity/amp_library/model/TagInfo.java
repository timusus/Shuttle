package com.simplecity.amp_library.model;

import android.text.TextUtils;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.File;
import java.io.Serializable;

/**
 * A holder for various id3 tag information associated with a file.
 */
public class TagInfo implements Serializable {

    public String artistName;
    public String albumArtistName;
    public String albumName;
    public String trackName;
    public int trackNumber;
    public int trackTotal;
    public int discNumber;
    public int discTotal;
    public String bitrate;
    public String format;
    public int sampleRate;
    public String genre;

    public TagInfo(String filePath) {
        if (filePath != null) {
            File file = new File(filePath);
            if (file.exists()) {
                try {
                    AudioFile audioFile = AudioFileIO.read(file);

                    this.artistName = getTag(audioFile, FieldKey.ARTIST);
                    this.albumArtistName = getTag(audioFile, FieldKey.ALBUM_ARTIST);
                    this.albumName = getTag(audioFile, FieldKey.ALBUM);
                    this.trackName = getTag(audioFile, FieldKey.TITLE);
                    this.trackNumber = Integer.parseInt(getTag(audioFile, FieldKey.TRACK));
                    this.trackTotal = Integer.parseInt(getTag(audioFile, FieldKey.TRACK_TOTAL));
                    this.discNumber = Integer.parseInt(getTag(audioFile, FieldKey.DISC_NO));
                    this.discTotal = Integer.parseInt(getTag(audioFile, FieldKey.DISC_TOTAL));
                    this.bitrate = getBitrate(audioFile);
                    this.format = getFormat(audioFile);
                    this.sampleRate = getSampleRate(audioFile);
                    this.genre = getTag(audioFile, FieldKey.GENRE);

                } catch (Exception ignored) {
                }
            }
        }
    }

    public String getTag(AudioFile audioFile, FieldKey key) {
        try {
            if (audioFile != null) {
                Tag tag = audioFile.getTag();
                if (tag != null) {
                    String result = tag.getFirst(key);
                    if (!TextUtils.isEmpty(result)) {
                        return result;
                    }
                }
            }
        } catch (UnsupportedOperationException ignored) {
        }
        return "Unknown";
    }

    public static String getBitrate(AudioFile audioFile) {
        try {
            if (audioFile != null) {
                AudioHeader audioHeader = audioFile.getAudioHeader();
                return audioHeader.getBitRate();
            }
        } catch (UnsupportedOperationException ignored) {
        }
        return null;
    }

    public static String getFormat(AudioFile audioFile) {
        try {
            if (audioFile != null) {
                AudioHeader audioHeader = audioFile.getAudioHeader();
                return audioHeader.getFormat();
            }
        } catch (UnsupportedOperationException ignored) {
        }
        return null;
    }

    public static int getSampleRate(AudioFile audioFile) {
        try {
            if (audioFile != null) {
                AudioHeader audioHeader = audioFile.getAudioHeader();
                return audioHeader.getSampleRateAsNumber();
            }
        } catch (UnsupportedOperationException ignored) {
        }
        return -1;
    }
}