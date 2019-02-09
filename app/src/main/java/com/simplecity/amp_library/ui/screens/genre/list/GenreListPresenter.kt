package com.simplecity.amp_library.ui.screens.genre.list

import com.simplecity.amp_library.data.GenresRepository
import com.simplecity.amp_library.ui.common.Presenter
import com.simplecity.amp_library.ui.screens.genre.list.GenreListContract.View
import com.simplecity.amp_library.ui.screens.genre.menu.GenreMenuContract
import com.simplecity.amp_library.ui.screens.genre.menu.GenreMenuPresenter
import com.simplecity.amp_library.utils.ComparisonUtils
import com.simplecity.amp_library.utils.LogUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

class GenreListPresenter @Inject constructor(
    private val genreMenuPresenter: GenreMenuPresenter,
    private val genresRepository: GenresRepository
) : Presenter<GenreListContract.View>(),
    GenreListContract.Presenter,
    GenreMenuContract.Presenter by genreMenuPresenter {

    override fun bindView(view: View) {
        super.bindView(view)

        genreMenuPresenter.bindView(view)
    }

    override fun unbindView(view: View) {
        super.unbindView(view)

        genreMenuPresenter.unbindView(view)
    }

    override fun loadGenres() {
        addDisposable(genresRepository
            .getGenres()
            .map { genres -> genres.sortedWith(Comparator { a, b -> ComparisonUtils.compare(a.name, b.name) }) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { genres -> view?.setData(genres) },
                { error -> LogUtils.logException(TAG, "Error refreshing adapter items", error) }
            ))
    }

    companion object {
        const val TAG = "GenreListPresenter"
    }
}