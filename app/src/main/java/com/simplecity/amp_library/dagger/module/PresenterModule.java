package com.simplecity.amp_library.dagger.module;

import com.simplecity.amp_library.search.SearchPresenter;
import com.simplecity.amp_library.search.SearchView;
import com.simplecity.amp_library.ui.presenters.PlayerPresenter;
import com.simplecity.amp_library.ui.presenters.Presenter;
import com.simplecity.amp_library.ui.presenters.QueuePagerPresenter;
import com.simplecity.amp_library.ui.queue.QueueContract;
import com.simplecity.amp_library.ui.queue.QueuePresenter;
import com.simplecity.amp_library.ui.views.PlayerView;
import com.simplecity.amp_library.ui.views.QueuePagerView;
import dagger.Binds;
import dagger.Module;

@Module
public abstract class PresenterModule {

    @Binds
    abstract Presenter<PlayerView> bindPlayerPresenter(PlayerPresenter playerPresenter);

    @Binds
    abstract Presenter<QueuePagerView> bindQueuePagerPresenter(QueuePagerPresenter queuePagerPresenter);

    @Binds
    abstract Presenter<QueueContract.View> bindQueuePresenter(QueuePresenter queuePresenter);

    @Binds
    abstract Presenter<SearchView> bindSearchPresenter(SearchPresenter queuePresenter);
}