package com.simplecity.amp_library.playback;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.annimon.stream.Stream;
import com.simplecity.amp_library.data.Repository;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.constants.InternalIntents;
import com.simplecity.amp_library.rx.UnsafeAction;
import com.simplecity.amp_library.rx.UnsafeConsumer;
import com.simplecity.amp_library.ui.screens.queue.QueueItem;
import com.simplecity.amp_library.ui.screens.queue.QueueItemKt;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;

public class QueueManager {

    private static final String TAG = "QueueManager";

    public @interface ShuffleMode {
        int OFF = 0;
        int ON = 1;
    }

    public @interface RepeatMode {
        int OFF = 0;
        int ONE = 1;
        int ALL = 2;
    }

    public @interface EnqueueAction {
        int NEXT = 0;
        int LAST = 1;
    }

    private final char hexDigits[] = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    @NonNull
    List<QueueItem> playlist = new ArrayList<>();

    @NonNull
    List<QueueItem> shuffleList = new ArrayList<>();

    @ShuffleMode
    int shuffleMode = ShuffleMode.OFF;

    @RepeatMode
    int repeatMode = RepeatMode.OFF;

    boolean queueReloading;

    boolean queueIsSaveable = true;

    int queuePosition = -1;
    int nextPlayPos = -1;

    private MusicService.Callbacks musicServiceCallbacks;

    private Repository.SongsRepository songsRepository;

    private PlaybackSettingsManager playbackSettingsManager;

    private SettingsManager settingsManager;

    public QueueManager(
            MusicService.Callbacks musicServiceCallbacks,
            Repository.SongsRepository songsRepository,
            PlaybackSettingsManager playbackSettingsManager,
            SettingsManager settingsManager
    ) {
        this.musicServiceCallbacks = musicServiceCallbacks;
        this.songsRepository = songsRepository;
        this.playbackSettingsManager = playbackSettingsManager;
        this.settingsManager = settingsManager;
    }

    private void notifyQueueChanged() {
        saveQueue(true);
        musicServiceCallbacks.notifyChange(InternalIntents.QUEUE_CHANGED);
    }

    private void notifyShuffleChanged() {
        musicServiceCallbacks.notifyChange(InternalIntents.SHUFFLE_CHANGED);
    }

    private void notifyMetaChanged() {
        musicServiceCallbacks.notifyChange(InternalIntents.META_CHANGED);
    }

    public void setRepeatMode(@RepeatMode int repeatMode) {
        this.repeatMode = repeatMode;
        saveQueue(false);
    }

    void setShuffleMode(@ShuffleMode int shuffleMode) {
        if (this.shuffleMode == shuffleMode && !getCurrentPlaylist().isEmpty()) {
            return;
        }
        if (shuffleMode == ShuffleMode.ON) {
            makeShuffleList();
        }

        this.shuffleMode = shuffleMode;
        notifyShuffleChanged();
        notifyQueueChanged();
        saveQueue(false);
    }

    public void load(@NonNull List<Song> songs, final int position, @NonNull UnsafeAction openCurrentAndNext) {

        List<QueueItem> queueItems = QueueItemKt.toQueueItems(songs);

        if (!playlist.equals(queueItems)) {
            playlist.clear();
            shuffleList.clear();

            playlist.addAll(queueItems);
            QueueItemKt.updateOccurrence(playlist);
        }

        queuePosition = position;

        if (shuffleMode == QueueManager.ShuffleMode.ON) {
            makeShuffleList();
        }

        openCurrentAndNext.run();

        notifyMetaChanged();
        notifyQueueChanged();
    }

    void previous() {
        if (queuePosition > 0) {
            queuePosition--;
        } else {
            queuePosition = getCurrentPlaylist().size() - 1;
        }
    }

    void moveQueueItem(int from, int to) {

        if (from >= getCurrentPlaylist().size()) {
            from = getCurrentPlaylist().size() - 1;
        }
        if (to >= getCurrentPlaylist().size()) {
            to = getCurrentPlaylist().size() - 1;
        }

        getCurrentPlaylist().add(to, getCurrentPlaylist().remove(from));

        if (from < to) {
            if (queuePosition == from) {
                queuePosition = to;
            } else if (queuePosition >= from && queuePosition <= to) {
                queuePosition--;
            }
        } else if (to < from) {
            if (queuePosition == from) {
                queuePosition = to;
            } else if (queuePosition >= to && queuePosition <= from) {
                queuePosition++;
            }
        }

        QueueItemKt.updateOccurrence(getCurrentPlaylist());

        notifyQueueChanged();
    }

    void clearQueue() {
        playlist.clear();
        shuffleList.clear();

        queuePosition = -1;
        nextPlayPos = -1;

        if (!settingsManager.getRememberShuffle()) {
            setShuffleMode(ShuffleMode.OFF);
        }

        notifyQueueChanged();
    }

    @NonNull
    List<QueueItem> getCurrentPlaylist() {
        if (shuffleMode == ShuffleMode.OFF) {
            return playlist;
        } else {
            return shuffleList;
        }
    }

    @Nullable
    QueueItem getCurrentQueueItem() {
        if (queuePosition >= 0 && queuePosition < getCurrentPlaylist().size()) {
            return getCurrentPlaylist().get(queuePosition);
        }

        return null;
    }

    @Nullable
    Song getCurrentSong() {
        QueueItem currentQueueItem = getCurrentQueueItem();
        if (currentQueueItem != null) {
            return currentQueueItem.getSong();
        } else {
            return null;
        }
    }

    /**
     * @return The next position to play, ot -1 if playback should complete.
     */
    int getNextPosition(boolean ignoreRepeatMode) {
        boolean queueComplete = queuePosition >= getCurrentPlaylist().size() - 1;

        if (ignoreRepeatMode) {
            return queueComplete ? 0 : queuePosition + 1;
        } else {
            switch (repeatMode) {
                case RepeatMode.ONE:
                    return queuePosition < 0 ? 0 : queuePosition;
                case RepeatMode.OFF:
                    return queueComplete ? -1 : queuePosition + 1;
                case RepeatMode.ALL:
                    return queueComplete ? 0 : queuePosition + 1;
                default:
                    return -1;
            }
        }
    }

    /**
     * Removes the first instance of the Song the playlist & shuffleList.
     */
    void removeQueueItem(QueueItem queueItem, UnsafeAction stop, UnsafeAction moveToNextTrack) {

        QueueItem currentQueueItem = getCurrentQueueItem();

        playlist.remove(queueItem);
        shuffleList.remove(queueItem);

        if (queueItem == currentQueueItem) {
            onCurrentSongRemoved(stop, moveToNextTrack);
        } else {
            queuePosition = getCurrentPlaylist().indexOf(currentQueueItem);
        }

        QueueItemKt.updateOccurrence(getCurrentPlaylist());

        notifyQueueChanged();
    }

    /**
     * Removes the range of Songs specified from the playlist & shuffleList. If a Song
     * within the range is the file currently being played, playback will move
     * to the next Song after the range.
     *
     * @param queueItems the QueueItems to remove
     */
    void removeQueueItems(@NonNull List<QueueItem> queueItems, UnsafeAction stop, UnsafeAction moveToNextTrack) {

        playlist.removeAll(queueItems);
        shuffleList.removeAll(queueItems);

        QueueItemKt.updateOccurrence(getCurrentPlaylist());

        if (queueItems.contains(getCurrentQueueItem())) {
            /*
             * If we remove a list of songs from the current queue, and that list contains our currently
             * playing song, we need to figure out which song should play next. We'll play the first song
             * that comes after the list of songs to be removed.
             *
             * In this example, let's say Song 7 is currently playing
             *
             * Playlist:                    [Song 3,    Song 4,     Song 5,     Song 6,     Song 7,     Song 8]
             * Indices:                     [0,         1,          2,          3,          4,          5]
             *
             * Remove;                                              [Song 5,     Song 6,     Song 7]
             *
             * First removed song:                                  Song 5
             * Index of first removed song:                         2
             *
             * Playlist after removal:      [Song 3,    Song 4,     Song 8]
             * Indices:                     [0,         1,          2]
             *
             *
             * So after the removal, we'll play index 2, which is Song 8.
             */
            queuePosition = Collections.indexOfSubList(getCurrentPlaylist(), queueItems);
            onCurrentSongRemoved(stop, moveToNextTrack);
        } else {
            queuePosition = getCurrentPlaylist().indexOf(getCurrentQueueItem());
        }

        notifyQueueChanged();
    }

    void removeSongs(@NonNull List<Song> songs, UnsafeAction stop, UnsafeAction moveToNextTrack) {
        List<QueueItem> queueItems = Stream.of(playlist).filter(value -> songs.contains(value.getSong())).toList();
        removeQueueItems(queueItems, stop, moveToNextTrack);
    }

    private void onCurrentSongRemoved(UnsafeAction stop, UnsafeAction moveToNextTrack) {
        if (getCurrentPlaylist().isEmpty()) {
            queuePosition = -1;
            stop.run();
        } else {
            if (queuePosition >= getCurrentPlaylist().size()) {
                queuePosition = 0;
            }
            moveToNextTrack.run();
        }
        notifyMetaChanged();
    }

    /**
     * Queues a new list for playback
     *
     * @param songs The list to queue
     * @param action The action to take
     */
    public void enqueue(List<Song> songs, @EnqueueAction int action, UnsafeAction setNextTrack, UnsafeAction openCurrentAndNext) {

        List<QueueItem> queueItems = QueueItemKt.toQueueItems(songs);

        switch (action) {
            case EnqueueAction.NEXT:
                List<QueueItem> otherList = getCurrentPlaylist() == playlist ? shuffleList : playlist;
                getCurrentPlaylist().addAll(queuePosition + 1, queueItems);
                otherList.addAll(queueItems);

                QueueItemKt.updateOccurrence(getCurrentPlaylist());

                setNextTrack.run();
                notifyQueueChanged();
                break;
            case EnqueueAction.LAST:
                playlist.addAll(queueItems);
                shuffleList.addAll(queueItems);

                QueueItemKt.updateOccurrence(getCurrentPlaylist());

                notifyQueueChanged();
                break;
        }
        if (queuePosition < 0) {
            queuePosition = 0;
            openCurrentAndNext.run();
            notifyMetaChanged();
        }
    }

    /**
     * Saves our state to preferences, including the queue position, repeat mode & shuffle mode.
     *
     * @param saveQueue boolean whether to serialize the playlist/shuffleList and store those in preferences
     * as well.
     */
    void saveQueue(boolean saveQueue) {

        if (!queueIsSaveable) {
            return;
        }

        if (queueReloading) {
            return;
        }

        if (saveQueue) {
            playbackSettingsManager.setQueueList(serializePlaylist(playlist));
            if (shuffleMode == ShuffleMode.ON) {
                playbackSettingsManager.setShuffleList(serializePlaylist(shuffleList));
            }
        }

        playbackSettingsManager.setQueuePosition(queuePosition);
        playbackSettingsManager.setRepeatMode(repeatMode);
        playbackSettingsManager.setShuffleMode(shuffleMode);
    }

    Disposable reloadQueue(@NonNull Function0<Unit> onComplete) {
        queueReloading = true;

        shuffleMode = playbackSettingsManager.getShuffleMode();
        repeatMode = playbackSettingsManager.getRepeatMode();

        return songsRepository.getAllSongs()
                .first(Collections.emptyList())
                .map(QueueItemKt::toQueueItems)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((UnsafeConsumer<List<QueueItem>>) queueItems -> {
                    String queueList = playbackSettingsManager.getQueueList();
                    if (queueList != null) {
                        playlist = deserializePlaylist(queueList, queueItems);

                        final int queuePosition = playbackSettingsManager.getQueuePosition();

                        if (queuePosition < 0 || queuePosition >= playlist.size()) {
                            // The saved playlist is bogus, discard it
                            playlist.clear();
                            queueReloading = false;
                            onComplete.invoke();
                            return;
                        }

                        QueueManager.this.queuePosition = queuePosition;

                        if (repeatMode != RepeatMode.ALL && repeatMode != RepeatMode.ONE) {
                            repeatMode = RepeatMode.OFF;
                        }
                        if (shuffleMode != ShuffleMode.ON) {
                            shuffleMode = ShuffleMode.OFF;
                        }
                        if (shuffleMode == ShuffleMode.ON) {
                            queueList = playbackSettingsManager.getShuffleList();
                            if (queueList != null) {
                                shuffleList = deserializePlaylist(queueList, queueItems);

                                if (queuePosition >= shuffleList.size()) {
                                    // The saved playlist is bogus, discard it
                                    shuffleList.clear();
                                    queueReloading = false;
                                    onComplete.invoke();
                                    return;
                                }
                            }
                        }

                        if (QueueManager.this.queuePosition < 0 || QueueManager.this.queuePosition >= getCurrentPlaylist().size()) {
                            QueueManager.this.queuePosition = 0;
                        }
                    }
                    queueReloading = false;
                    onComplete.invoke();
                }, error -> {
                    queueReloading = false;
                    onComplete.invoke();
                    LogUtils.logException(TAG, "Reloading queue", error);
                });
    }

    /**
     * Converts a playlist to a String which can be saved to SharedPrefs
     */
    private String serializePlaylist(List<QueueItem> queueItems) {

        // The current playlist is saved as a list of "reverse hexadecimal"
        // numbers, which we can generate faster than normal decimal or
        // hexadecimal numbers, which in turn allows us to save the playlist
        // more often without worrying too much about performance.

        StringBuilder q = new StringBuilder();

        List<Song> songs = Stream.of(queueItems).map(QueueItem::getSong).toList();
        int len = songs.size();
        for (int i = 0; i < len; i++) {
            long n = songs.get(i).id;
            if (n >= 0) {
                if (n == 0) {
                    q.append("0;");
                } else {
                    while (n != 0) {
                        final int digit = (int) (n & 0xf);
                        n >>>= 4;
                        q.append(hexDigits[digit]);
                    }
                    q.append(";");
                }
            }
        }

        return q.toString();
    }

    /**
     * Converts a string representation of a playlist from SharedPrefs into a list of songs.
     */
    private List<QueueItem> deserializePlaylist(String listString, List<QueueItem> queueItems) {
        List<Long> ids = new ArrayList<>();
        int n = 0;
        int shift = 0;
        for (int i = 0; i < listString.length(); i++) {
            char c = listString.charAt(i);
            if (c == ';') {
                ids.add((long) n);
                n = 0;
                shift = 0;
            } else {
                if (c >= '0' && c <= '9') {
                    n += ((c - '0') << shift);
                } else if (c >= 'a' && c <= 'f') {
                    n += ((10 + c - 'a') << shift);
                } else {
                    // bogus playlist data
                    playlist.clear();
                    break;
                }
                shift += 4;
            }
        }

        Map<Integer, Song> map = new TreeMap<>();

        Stream.of(queueItems).map(QueueItem::getSong).forEach(song -> {
            int index = ids.indexOf(song.id);
            if (index != -1) {
                map.put(index, song);
            }
        });

        return QueueItemKt.toQueueItems(new ArrayList<>(map.values()));
    }

    void makeShuffleList() {
        if (playlist.isEmpty()) {
            return;
        }

        shuffleList = new ArrayList<>(playlist);
        QueueItem currentSong = null;
        if (queuePosition >= 0 && queuePosition < shuffleList.size()) {
            currentSong = shuffleList.remove(queuePosition);
        }

        Collections.shuffle(shuffleList);

        if (currentSong != null) {
            shuffleList.add(0, currentSong);
        }
        queuePosition = 0;

        QueueItemKt.updateOccurrence(shuffleList);
    }
}