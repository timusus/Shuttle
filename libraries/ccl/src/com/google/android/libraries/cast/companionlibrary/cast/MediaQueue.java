/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.libraries.cast.companionlibrary.cast;

import com.google.android.gms.cast.MediaQueueItem;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A simple class to model a queue for bookkeeping purposes.
 */
public class MediaQueue {

    private List<MediaQueueItem> mQueueItems = new CopyOnWriteArrayList<>();
    public static final int INVALID_POSITION = -1;
    private MediaQueueItem mCurrentItem;
    private boolean mShuffle;
    private int mRepeatMode;

    public MediaQueue(List<MediaQueueItem> queueItems,
            MediaQueueItem currentItem, boolean shuffle, int repeatMode) {
        mQueueItems = queueItems;
        mCurrentItem = currentItem;
        mShuffle = shuffle;
        mRepeatMode = repeatMode;
    }

    public final List<MediaQueueItem> getQueueItems() {
        return mQueueItems;
    }

    public final void setQueueItems(List<MediaQueueItem> queue) {
        if (queue == null) {
            mQueueItems = null;
        } else {
            mQueueItems = new CopyOnWriteArrayList<>(queue);
        }
    }

    public final MediaQueueItem getCurrentItem() {
        return mCurrentItem;
    }

    public final void setCurrentItem(MediaQueueItem currentItem) {
        mCurrentItem = currentItem;
    }

    public final boolean isShuffle() {
        return mShuffle;
    }

    public final void setShuffle(boolean shuffle) {
        mShuffle = shuffle;
    }

    public final int getRepeatMode() {
        return mRepeatMode;
    }

    public final void setRepeatMode(int repeatMode) {
        mRepeatMode = repeatMode;
    }

    /**
     * Returns the size of queue, or 0 if it is {@code null}
     */
    public final int getCount() {
        return mQueueItems == null || mQueueItems.isEmpty() ? 0 : mQueueItems.size();
    }

    /**
     * Returns {@code true} if and only if the queue is empty or {@code null}
     */
    public final boolean isEmpty() {
        return mQueueItems == null || mQueueItems.isEmpty();
    }

    /**
     * Returns the position of the current item in the queue. If the queue is {@code null}, it
     * will return {@link #INVALID_POSITION}. If the queue is empty, it returns 0.
     */
    public final int getCurrentItemPosition() {
        if (mQueueItems == null) {
            return INVALID_POSITION;
        }

        if (mQueueItems.isEmpty()) {
            return 0;
        }

        return mQueueItems.indexOf(mCurrentItem);
    }
}
