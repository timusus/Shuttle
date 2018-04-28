package com.simplecity.amp_library.utils.menu.genre

import android.support.v4.app.Fragment
import android.widget.Toast
import com.simplecity.amp_library.model.Genre
import com.simplecity.amp_library.utils.MusicUtils
import io.reactivex.disposables.CompositeDisposable

class GenreMenuFragmentHelper(val fragment: Fragment, val disposables: CompositeDisposable) {

    val callbacks = object : GenreMenuUtils.Callbacks {
        override fun showToast(message: String) {
            Toast.makeText(fragment.context, message, Toast.LENGTH_LONG).show()
        }

        override fun onPlaylistItemsInserted() {

        }

        override fun onQueueItemsInserted(message: String) {

        }

        override fun playNext(genre: Genre) {
            MusicUtils.playNext(genre.songsObservable) { message -> Toast.makeText(fragment.context, message, Toast.LENGTH_LONG).show() }
        }
    }
}