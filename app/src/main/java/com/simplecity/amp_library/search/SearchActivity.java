package com.simplecity.amp_library.search;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.WindowManager;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.jakewharton.rxbinding.support.v7.widget.RxSearchView;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.format.PrefixHighlighter;
import com.simplecity.amp_library.model.AdaptableItem;
import com.simplecity.amp_library.tagger.TaggerDialog;
import com.simplecity.amp_library.ui.activities.BaseActivity;
import com.simplecity.amp_library.ui.adapters.SearchAdapter;
import com.simplecity.amp_library.ui.modelviews.EmptyView;
import com.simplecity.amp_library.ui.modelviews.LoadingView;
import com.simplecity.amp_library.utils.ColorUtils;
import com.simplecity.amp_library.utils.ResourceUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.ThemeUtils;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

public class SearchActivity extends BaseActivity implements
        com.simplecity.amp_library.search.SearchView {

    private static final String TAG = "SearchActivity";

    private SystemBarTintManager tintManager;

    private Toolbar toolbar;

    private FastScrollRecyclerView recyclerView;

    private SearchAdapter adapter;

    private LoadingView loadingView;

    private EmptyView emptyView;

    private SearchPresenter searchPresenter;

    private CompositeSubscription compositeSubscription = new CompositeSubscription();

    @SuppressLint("InlinedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        loadingView = new LoadingView();

        emptyView = new EmptyView(R.string.empty_search);
        emptyView.setHeight(ResourceUtils.toPixels(96));

        ThemeUtils.setTheme(this);

        if (!ShuttleUtils.hasLollipop() && ShuttleUtils.hasKitKat()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            tintManager = new SystemBarTintManager(this);
        }
        if (!ShuttleUtils.hasKitKat()) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        }

        if (SettingsManager.getInstance().canTintNavBar()) {
            getWindow().setNavigationBarColor(ColorUtils.getPrimaryColorDark(this));
        }

        super.onCreate(savedInstanceState);

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        setContentView(R.layout.activity_search);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(ColorUtils.getPrimaryColor());
        toolbar.inflateMenu(R.menu.menu_search_activity);
        toolbar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.search_fuzzy:
                    item.setChecked(!item.isChecked());
                    searchPresenter.setSearchFuzzy(item.isChecked());
                    break;
                case R.id.search_title_only:
                    item.setChecked(!item.isChecked());
                    searchPresenter.setSearchTitleOnly(item.isChecked());
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

        adapter = new SearchAdapter();

        SearchView searchView = (SearchView) findViewById(R.id.searchView);
        searchView.setIconifiedByDefault(false);
        compositeSubscription.add(RxSearchView.queryTextChangeEvents(searchView)
                .skip(1)
                .debounce(200, TimeUnit.MILLISECONDS)
                .onBackpressureLatest()
                .subscribe(searchViewQueryTextEvent ->
                        searchPresenter.queryChanged((String) searchViewQueryTextEvent.queryText())));

        recyclerView = (FastScrollRecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setThumbColor(ColorUtils.getAccentColor());
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                ThemeUtils.themeRecyclerView(recyclerView);
                super.onScrollStateChanged(recyclerView, newState);
            }
        });

        ThemeUtils.themeStatusBar(this, tintManager);
        ThemeUtils.themeSearchView(this, searchView);
        ThemeUtils.themeRecyclerView(recyclerView);

        PrefixHighlighter prefixHighlighter = new PrefixHighlighter(this);

        RequestManager requestManager = Glide.with(this);

        searchPresenter = new SearchPresenter(prefixHighlighter, requestManager);
        searchPresenter.bindView(this);
        adapter.setListener(searchPresenter);

        final String query = getIntent().getStringExtra(SearchManager.QUERY);
        searchPresenter.queryChanged(query);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        compositeSubscription.unsubscribe();
        searchPresenter.unbindView(this);
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
    public Subscription setItems(@NonNull List<AdaptableItem> items) {
        Subscription subscription = adapter.setItems(items);
        recyclerView.scrollToPosition(0);
        return subscription;
    }

    @Override
    public void setFilterFuzzyChecked(boolean checked) {
        toolbar.getMenu().findItem(R.id.search_fuzzy).setChecked(checked);
    }

    @Override
    public void setFilterTitleOnlyChecked(boolean checked) {
        toolbar.getMenu().findItem(R.id.search_title_only).setChecked(checked);
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
    public void showEmptyPlaylistToast() {
        Toast.makeText(this, (ShuttleApplication.getInstance().getString(R.string.emptyplaylist)), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showTaggerDialog(@NonNull TaggerDialog taggerDialog) {
        taggerDialog.show(getSupportFragmentManager());
    }

    @Override
    public void showDeleteDialog(@NonNull MaterialDialog deleteDialog) {
        deleteDialog.show();
    }

    @Override
    public void finish(int resultCode, Intent data) {
        setResult(resultCode, data);
        this.finish();
    }
}