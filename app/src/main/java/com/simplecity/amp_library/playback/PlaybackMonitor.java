package com.simplecity.amp_library.playback;

import com.simplecity.amp_library.utils.MusicServiceConnectionUtils;
import com.simplecity.amp_library.utils.MusicUtils;

import java.util.concurrent.TimeUnit;

import rx.Observable;

public class PlaybackMonitor {

    private static PlaybackMonitor instance;

    private Observable<Float> progressObservable;
    private Observable<Long> currentTimeObservable;

    public static PlaybackMonitor getInstance() {
        if (instance == null) {
            instance = new PlaybackMonitor();
        }
        return instance;
    }

    private PlaybackMonitor() {

        progressObservable = Observable.defer(() -> Observable.interval(32, TimeUnit.MILLISECONDS)
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
                .onBackpressureDrop())
                .share();

        currentTimeObservable = Observable.defer(() -> Observable.interval(150, TimeUnit.MILLISECONDS)
                .filter(aLong -> MusicServiceConnectionUtils.sServiceBinder != null
                        && MusicServiceConnectionUtils.sServiceBinder.getService() != null)
                .map(time -> MusicUtils.getPosition())
                .onBackpressureDrop())
                .share();
    }

    public Observable<Float> getProgressObservable() {
        return progressObservable;
    }

    public Observable<Long> getCurrentTimeObservable() {
        return currentTimeObservable;
    }

}