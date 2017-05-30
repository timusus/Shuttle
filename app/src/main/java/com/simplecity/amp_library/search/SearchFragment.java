package com.simplecity.amp_library.search;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.jakewharton.rxbinding.support.v7.widget.RxSearchView;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.dagger.module.FragmentModule;
import com.simplecity.amp_library.tagger.TaggerDialog;
import com.simplecity.amp_library.ui.adapters.SearchAdapter;
import com.simplecity.amp_library.ui.fragments.BaseFragment;
import com.simplecity.amp_library.ui.modelviews.EmptyView;
import com.simplecity.amp_library.ui.modelviews.LoadingView;
import com.simplecity.amp_library.utils.ResourceUtils;
import com.simplecityapps.recycler_adapter.model.ViewModel;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

public class SearchFragment extends BaseFragment implements com.simplecity.amp_library.search.SearchView {

    private static final String TAG = "SearchFragment";

    public static final String ARG_QUERY = "query";

    private String query;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.recyclerView)
    FastScrollRecyclerView recyclerView;

    private SearchAdapter adapter;

    private LoadingView loadingView;

    private EmptyView emptyView;

    private CompositeSubscription subscriptions = new CompositeSubscription();

    @Inject
    SearchPresenter searchPresenter;

    private SearchView searchView;

    public static SearchFragment newInstance(String query) {
        Bundle args = new Bundle();
        args.putString(ARG_QUERY, query);
        SearchFragment fragment = new SearchFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @SuppressLint("InlinedApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ShuttleApplication.getInstance().getAppComponent()
                .plus(new FragmentModule(this))
                .inject(this);

        query = getArguments().getString(ARG_QUERY);

        loadingView = new LoadingView();

        emptyView = new EmptyView(R.string.empty_search);
        emptyView.setHeight(ResourceUtils.toPixels(96));

        adapter = new SearchAdapter();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_search, container, false);

        ButterKnife.bind(this, rootView);

        toolbar.inflateMenu(R.menu.menu_search_activity);
        toolbar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.search_fuzzy:
                    item.setChecked(!item.isChecked());
                    searchPresenter.setSearchFuzzy(item.isChecked());
                    break;
                case R.id.search_artist:
                    item.setChecked(!item.isChecked());
                    searchPresenter.setSearchArtists(item.isChecked());
                    break;
                case R.id.search_album:
                    item.setChecked(!item.isChecked());
                    searchPresenter.setSearchAlbums(item.isChecked());
                    break;
            }
            return false;
        });

        MenuItem searchItem = toolbar.getMenu().findItem(R.id.search);
        MenuItemCompat.expandActionView(searchItem);
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);

        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return false;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(rootView.getWindowToken(), 0);
                searchView.getHandler().postDelayed(() -> getNavigationController().popViewController(), 150);
                return false;
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        adapter.setListener(searchPresenter);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        searchPresenter.bindView(this);

        subscriptions.add(RxSearchView.queryTextChangeEvents(searchView)
                .skip(1)
                .debounce(200, TimeUnit.MILLISECONDS)
                .onBackpressureLatest()
                .subscribe(searchViewQueryTextEvent -> {
                    query = searchViewQueryTextEvent.queryText().toString();
                    searchPresenter.queryChanged(query);
                }));

        searchPresenter.queryChanged(query);
    }

    @Override
    public void onPause() {
        subscriptions.clear();
        searchPresenter.unbindView(this);

        super.onPause();
    }

    @Override
    protected String screenName() {
        return TAG;
    }

    @Override
    public void setLoading(boolean loading) {
        adapter.setItems(Collections.singletonList(loadingView));
    }

    @Override
    public void setEmpty(boolean empty) {
        adapter.setItems(Collections.singletonList(emptyView));
    }

    @Override
    public Subscription setItems(@NonNull List<ViewModel> items) {
        Subscription subscription = adapter.setItems(items);
        recyclerView.scrollToPosition(0);
        return subscription;
    }

    @Override
    public void setFilterFuzzyChecked(boolean checked) {
        toolbar.getMenu().findItem(R.id.search_fuzzy).setChecked(checked);
    }

    @Override
    public void setFilterArtistsChecked(boolean checked) {
        toolbar.getMenu().findItem(R.id.search_artist).setChecked(checked);
    }

    @Override
    public void setFilterAlbumsChecked(boolean checked) {
        toolbar.getMenu().findItem(R.id.search_album).setChecked(checked);
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(getContext(), (ShuttleApplication.getInstance().getString(R.string.emptyplaylist)), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showTaggerDialog(@NonNull TaggerDialog taggerDialog) {
        taggerDialog.show(getChildFragmentManager());
    }

    @Override
    public void showDeleteDialog(@NonNull MaterialDialog deleteDialog) {
        deleteDialog.show();
    }
}