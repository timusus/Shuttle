package com.simplecity.amp_library.ui.fragments;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.RequestManager;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.dagger.module.FragmentModule;
import com.simplecity.amp_library.ui.presenters.QueuePagerPresenter;
import com.simplecity.amp_library.ui.views.QueuePagerView;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecityapps.recycler_adapter.adapter.ViewModelAdapter;
import com.simplecityapps.recycler_adapter.model.ViewModel;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import rx.schedulers.Schedulers;

public class QueuePagerFragment extends BaseFragment implements
        RequestManagerProvider,
        QueuePagerView {

    private final String TAG = "QueuePagerFragment";

    private Unbinder unbinder;

    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;

    @Inject
    RequestManager requestManager;

    @Inject
    QueuePagerPresenter queuePagerPresenter;

    private ViewModelAdapter viewModelAdapter;

    public static QueuePagerFragment newInstance() {
        Bundle args = new Bundle();
        QueuePagerFragment fragment = new QueuePagerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public QueuePagerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ShuttleApplication.getInstance().getAppComponent()
                .plus(new FragmentModule(this))
                .inject(this);

        viewModelAdapter = new ViewModelAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_queue_pager, container, false);

        unbinder = ButterKnife.bind(this, rootView);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(viewModelAdapter);
        SnapHelper snapHelper = new PagerSnapHelper() {
            @Override
            public int findTargetSnapPosition(RecyclerView.LayoutManager layoutManager, int velocityX, int velocityY) {

                int snapPosition = super.findTargetSnapPosition(layoutManager, velocityX, velocityY);

                rx.Observable.defer(() -> {
                    if (MusicUtils.getQueuePosition() != snapPosition) {
                        MusicUtils.setQueuePosition(snapPosition);
                    }
                    return rx.Observable.empty();
                })
                        .delaySubscription(150, TimeUnit.MILLISECONDS)
                        .subscribeOn(Schedulers.io())
                        .subscribe();

                return snapPosition;
            }
        };
        snapHelper.attachToRecyclerView(recyclerView);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        queuePagerPresenter.bindView(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        queuePagerPresenter.unbindView(this);
    }

    @Override
    public void onDestroyView() {
        unbinder.unbind();
        super.onDestroyView();
    }

    @Override
    public RequestManager getRequestManager() {
        return requestManager;
    }

    @Override
    public void loadData(List<ViewModel> viewModels, int position) {
        viewModelAdapter.items.clear();
        viewModelAdapter.items.addAll(viewModels);
        viewModelAdapter.notifyDataSetChanged();
        recyclerView.getLayoutManager().scrollToPosition(position);
    }

    @Override
    public void updateQueuePosition(int position) {
        recyclerView.getLayoutManager().scrollToPosition(position);
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}