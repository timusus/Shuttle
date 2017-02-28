package com.simplecity.amp_library.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v4.content.IntentCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.http.HttpClient;
import com.simplecity.amp_library.lastfm.LastFmAlbum;
import com.simplecity.amp_library.lastfm.LastFmArtist;
import com.simplecity.amp_library.model.AdaptableItem;
import com.simplecity.amp_library.model.BlacklistedSong;
import com.simplecity.amp_library.model.FileObject;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.sql.databases.BlacklistHelper;
import com.simplecity.amp_library.sql.databases.WhitelistHelper;
import com.simplecity.amp_library.sql.providers.PlayCountTable;
import com.simplecity.amp_library.sql.sqlbrite.SqlBriteUtils;
import com.simplecity.amp_library.ui.activities.MainActivity;
import com.simplecity.amp_library.ui.activities.SettingsActivity;
import com.simplecity.amp_library.ui.adapters.BlacklistAdapter;
import com.simplecity.amp_library.ui.adapters.ColorAdapter;
import com.simplecity.amp_library.ui.adapters.WhitelistAdapter;
import com.simplecity.amp_library.ui.fragments.SettingsFragment;
import com.simplecity.amp_library.ui.modelviews.BlacklistView;
import com.simplecity.amp_library.ui.modelviews.ColorView;
import com.simplecity.amp_library.ui.modelviews.EmptyView;
import com.simplecity.amp_library.ui.modelviews.WhitelistView;
import com.simplecity.amp_library.ui.views.CustomColorPicker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static com.simplecity.amp_library.R.id.album;
import static com.simplecity.amp_library.R.id.artist;

public class DialogUtils {

    private static final String TAG = "DialogUtils";

    public static MaterialDialog.Builder getBuilder(Context context) {

        int themeType = ThemeUtils.getInstance().themeType;
        boolean isDark = themeType == ThemeUtils.ThemeType.TYPE_BLACK
                || themeType == ThemeUtils.ThemeType.TYPE_DARK
                || themeType == ThemeUtils.ThemeType.TYPE_SOLID_BLACK
                || themeType == ThemeUtils.ThemeType.TYPE_SOLID_DARK;

        int accentColor = ColorUtils.getContrastAwareColorAccent(context);

        return new MaterialDialog.Builder(context)
                .titleColorRes(isDark ? R.color.primary_text_dark : R.color.primary_text_light)
                .contentColorRes(isDark ? R.color.primary_text_dark : R.color.primary_text_light)
                .dividerColor(accentColor)
                .backgroundColorRes(isDark ? R.color.bg_dark : R.color.bg_light)
                .positiveColor(accentColor)
                .neutralColor(accentColor)
                .negativeColor(accentColor)
                .widgetColor(accentColor)
                .buttonRippleColor(accentColor);
    }

    public interface ColorSelectionListener {
        void colorSelected(int color);
    }

    public static void showColorPickerDialog(SettingsFragment fragment, int selectedColor, ColorSelectionListener listener) {
        showColorPickerDialog(fragment, selectedColor, ColorPalette.getPrimaryColors(), ColorPalette.getPrimaryColorsSub(), listener);
    }

    public static void showColorPickerDialog(SettingsFragment fragment, int selectedColor, int[] mainColors, int[][] subColors, ColorSelectionListener listener) {

        View customView = LayoutInflater.from(fragment.getActivity()).inflate(R.layout.dialog_color_picker, null);

        RecyclerView recyclerView = (RecyclerView) customView.findViewById(R.id.recyclerView);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(fragment.getActivity(), 5);
        recyclerView.setLayoutManager(gridLayoutManager);
        ThemeUtils.themeRecyclerView(recyclerView);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                ThemeUtils.themeRecyclerView(recyclerView);
                super.onScrollStateChanged(recyclerView, newState);
            }
        });

        ColorAdapter colorAdapter = new ColorAdapter();

        List<AdaptableItem> colorViews = new ArrayList<>();
        for (int i = 0, length = mainColors.length; i < length; i++) {
            ColorView colorView = new ColorView(mainColors[i]);
            boolean selected = false;

            //If the sub colors array contains our selected color, then we set this colorView to selected.
            for (int j = 0, jLength = subColors[i].length; j < jLength; j++) {
                if (subColors[i][j] == selectedColor) {
                    selected = true;
                }
            }
            colorView.selected = selected;
            colorViews.add(colorView);
        }
        colorAdapter.setItems(colorViews);
        recyclerView.setAdapter(colorAdapter);

        colorAdapter.setColorListener((position, color, isSubColor) -> {
            if (isSubColor) {
                colorAdapter.setSelectedPosition(position);
            } else {
                List<AdaptableItem> subColorViews = new ArrayList<>();
                for (int i = 0, length = subColors[position].length; i < length; i++) {
                    ColorView colorView = new ColorView(subColors[position][i]);
                    colorView.selected = colorView.color == selectedColor;
                    subColorViews.add(colorView);
                    colorAdapter.isSubColor = true;
                }
                colorAdapter.setItems(subColorViews);
            }
        });

        int neutralTextResId;
        TextView textView = (TextView) customView.findViewById(R.id.text1);
        if (ShuttleUtils.isUpgraded()) {
            textView.setVisibility(View.GONE);
            neutralTextResId = R.string.dialog_custom;
        } else {
            textView.setVisibility(View.VISIBLE);
            if (ShuttleUtils.isAmazonBuild()) {
                neutralTextResId = R.string.get_pro_button_amazon;
            } else {
                neutralTextResId = R.string.btn_upgrade;
            }
        }

        getBuilder(fragment.getActivity())
                .title(fragment.getActivity().getString(R.string.color_pick))
                .negativeText(R.string.cancel)
                .onNegative((dialog, which) -> dialog.dismiss())
                .positiveText(R.string.button_done)
                .onPositive((dialog, which) -> {
                    int color = selectedColor;
                    for (AdaptableItem item : colorAdapter.items) {
                        if (((ColorView) item).selected) {
                            color = ((ColorView) item).color;
                            break;
                        }
                    }
                    listener.colorSelected(color);
                    dialog.dismiss();
                })
                .neutralText(neutralTextResId)
                .autoDismiss(false)
                .onNeutral((dialog, which) -> {
                    if (ShuttleUtils.isUpgraded()) {
                        showCustomColorPickerDialog(fragment.getActivity(), selectedColor, listener);
                        dialog.dismiss();
                    } else {
                        showUpgradeDialog(fragment.getActivity(), (upgradeDialog, which1) -> {
                            if (ShuttleUtils.isAmazonBuild()) {
                                ShuttleUtils.openShuttleLink(fragment.getActivity(), "com.simplecity.amp_pro");
                            } else {
                                AnalyticsManager.logUpgrade(AnalyticsManager.UpgradeType.COLORS);
                                ((SettingsActivity) fragment.getActivity()).purchasePremiumUpgrade();
                            }
                        });
                    }
                })
                .customView(customView, false)
                .show();
    }

    public static void showCustomColorPickerDialog(Context context, int selectedColor, ColorSelectionListener listener) {

        CustomColorPicker customColorPicker = new CustomColorPicker(context, selectedColor);

        getBuilder(context)
                .title(context.getString(R.string.color_pick))
                .negativeText(R.string.cancel)
                .positiveText(R.string.button_done)
                .onPositive((d, which) -> listener.colorSelected(customColorPicker.color))
                .customView(customColorPicker, false)
                .show();
    }


    public static class DeleteDialogBuilder {

        private Context context;

        private
        @StringRes int deleteSingleMessageId;
        private
        @StringRes int deleteMultipleMessageId;

        private List<String> itemNames;
        private Observable<List<Song>> songsObservable;

        public DeleteDialogBuilder context(Context context) {
            this.context = context;
            return this;
        }

        public DeleteDialogBuilder singleMessageId(@StringRes int deleteSingleMessageId) {
            this.deleteSingleMessageId = deleteSingleMessageId;
            return this;
        }

        public DeleteDialogBuilder multipleMessage(@StringRes int deleteMultipleMessageId) {
            this.deleteMultipleMessageId = deleteMultipleMessageId;
            return this;
        }

        public DeleteDialogBuilder itemNames(List<String> itemNames) {
            this.itemNames = itemNames;
            return this;
        }

        public DeleteDialogBuilder songsToDelete(Observable<List<Song>> songsObservable) {
            this.songsObservable = songsObservable;
            return this;
        }

        void deleteSongs() {
            songsObservable
                    .map(lists -> Stream.of(lists)
                            .flatMap(Stream::of)
                            .filter(Song::delete)
                            .collect(Collectors.toList()))
                    .doOnNext(songs -> {

                        //Current play queue
                        MusicUtils.removeFromQueue(songs, true);

                        //Play Count Table
                        ArrayList<ContentProviderOperation> operations = Stream.of(songs).map(song -> ContentProviderOperation
                                .newDelete(PlayCountTable.URI)
                                .withSelection(PlayCountTable.COLUMN_ID + "=" + song.id, null)
                                .build())
                                .collect(Collectors.toCollection(ArrayList<ContentProviderOperation>::new));

                        try {
                            context.getContentResolver().applyBatch(PlayCountTable.AUTHORITY, operations);
                        } catch (RemoteException | OperationApplicationException e) {
                            e.printStackTrace();
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(deletedSongs -> {
                        if (deletedSongs.size() > 0) {
                            CustomMediaScanner.scanFiles(Stream.of(deletedSongs)
                                    .map(song -> song.path)
                                    .collect(Collectors.toList()), null);
                            Toast.makeText(context, String.format(context.getString(R.string.delete_songs_success_toast), deletedSongs.size()), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, R.string.delete_songs_failure_toast, Toast.LENGTH_SHORT).show();
                        }
                    });
        }

        public MaterialDialog build() {

            String stringToFormat = context.getString(deleteSingleMessageId);

            String names;

            if (itemNames.size() > 1) {
                stringToFormat = context.getString(deleteMultipleMessageId);
                names = Stream.of(itemNames)
                        .map(itemName -> "\n\u2022 " + itemName)
                        .collect(Collectors.joining()) + "\n";
            } else {
                names = itemNames.get(0);
            }

            String message = String.format(stringToFormat, names);

            Drawable drawable = DrawableUtils.getBaseDrawable(context, R.drawable.ic_dialog_alert);
            return getBuilder(context)
                    .icon(drawable)
                    .title(R.string.delete_item)
                    .content(message)
                    .positiveText(R.string.button_ok)
                    .onPositive((materialDialog, dialogAction) -> deleteSongs())
                    .negativeText(R.string.cancel)
                    .build();
        }
    }

    public static void createRestartDialog(final Context context) {
        MaterialDialog.Builder builder = getBuilder(context)
                .title(R.string.restart_tite)
                .content(R.string.restart_message)
                .positiveText(R.string.restart_button)
                .onPositive((materialDialog, dialogAction) -> {
                    Intent intent = new Intent(context, MainActivity.class);
                    ComponentName componentNAme = intent.getComponent();
                    Intent mainIntent = IntentCompat.makeRestartActivityTask(componentNAme);
                    context.startActivity(mainIntent);
                });
        if (!((Activity) context).isFinishing()) {
            builder.show();
        }
    }

    public static void showShareDialog(final Context context, final Song song) {

        if (song == null) {
            Toast.makeText(context, context.getString(R.string.share_failed), Toast.LENGTH_LONG).show();
            return;
        }

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_listview, null);

        final ListView listView = (ListView) view.findViewById(android.R.id.list);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(context, R.layout.dialog_list_item);
        arrayAdapter.add(context.getString(R.string.share_option_song_info));
        arrayAdapter.add(context.getString(R.string.share_option_audio_file));
        listView.setAdapter(arrayAdapter);

        MaterialDialog.Builder builder = getBuilder(context)
                .title(context.getString(R.string.share_dialog_title))
                .customView(view, false);

        final Dialog dialog = builder.show();
        builder.negativeText(R.string.close);

        listView.setOnItemClickListener((adapterView, view1, i, l) -> {
            switch (i) {
                case 0:
                    // Use the compress method on the Bitmap object to write image to the OutputStream
                    Glide.with(context)
                            .load(song)
                            .asBitmap()
                            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                            .into(new SimpleTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                                    Intent sendIntent = new Intent();
                                    sendIntent.setType("text/plain");
                                    FileOutputStream fileOutputStream = null;
                                    try {
                                        File file = new File(context.getFilesDir() + "/share_image.jpg");
                                        fileOutputStream = new FileOutputStream(file);
                                        if (resource != null) {
                                            resource.compress(Bitmap.CompressFormat.JPEG, 80, fileOutputStream);
                                            sendIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", file));
                                            sendIntent.setType("image/jpeg");
                                        }
                                    } catch (FileNotFoundException ignored) {

                                    } finally {
                                        try {
                                            if (fileOutputStream != null) {
                                                fileOutputStream.close();
                                            }
                                        } catch (IOException ignored) {

                                        }
                                    }

                                    sendIntent.setAction(Intent.ACTION_SEND);
                                    sendIntent.putExtra(Intent.EXTRA_TEXT, "#NowPlaying " + song.artistName + " - " + song.name + "\n\n" + "#Shuttle");
                                    context.startActivity(Intent.createChooser(sendIntent, "Share current song via: "));
                                    dialog.dismiss();
                                }
                            });
                    break;

                case 1:
                    song.share(context);
                    dialog.dismiss();
                    break;
            }
        });
    }

    /**
     * Displays the popup dialog recommending the user try the paid version
     */
    public static void showUpgradeNagDialog(final Context context, MaterialDialog.SingleButtonCallback listener) {

        //If we're in the free version, the app has been launched more than 15 times,
        //The message hasn't been read before, display the 'upgrade to pro' dialog.
        if (!ShuttleUtils.isUpgraded()
                && SettingsManager.getInstance().getLaunchCount() > 15
                && !SettingsManager.getInstance().getNagMessageRead()) {

            MaterialDialog.Builder builder = getBuilder(context)
                    .title(context.getResources().getString(R.string.get_pro_title))
                    .content(context.getResources().getString(R.string.get_pro_message))
                    .positiveText(R.string.btn_upgrade)
                    .onPositive(listener)
                    .negativeText(R.string.get_pro_button_no);

            builder.show();
            SettingsManager.getInstance().setNagMessageRead();

            AnalyticsManager.logUpgrade(AnalyticsManager.UpgradeType.NAG);
        }
    }

    /**
     * Displayed when the user chooses to upgrade
     */
    public static void showUpgradeDialog(final Context context, MaterialDialog.SingleButtonCallback listener) {
        getBuilder(context)
                .title(context.getResources().getString(R.string.get_pro_title))
                .content(context.getResources().getString(R.string.upgrade_dialog_message))
                .positiveText(R.string.btn_upgrade)
                .onPositive(listener)
                .negativeText(R.string.get_pro_button_no)
                .show();

        AnalyticsManager.logUpgrade(AnalyticsManager.UpgradeType.UPGRADE);
    }

    /**
     * Displays the popup dialog recommending the user try the paid version
     */
    public static void showUpgradeThankyouDialog(final Context context) {
        getBuilder(context)
                .title(context.getResources().getString(R.string.upgraded_title))
                .content(context.getResources().getString(R.string.upgraded_message))
                .positiveText(R.string.restart_button)
                .onPositive((materialDialog, dialogAction) -> {
                    Intent intent = new Intent(context, MainActivity.class);
                    ComponentName componentNAme = intent.getComponent();
                    Intent mainIntent = IntentCompat.makeRestartActivityTask(componentNAme);
                    context.startActivity(mainIntent);
                })
                .show();
    }

    public @interface BioType {
        int ARTIST = 0;
        int ALBUM = 1;
    }

    public static void showArtistBiographyDialog(final Context context, String artistName) {
        showBiographyDialog(context, BioType.ARTIST, artistName, null);
    }

    public static void showAlbumBiographyDialog(final Context context, String artistName, String albumName) {
        showBiographyDialog(context, BioType.ALBUM, artistName, albumName);
    }

    public static void showBiographyDialog(final Context context, @BioType int type, String artistName, String albumName) {

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View customView = inflater.inflate(R.layout.dialog_biography, null, false);

        final ProgressBar progressBar = (ProgressBar) customView.findViewById(R.id.progress);
        final TextView message = (TextView) customView.findViewById(R.id.message);
        final ScrollView scrollView = (ScrollView) customView.findViewById(R.id.scrollView);
        ThemeUtils.themeScrollView(scrollView);

        Callback<LastFmArtist> artistCallback = new Callback<LastFmArtist>() {
            @Override
            public void onResponse(Call<LastFmArtist> call, Response<LastFmArtist> response) {
                progressBar.setVisibility(View.GONE);
                if (response != null && response.isSuccessful()) {
                    if (response.body() != null && response.body().artist != null && response.body().artist.bio != null) {
                        message.setText(Html.fromHtml(response.body().artist.bio.summary));
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
                        message.setText(Html.fromHtml(response.body().album.wiki.summary));
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

        MaterialDialog.Builder builder = getBuilder(context)
                .title(R.string.info)
                .customView(customView, false)
                .negativeText(R.string.close);

        Dialog dialog = builder.show();
        dialog.setOnDismissListener(dialog1 -> {
        });
    }

    public static void showBlacklistDialog(final Context context) {

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_blacklist, null);

        final MaterialDialog.Builder builder = getBuilder(context)
                .title(R.string.blacklist_title)
                .customView(view, false)
                .positiveText(R.string.close)
                .negativeText(R.string.pref_title_clear_blacklist)
                .onNegative((materialDialog, dialogAction) -> {
                    BlacklistHelper.deleteAllSongs();
                    Toast.makeText(context, R.string.blacklist_deleted, Toast.LENGTH_SHORT).show();
                });

        final Dialog dialog = builder.build();

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        final BlacklistAdapter blacklistAdapter = new BlacklistAdapter();
        blacklistAdapter.setBlackListListener((v, position, song) -> {
            BlacklistHelper.deleteSong(song.id);
            if (blacklistAdapter.items.size() == 0) {
                dialog.dismiss();
            }
        });
        recyclerView.setAdapter(blacklistAdapter);

        Observable<List<Song>> songsObservable = SqlBriteUtils.createContinuousQuery(ShuttleApplication.getInstance(), Song::new, Song.getQuery()).first();
        Observable<List<BlacklistedSong>> blacklistObservable = BlacklistHelper.getBlacklistSongsObservable();

        Subscription subscription = Observable.combineLatest(songsObservable, blacklistObservable, (songs, blacklistedSongs) ->
                Stream.of(songs)
                        .filter(song -> Stream.of(blacklistedSongs)
                                .anyMatch(blacklistedSong -> blacklistedSong.songId == song.id))
                        .sorted((a, b) -> ComparisonUtils.compare(a.albumArtistName, b.albumArtistName))
                        .sorted((a, b) -> ComparisonUtils.compareInt(b.year, a.year))
                        .sorted((a, b) -> ComparisonUtils.compareInt(a.track, b.track))
                        .sorted((a, b) -> ComparisonUtils.compareInt(a.discNumber, b.discNumber))
                        .sorted((a, b) -> ComparisonUtils.compare(a.albumName, b.albumName))
                        .map(song -> (AdaptableItem) new BlacklistView(song))
                        .collect(Collectors.toList()))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(blacklistViews -> {
                    if (blacklistViews.size() == 0) {
                        blacklistAdapter.addItem(0, new EmptyView(R.string.blacklist_empty));
                    } else {
                        blacklistAdapter.setItems(blacklistViews);
                    }
                });

        dialog.setOnDismissListener(dialogInterface -> subscription.unsubscribe());
        dialog.show();
    }

    public static void showWhitelistDialog(final Context context) {

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_blacklist, null);

        final MaterialDialog.Builder builder = getBuilder(context)
                .title(R.string.whitelist_title)
                .customView(view, false)
                .positiveText(R.string.close)
                .negativeText(R.string.pref_title_clear_whitelist)
                .onNegative((materialDialog, dialogAction) -> {
                    WhitelistHelper.deleteAllFolders();
                    Toast.makeText(context, R.string.whitelist_deleted, Toast.LENGTH_SHORT).show();
                });

        final Dialog dialog = builder.build();

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        final WhitelistAdapter whitelistadapter = new WhitelistAdapter();
        whitelistadapter.setWhitelistListener((v, position, songWhitelist) -> {
            WhitelistHelper.deleteFolder(songWhitelist);
            whitelistadapter.removeItem(position);
            if (whitelistadapter.items.size() == 0) {
                dialog.dismiss();
            }
        });

        recyclerView.setAdapter(whitelistadapter);

        Subscription subscription = WhitelistHelper.getWhitelistFolders()
                .map(whitelistFolders -> Stream.of(whitelistFolders)
                        .map(folder -> (AdaptableItem) new WhitelistView(folder))
                        .collect(Collectors.toList()))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(whitelistViews -> {
                    if (whitelistViews.size() == 0) {
                        whitelistadapter.addItem(0, new EmptyView(R.string.whitelist_empty));
                    } else {
                        whitelistadapter.setItems(whitelistViews);
                    }
                });

        dialog.setOnDismissListener(dialogInterface -> subscription.unsubscribe());
        dialog.show();
    }

    public static void showSongInfoDialog(Context context, Song song) {

        if (song == null) {
            return;
        }

        View view = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.dialog_song_info, null);

        View titleView = view.findViewById(R.id.title);
        TextView titleKey = (TextView) titleView.findViewById(R.id.key);
        titleKey.setText(R.string.song_title);
        TextView titleValue = (TextView) titleView.findViewById(R.id.value);
        titleValue.setText(song.name);

        View trackNumberView = view.findViewById(R.id.track_number);
        TextView trackNumberKey = (TextView) trackNumberView.findViewById(R.id.key);
        trackNumberKey.setText(R.string.track_number);
        TextView trackNumberValue = (TextView) trackNumberView.findViewById(R.id.value);
        trackNumberValue.setText(String.valueOf(song.getTrackNumberLabel()));

        View artistView = view.findViewById(artist);
        TextView artistKey = (TextView) artistView.findViewById(R.id.key);
        artistKey.setText(R.string.artist_title);
        TextView artistValue = (TextView) artistView.findViewById(R.id.value);
        artistValue.setText(song.artistName);

        View albumView = view.findViewById(album);
        TextView albumKey = (TextView) albumView.findViewById(R.id.key);
        albumKey.setText(R.string.album_title);
        TextView albumValue = (TextView) albumView.findViewById(R.id.value);
        albumValue.setText(song.albumName);

        View genreView = view.findViewById(R.id.genre);
        TextView genreKey = (TextView) genreView.findViewById(R.id.key);
        genreKey.setText(R.string.genre_title);
        TextView genreValue = (TextView) genreView.findViewById(R.id.value);
        Observable.fromCallable(song::getGenre)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(genre -> genreValue.setText(genre == null ? null : genre.name));

        View albumArtistView = view.findViewById(R.id.album_artist);
        TextView albumArtistKey = (TextView) albumArtistView.findViewById(R.id.key);
        albumArtistKey.setText(R.string.album_artist_title);
        TextView albumArtistValue = (TextView) albumArtistView.findViewById(R.id.value);
        albumArtistValue.setText(song.albumArtistName);

        View durationView = view.findViewById(R.id.duration);
        TextView durationKey = (TextView) durationView.findViewById(R.id.key);
        durationKey.setText(R.string.sort_song_duration);
        TextView durationValue = (TextView) durationView.findViewById(R.id.value);
        durationValue.setText(song.getDurationLabel());

        View pathView = view.findViewById(R.id.path);
        TextView pathKey = (TextView) pathView.findViewById(R.id.key);
        pathKey.setText(R.string.song_info_path);
        TextView pathValue = (TextView) pathView.findViewById(R.id.value);
        pathValue.setText(song.path);

        View discNumberView = view.findViewById(R.id.disc_number);
        TextView discNumberKey = (TextView) discNumberView.findViewById(R.id.key);
        discNumberKey.setText(R.string.disc_number);
        TextView discNumberValue = (TextView) discNumberView.findViewById(R.id.value);
        discNumberValue.setText(String.valueOf(song.getDiscNumberLabel()));

        View fileSizeView = view.findViewById(R.id.file_size);
        TextView fileSizeKey = (TextView) fileSizeView.findViewById(R.id.key);
        fileSizeKey.setText(R.string.song_info_file_size);
        TextView fileSizeValue = (TextView) fileSizeView.findViewById(R.id.value);
        fileSizeValue.setText(song.getFileSizeLabel());

        View formatView = view.findViewById(R.id.format);
        TextView formatKey = (TextView) formatView.findViewById(R.id.key);
        formatKey.setText(R.string.song_info_format);
        TextView formatValue = (TextView) formatView.findViewById(R.id.value);
        formatValue.setText(song.getFormatLabel());

        View bitrateView = view.findViewById(R.id.bitrate);
        TextView bitrateKey = (TextView) bitrateView.findViewById(R.id.key);
        bitrateKey.setText(R.string.song_info_bitrate);
        TextView bitrateValue = (TextView) bitrateView.findViewById(R.id.value);
        bitrateValue.setText(song.getBitrateLabel());

        View samplingRateView = view.findViewById(R.id.sample_rate);
        TextView samplingRateKey = (TextView) samplingRateView.findViewById(R.id.key);
        samplingRateKey.setText(R.string.song_info_sample_Rate);
        TextView samplingRateValue = (TextView) samplingRateView.findViewById(R.id.value);
        samplingRateValue.setText(song.getSampleRateLabel());

        View playCountView = view.findViewById(R.id.play_count);
        TextView playCountKey = (TextView) playCountView.findViewById(R.id.key);
        playCountKey.setText(R.string.song_info_play_count);
        TextView playCountValue = (TextView) playCountView.findViewById(R.id.value);
        Observable.fromCallable(() -> song.getPlayCount(context))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(playCount -> playCountValue.setText(String.valueOf(playCount)));

        getBuilder(context)
                .title(context.getString(R.string.dialog_song_info_title))
                .customView(view, false)
                .negativeText(R.string.close)
                .show();
    }

    public static void showFileInfoDialog(Context context, FileObject fileObject) {

        if (fileObject == null) {
            return;
        }

        View view = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.dialog_song_info, null);

        View titleView = view.findViewById(R.id.title);
        TextView titleKey = (TextView) titleView.findViewById(R.id.key);
        titleKey.setText(R.string.song_title);
        TextView titleValue = (TextView) titleView.findViewById(R.id.value);
        titleValue.setText(fileObject.tagInfo.trackName);

        View trackNumberView = view.findViewById(R.id.track_number);
        TextView trackNumberKey = (TextView) trackNumberView.findViewById(R.id.key);
        trackNumberKey.setText(R.string.track_number);
        TextView trackNumberValue = (TextView) trackNumberView.findViewById(R.id.value);
        if (fileObject.tagInfo.trackTotal != 0) {
            trackNumberValue.setText(String.format(context.getString(R.string.track_count), String.valueOf(fileObject.tagInfo.trackNumber), String.valueOf(fileObject.tagInfo.trackTotal)));
        } else {
            trackNumberValue.setText(String.valueOf(fileObject.tagInfo.trackNumber));
        }

        View artistView = view.findViewById(artist);
        TextView artistKey = (TextView) artistView.findViewById(R.id.key);
        artistKey.setText(R.string.artist_title);
        TextView artistValue = (TextView) artistView.findViewById(R.id.value);
        artistValue.setText(fileObject.tagInfo.artistName);

        View albumView = view.findViewById(album);
        TextView albumKey = (TextView) albumView.findViewById(R.id.key);
        albumKey.setText(R.string.album_title);
        TextView albumValue = (TextView) albumView.findViewById(R.id.value);
        albumValue.setText(fileObject.tagInfo.albumName);

        View genreView = view.findViewById(R.id.genre);
        TextView genreKey = (TextView) genreView.findViewById(R.id.key);
        genreKey.setText(R.string.genre_title);
        TextView genreValue = (TextView) genreView.findViewById(R.id.value);
        genreValue.setText(fileObject.tagInfo.genre);

        View albumArtistView = view.findViewById(R.id.album_artist);
        TextView albumArtistKey = (TextView) albumArtistView.findViewById(R.id.key);
        albumArtistKey.setText(R.string.album_artist_title);
        TextView albumArtistValue = (TextView) albumArtistView.findViewById(R.id.value);
        albumArtistValue.setText(fileObject.tagInfo.albumArtistName);

        View durationView = view.findViewById(R.id.duration);
        TextView durationKey = (TextView) durationView.findViewById(R.id.key);
        durationKey.setText(R.string.sort_song_duration);
        TextView durationValue = (TextView) durationView.findViewById(R.id.value);
        durationValue.setText(fileObject.getTimeString());

        View pathView = view.findViewById(R.id.path);
        TextView pathKey = (TextView) pathView.findViewById(R.id.key);
        pathKey.setText(R.string.song_info_path);
        TextView pathValue = (TextView) pathView.findViewById(R.id.value);
        pathValue.setText(fileObject.path + "/" + fileObject.name + "." + fileObject.extension);

        View discNumberView = view.findViewById(R.id.disc_number);
        TextView discNumberKey = (TextView) discNumberView.findViewById(R.id.key);
        discNumberKey.setText(R.string.disc_number);
        TextView discNumberValue = (TextView) discNumberView.findViewById(R.id.value);
        if (fileObject.tagInfo.discTotal != 0) {
            discNumberValue.setText(String.format(context.getString(R.string.track_count), String.valueOf(fileObject.tagInfo.discNumber), String.valueOf(fileObject.tagInfo.discTotal)));
        } else {
            discNumberValue.setText(String.valueOf(fileObject.tagInfo.discNumber));
        }

        View fileSizeView = view.findViewById(R.id.file_size);
        TextView fileSizeKey = (TextView) fileSizeView.findViewById(R.id.key);
        fileSizeKey.setText(R.string.song_info_file_size);
        TextView fileSizeValue = (TextView) fileSizeView.findViewById(R.id.value);
        fileSizeValue.setText(FileHelper.getHumanReadableSize(fileObject.size));

        View formatView = view.findViewById(R.id.format);
        TextView formatKey = (TextView) formatView.findViewById(R.id.key);
        formatKey.setText(R.string.song_info_format);
        TextView formatValue = (TextView) formatView.findViewById(R.id.value);
        formatValue.setText(fileObject.tagInfo.format);

        View bitrateView = view.findViewById(R.id.bitrate);
        TextView bitrateKey = (TextView) bitrateView.findViewById(R.id.key);
        bitrateKey.setText(R.string.song_info_bitrate);
        TextView bitrateValue = (TextView) bitrateView.findViewById(R.id.value);
        bitrateValue.setText(fileObject.tagInfo.bitrate + ShuttleApplication.getInstance().getString(R.string.song_info_bitrate_suffix));

        View samplingRateView = view.findViewById(R.id.sample_rate);
        TextView samplingRateKey = (TextView) samplingRateView.findViewById(R.id.key);
        samplingRateKey.setText(R.string.song_info_sample_Rate);
        TextView samplingRateValue = (TextView) samplingRateView.findViewById(R.id.value);
        samplingRateValue.setText(fileObject.tagInfo.sampleRate / 1000 + ShuttleApplication.getInstance().getString(R.string.song_info_sample_rate_suffix));

        View playCountView = view.findViewById(R.id.play_count);
        playCountView.setVisibility(View.GONE);

        getBuilder(context)
                .title(context.getString(R.string.dialog_song_info_title))
                .customView(view, false)
                .negativeText(R.string.close)
                .show();
    }

    public static void showDownloadWarningDialog(Context context, MaterialDialog.SingleButtonCallback listener) {
        getBuilder(context)
                .title(R.string.pref_title_download_artwork)
                .content(R.string.pref_warning_download_artwork)
                .positiveText(R.string.download)
                .onPositive(listener)
                .negativeText(R.string.cancel)
                .show();
    }

    public static void showWeekSelectorDialog(final Context context, final MaterialDialog.SingleButtonCallback listener) {

        View view = LayoutInflater.from(context).inflate(R.layout.weekpicker, null);

        final NumberPicker numberPicker;
        numberPicker = (NumberPicker) view.findViewById(R.id.weeks);
        numberPicker.setMaxValue(12);
        numberPicker.setMinValue(1);
        numberPicker.setValue(MusicUtils.getIntPref(context, "numweeks", 2));

        getBuilder(context)
                .title(R.string.week_selector)
                .customView(view, false)
                .negativeText(R.string.cancel)
                .positiveText(R.string.picker_set)
                .onPositive((materialDialog, dialogAction) -> {
                    int numweeks;
                    numweeks = numberPicker.getValue();
                    MusicUtils.setIntPref(context, "numweeks", numweeks);
                    if (listener != null) {
                        listener.onClick(materialDialog, dialogAction);
                    }
                })
                .show();
    }

    public static void showRateSnackbar(final Activity activity, final View view) {
        //If the user hasn't pressed 'rate' in the past
        if (!SettingsManager.getInstance().getHasRated()) {
            //If this is the tenth launch, or a multiple of 50
            if (SettingsManager.getInstance().getLaunchCount() == 10 || (SettingsManager.getInstance().getLaunchCount() != 0 && SettingsManager.getInstance().getLaunchCount() % 50 == 0)) {
                Snackbar snackbar = Snackbar.make(view, R.string.snackbar_rate_text, Snackbar.LENGTH_INDEFINITE)
                        .setDuration(15000)
                        .setAction(R.string.snackbar_rate_action, v -> {
                            final String appPackageName = ShuttleApplication.getInstance().getPackageName();
                            ShuttleUtils.openShuttleLink(activity, appPackageName);
                            SettingsManager.getInstance().setHasRated();
                        })
                        .setActionTextColor(ColorUtils.getAccentColor());
                snackbar.show();

                TextView snackbarText = (TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                if (snackbarText != null) {
                    snackbarText.setTextColor(Color.WHITE);
                }
            }
        }
    }
}
