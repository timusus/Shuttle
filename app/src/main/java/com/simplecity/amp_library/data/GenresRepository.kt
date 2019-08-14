package com.simplecity.amp_library.data

import com.jakewharton.rxrelay2.BehaviorRelay
import com.simplecity.amp_library.ShuttleApplication
import com.simplecity.amp_library.model.Genre
import com.simplecity.amp_library.sql.sqlbrite.SqlBriteUtils
import com.simplecity.amp_library.utils.LogUtils
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GenresRepository @Inject constructor(private val application: ShuttleApplication) : Repository.GenresRepository {

    private var genresSubscription: Disposable? = null
    private val genresRelay = BehaviorRelay.create<List<Genre>>()

    override fun getGenres(): Observable<List<Genre>> {
        if (genresSubscription == null || genresSubscription?.isDisposed == true) {
            genresSubscription = SqlBriteUtils.createObservableList<Genre>(application, { Genre(it) }, Genre.getQuery())
                .subscribe(
                    genresRelay,
                    Consumer { error -> LogUtils.logException(PlaylistsRepository.TAG, "Failed to get genres", error) }
                )
        }

        return genresRelay.subscribeOn(Schedulers.io())
    }

    companion object {
        const val TAG = "GenresRepository"
    }
}