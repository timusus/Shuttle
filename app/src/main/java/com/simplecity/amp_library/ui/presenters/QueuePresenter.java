package com.simplecity.amp_library.ui.presenters;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.view.View;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.bumptech.glide.RequestManager;
import com.f2prateek.rx.receivers.RxBroadcastReceiver;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.MusicService;
import com.simplecity.amp_library.ui.modelviews.SongView;
import com.simplecity.amp_library.ui.views.QueueView;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecityapps.recycler_adapter.model.ViewModel;

import java.util.List;

import javax.inject.Inject;

import rx.android.schedulers.AndroidSchedulers;

public class QueuePresenter extends Presenter<QueueView> {

    @Inject
    RequestManager requestManager;

    @Inject
    public QueuePresenter(Context context) {
    }

    @Override
    public void bindView(@NonNull QueueView view) {
        super.bindView(view);

        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.InternalIntents.META_CHANGED);
        filter.addAction(MusicService.InternalIntents.REPEAT_CHANGED);
        filter.addAction(MusicService.InternalIntents.SHUFFLE_CHANGED);
        filter.addAction(MusicService.InternalIntents.QUEUE_CHANGED);
        filter.addAction(MusicService.InternalIntents.SERVICE_CONNECTED);

        addSubcscription(RxBroadcastReceiver.create(ShuttleApplication.getInstance(), filter)
                .startWith(new Intent(MusicService.InternalIntents.QUEUE_CHANGED))
                .onBackpressureLatest()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(intent -> {
                    final String action = intent.getAction();

                    QueueView queueView = getView();
                    if (queueView == null) {
                        return;
                    }

                    if (action != null) {
                        switch (action) {
                            case MusicService.InternalIntents.META_CHANGED:
                                queueView.updateQueuePosition(MusicUtils.getQueuePosition());
                                break;

                            case MusicService.InternalIntents.QUEUE_CHANGED:
                                if (!intent.getBooleanExtra(MusicService.FROM_USER, false)) {
                                    loadData();
                                }
                                break;
                            case MusicService.InternalIntents.SERVICE_CONNECTED:
                            case MusicService.InternalIntents.REPEAT_CHANGED:
                            case MusicService.InternalIntents.SHUFFLE_CHANGED:
                                loadData();
                                break;
                        }
                    }
                }));
    }

    public void saveQueue(Context context) {
        PlaylistUtils.createPlaylistDialog(context, MusicUtils.getQueue());
    }

    public void saveQueue(Context context, Playlist playlist) {
        PlaylistUtils.addToPlaylist(context, playlist, MusicUtils.getQueue());
    }

    public void clearQueue() {
        MusicUtils.clearQueue();
    }

    private void loadData() {
        QueueView queueView = getView();
        List<ViewModel> items = Stream.of(MusicUtils.getQueue())
                .map(song -> {
                    SongView songView = new SongView(song, requestManager);
                    songView.setClickListener(clickListener);
                    songView.setShowAlbumArt(true);
                    songView.setEditable(true);
                    return songView;
                })
                .collect(Collectors.toList());
        if (queueView != null) {
            queueView.loadData(items, MusicUtils.getQueuePosition());
        }
    }

    private SongView.ClickListener clickListener = new SongView.ClickListener() {
        @Override
        public void onSongClick(Song song, SongView.ViewHolder holder) {
            MusicUtils.setQueuePosition(holder.getAdapterPosition());
            QueueView queueView = getView();
            if (queueView != null) {
                queueView.setCurrentQueueItem(holder.getAdapterPosition());
            }
        }

        @Override
        public boolean onSongLongClick(Song song) {
            return false;
        }

        @Override
        public void onSongOverflowClick(View v, Song song) {

        }

        @Override
        public void onStartDrag(SongView.ViewHolder holder) {
            QueueView queueView = getView();
            if (queueView != null) {
                queueView.startDrag(holder);
            }
        }
    };
}