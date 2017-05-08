package com.simplecity.amp_library.ui.presenters;

import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.f2prateek.rx.receivers.RxBroadcastReceiver;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.playback.MusicService;
import com.simplecity.amp_library.ui.fragments.RequestManagerProvider;
import com.simplecity.amp_library.ui.modelviews.QueuePagerItemView;
import com.simplecity.amp_library.ui.views.QueuePagerView;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecityapps.recycler_adapter.model.ViewModel;

import java.util.List;

import rx.android.schedulers.AndroidSchedulers;

public class QueuePresenter extends Presenter<QueuePagerView> {

    private RequestManagerProvider requestManagerProvider;

    public QueuePresenter(RequestManagerProvider provider) {
        requestManagerProvider = provider;
    }

    @Override
    public void bindView(@NonNull QueuePagerView view) {
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

                    QueuePagerView queuePagerView = getView();
                    if (queuePagerView == null) {
                        return;
                    }

                    if (action != null) {
                        switch (action) {
                            case MusicService.InternalIntents.META_CHANGED:
                                queuePagerView.updateQueuePosition(MusicUtils.getQueuePosition());
                                break;
                            case MusicService.InternalIntents.REPEAT_CHANGED:
                            case MusicService.InternalIntents.SHUFFLE_CHANGED:
                            case MusicService.InternalIntents.QUEUE_CHANGED:
                            case MusicService.InternalIntents.SERVICE_CONNECTED:

                                List<ViewModel> items = Stream.of(MusicUtils.getQueue())
                                        .map(song -> new QueuePagerItemView(song, requestManagerProvider))
                                        .collect(Collectors.toList());

                                queuePagerView.loadData(items, MusicUtils.getQueuePosition());
                                break;
                        }
                    }
                }));
    }
}