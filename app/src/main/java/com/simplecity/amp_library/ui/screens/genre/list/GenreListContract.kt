package com.simplecity.amp_library.ui.screens.genre.list

import com.simplecity.amp_library.model.Genre
import com.simplecity.amp_library.ui.screens.genre.menu.GenreMenuContract

interface GenreListContract {

    interface View : GenreMenuContract.View {

        fun setData(genres: List<Genre>)
    }

    interface Presenter {

        fun loadGenres()
    }
}