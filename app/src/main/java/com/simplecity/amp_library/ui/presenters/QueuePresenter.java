package com.simplecity.amp_library.ui.presenters;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.view.MenuItem;
import com.cantrowitz.rxbroadcast.RxBroadcast;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.playback.MediaManager;
import com.simplecity.amp_library.playback.constants.InternalIntents;
import com.simplecity.amp_library.ui.views.QueueView;
import com.simplecity.amp_library.utils.PlaylistUtils;
import io.reactivex.BackpressureStrategy;
import io.reactivex.android.schedulers.AndroidSchedulers;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class QueuePresenter extends Presenter<QueueView> {

    private static final String TAG = "QueuePresenter";

    MediaManager musicUtils;

    @Inject
    public QueuePresenter(MediaManager musicUtils) {
        this.musicUtils = musicUtils;
    }

    @Override
    public void bindView(@NonNull QueueView view) {
        super.bindView(view);

        IntentFilter filter = new IntentFilter();
        filter.addAction(InternalIntents.META_CHANGED);
        addDisposable(RxBroadcast.fromBroadcast(ShuttleApplication.getInstance(), filter)
                .startWith(new Intent(InternalIntents.QUEUE_CHANGED))
                .toFlowable(BackpressureStrategy.LATEST)
                .debounce(150, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(intent -> {
                    QueueView queueView = getView();
                    if (queueView != null) {
                        queueView.updateQueuePosition(musicUtils.getQueuePosition(), false);
                    }
                }));

        filter = new IntentFilter();
        filter.addAction(InternalIntents.REPEAT_CHANGED);
        filter.addAction(InternalIntents.SHUFFLE_CHANGED);
        filter.addAction(InternalIntents.QUEUE_CHANGED);
        filter.addAction(InternalIntents.SERVICE_CONNECTED);
        addDisposable(RxBroadcast.fromBroadcast(ShuttleApplication.getInstance(), filter)
                .startWith(new Intent(InternalIntents.QUEUE_CHANGED))
                .toFlowable(BackpressureStrategy.LATEST)
                .debounce(150, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(intent -> loadData()));
    }

    public void saveQueue(Context context) {
        PlaylistUtils.createPlaylistDialog(context, musicUtils.getQueue(), null);
    }

    public void saveQueue(Context context, MenuItem item) {
        Playlist playlist = (Playlist) item.getIntent().getSerializableExtra(PlaylistUtils.ARG_PLAYLIST);
        PlaylistUtils.addToPlaylist(context, playlist, musicUtils.getQueue(), null);
    }

    public void clearQueue() {
        musicUtils.clearQueue();
    }

    public void removeFromQueue(int position) {
        QueueView queueView = getView();
        if (queueView != null) {
            queueView.onRemovedFromQueue(position);
        }
        musicUtils.removeFromQueue(position);
    }

    private void loadData() {
        QueueView queueView = getView();
        if (queueView != null) {
            queueView.setData(musicUtils.getQueue(), musicUtils.getQueuePosition());
        }
    }

    public void onSongClick(int position) {
        musicUtils.setQueuePosition(position);
        QueueView queueView = getView();
        if (queueView != null) {
            queueView.updateQueuePosition(position, true);
        }
    }
}
