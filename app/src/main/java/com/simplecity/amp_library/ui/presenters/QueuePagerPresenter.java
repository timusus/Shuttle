package com.simplecity.amp_library.ui.presenters;

import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.bumptech.glide.RequestManager;
import com.cantrowitz.rxbroadcast.RxBroadcast;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.playback.MediaManager;
import com.simplecity.amp_library.playback.constants.InternalIntents;
import com.simplecity.amp_library.ui.modelviews.QueuePagerItemView;
import com.simplecity.amp_library.ui.views.QueuePagerView;
import com.simplecityapps.recycler_adapter.model.ViewModel;
import io.reactivex.BackpressureStrategy;
import io.reactivex.android.schedulers.AndroidSchedulers;
import java.util.List;
import javax.inject.Inject;

public class QueuePagerPresenter extends Presenter<QueuePagerView> {

    @Inject
    RequestManager requestManager;

    @Inject
    MediaManager mediaManager;

    @Inject
    public QueuePagerPresenter() {

    }

    @Override
    public void bindView(@NonNull QueuePagerView view) {
        super.bindView(view);

        IntentFilter filter = new IntentFilter();
        filter.addAction(InternalIntents.META_CHANGED);
        filter.addAction(InternalIntents.REPEAT_CHANGED);
        filter.addAction(InternalIntents.SHUFFLE_CHANGED);
        filter.addAction(InternalIntents.QUEUE_CHANGED);
        filter.addAction(InternalIntents.SERVICE_CONNECTED);

        addDisposable(RxBroadcast.fromBroadcast(ShuttleApplication.getInstance(), filter)
                .startWith(new Intent(InternalIntents.QUEUE_CHANGED))
                .toFlowable(BackpressureStrategy.LATEST)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(intent -> {
                    final String action = intent.getAction();

                    QueuePagerView queuePagerView = getView();
                    if (queuePagerView == null) {
                        return;
                    }

                    if (action != null) {
                        switch (action) {
                            case InternalIntents.META_CHANGED:
                                queuePagerView.updateQueuePosition(mediaManager.getQueuePosition());
                                break;
                            case InternalIntents.REPEAT_CHANGED:
                            case InternalIntents.SHUFFLE_CHANGED:
                            case InternalIntents.QUEUE_CHANGED:
                            case InternalIntents.SERVICE_CONNECTED:

                                List<ViewModel> items = Stream.of(mediaManager.getQueue())
                                        .map(queueItem -> new QueuePagerItemView(queueItem.getSong(), requestManager))
                                        .collect(Collectors.toList());

                                queuePagerView.loadData(items, mediaManager.getQueuePosition());
                                break;
                        }
                    }
                }));
    }
}