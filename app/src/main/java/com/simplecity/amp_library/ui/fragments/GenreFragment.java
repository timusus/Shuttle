package com.simplecity.amp_library.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.annimon.stream.Stream;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.Genre;
import com.simplecity.amp_library.ui.adapters.SectionedAdapter;
import com.simplecity.amp_library.ui.modelviews.EmptyView;
import com.simplecity.amp_library.ui.modelviews.GenreView;
import com.simplecity.amp_library.utils.ComparisonUtils;
import com.simplecity.amp_library.utils.DataManager;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.MenuUtils;
import com.simplecity.amp_library.utils.PermissionUtils;
import com.simplecityapps.recycler_adapter.model.ViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.RecyclerListener;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.Collections;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class GenreFragment extends BaseFragment implements
        GenreView.ClickListener {

    private static final String TAG = "GenreFragment";

    public interface GenreClickListener {

        void onGenreClicked(Genre genre);
    }

    private static final String ARG_PAGE_TITLE = "page_title";

    @Nullable
    private GenreClickListener genreClickListener;

    private FastScrollRecyclerView recyclerView;

    private SectionedAdapter adapter;

    private Disposable disposable;

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

        adapter = new SectionedAdapter("GenreFragment");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (recyclerView == null) {
            recyclerView = (FastScrollRecyclerView) inflater.inflate(R.layout.fragment_recycler, container, false);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.setRecyclerListener(new RecyclerListener());
        }
        if (recyclerView.getAdapter() != adapter) {
            recyclerView.setAdapter(adapter);
        }

        return recyclerView;
    }

    @Override
    public void onPause() {

        if (disposable != null) {
            disposable.dispose();
        }

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshAdapterItems(false);
    }

    private void refreshAdapterItems(boolean force) {
        PermissionUtils.RequestStoragePermissions(() -> {
            if (getActivity() != null && isAdded()) {
                disposable = DataManager.getInstance().getGenresRelay()
                        .skipWhile(genres -> !force && adapter.items.size() == genres.size())
                        .map(genres -> Stream.of(genres)
                                .sorted((a, b) -> ComparisonUtils.compare(a.name, b.name))
                                .map(genre -> {
                                    GenreView genreView = new GenreView(genre);
                                    genreView.setClickListener(this);
                                    return (ViewModel) genreView;
                                })
                                .toList())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(items -> {
                            if (items.isEmpty()) {
                                adapter.setItems(Collections.singletonList(new EmptyView(R.string.empty_genres)));
                            } else {
                                adapter.setItems(items);
                            }
                        }, error -> LogUtils.logException(TAG, "Error refreshing adapter items", error));
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
    public void onOverflowClick(View v, Genre genre) {
        PopupMenu popupMenu = new PopupMenu(getContext(), v);
        popupMenu.inflate(R.menu.menu_genre);
        popupMenu.setOnMenuItemClickListener(MenuUtils.getGenreClickListener(getContext(), genre));
        popupMenu.show();
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}
