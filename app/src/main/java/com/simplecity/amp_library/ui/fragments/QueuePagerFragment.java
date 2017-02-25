package com.simplecity.amp_library.ui.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.MusicService;
import com.simplecity.amp_library.utils.MusicServiceConnectionUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.PermissionUtils;

import java.util.ArrayList;
import java.util.List;

public class QueuePagerFragment extends BaseFragment implements
        ViewPager.OnPageChangeListener,
        RequestManagerProvider {

    private final String TAG = "QueuePagerFragment";

    private View mRootView;

    private ViewPager mPager;

    private ImagePagerAdapter mAdapter;

    private RequestManager requestManager;

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

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MusicService.InternalIntents.META_CHANGED);
        intentFilter.addAction(MusicService.InternalIntents.REPEAT_CHANGED);
        intentFilter.addAction(MusicService.InternalIntents.SHUFFLE_CHANGED);
        intentFilter.addAction(MusicService.InternalIntents.QUEUE_CHANGED);
        getActivity().registerReceiver(mReceiver, intentFilter);

        if (requestManager == null) {
            requestManager = Glide.with(this);
        }
    }

    @Override
    public void onPause() {

        if (getParentFragment() != null && getParentFragment() instanceof PlayerFragment) {
            ((PlayerFragment) getParentFragment()).setDragView(null);
        }

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getParentFragment() != null && getParentFragment() instanceof PlayerFragment) {
            ((PlayerFragment) getParentFragment()).setDragView(mRootView);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mRootView == null) {
            mRootView = inflater.inflate(R.layout.fragment_queue_pager, container, false);
            mPager = (ViewPager) mRootView.findViewById(R.id.pager);
            resetAdapter();
        }
        return mRootView;
    }

    @Override
    public void onDestroy() {
        getActivity().unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    private void refreshAdapterItems() {
        PermissionUtils.RequestStoragePermissions(() -> {
            if (getActivity() != null && isAdded()) {
                mAdapter.setData(MusicUtils.getQueue());
                mPager.clearOnPageChangeListeners();
                mPager.setAdapter(mAdapter);
                mPager.setCurrentItem(MusicUtils.getQueuePosition());
                mPager.addOnPageChangeListener(this);
            }
        });
    }

    @Override
    public RequestManager getRequestManager() {
        return requestManager;
    }

    private static class ImagePagerAdapter extends FragmentStatePagerAdapter {

        private List<Song> songs = new ArrayList<>();

        ImagePagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        public void setData(List<Song> songs) {
            this.songs.clear();
            this.songs.addAll(songs);
            notifyDataSetChanged();
        }

        public void clear() {
            songs.clear();
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return songs.size();
        }

        @Override
        public Fragment getItem(int position) {
            return ArtworkFragment.newInstance(songs.get(position));
        }
    }

    public void updateQueuePosition() {
        if (mPager == null) {
            return;
        }
        mPager.clearOnPageChangeListeners();
        mPager.setCurrentItem(MusicUtils.getQueuePosition(), true);
        mPager.addOnPageChangeListener(this);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        int oldPos = MusicUtils.getQueuePosition();
        if (position > oldPos) {
            MusicUtils.next();
        } else if (position < oldPos) {
            MusicUtils.previous(false);
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case MusicService.InternalIntents.META_CHANGED:
                    updateQueuePosition();
                    break;
                case MusicService.InternalIntents.REPEAT_CHANGED:
                case MusicService.InternalIntents.SHUFFLE_CHANGED:
                case MusicService.InternalIntents.QUEUE_CHANGED:
                    resetAdapter();
                    break;
            }
        }
    };

    public void resetAdapter() {
        if (MusicServiceConnectionUtils.sServiceBinder != null) {
            mAdapter = new ImagePagerAdapter(getChildFragmentManager());
            refreshAdapterItems();
        }
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}