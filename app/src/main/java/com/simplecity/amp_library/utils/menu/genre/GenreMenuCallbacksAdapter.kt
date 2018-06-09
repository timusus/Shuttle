package com.simplecity.amp_library.utils.menu.genre

import android.widget.Toast
import com.simplecity.amp_library.model.Genre
import com.simplecity.amp_library.ui.fragments.BaseFragment
import io.reactivex.disposables.CompositeDisposable

class GenreMenuCallbacksAdapter(val fragment: BaseFragment, val disposables: CompositeDisposable) : GenreMenuUtils.Callbacks {

    override fun showToast(message: String) {
        Toast.makeText(fragment.context, message, Toast.LENGTH_LONG).show()
    }

    override fun onPlaylistItemsInserted() {

    }

    override fun onQueueItemsInserted(message: String) {

    }

    override fun playNext(genre: Genre) {
        fragment.mediaManager.playNext(genre.songsObservable) { message -> Toast.makeText(fragment.context, message, Toast.LENGTH_LONG).show() }?.let { disposable ->
            disposables.add(disposable)
        }
    }
}