package com.simplecity.amp_library.ui.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.Genre;
import com.simplecity.amp_library.ui.adapters.SectionedAdapter;
import com.simplecity.amp_library.ui.modelviews.EmptyView;
import com.simplecity.amp_library.ui.modelviews.GenreView;
import com.simplecity.amp_library.ui.recyclerview.GridDividerDecoration;
import com.simplecity.amp_library.utils.ComparisonUtils;
import com.simplecity.amp_library.utils.DataManager;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.PermissionUtils;
import com.simplecityapps.recycler_adapter.model.ViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.RecyclerListener;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.Collections;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

public class GenreFragment extends BaseFragment implements
        MusicUtils.Defs, GenreView.ClickListener {

    private static final String TAG = "GenreFragment";

    public interface GenreClickListener {

        void onGenreClicked(Genre genre);
    }

    private static final String ARG_PAGE_TITLE = "page_title";

    private SharedPreferences mPrefs;

    @Nullable
    private GenreClickListener genreClickListener;

    private FastScrollRecyclerView mRecyclerView;

    private SectionedAdapter adapter;

    private SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferenceChangeListener;

    private Subscription subscription;

    public GenreFragment() {

    }

    public static GenreFragment newInstance(String pageTitle) {
        GenreFragment fragment = new GenreFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PAGE_TITLE, pageTitle);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (getParentFragment() instanceof GenreClickListener) {
            genreClickListener = (GenreClickListener) getParentFragment();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = new SectionedAdapter();

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());

        mPrefs.registerOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (mRecyclerView == null) {

            mRecyclerView = (FastScrollRecyclerView) inflater.inflate(R.layout.fragment_recycler, container, false);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            mRecyclerView.addItemDecoration(new GridDividerDecoration(getResources(), 4, true));
            mRecyclerView.setRecyclerListener(new RecyclerListener());
            mRecyclerView.setAdapter(adapter);
        }

        return mRecyclerView;
    }

    @Override
    public void onPause() {

        if (subscription != null) {
            subscription.unsubscribe();
        }

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshAdapterItems();
    }

    @Override
    public void onDestroy() {
        mPrefs.unregisterOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
        super.onDestroy();
    }

    private void refreshAdapterItems() {
        PermissionUtils.RequestStoragePermissions(() -> {
            if (getActivity() != null && isAdded()) {
                subscription = DataManager.getInstance().getGenresRelay()
                        .map(genres -> Stream.of(genres)
                                .sorted((a, b) -> ComparisonUtils.compare(a.name, b.name))
                                .map(genre -> {
                                    GenreView genreView = new GenreView(genre);
                                    genreView.setClickListener(this);
                                    return (ViewModel) genreView;
                                })
                                .collect(Collectors.toList()))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(items -> {
                            if (items.isEmpty()) {
                                adapter.setItems(Collections.singletonList(new EmptyView(R.string.empty_genres)));
                            } else {
                                adapter.setItems(items);
                            }
                        });
            }
        });
    }

    @Override
    public void onItemClick(Genre genre) {
        if (genreClickListener != null) {
            genreClickListener.onGenreClicked(genre);
        }
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}
