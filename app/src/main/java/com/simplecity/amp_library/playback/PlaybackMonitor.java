package com.simplecity.amp_library.playback;

import com.simplecity.amp_library.utils.MusicServiceConnectionUtils;
import com.simplecity.amp_library.utils.MusicUtils;

import java.util.concurrent.TimeUnit;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;

public class PlaybackMonitor {

    private static final String TAG = "PlaybackMonitor";

    private static PlaybackMonitor instance;

    private Flowable<Float> progressObservable;
    private Flowable<Long> currentTimeObservable;

    public static PlaybackMonitor getInstance() {
        if (instance == null) {
            instance = new PlaybackMonitor();
        }
        return instance;
    }

    private PlaybackMonitor() {
        progressObservable = Flowable.defer(() -> Observable.interval(32, TimeUnit.MILLISECONDS)
                .filter(aLong -> {
                    if (MusicServiceConnectionUtils.sServiceBinder != null
                            && MusicServiceConnectionUtils.sServiceBinder.getService() != null) {
                        if (MusicUtils.getDuration() > 0) {
                            return true;
                        }
                    }
                    return false;
                })
                .map(aLong -> (float) MusicUtils.getPosition() / (float) MusicUtils.getDuration())
                .toFlowable(BackpressureStrategy.DROP))
                .share();

        currentTimeObservable = Flowable.defer(() -> Observable.interval(150, TimeUnit.MILLISECONDS)
                .filter(aLong -> MusicServiceConnectionUtils.sServiceBinder != null
                        && MusicServiceConnectionUtils.sServiceBinder.getService() != null)
                .map(time -> MusicUtils.getPosition())
                .toFlowable(BackpressureStrategy.DROP))
                .share();
    }

    public Flowable<Float> getProgressObservable() {
        return progressObservable;
    }

    public Flowable<Long> getCurrentTimeObservable() {
        return currentTimeObservable;
    }

}