package com.simplecity.amp_library.ui.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.bignerdranch.android.multiselector.MultiSelector;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.jakewharton.rxbinding.support.v7.widget.RxSearchView;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.format.PrefixHighlighter;
import com.simplecity.amp_library.model.AdaptableItem;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.Header;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.tagger.TaggerDialog;
import com.simplecity.amp_library.ui.adapters.SearchAdapter;
import com.simplecity.amp_library.ui.modelviews.AlbumArtistView;
import com.simplecity.amp_library.ui.modelviews.AlbumView;
import com.simplecity.amp_library.ui.modelviews.SearchHeaderView;
import com.simplecity.amp_library.ui.modelviews.SongView;
import com.simplecity.amp_library.ui.modelviews.ViewType;
import com.simplecity.amp_library.utils.ColorUtils;
import com.simplecity.amp_library.utils.DataManager;
import com.simplecity.amp_library.utils.DialogUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.Operators;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.SearchUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.ThemeUtils;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

import static android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;

public class SearchActivity extends BaseActivity implements
        MusicUtils.Defs,
        SearchAdapter.SearchListener {

    private static final String TAG = "SearchActivity";

    private SearchAdapter adapter;
    private FastScrollRecyclerView recyclerView;
    private String filterString;

    private SystemBarTintManager mTintManager;

    private SearchView searchView;

    private RequestManager requestManager;

    private MultiSelector dummySelector;

    private SearchHeaderView songsHeader;
    private SearchHeaderView albumsHeader;
    private SearchHeaderView artistsHeader;

    private PrefixHighlighter prefixHighlighter;

    private CompositeSubscription subscriptions;

    private Subscription setItemsSubscription = null;

    @SuppressLint("InlinedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ThemeUtils.setTheme(this);

        if (!ShuttleUtils.hasLollipop() && ShuttleUtils.hasKitKat()) {
            getWindow().setFlags(FLAG_TRANSLUCENT_STATUS, FLAG_TRANSLUCENT_STATUS);
            mTintManager = new SystemBarTintManager(this);
        }
        if (!ShuttleUtils.hasKitKat()) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        }

        if (SettingsManager.getInstance().canTintNavBar()) {
            getWindow().setNavigationBarColor(ColorUtils.getPrimaryColorDark(this));
        }

        super.onCreate(savedInstanceState);

        final String query = getIntent().getStringExtra(SearchManager.QUERY);
        filterString = !TextUtils.isEmpty(query) ? query.toLowerCase().trim() : "";

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        setContentView(R.layout.activity_search);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Get the ActionBar
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(null);

        ThemeUtils.themeActionBar(this);
        ThemeUtils.themeStatusBar(this, mTintManager);

        adapter = new SearchAdapter();
        adapter.setListener(this);

        recyclerView = (FastScrollRecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        ThemeUtils.themeRecyclerView(recyclerView);
        recyclerView.setThumbColor(ColorUtils.getAccentColor());
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                ThemeUtils.themeRecyclerView(recyclerView);
                super.onScrollStateChanged(recyclerView, newState);
            }
        });

        if (requestManager == null) {
            requestManager = Glide.with(this);
        }

        dummySelector = new MultiSelector();

        songsHeader = new SearchHeaderView(new Header(getString(R.string.tracks_title)));
        albumsHeader = new SearchHeaderView(new Header(getString(R.string.albums_title)));
        artistsHeader = new SearchHeaderView(new Header(getString(R.string.artists_title)));

        prefixHighlighter = new PrefixHighlighter(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (subscriptions == null || subscriptions.isUnsubscribed()) {
            subscriptions = new CompositeSubscription();
        }

        refreshAdapterItems();

        if (searchView != null) {
            subscriptions.add(getSearchViewSubscription());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        subscriptions.unsubscribe();
    }

    private void refreshAdapterItems() {

        subscriptions.add(DataManager.getInstance().getSongsRelay()
                .first()
                .map(songs -> {

                    char[] prefix = filterString.toUpperCase().toCharArray();

                    List<Album> albums = Operators.songsToAlbums(songs);
                    Collections.sort(albums, Album::compareTo);

                    List<AlbumArtist> albumArtists = Operators.albumsToAlbumArtists(albums);
                    Collections.sort(albumArtists, AlbumArtist::compareTo);

                    //Album artists
                    List<AdaptableItem> adaptableItems = Stream.of(albumArtists)
                            .filter(album -> album.name != null)
                            .map(albumArtist -> new SearchUtils.JaroWinklerObject<>(albumArtist, filterString, albumArtist.name))
                            .filter(jaroWinklerObject -> jaroWinklerObject.score > 0.75 || TextUtils.isEmpty(filterString))
                            .sorted((a, b) -> a.object.compareTo(b.object))
                            .sorted((a, b) -> Double.compare(b.score, a.score))
                            .map(jaroWinklerObject -> jaroWinklerObject.object)
                            .map(albumArtist -> {
                                AlbumArtistView albumArtistView = new AlbumArtistView(albumArtist, ViewType.ARTIST_LIST, requestManager);
                                albumArtistView.setPrefix(prefixHighlighter, prefix);
                                return (AdaptableItem) albumArtistView;
                            })
                            .collect(Collectors.toList());

                    if (!adaptableItems.isEmpty()) {
                        adaptableItems.add(0, artistsHeader);
                    }

                    //Albums
                    List<AdaptableItem> albumItems = Stream.of(albums)
                            .filter(album -> album.name != null)
                            .map(album -> new SearchUtils.JaroWinklerObject<>(album, filterString, album.name, album.albumArtistName))
                            .filter(jaroWinklerObject -> jaroWinklerObject.score > 0.75 || TextUtils.isEmpty(filterString))
                            .sorted((a, b) -> a.object.compareTo(b.object))
                            .sorted((a, b) -> Double.compare(b.score, a.score))
                            .map(jaroWinklerObject -> jaroWinklerObject.object)
                            .map(album -> {
                                AlbumView albumView = new AlbumView(album, ViewType.ALBUM_LIST, requestManager);
                                albumView.setPrefix(prefixHighlighter, prefix);
                                return albumView;
                            })
                            .collect(Collectors.toList());

                    if (!albumItems.isEmpty()) {
                        albumItems.add(0, albumsHeader);
                    }
                    adaptableItems.addAll(albumItems);

                    //Songs
                    songs = Stream.of(songs).filter(song -> song.name != null)
                            .map(song -> new SearchUtils.JaroWinklerObject<>(song, filterString, song.name, song.albumName, song.artistName, song.albumArtistName))
                            .filter(jaroWinklerObject -> jaroWinklerObject.score > 0.75 || TextUtils.isEmpty(filterString))
                            .sorted((a, b) -> a.object.compareTo(b.object))
                            .sorted((a, b) -> Double.compare(b.score, a.score))
                            .map(jaroWinklerObject -> jaroWinklerObject.object)
                            .collect(Collectors.toList());

                    List<AdaptableItem> songItems = Stream.of(songs)
                            .map(song -> {
                                SongView songView = new SongView(song, dummySelector, requestManager);
                                songView.setPrefix(prefixHighlighter, prefix);
                                return songView;
                            })
                            .collect(Collectors.toList());

                    if (!songItems.isEmpty()) {
                        songItems.add(0, songsHeader);
                    }
                    adaptableItems.addAll(songItems);

                    return adaptableItems;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(adaptableItems -> {
                    //We've got a new set of items to adapt.. Cancel the in-flight subscription.
                    if (setItemsSubscription != null) {
                        setItemsSubscription.unsubscribe();
                    }
                    setItemsSubscription = adapter.setItems(adaptableItems);
                    recyclerView.scrollToPosition(0);
                }));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_search_activity, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchableInfo searchableInfo = searchManager.getSearchableInfo(getComponentName());
        if (searchView != null) {
            ThemeUtils.themeSearchView(this, searchView);
            searchView.setIconified(false);
            searchView.setSearchableInfo(searchableInfo);
            if (!TextUtils.isEmpty(filterString)) {
                searchView.setQuery(filterString, false);
            }
        }

        if (subscriptions == null || subscriptions.isUnsubscribed()) {
            subscriptions = new CompositeSubscription();
        }

        if (searchView != null) {
            subscriptions.add(getSearchViewSubscription());
        }

        return super.onCreateOptionsMenu(menu);
    }

    private Subscription getSearchViewSubscription() {
        return RxSearchView.queryTextChangeEvents(searchView)
                .skip(1)
                .debounce(50, TimeUnit.MILLISECONDS)
                .onBackpressureLatest()
                .subscribe(searchViewQueryTextEvent -> {
                    filterString = !TextUtils.isEmpty(searchViewQueryTextEvent.queryText()) ? searchViewQueryTextEvent.queryText().toString() : "";
                    refreshAdapterItems();
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(View v, int position, Object item) {

        if (item instanceof AlbumArtist || item instanceof Album) {
            Intent intent = new Intent();
            intent.putExtra(MainActivity.ARG_MODEL, (Serializable) item);
            setResult(Activity.RESULT_OK, intent);
            finish();
        } else if (item instanceof Song) {
            List<Song> songs = new ArrayList<>();
            int curPos = 0;
            for (int i = 0, count = adapter.getItemCount(); i < count; i++) {
                Object aModel = adapter.getItem(i);
                if (aModel != null && aModel instanceof Song) {
                    songs.add((Song) aModel);
                    if (aModel.equals(item)) {
                        curPos = songs.size() - 1;
                    }
                }
            }
            MusicUtils.playAll(songs, curPos, false, () -> {
                final String message = getString(R.string.emptyplaylist);
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            });
            finish();
        }
    }

    @Override
    public void onOverflowClick(View v, int position, Object object) {

        PopupMenu menu = new PopupMenu(this, v);

        if (object instanceof AlbumArtist) {
            menu.getMenu().add(0, PLAY_SELECTION, 0, R.string.play_selection);
        } else if (object instanceof Album) {
            menu.getMenu().add(0, PLAY_SELECTION, 0, R.string.play_selection);
        } else if (object instanceof Song) {
            menu.getMenu().add(0, PLAY_NEXT, 0, R.string.play_next);
            menu.getMenu().add(0, USE_AS_RINGTONE, 4, R.string.ringtone_menu);
        }

        SubMenu sub = menu.getMenu().addSubMenu(0, ADD_TO_PLAYLIST, 1, R.string.add_to_playlist);
        PlaylistUtils.makePlaylistMenu(this, sub, 0);

        menu.getMenu().add(0, QUEUE, 2, R.string.add_to_queue);

        if (ShuttleUtils.isUpgraded()) {
            menu.getMenu().add(0, TAGGER, 3, R.string.edit_tags);
        }

        menu.getMenu().add(0, DELETE_ITEM, 6, R.string.delete_item);

        menu.setOnMenuItemClickListener(item -> {

                    Observable<List<Song>> songsObservable = null;
                    if (object instanceof Song) {
                        songsObservable = Observable.just(Collections.singletonList((Song) object));
                    } else if (object instanceof AlbumArtist) {
                        songsObservable = ((AlbumArtist) object).getSongsObservable();
                    } else if (object instanceof Album) {
                        songsObservable = ((Album) object).getSongsObservable();
                    }

                    switch (item.getItemId()) {
                        case PLAY_SELECTION:
                            songsObservable
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(songs -> MusicUtils.playAll(songs, () -> {
                                        final String message = getString(R.string.emptyplaylist);
                                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                                    }));
                            return true;
                        case PLAY_NEXT:
                            songsObservable
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(songs -> MusicUtils.playNext(SearchActivity.this, songs));
                            return true;
                        case NEW_PLAYLIST:
                            songsObservable
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(songs -> PlaylistUtils.createPlaylistDialog(SearchActivity.this, songs));
                            return true;
                        case PLAYLIST_SELECTED:
                            songsObservable
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(songs -> {
                                        Playlist playlist = (Playlist) item.getIntent().getSerializableExtra(ShuttleUtils.ARG_PLAYLIST);
                                        PlaylistUtils.addToPlaylist(this, playlist, songs);
                                    });
                            return true;
                        case USE_AS_RINGTONE:
                            // Set the system setting to make this the current
                            // ringtone
                            ShuttleUtils.setRingtone(SearchActivity.this, ((Song) object));
                            return true;
                        case QUEUE:
                            songsObservable
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(songs -> MusicUtils.addToQueue(SearchActivity.this, songs));
                            return true;
                        case TAGGER:
                            TaggerDialog.newInstance((Serializable) object)
                                    .show(getSupportFragmentManager());
                            return true;
                        case DELETE_ITEM:
                            DialogUtils.DeleteDialogBuilder builder = new DialogUtils.DeleteDialogBuilder()
                                    .context(this)
                                    .songsToDelete(songsObservable);
                            if (object instanceof Song) {
                                builder.singleMessageId(R.string.delete_song_desc)
                                        .multipleMessage(R.string.delete_song_desc_multiple)
                                        .itemNames(Collections.singletonList(((Song) object).name));
                            } else if (object instanceof AlbumArtist) {
                                builder.singleMessageId(R.string.delete_album_artist_desc)
                                        .multipleMessage(R.string.delete_album_artist_desc_multiple)
                                        .itemNames(Collections.singletonList(((AlbumArtist) object).name));
                            } else if (object instanceof Album) {
                                builder.singleMessageId(R.string.delete_album_desc)
                                        .multipleMessage(R.string.delete_album_desc_multiple)
                                        .itemNames(Collections.singletonList(((Album) object).name));
                            }

                            builder.build().show();

                            return true;
                    }
                    return false;
                }
        );
        menu.show();
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}
