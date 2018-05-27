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
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

public class QueuePresenter extends Presenter<QueueView> {

    private static final String TAG = "QueuePresenter";

    MediaManager mediaManager;

    @Inject
    public QueuePresenter(MediaManager mediaManager) {
        this.mediaManager = mediaManager;
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
                        queueView.updateQueuePosition(mediaManager.getQueuePosition(), false);
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
        PlaylistUtils.createPlaylistDialog(context, mediaManager.getQueue(), null);
    }

    public void saveQueue(Context context, MenuItem item) {
        Playlist playlist = (Playlist) item.getIntent().getSerializableExtra(PlaylistUtils.ARG_PLAYLIST);
        PlaylistUtils.addToPlaylist(context, playlist, mediaManager.getQueue(), null);
    }

    public void clearQueue() {
        mediaManager.clearQueue();
    }

    public void removeFromQueue(int position) {
        QueueView queueView = getView();
        if (queueView != null) {
            queueView.onRemovedFromQueue(position);
        }
        mediaManager.removeFromQueue(position);
    }

    private void loadData() {
        QueueView queueView = getView();
        if (queueView != null) {
            queueView.setData(mediaManager.getQueue(), mediaManager.getQueuePosition());
        }
    }

    public void onSongClick(int position) {
        mediaManager.setQueuePosition(position);
        QueueView queueView = getView();
        if (queueView != null) {
            queueView.updateQueuePosition(position, true);
        }
    }
}
