package com.simplecity.amp_library.ui.screens.suggested

import com.simplecity.amp_library.ui.screens.album.menu.AlbumMenuContract
import com.simplecity.amp_library.ui.screens.songs.menu.SongMenuContract
import com.simplecity.amp_library.ui.screens.suggested.SuggestedPresenter.SuggestedData

interface SuggestedContract {

    interface Presenter {

        fun loadData()

    }

    interface View : AlbumMenuContract.View, SongMenuContract.View {

        fun setData(suggestedData: SuggestedData)
    }

}