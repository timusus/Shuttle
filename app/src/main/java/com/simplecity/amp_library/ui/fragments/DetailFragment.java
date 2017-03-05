package com.simplecity.amp_library.ui.fragments;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.transition.Transition;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback;
import com.bignerdranch.android.multiselector.MultiSelector;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.crashlytics.android.core.CrashlyticsCore;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.glide.utils.AlwaysCrossFade;
import com.simplecity.amp_library.glide.utils.GlideUtils;
import com.simplecity.amp_library.model.AdaptableItem;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.Genre;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.sql.databases.BlacklistHelper;
import com.simplecity.amp_library.ui.activities.MainActivity;
import com.simplecity.amp_library.ui.adapters.DetailAdapter;
import com.simplecity.amp_library.ui.adapters.ItemAdapter;
import com.simplecity.amp_library.ui.modelviews.BaseAdaptableItem;
import com.simplecity.amp_library.ui.modelviews.DiscNumberView;
import com.simplecity.amp_library.ui.modelviews.EmptyView;
import com.simplecity.amp_library.ui.modelviews.HorizontalAlbumView;
import com.simplecity.amp_library.ui.modelviews.HorizontalRecyclerView;
import com.simplecity.amp_library.ui.modelviews.SongView;
import com.simplecity.amp_library.ui.modelviews.ViewType;
import com.simplecity.amp_library.ui.recyclerview.ItemTouchHelperCallback;
import com.simplecity.amp_library.ui.views.NonScrollImageButton;
import com.simplecity.amp_library.utils.ColorUtils;
import com.simplecity.amp_library.utils.ComparisonUtils;
import com.simplecity.amp_library.utils.DataManager;
import com.simplecity.amp_library.utils.DialogUtils;
import com.simplecity.amp_library.utils.DrawableUtils;
import com.simplecity.amp_library.utils.MenuUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.Operators;
import com.simplecity.amp_library.utils.PermissionUtils;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.ResourceUtils;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.SortManager;
import com.simplecity.amp_library.utils.StringUtils;
import com.simplecity.amp_library.utils.ThemeUtils;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class DetailFragment extends BaseFragment implements
        MusicUtils.Defs,
        RecyclerView.RecyclerListener,
        View.OnClickListener,
        DetailAdapter.Listener,
        HorizontalRecyclerView.HorizontalAdapter.ItemListener {

    private static final String TAG = "DetailFragment";

    public static String ARG_MODEL = "model";

    private static final String ARG_TRANSITION_NAME = "transition_name";

    private AlbumArtist albumArtist;

    private Album album;

    private Genre genre;

    private Playlist playlist;

    private TextView lineOne;

    private TextView lineTwo;

    private NonScrollImageButton overflowButton;

    private MultiSelector multiSelector = new MultiSelector();

    private ActionMode actionMode;

    boolean inActionMode;

    private View rootView;

    private DetailAdapter adapter;

    private BroadcastReceiver receiver;

    ImageView headerImageView;

    private SharedPreferences prefs;

    private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener;

    FloatingActionButton fab;

    private RecyclerView recyclerView;

    private ItemTouchHelper itemTouchHelper;

    private HorizontalRecyclerView horizontalRecyclerView;

    HeaderView headerItem;

    private View headerView;

    private CompositeSubscription subscriptions;

    private RequestManager requestManager;

    private Album currentSlideShowAlbum;

    View textProtectionScrim;

    float headerTranslation;
    float headerImageTranslation;

    private Subscription slideShowObservable;

    private boolean sortChanged = false;

    public DetailFragment() {
    }

    public static DetailFragment newInstance(Serializable model) {
        return newInstance(model, null);
    }

    public static DetailFragment newInstance(Serializable model, String transitionName) {

        final DetailFragment fragment = new DetailFragment();
        final Bundle args = new Bundle();
        args.putSerializable(ARG_MODEL, model);
        args.putString(ARG_TRANSITION_NAME, transitionName);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);

        setHasOptionsMenu(true);

        setEnterSharedElementCallback(enterSharedElementCallback);

        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        Serializable object = getArguments().getSerializable(ARG_MODEL);

        if (object instanceof AlbumArtist) {
            albumArtist = (AlbumArtist) object;
        } else if (object instanceof Album) {
            album = (Album) object;
        } else if (object instanceof Genre) {
            genre = (Genre) object;
        } else if (object instanceof Playlist) {
            playlist = (Playlist) object;
        }

        if (adapter == null) {
            adapter = new DetailAdapter();
            adapter.setListener(this);
        }

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() != null && intent.getAction().equals("restartLoader")) {
                    refreshAdapterItems();
                }
            }
        };

        sharedPreferenceChangeListener = (sharedPreferences, key) -> {
            if (key.equals("pref_theme_highlight_color") || key.equals("pref_theme_accent_color") || key.equals("pref_theme_white_accent")) {
                themeUIComponents();
            } else if (key.equals("songWhitelist")) {
                refreshAdapterItems();
            }
        };

        prefs.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

        if (requestManager == null) {
            requestManager = Glide.with(this);
        }

        if (headerItem == null) {
            headerItem = new HeaderView();
        }

        if (horizontalRecyclerView == null) {
            horizontalRecyclerView = new HorizontalRecyclerView();
            horizontalRecyclerView.setListener(this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        recyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        if (canEdit()) {
            itemTouchHelper = new ItemTouchHelper(new ItemTouchHelperCallback(
                    (from, to) ->
                    {
                        long songViewCount = Stream.of(adapter.items)
                                .filter(adaptableItem -> adaptableItem instanceof SongView).count();
                        int offset = (int) (adapter.getItemCount() - songViewCount);
                        if(to >= offset) {
                            adapter.moveItem(from, to);
                        }
                    },
                    (from, to) ->
                    {
                        // The 'offset' here is the number of items in the list which are not
                        // SongViews. We need this to determine the actual playlist positions of the items.
                        long songViewCount = Stream.of(adapter.items)
                                .filter(adaptableItem -> adaptableItem instanceof SongView).count();
                        int offset = (int) (adapter.getItemCount() - songViewCount);
                        from -= offset;
                        to -= offset;

                        try {
                                MediaStore.Audio.Playlists.Members.moveItem(getActivity().getContentResolver(), playlist.id, from, to);
                        } catch (IllegalArgumentException e) {
                            CrashlyticsCore.getInstance().log(String.format("Failed to move playlist item from %s to %s. Adapter count: %s. Error:%s", from, to, adapter.getItemCount(), e.getMessage()));
                        }
                    },
                    null
            ));
            itemTouchHelper.attachToRecyclerView(recyclerView);
        }

        fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
        fab.setOnClickListener(this);

        lineOne = (TextView) rootView.findViewById(R.id.line_one);
        lineTwo = (TextView) rootView.findViewById(R.id.line_two);
        overflowButton = (NonScrollImageButton) rootView.findViewById(R.id.btn_overflow);
        overflowButton.setOnClickListener(this);

        if (albumArtist != null) {
            lineOne.setText(albumArtist.name);
            overflowButton.setContentDescription(getString(R.string.btn_options, albumArtist.name));
        } else if (album != null) {
            lineOne.setText(album.name);
            overflowButton.setContentDescription(getString(R.string.btn_options, album.name));
        } else if (genre != null) {
            lineOne.setText(genre.name);
            overflowButton.setVisibility(View.GONE);
        } else if (playlist != null) {
            lineOne.setText(playlist.name);
            overflowButton.setContentDescription(getString(R.string.btn_options, playlist.name));
        }

        textProtectionScrim = rootView.findViewById(R.id.textProtectionScrim);

        headerImageView = (ImageView) rootView.findViewById(R.id.background);
        String transitionName = getArguments().getString(ARG_TRANSITION_NAME);
        ViewCompat.setTransitionName(headerImageView, transitionName);
        if (transitionName != null) {
            textProtectionScrim.setVisibility(View.GONE);
            fab.setVisibility(View.GONE);
        }

        int width = ResourceUtils.getScreenSize().width + ResourceUtils.toPixels(60);
        int height = getResources().getDimensionPixelSize(R.dimen.header_view_height);

        if (albumArtist != null || album != null) {
            requestManager
                    .load(albumArtist == null ? album : albumArtist)
                    //Need to override the height/width, as the shared element transition tricks Glide into thinking this ImageView has
                    //the same dimensions as the ImageView that the transition starts with.
                    //So we'll set it to screen width (plus a little extra, which might fix an issue on some devices..)
                    .override(width, height)
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .priority(Priority.HIGH)
                    .placeholder(GlideUtils.getPlaceHolderDrawable(albumArtist == null ? album.name : albumArtist.name, false))
                    .centerCrop()
                    .animate(new AlwaysCrossFade(false))
                    .into(headerImageView);
        }
        actionMode = null;

        //Set the RecyclerView HeaderView height equal to the headerItem height
        headerView = rootView.findViewById(R.id.headerView);
        headerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                headerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                DetailFragment.this.headerItem.height = headerView.getHeight();
            }
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                headerTranslation = headerView.getTranslationY() - dy;
                headerImageTranslation = headerImageView.getTranslationY() + dy / 2;

                //Fixes an issue where the image translation gets a little out of sync with
                //the header translation.
                if (headerTranslation == 0) {
                    headerImageTranslation = 0;
                }

                float ratio = Math.min(1, -headerTranslation / headerView.getHeight());

                headerView.setTranslationY(headerTranslation);
                headerImageView.setTranslationY(headerImageTranslation);

                //Check to make sure the sliding panel isn't currently expanded or being
                //dragged. Workaround for issue where the action bar ends up being transparent
                //when recreating this fragment.
                if (getActivity() != null) {
                    if (((MainActivity) getActivity()).canSetAlpha()) {
                        ((MainActivity) getActivity()).setActionBarAlpha(ratio, true);
                    }
                }
            }
        });

        themeUIComponents();

        headerView.setTranslationY(headerTranslation);
        headerImageView.setTranslationY(headerImageTranslation);

        return rootView;
    }

    private void themeUIComponents() {

        if (rootView != null) {
            int themeType = ThemeUtils.getThemeType(getActivity());
            if (themeType == ThemeUtils.ThemeType.TYPE_DARK
                    || themeType == ThemeUtils.ThemeType.TYPE_SOLID_DARK) {
                rootView.setBackgroundColor(getResources().getColor(R.color.bg_dark));
            } else if (themeType == ThemeUtils.ThemeType.TYPE_BLACK
                    || themeType == ThemeUtils.ThemeType.TYPE_SOLID_BLACK) {
                rootView.setBackgroundColor(getResources().getColor(R.color.bg_black));
            } else {
                rootView.setBackgroundColor(getResources().getColor(R.color.bg_light));
            }
        }

        ThemeUtils.themeRecyclerView(recyclerView);

        if (fab != null) {
            fab.setBackgroundTintList(ColorStateList.valueOf(ColorUtils.getAccentColor()));
            fab.setRippleColor(ColorUtils.darkerise(ColorUtils.getAccentColor(), 0.85f));
        }

        if (overflowButton != null) {
            overflowButton.setImageDrawable(DrawableUtils.getBaseDrawable(getActivity(), R.drawable.ic_overflow_white));
        }

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                ThemeUtils.themeRecyclerView(recyclerView);
                super.onScrollStateChanged(recyclerView, newState);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction("restartLoader");
        getActivity().registerReceiver(receiver, filter);

        subscriptions = new CompositeSubscription();

        refreshAdapterItems();
    }

    void refreshAdapterItems() {

        PermissionUtils.RequestStoragePermissions(() -> {
            if (getActivity() != null && isAdded()) {

                boolean albumsAscending = getAlbumsAscending();
                boolean songsAscending = getSongsAscending();
                @SortManager.SongSort int songSort = getSongsSortOrder();
                @SortManager.AlbumSort int albumSort = getAlbumsSortOrder();

                Observable<List<Song>> observable = null;

                if (albumArtist != null) {
                    observable = DataManager.getInstance().getSongsRelay()
                            .first()
                            .map(songs -> Stream.of(songs)
                                    .filter(song -> Stream.of(albumArtist.albums)
                                            .anyMatch(album1 -> album1.id == song.albumId))
                                    .collect(Collectors.toList()));
                } else if (album != null) {
                    observable = DataManager.getInstance().getSongsRelay()
                            .first()
                            .map(songs -> Stream.of(songs)
                                    .filter(song -> song.albumId == album.id)
                                    .collect(Collectors.toList()));
                } else if (genre != null) {
                    observable = genre.getSongsObservable(getContext());
                } else if (playlist != null) {
                    observable = playlist.getSongsObservable(getContext());
                }

                subscriptions.add(observable
                        .map(songs -> {
                            songs = Stream.of(songs)
                                    .filter(song -> {
                                        if (albumArtist != null) {
                                            return Stream.of(albumArtist.albums)
                                                    .anyMatch(album -> album.id == song.albumId);
                                        } else if (album != null) {
                                            return song.albumId == album.id;
                                        }
                                        return true;
                                    }).collect(Collectors.toList());

                            List<Album> albums = Stream.of(Operators.songsToAlbums(songs))
                                    .collect(Collectors.toList());

                            if (playlist != null && albumSort == SortManager.AlbumSort.DEFAULT) {
                                switch (playlist.type) {
                                    case Playlist.Type.MOST_PLAYED:
                                        Collections.sort(albums, (a, b) -> ComparisonUtils.compareInt(b.songPlayCount, a.songPlayCount));
                                        break;
                                    case Playlist.Type.RECENTLY_PLAYED:
                                        Collections.sort(albums, (a, b) -> ComparisonUtils.compareLong(b.lastPlayed, a.lastPlayed));
                                        break;
                                    case Playlist.Type.RECENTLY_ADDED:
                                        Collections.sort(albums, (a, b) -> ComparisonUtils.compareLong(b.dateAdded, a.dateAdded));
                                        break;
                                    case Playlist.Type.USER_CREATED:
                                    case Playlist.Type.FAVORITES:
                                    case Playlist.Type.PODCAST:
                                        Collections.sort(albums, (a, b) -> ComparisonUtils.compare(a.name, b.name));
                                        break;
                                }
                            } else {
                                SortManager.getInstance().sortAlbums(albums, albumSort);
                                if (!albumsAscending) {
                                    Collections.reverse(albums);
                                }
                            }

                            //If we're not looking at a playlist, or we are, but it's not sorted by 'default',
                            //then we just leave the songs in what ever sort order they came in
                            if (playlist == null || songSort != SortManager.SongSort.DETAIL_DEFAULT) {
                                SortManager.getInstance().sortSongs(songs, songSort);
                                if (!songsAscending) {
                                    Collections.reverse(songs);
                                }
                            }

                            if (album == null) {
                                List<AdaptableItem> items = Stream.of(albums)
                                        .map(album -> new HorizontalAlbumView(album, requestManager))
                                        .collect(Collectors.toList());
                                horizontalRecyclerView.setItems(items);
                            }

                            List<AdaptableItem> songViews = Stream.of(songs)
                                    .map(song -> {
                                        SongView songView = new SongView(song, multiSelector, requestManager);
                                        songView.setShowAlbumArt(false);
                                        songView.setEditable(canEdit());
                                        songView.setShowTrackNumber(album != null && (songSort == SortManager.SongSort.DETAIL_DEFAULT || songSort == SortManager.SongSort.TRACK_NUMBER));
                                        return songView;
                                    })
                                    .collect(Collectors.toList());

                            if (album != null && album.numDiscs > 1 && (songSort == SortManager.SongSort.DETAIL_DEFAULT || songSort == SortManager.SongSort.TRACK_NUMBER)) {

                                int discNumber = 0;
                                int length = songViews.size();
                                for (int i = 0; i < length; i++) {

                                    SongView songView = (SongView) songViews.get(i);

                                    if (discNumber != songView.song.discNumber) {
                                        discNumber = songView.song.discNumber;
                                        songViews.add(i, new DiscNumberView(discNumber));
                                    }
                                }
                            }

                            List<AdaptableItem> adaptableItems = new ArrayList<>();
                            adaptableItems.add(headerItem);
                            if (album == null) {
                                adaptableItems.add(horizontalRecyclerView);
                            }

                            adaptableItems.addAll(songViews);

                            return adaptableItems;
                        })
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(adaptableItems -> {
                            if (adaptableItems.isEmpty()) {
                                adapter.setEmpty(new EmptyView(R.string.empty_songlist));
                            } else {
                                if (sortChanged) {
                                    //If the sort order has changed, we can't let the RecyclerView calculate the diff and do a nice
                                    //animation, as we can't keep track of changes to the scroll position of the recycler view,
                                    //so our header translation gets messed up.
                                    adapter.items.clear();
                                    adapter.items = adaptableItems;
                                    adapter.notifyDataSetChanged();
                                    recyclerView.smoothScrollToPosition(0);
                                    sortChanged = false;
                                } else {
                                    adapter.setItems(adaptableItems);
                                }
                            }

                            int numSongs = (int) Stream.of(adaptableItems)
                                    .filter(value -> value instanceof SongView)
                                    .count();

                            int numAlbums = horizontalRecyclerView.getCount();

                            if (lineTwo != null) {
                                if (albumArtist != null) {
                                    lineTwo.setText(StringUtils.makeAlbumAndSongsLabel(getActivity(), numAlbums, albumArtist.getNumSongs()));
                                } else if (album != null) {
                                    lineTwo.setText(String.format("%s%s", album.getAlbumArtist().name, numSongs > 0 ? " | " + album.getNumSongsLabel() : ""));
                                } else if (genre != null) {
                                    lineTwo.setText(StringUtils.makeAlbumAndSongsLabel(getActivity(), numAlbums, numSongs));
                                } else if (playlist != null) {
                                    lineTwo.setText(StringUtils.makeAlbumAndSongsLabel(getActivity(), numAlbums, numSongs));
                                }
                            }

                            //Start the slideshow observable.

                            List<Album> albums = Stream.of(adaptableItems)
                                    .filter(adaptableItem -> adaptableItem instanceof SongView)
                                    .map(songView -> (Song) songView.getItem())
                                    .map(Song::getAlbum)
                                    .distinct()
                                    .collect(Collectors.toList());

                            if (playlist != null || genre != null && !albums.isEmpty()) {
                                if (slideShowObservable != null && !slideShowObservable.isUnsubscribed()) {
                                    slideShowObservable.unsubscribe();
                                }
                                slideShowObservable = Observable.interval(8, TimeUnit.SECONDS)
                                        .onBackpressureDrop()
                                        .startWith(0L)
                                        .map(aLong -> {
                                            if (albums.isEmpty() || aLong == 0L && currentSlideShowAlbum != null) {
                                                //This is our first emission since onResume, but not our first since view creation.
                                                //Skip this load.
                                                return null;
                                            }
                                            return albums.get(new Random().nextInt(albums.size()));
                                        })
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(nextSlideShowAlbum -> {
                                            if (nextSlideShowAlbum != null && nextSlideShowAlbum != currentSlideShowAlbum) {
                                                //This crazy business is what's required to have a smooth Glide crossfade with no 'white flicker'
                                                requestManager
                                                        .load(nextSlideShowAlbum)
                                                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                                                        .priority(Priority.HIGH)
                                                        .error(GlideUtils.getPlaceHolderDrawable(nextSlideShowAlbum.name, true))
                                                        .centerCrop()
                                                        .thumbnail(Glide
                                                                .with(this)
                                                                .load(currentSlideShowAlbum)
                                                                .centerCrop())
                                                        .animate(new AlwaysCrossFade(false))
                                                        .into(headerImageView);
                                                currentSlideShowAlbum = nextSlideShowAlbum;
                                            }
                                        });
                                subscriptions.add(slideShowObservable);
                            }
                        }));
            }
        });
    }

    @Override
    public void onPause() {

        if (receiver != null) {
            getActivity().unregisterReceiver(receiver);
        }
        subscriptions.unsubscribe();

        super.onPause();
    }

    @Override
    public void onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        //If we've already inflated artist, playlist or genre sorting items by now, then we're looking
        //at at the albums detail screen. We need to remove the duplicate sorting menu items.

        MenuItem artistSortItem = menu.findItem(R.id.artist_sort);
        if (artistSortItem != null) {
            artistSortItem.setVisible(false);
        }

        if (albumArtist != null || genre != null || playlist != null) {
            inflater.inflate(R.menu.menu_sort_detail, menu);
        } else if (album != null) {
            inflater.inflate(R.menu.menu_sort_detail_album, menu);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        //Songs
        switch (getSongsSortOrder()) {
            case SortManager.SongSort.DETAIL_DEFAULT:
                menu.findItem(R.id.sort_song_default).setChecked(true);
                break;
            case SortManager.SongSort.NAME:
                menu.findItem(R.id.sort_song_name).setChecked(true);
                break;
            case SortManager.SongSort.TRACK_NUMBER:
                menu.findItem(R.id.sort_song_track_number).setChecked(true);
                break;
            case SortManager.SongSort.DURATION:
                menu.findItem(R.id.sort_song_duration).setChecked(true);
                break;
            case SortManager.SongSort.DATE:
                menu.findItem(R.id.sort_song_date).setChecked(true);
                break;
            case SortManager.SongSort.YEAR:
                menu.findItem(R.id.sort_song_year).setChecked(true);
                break;
            case SortManager.SongSort.ALBUM_NAME:
                menu.findItem(R.id.sort_song_album_name).setChecked(true);
                break;
            case SortManager.SongSort.ARTIST_NAME:
                menu.findItem(R.id.sort_song_artist_name).setChecked(true);
                break;
        }

        menu.findItem(R.id.sort_songs_ascending).setChecked(getSongsAscending());

        if (albumArtist != null || playlist != null || genre != null) {
            switch (getAlbumsSortOrder()) {
                case SortManager.AlbumSort.DEFAULT:
                    menu.findItem(R.id.sort_album_default).setChecked(true);
                    break;
                case SortManager.AlbumSort.NAME:
                    menu.findItem(R.id.sort_album_name).setChecked(true);
                    break;
                case SortManager.AlbumSort.YEAR:
                    menu.findItem(R.id.sort_album_year).setChecked(true);
                    break;
                case SortManager.AlbumSort.ARTIST_NAME:
                    menu.findItem(R.id.sort_album_artist_name).setChecked(true);
                    break;
            }

            menu.findItem(R.id.sort_albums_ascending).setChecked(getAlbumsAscending());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            //Songs
            case R.id.sort_song_default:
                setSongsSortOrder(SortManager.SongSort.DETAIL_DEFAULT);
                sortChanged = true;
                break;
            case R.id.sort_song_name:
                setSongsSortOrder(SortManager.SongSort.NAME);
                sortChanged = true;
                break;
            case R.id.sort_song_track_number:
                setSongsSortOrder(SortManager.SongSort.TRACK_NUMBER);
                sortChanged = true;
                break;
            case R.id.sort_song_duration:
                setSongsSortOrder(SortManager.SongSort.DURATION);
                sortChanged = true;
                break;
            case R.id.sort_song_year:
                setSongsSortOrder(SortManager.SongSort.YEAR);
                sortChanged = true;
                break;
            case R.id.sort_song_date:
                setSongsSortOrder(SortManager.SongSort.DATE);
                sortChanged = true;
                break;
            case R.id.sort_song_album_name:
                setSongsSortOrder(SortManager.SongSort.ALBUM_NAME);
                sortChanged = true;
                break;
            case R.id.sort_songs_ascending:
                setSongsAscending(!item.isChecked());
                sortChanged = true;
                break;

            //Albums
            case R.id.sort_album_default:
                setAlbumsOrder(SortManager.AlbumSort.DEFAULT);
                sortChanged = true;
                break;
            case R.id.sort_album_name:
                setAlbumsOrder(SortManager.AlbumSort.NAME);
                sortChanged = true;
                break;
            case R.id.sort_album_year:
                setAlbumsOrder(SortManager.AlbumSort.YEAR);
                sortChanged = true;
                break;
            case R.id.sort_albums_ascending:
                setAlbumsAscending(!item.isChecked());
                sortChanged = true;
                break;
        }

        if (sortChanged) {
            refreshAdapterItems();
        }

        getActivity().supportInvalidateOptionsMenu();

        return super.onOptionsItemSelected(item);
    }

    private void setSongsSortOrder(@SortManager.SongSort int sortOrder) {
        if (playlist != null) {
            SortManager.getInstance().setDetailPlaylistSongsSortOrder(sortOrder);
        } else if (genre != null) {
            SortManager.getInstance().setDetailGenreSongsSortOrder(sortOrder);
        } else if (album != null) {
            SortManager.getInstance().setDetailAlbumSongsSortOrder(sortOrder);
        } else {
            SortManager.getInstance().setDetailSongsSortOrder(sortOrder);
        }
    }

    @SortManager.SongSort
    private int getSongsSortOrder() {
        if (playlist != null) {
            return SortManager.getInstance().getDetailPlaylistSongsSortOrder();
        } else if (genre != null) {
            return SortManager.getInstance().getDetailGenreSongsSortOrder();
        } else if (album != null) {
            return SortManager.getInstance().getDetailAlbumSongsSortOrder();
        } else {
            return SortManager.getInstance().getDetailSongsSortOrder();
        }
    }

    private void setSongsAscending(boolean ascending) {
        if (playlist != null) {
            SortManager.getInstance().setDetailPlaylistSongsAscending(ascending);
        } else if (genre != null) {
            SortManager.getInstance().setDetailGenreSongsAscending(ascending);
        } else {
            SortManager.getInstance().setDetailSongsAscending(ascending);
        }
    }

    private boolean getSongsAscending() {
        if (playlist != null) {
            return SortManager.getInstance().getDetailPlaylistSongsAscending();
        } else if (genre != null) {
            return SortManager.getInstance().getDetailGenreSongsAscending();
        } else {
            return SortManager.getInstance().getDetailSongsAscending();
        }
    }

    private void setAlbumsOrder(@SortManager.AlbumSort int sortOrder) {
        if (playlist != null) {
            SortManager.getInstance().setDetailPlaylistAlbumsSortOrder(sortOrder);
        } else if (genre != null) {
            SortManager.getInstance().setDetailGenreAlbumsSortOrder(sortOrder);
        } else {
            SortManager.getInstance().setDetailAlbumsSortOrder(sortOrder);
        }
    }

    @SortManager.AlbumSort
    private int getAlbumsSortOrder() {
        if (playlist != null) {
            return SortManager.getInstance().getDetailPlaylistAlbumsSortOrder();
        } else if (genre != null) {
            return SortManager.getInstance().getDetailGenreAlbumsSortOrder();
        } else {
            return SortManager.getInstance().getDetailAlbumsSortOrder();
        }
    }

    private void setAlbumsAscending(boolean ascending) {
        if (playlist != null) {
            SortManager.getInstance().setDetailPlaylistAlbumsAscending(ascending);
        } else if (genre != null) {
            SortManager.getInstance().setDetailGenreAlbumsAscending(ascending);
        } else {
            SortManager.getInstance().setDetailAlbumsAscending(ascending);
        }
    }

    private boolean getAlbumsAscending() {
        if (playlist != null) {
            return SortManager.getInstance().getDetailPlaylistAlbumsAscending();
        } else if (genre != null) {
            return SortManager.getInstance().getDetailGenreAlbumsAscending();
        } else {
            return SortManager.getInstance().getDetailAlbumsAscending();
        }
    }

    boolean canEdit() {
        return playlist != null
                && playlist.canEdit
                && getSongsSortOrder() == SortManager.SongSort.DETAIL_DEFAULT;
    }

    void onCreateActionMode(Menu menu) {
        ThemeUtils.themeContextualActionBar(getActivity());
        inActionMode = true;
        getActivity().getMenuInflater().inflate(R.menu.context_menu_songs, menu);
        final SubMenu sub = menu.getItem(0).getSubMenu();
        PlaylistUtils.makePlaylistMenu(getActivity(), sub, SONG_FRAGMENT_GROUP_ID);
    }

    boolean onActionItemClicked(MenuItem item) {
        final ArrayList<Song> checkedSongs = getCheckedSongs();
        if (checkedSongs == null || checkedSongs.isEmpty()) {
            return true;
        }
        switch (item.getItemId()) {
            case NEW_PLAYLIST:
                PlaylistUtils.createPlaylistDialog(getActivity(), checkedSongs);
                return true;
            case PLAYLIST_SELECTED:
                Playlist playlist = (Playlist) item.getIntent().getSerializableExtra(ShuttleUtils.ARG_PLAYLIST);
                PlaylistUtils.addToPlaylist(getContext(), playlist, checkedSongs);
                return true;
            case R.id.delete:
                new DialogUtils.DeleteDialogBuilder()
                        .context(getContext())
                        .singleMessageId(R.string.delete_song_desc)
                        .multipleMessage(R.string.delete_song_desc_multiple)
                        .itemNames(Stream.of(checkedSongs)
                                .map(song -> song.name)
                                .collect(Collectors.toList()))
                        .songsToDelete(Observable.just(checkedSongs))
                        .build()
                        .show();
                return true;
            case R.id.menu_add_to_queue:
                MusicUtils.addToQueue(getActivity(), checkedSongs);
                return true;
        }
        return false;
    }

    void onDestroyActionMode() {
        actionMode = null;
        inActionMode = false;
        multiSelector.clearSelections();
    }

    private static final class SelectorCallback extends ModalMultiSelectorCallback {

        private final WeakReference<DetailFragment> mParent;

        SelectorCallback(DetailFragment parent) {
            super(null);
            mParent = new WeakReference<>(parent);
        }

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            final DetailFragment parent = mParent.get();
            if (parent != null) {
                parent.onCreateActionMode(menu);
                return true;
            }
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            final DetailFragment parent = mParent.get();
            if (parent != null) {
                if (parent.onActionItemClicked(item)) {
                    mode.finish();
                    return true;
                }
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            super.onDestroyActionMode(actionMode);
            final DetailFragment parent = mParent.get();
            if (parent != null) {
                parent.onDestroyActionMode();
            }
        }

    }

    ArrayList<Song> getCheckedSongs() {
        return Stream.of(multiSelector.getSelectedPositions())
                .map(i -> ((SongView) adapter.items.get(i)).song)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab:
                if (albumArtist != null) {
                    MusicUtils.shuffleAll(getActivity(), albumArtist.getSongsObservable());
                } else if (album != null) {
                    MusicUtils.shuffleAll(getActivity(), album.getSongsObservable());
                } else if (genre != null) {
                    MusicUtils.shuffleAll(getActivity(), genre.getSongsObservable(getContext()));
                } else if (playlist != null) {
                    MusicUtils.shuffleAll(getActivity(), playlist.getSongsObservable(getContext()));
                }
                break;
            case R.id.btn_overflow:
                final PopupMenu menu = new PopupMenu(getActivity(), v);
                if (album != null) {
                    MenuUtils.addAlbumMenuOptions(getActivity(), menu);
                    MenuUtils.addClickHandler((AppCompatActivity) getActivity(), menu, album);
                    menu.getMenu().add(ALBUM_FRAGMENT_GROUP_ID, VIEW_INFO, Menu.NONE, R.string.info);
                } else if (albumArtist != null) {
                    MenuUtils.addAlbumArtistMenuOptions(getActivity(), menu);
                    MenuUtils.addClickHandler((AppCompatActivity) getActivity(), menu, albumArtist);
                    menu.getMenu().add(ALBUM_FRAGMENT_GROUP_ID, VIEW_INFO, Menu.NONE, R.string.info);
                } else if (genre != null) {

                } else if (playlist != null) {
                    MenuUtils.addPlaylistMenuOptions(menu, playlist);
                    //Remove the delete menu option, since we're looking at the playlist we would delete.
                    if (menu.getMenu().findItem(MusicUtils.PlaylistMenuOrder.DELETE_PLAYLIST) != null) {
                        menu.getMenu().removeItem(MusicUtils.PlaylistMenuOrder.DELETE_PLAYLIST);
                    }
                    MenuUtils.addClickHandler(getActivity(), menu, playlist, (materialDialog, dialogAction) -> {
                        //The user might have changed the playlist name
                        lineOne.setText(playlist.name);
                    }, (materialDialog, dialogAction) -> {
                        //If the user clicked 'edit', they've probably set a new 'week' range. Restart the loader.
                        refreshAdapterItems();
                    });
                }
                menu.show();
                break;
        }
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        if (holder.getAdapterPosition() != -1) {
            adapter.items.get(holder.getAdapterPosition()).recycle(holder);
        }
    }

    @Override
    public void onItemClick(View v, int position, Song song) {

        if (inActionMode) {
            multiSelector.setSelected(position, adapter.getItemId(position), !multiSelector.isSelected(position, adapter.getItemId(position)));
            if (multiSelector.getSelectedPositions().isEmpty()) {
                if (actionMode != null) {
                    actionMode.finish();
                }
            }
        } else {
            List<Song> songs = Stream.of(adapter.items)
                    .filter(adaptableItem -> adaptableItem instanceof SongView)
                    .map(viewHolderAdaptableItem -> ((SongView) viewHolderAdaptableItem).song)
                    .collect(Collectors.toList());

            MusicUtils.playAll(songs, songs.indexOf(song), () -> {
                final String message = getContext().getString(R.string.emptyplaylist);
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public void onOverflowClick(View v, final int position, final Song song) {

        PopupMenu menu = new PopupMenu(getActivity(), v);
        MenuUtils.addSongMenuOptions(getActivity(), menu);

        if (playlist != null && playlist.canEdit) {
            menu.getMenu().add(SONG_FRAGMENT_GROUP_ID, REMOVE, 10, R.string.remove_from_playlist);
        }

        MenuUtils.addClickHandler((AppCompatActivity) getActivity(), menu, song, item -> {
            switch (item.getItemId()) {
                case BLACKLIST:
                    adapter.removeItem(position);
                    BlacklistHelper.addToBlacklist(song);
                    return true;
                case REMOVE:
                    if (playlist != null) {
                        adapter.removeItem(position);
                        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlist.id);
                        getActivity().getContentResolver().delete(ContentUris.withAppendedId(uri, song.playlistSongId), null, null);
                    }
            }
            return false;
        });
        menu.show();
    }

    @Override
    public void onLongClick(View v, int position, Song song) {
        if (inActionMode) {
            return;
        }

        final SelectorCallback callback = new SelectorCallback(this);
        callback.setMultiSelector(multiSelector);

        if (multiSelector.getSelectedPositions().isEmpty()) {
            actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(callback);
            inActionMode = true;
        }
        multiSelector.setSelected(position, adapter.getItemId(position), !multiSelector.isSelected(position, adapter.getItemId(position)));
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        if (itemTouchHelper != null) {
            itemTouchHelper.startDrag(viewHolder);
        }
    }

    @Override
    public void onItemClick(ItemAdapter adapter, View v, int position, Object item) {
        if (inActionMode) {
            if (actionMode != null) {
                actionMode.finish();
            }
        }

        ((MainActivity) getActivity()).swapFragments((Album) item, v.findViewById(R.id.image));
    }

    @Override
    public void onOverflowClick(View v, int position, Object item) {
        PopupMenu menu = new PopupMenu(getActivity(), v);
        MenuUtils.addAlbumMenuOptions(getActivity(), menu);
        MenuUtils.addClickHandler((AppCompatActivity) getActivity(), menu, (Album) item);
        menu.show();
    }

    private static class HeaderView extends BaseAdaptableItem {

        public int height = ResourceUtils.toPixels(350);

        HeaderView() {
        }

        @Override
        public int getViewType() {
            return ViewType.DETAIL_HEADER;
        }

        @Override
        public int getLayoutResId() {
            return -1;
        }

        @Override
        public void bindView(RecyclerView.ViewHolder holder) {
            holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
        }

        @Override
        public void bindView(RecyclerView.ViewHolder holder, int position, List payloads) {
        }

        @Override
        public RecyclerView.ViewHolder getViewHolder(ViewGroup parent) {

            FrameLayout headerView = new FrameLayout(parent.getContext());
            //Set the headerItem layout params.. Arbitrary height, this will be adjusted via the
            //ViewTreeObserver in onCreateView
            headerView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
            return new ViewHolder(headerView);
        }

        @Override
        public Object getItem() {
            return null;
        }

        private static class ViewHolder extends RecyclerView.ViewHolder {

            public ViewHolder(View itemView) {
                super(itemView);
            }

            @Override
            public String toString() {
                return "HeaderView.ViewHolder";
            }
        }
    }

    @Override
    public void setSharedElementEnterTransition(Object transition) {
        super.setSharedElementEnterTransition(transition);
        if (ShuttleUtils.hasLollipop()) {
            ((Transition) transition).addListener(getSharedElementEnterTransitionListenerAdapter());
        }
    }

    SharedElementCallback enterSharedElementCallback = new SharedElementCallback() {
        @Override
        public void onSharedElementStart(List<String> sharedElementNames, List<View> sharedElements, List<View> sharedElementSnapshots) {
            super.onSharedElementStart(sharedElementNames, sharedElements, sharedElementSnapshots);

            if (fab != null) {
                fab.setVisibility(View.GONE);
            }
        }
    };

    private TransitionListenerAdapter getSharedElementEnterTransitionListenerAdapter() {
        if (ShuttleUtils.hasLollipop()) {
            return new TransitionListenerAdapter() {

                @Override
                public void onTransitionEnd(Transition transition) {

                    if (ShuttleUtils.hasLollipop()) {

                        //Todo:
                        //This is a partial fix for an issue where the initial artwork load tricks Glide into thinking the final
                        //image in the transition has the dimensions of the initial image.
                        //The idea is we would call this to force Glide to load a full res image, and we wouldn't need to use the
                        //override call in our initial Glide load in onCreateView.
                        //Unfortunately, because the initial image is square, but our header image is not, the transition isn't very nice..
                        //So for now, we'll stick with overriding the initial image dimensions..

//                        if (isAdded()) {
//                            requestManager
//                                    .load(albumArtist == null ? album : albumArtist)
//                                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
//                                    .priority(Priority.HIGH)
//                                    .centerCrop()
//                                    .thumbnail(Glide
//                                            .with(DetailFragment.this)
//                                            .load(albumArtist == null ? album : albumArtist)
//                                            .override(headerImageView.getDrawable().getIntrinsicWidth(), headerImageView.getDrawable().getIntrinsicHeight())
//                                            .centerCrop())
//                                    .animate(new AlwaysCrossFade(false))
//                                    .into(headerImageView);
//                        }

                        transition.removeListener(this);

                        //Fade in the text protection scrim
                        textProtectionScrim.setAlpha(0f);
                        textProtectionScrim.setVisibility(View.VISIBLE);
                        ObjectAnimator fadeAnimator = ObjectAnimator.ofFloat(textProtectionScrim, View.ALPHA, 0f, 1f);
                        fadeAnimator.setDuration(800);
                        fadeAnimator.start();

                        //Fade & grow the FAB
                        fab.setAlpha(0f);
                        fab.setVisibility(View.VISIBLE);

                        fadeAnimator = ObjectAnimator.ofFloat(fab, View.ALPHA, 0.5f, 1f);
                        ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(fab, View.SCALE_X, 0f, 1f);
                        ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(fab, View.SCALE_Y, 0f, 1f);

                        AnimatorSet animatorSet = new AnimatorSet();
                        animatorSet.setInterpolator(new OvershootInterpolator(2f));
                        animatorSet.playTogether(fadeAnimator, scaleXAnimator, scaleYAnimator);
                        animatorSet.setDuration(250);
                        animatorSet.start();
                    }
                }
            };
        }
        return null;
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}