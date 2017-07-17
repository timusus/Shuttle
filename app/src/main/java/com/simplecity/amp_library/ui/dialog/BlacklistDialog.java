package com.simplecity.amp_library.ui.dialog;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.annimon.stream.Stream;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.model.BlacklistedSong;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.sql.databases.BlacklistHelper;
import com.simplecity.amp_library.sql.sqlbrite.SqlBriteUtils;
import com.simplecity.amp_library.ui.modelviews.BlacklistView;
import com.simplecity.amp_library.ui.modelviews.EmptyView;
import com.simplecity.amp_library.utils.ComparisonUtils;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecityapps.recycler_adapter.adapter.ViewModelAdapter;
import com.simplecityapps.recycler_adapter.model.ViewModel;

import java.util.Collections;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class BlacklistDialog {

    private static final String TAG = "BlacklistDialog";

    private BlacklistDialog() {
        //no instance
    }

    public static MaterialDialog getDialog(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_blacklist, null);

        final MaterialDialog.Builder builder = new MaterialDialog.Builder(context)
                .title(R.string.blacklist_title)
                .customView(view, false)
                .positiveText(R.string.close)
                .negativeText(R.string.pref_title_clear_blacklist)
                .onNegative((materialDialog, dialogAction) -> {
                    BlacklistHelper.deleteAllSongs();
                    Toast.makeText(context, R.string.blacklist_deleted, Toast.LENGTH_SHORT).show();
                });

        final MaterialDialog dialog = builder.build();

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        final ViewModelAdapter blacklistAdapter = new ViewModelAdapter();

        recyclerView.setAdapter(blacklistAdapter);

        Single<List<Song>> songsObservable = SqlBriteUtils.createObservableList(ShuttleApplication.getInstance(), Song::new, Song.getQuery()).first(Collections.emptyList());
        Single<List<BlacklistedSong>> blacklistObservable = BlacklistHelper.getBlacklistSongsObservable();

        BlacklistView.ClickListener listener = blacklistView -> {
            BlacklistHelper.deleteSong(blacklistView.song.id);
            blacklistAdapter.removeItem(blacklistView);
            if (blacklistAdapter.items.size() == 0) {
                dialog.dismiss();
            }
        };

        Single.zip(songsObservable, blacklistObservable, (songs, blacklistedSongs) ->
                Stream.of(songs)
                        .filter(song -> Stream.of(blacklistedSongs)
                                .anyMatch(blacklistedSong -> blacklistedSong.songId == song.id))
                        .sorted((a, b) -> ComparisonUtils.compare(a.albumArtistName, b.albumArtistName))
                        .sorted((a, b) -> ComparisonUtils.compareInt(b.year, a.year))
                        .sorted((a, b) -> ComparisonUtils.compareInt(a.track, b.track))
                        .sorted((a, b) -> ComparisonUtils.compareInt(a.discNumber, b.discNumber))
                        .sorted((a, b) -> ComparisonUtils.compare(a.albumName, b.albumName))
                        .map(song -> {
                            BlacklistView blacklistView = new BlacklistView(song);
                            blacklistView.setClickListener(listener);
                            return (ViewModel) blacklistView;
                        })
                        .toList())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(blacklistViews -> {
                    if (blacklistViews.size() == 0) {
                        blacklistAdapter.addItem(0, new EmptyView(R.string.blacklist_empty));
                    } else {
                        blacklistAdapter.setItems(blacklistViews);
                    }
                }, error -> LogUtils.logException(TAG, "Error setting blacklist vies", error));
        return dialog;
    }
}