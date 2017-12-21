package com.simplecity.amp_library.ui.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.http.HttpClient;
import com.simplecity.amp_library.http.lastfm.LastFmAlbum;
import com.simplecity.amp_library.http.lastfm.LastFmArtist;
import com.simplecity.amp_library.model.FileObject;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.utils.DialogUtils;
import com.simplecity.amp_library.utils.FileHelper;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.ShuttleUtils;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.simplecity.amp_library.R.id.album;
import static com.simplecity.amp_library.R.id.artist;

public class BiographyDialog {

    private static final String TAG = "BiographyDialog";

    private BiographyDialog() {
    }

    public @interface BioType {
        int ARTIST = 0;
        int ALBUM = 1;
    }

    public static MaterialDialog getArtistBiographyDialog(final Context context, String artistName) {
        return getBiographyDialog(context, BioType.ARTIST, artistName, null);
    }

    public static MaterialDialog getAlbumBiographyDialog(final Context context, String artistName, String albumName) {
        return getBiographyDialog(context, BioType.ALBUM, artistName, albumName);
    }

    private static MaterialDialog getBiographyDialog(final Context context, @BioType int type, String artistName, String albumName) {

        @SuppressLint("InflateParams")
        View customView = LayoutInflater.from(context).inflate(R.layout.dialog_biography, null, false);

        final ProgressBar progressBar = customView.findViewById(R.id.progress);
        final TextView message = customView.findViewById(R.id.message);

        Callback<LastFmArtist> artistCallback = new Callback<LastFmArtist>() {
            @Override
            public void onResponse(Call<LastFmArtist> call, Response<LastFmArtist> response) {
                progressBar.setVisibility(View.GONE);
                if (response != null && response.isSuccessful()) {
                    if (response.body() != null && response.body().artist != null && response.body().artist.bio != null) {
                        String summary = response.body().artist.bio.summary;
                        if (ShuttleUtils.hasNougat()) {
                            message.setText(Html.fromHtml(summary, Html.FROM_HTML_MODE_COMPACT));
                        } else {
                            message.setText(Html.fromHtml(summary));
                        }
                    } else {
                        message.setText(R.string.no_artist_info);
                    }
                }
            }

            @Override
            public void onFailure(Call<LastFmArtist> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                switch (type) {
                    case BioType.ARTIST:
                        message.setText(R.string.no_artist_info);
                        break;
                    case BioType.ALBUM:
                        message.setText(R.string.no_album_info);
                        break;
                }
            }
        };

        Callback<LastFmAlbum> albumCallback = new Callback<LastFmAlbum>() {
            @Override
            public void onResponse(Call<LastFmAlbum> call, Response<LastFmAlbum> response) {
                progressBar.setVisibility(View.GONE);
                if (response != null && response.isSuccessful()) {
                    if (response.body() != null && response.body().album != null && response.body().album.wiki != null) {
                        String summary = response.body().album.wiki.summary;
                        if (ShuttleUtils.hasNougat()) {
                            message.setText(Html.fromHtml(summary, Html.FROM_HTML_MODE_COMPACT));
                        } else {
                            message.setText(Html.fromHtml(summary));
                        }
                    } else {
                        message.setText(R.string.no_album_info);
                    }
                }
            }

            @Override
            public void onFailure(Call<LastFmAlbum> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                switch (type) {
                    case BioType.ARTIST:
                        message.setText(R.string.no_artist_info);
                        break;
                    case BioType.ALBUM:
                        message.setText(R.string.no_album_info);
                        break;
                }
            }
        };

        switch (type) {
            case BioType.ARTIST:
                HttpClient.getInstance().lastFmService.getLastFmArtistResult(artistName).enqueue(artistCallback);
                break;
            case BioType.ALBUM:
                HttpClient.getInstance().lastFmService.getLastFmAlbumResult(artistName, albumName).enqueue(albumCallback);
                break;
        }

        MaterialDialog.Builder builder = DialogUtils.getBuilder(context)
                .title(R.string.info)
                .customView(customView, false)
                .negativeText(R.string.close);

        return builder.build();
    }

    public static MaterialDialog getSongInfoDialog(@NonNull Context context, @NonNull Song song) {

        @SuppressLint("InflateParams")
        View view = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.dialog_song_info, null);

        View titleView = view.findViewById(R.id.title);
        TextView titleKey = titleView.findViewById(R.id.key);
        titleKey.setText(R.string.song_title);
        TextView titleValue = titleView.findViewById(R.id.value);
        titleValue.setText(song.name);

        View trackNumberView = view.findViewById(R.id.track_number);
        TextView trackNumberKey = trackNumberView.findViewById(R.id.key);
        trackNumberKey.setText(R.string.track_number);
        TextView trackNumberValue = trackNumberView.findViewById(R.id.value);
        trackNumberValue.setText(String.valueOf(song.getTrackNumberLabel()));

        View artistView = view.findViewById(artist);
        TextView artistKey = artistView.findViewById(R.id.key);
        artistKey.setText(R.string.artist_title);
        TextView artistValue = artistView.findViewById(R.id.value);
        artistValue.setText(song.artistName);

        View albumView = view.findViewById(album);
        TextView albumKey = albumView.findViewById(R.id.key);
        albumKey.setText(R.string.album_title);
        TextView albumValue = albumView.findViewById(R.id.value);
        albumValue.setText(song.albumName);

        View genreView = view.findViewById(R.id.genre);
        TextView genreKey = genreView.findViewById(R.id.key);
        genreKey.setText(R.string.genre_title);
        TextView genreValue = genreView.findViewById(R.id.value);
        song.getGenre()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(genre -> genreValue.setText(genre.name),
                        error -> LogUtils.logException(TAG, "Error getting genre", error));

        View albumArtistView = view.findViewById(R.id.album_artist);
        TextView albumArtistKey = albumArtistView.findViewById(R.id.key);
        albumArtistKey.setText(R.string.album_artist_title);
        TextView albumArtistValue = albumArtistView.findViewById(R.id.value);
        albumArtistValue.setText(song.albumArtistName);

        View durationView = view.findViewById(R.id.duration);
        TextView durationKey = durationView.findViewById(R.id.key);
        durationKey.setText(R.string.sort_song_duration);
        TextView durationValue = durationView.findViewById(R.id.value);
        durationValue.setText(song.getDurationLabel());

        View pathView = view.findViewById(R.id.path);
        TextView pathKey = pathView.findViewById(R.id.key);
        pathKey.setText(R.string.song_info_path);
        TextView pathValue = pathView.findViewById(R.id.value);
        pathValue.setText(song.path);

        View discNumberView = view.findViewById(R.id.disc_number);
        TextView discNumberKey = discNumberView.findViewById(R.id.key);
        discNumberKey.setText(R.string.disc_number);
        TextView discNumberValue = discNumberView.findViewById(R.id.value);
        discNumberValue.setText(String.valueOf(song.getDiscNumberLabel()));

        View fileSizeView = view.findViewById(R.id.file_size);
        TextView fileSizeKey = fileSizeView.findViewById(R.id.key);
        fileSizeKey.setText(R.string.song_info_file_size);
        TextView fileSizeValue = fileSizeView.findViewById(R.id.value);
        fileSizeValue.setText(song.getFileSizeLabel());

        View formatView = view.findViewById(R.id.format);
        TextView formatKey = formatView.findViewById(R.id.key);
        formatKey.setText(R.string.song_info_format);
        TextView formatValue = formatView.findViewById(R.id.value);
        formatValue.setText(song.getFormatLabel());

        View bitrateView = view.findViewById(R.id.bitrate);
        TextView bitrateKey = bitrateView.findViewById(R.id.key);
        bitrateKey.setText(R.string.song_info_bitrate);
        TextView bitrateValue = bitrateView.findViewById(R.id.value);
        bitrateValue.setText(song.getBitrateLabel());

        View samplingRateView = view.findViewById(R.id.sample_rate);
        TextView samplingRateKey = samplingRateView.findViewById(R.id.key);
        samplingRateKey.setText(R.string.song_info_sample_Rate);
        TextView samplingRateValue = samplingRateView.findViewById(R.id.value);
        samplingRateValue.setText(song.getSampleRateLabel());

        View playCountView = view.findViewById(R.id.play_count);
        TextView playCountKey = playCountView.findViewById(R.id.key);
        playCountKey.setText(R.string.song_info_play_count);
        TextView playCountValue = playCountView.findViewById(R.id.value);
        Observable.fromCallable(() -> song.getPlayCount(context))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(playCount -> playCountValue.setText(String.valueOf(playCount)),
                        error -> LogUtils.logException(TAG, "Error getting play count", error));

        return DialogUtils.getBuilder(context)
                .title(context.getString(R.string.dialog_song_info_title))
                .customView(view, false)
                .negativeText(R.string.close)
                .build();
    }

    public static void showFileInfoDialog(Context context, FileObject fileObject) {

        if (fileObject == null) {
            return;
        }

        @SuppressLint("InflateParams")
        View view = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.dialog_song_info, null);

        View titleView = view.findViewById(R.id.title);
        TextView titleKey = titleView.findViewById(R.id.key);
        titleKey.setText(R.string.song_title);
        TextView titleValue = titleView.findViewById(R.id.value);
        titleValue.setText(fileObject.tagInfo.trackName);

        View trackNumberView = view.findViewById(R.id.track_number);
        TextView trackNumberKey = trackNumberView.findViewById(R.id.key);
        trackNumberKey.setText(R.string.track_number);
        TextView trackNumberValue = trackNumberView.findViewById(R.id.value);
        if (fileObject.tagInfo.trackTotal != 0) {
            trackNumberValue.setText(String.format(context.getString(R.string.track_count), String.valueOf(fileObject.tagInfo.trackNumber), String.valueOf(fileObject.tagInfo.trackTotal)));
        } else {
            trackNumberValue.setText(String.valueOf(fileObject.tagInfo.trackNumber));
        }

        View artistView = view.findViewById(artist);
        TextView artistKey = artistView.findViewById(R.id.key);
        artistKey.setText(R.string.artist_title);
        TextView artistValue = artistView.findViewById(R.id.value);
        artistValue.setText(fileObject.tagInfo.artistName);

        View albumView = view.findViewById(album);
        TextView albumKey = albumView.findViewById(R.id.key);
        albumKey.setText(R.string.album_title);
        TextView albumValue = albumView.findViewById(R.id.value);
        albumValue.setText(fileObject.tagInfo.albumName);

        View genreView = view.findViewById(R.id.genre);
        TextView genreKey = genreView.findViewById(R.id.key);
        genreKey.setText(R.string.genre_title);
        TextView genreValue = genreView.findViewById(R.id.value);
        genreValue.setText(fileObject.tagInfo.genre);

        View albumArtistView = view.findViewById(R.id.album_artist);
        TextView albumArtistKey = albumArtistView.findViewById(R.id.key);
        albumArtistKey.setText(R.string.album_artist_title);
        TextView albumArtistValue = albumArtistView.findViewById(R.id.value);
        albumArtistValue.setText(fileObject.tagInfo.albumArtistName);

        View durationView = view.findViewById(R.id.duration);
        TextView durationKey = durationView.findViewById(R.id.key);
        durationKey.setText(R.string.sort_song_duration);
        TextView durationValue = durationView.findViewById(R.id.value);
        durationValue.setText(fileObject.getTimeString());

        View pathView = view.findViewById(R.id.path);
        TextView pathKey = pathView.findViewById(R.id.key);
        pathKey.setText(R.string.song_info_path);
        TextView pathValue = pathView.findViewById(R.id.value);
        pathValue.setText(fileObject.path + "/" + fileObject.name + "." + fileObject.extension);

        View discNumberView = view.findViewById(R.id.disc_number);
        TextView discNumberKey = discNumberView.findViewById(R.id.key);
        discNumberKey.setText(R.string.disc_number);
        TextView discNumberValue = discNumberView.findViewById(R.id.value);
        if (fileObject.tagInfo.discTotal != 0) {
            discNumberValue.setText(String.format(context.getString(R.string.track_count), String.valueOf(fileObject.tagInfo.discNumber), String.valueOf(fileObject.tagInfo.discTotal)));
        } else {
            discNumberValue.setText(String.valueOf(fileObject.tagInfo.discNumber));
        }

        View fileSizeView = view.findViewById(R.id.file_size);
        TextView fileSizeKey = fileSizeView.findViewById(R.id.key);
        fileSizeKey.setText(R.string.song_info_file_size);
        TextView fileSizeValue = fileSizeView.findViewById(R.id.value);
        fileSizeValue.setText(FileHelper.getHumanReadableSize(fileObject.size));

        View formatView = view.findViewById(R.id.format);
        TextView formatKey = formatView.findViewById(R.id.key);
        formatKey.setText(R.string.song_info_format);
        TextView formatValue = formatView.findViewById(R.id.value);
        formatValue.setText(fileObject.tagInfo.format);

        View bitrateView = view.findViewById(R.id.bitrate);
        TextView bitrateKey = bitrateView.findViewById(R.id.key);
        bitrateKey.setText(R.string.song_info_bitrate);
        TextView bitrateValue = bitrateView.findViewById(R.id.value);
        bitrateValue.setText(fileObject.tagInfo.bitrate + ShuttleApplication.getInstance().getString(R.string.song_info_bitrate_suffix));

        View samplingRateView = view.findViewById(R.id.sample_rate);
        TextView samplingRateKey = samplingRateView.findViewById(R.id.key);
        samplingRateKey.setText(R.string.song_info_sample_Rate);
        TextView samplingRateValue = samplingRateView.findViewById(R.id.value);
        samplingRateValue.setText(fileObject.tagInfo.sampleRate / 1000 + ShuttleApplication.getInstance().getString(R.string.song_info_sample_rate_suffix));

        View playCountView = view.findViewById(R.id.play_count);
        playCountView.setVisibility(View.GONE);

        DialogUtils.getBuilder(context)
                .title(context.getString(R.string.dialog_song_info_title))
                .customView(view, false)
                .negativeText(R.string.close)
                .show();
    }
}