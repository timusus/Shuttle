package com.simplecity.amp_library.ui.screens.queue.menu

import com.simplecity.amp_library.ui.screens.songs.menu.SongMenuContract
import com.simplecity.amp_library.utils.menu.queue.QueueMenuCallbacks

interface QueueMenuContract : SongMenuContract {

    interface View : SongMenuContract.View {

    }

    interface Presenter : SongMenuContract.Presenter, QueueMenuCallbacks {

    }
}