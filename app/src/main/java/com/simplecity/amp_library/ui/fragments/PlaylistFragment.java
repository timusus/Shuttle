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
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.ui.modelviews.EmptyView;
import com.simplecity.amp_library.ui.modelviews.PlaylistView;
import com.simplecity.amp_library.utils.ComparisonUtils;
import com.simplecity.amp_library.utils.DataManager;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.MenuUtils;
import com.simplecity.amp_library.utils.PermissionUtils;
import com.simplecityapps.recycler_adapter.adapter.ViewModelAdapter;
import com.simplecityapps.recycler_adapter.model.ViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.RecyclerListener;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class PlaylistFragment extends BaseFragment {

    public interface PlaylistClickListener {
        void onPlaylistClicked(Playlist playlist);
    }

    private static final String TAG = "PlaylistFragment";

    private static final String ARG_PAGE_TITLE = "page_title";

    private ViewModelAdapter adapter;

    @Nullable
    private PlaylistClickListener playlistClickListener;

    private Disposable disposable;

    public PlaylistFragment() {

    }

    public static PlaylistFragment newInstance(String pageTitle) {
        PlaylistFragment fragment = new PlaylistFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PAGE_TITLE, pageTitle);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (getParentFragment() instanceof PlaylistClickListener) {
            playlistClickListener = (PlaylistClickListener) getParentFragment();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        playlistClickListener = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = new ViewModelAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FastScrollRecyclerView recyclerView = (FastScrollRecyclerView) inflater.inflate(R.layout.fragment_recycler, container, false);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setRecyclerListener(new RecyclerListener());
        recyclerView.setAdapter(adapter);

        return recyclerView;
    }

    @Override
    public void onPause() {
        super.onPause();

        if (disposable != null) {
            disposable.dispose();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshAdapterItems();
    }


    private void refreshAdapterItems() {
        PermissionUtils.RequestStoragePermissions(() -> {
            if (getActivity() != null && isAdded()) {

                Observable<List<Playlist>> defaultPlaylistsObservable =
                        Observable.defer(() ->
                                {
                                    List<Playlist> playlists = new ArrayList<>();

                                    Playlist podcastPlaylist = Playlist.podcastPlaylist();
                                    if (podcastPlaylist != null) {
                                        playlists.add(podcastPlaylist);
                                    }

                                    playlists.add(Playlist.recentlyAddedPlaylist);
                                    playlists.add(Playlist.mostPlayedPlaylist);
                                    return Observable.just(playlists);
                                }
                        );

                Observable<List<Playlist>> playlistsObservable = DataManager.getInstance().getPlaylistsRelay();

                disposable = Observable.combineLatest(
                        defaultPlaylistsObservable, playlistsObservable, (defaultPlaylists, playlists) -> {
                            List<Playlist> list = new ArrayList<>();
                            list.addAll(defaultPlaylists);
                            list.addAll(playlists);
                            return list;
                        })
                        .subscribeOn(Schedulers.io())
                        .map(playlists -> {

                            PlaylistView.OnClickListener listener = new PlaylistView.OnClickListener() {
                                @Override
                                public void onPlaylistClick(int position, PlaylistView playlistView) {
                                    if (playlistClickListener != null) {
                                        playlistClickListener.onPlaylistClicked(playlistView.playlist);
                                    }
                                }

                                @Override
                                public void onPlaylistOverflowClick(int position, View v, Playlist playlist) {
                                    PopupMenu menu = new PopupMenu(PlaylistFragment.this.getActivity(), v);
                                    MenuUtils.setupPlaylistMenu(menu, playlist);
                                    menu.setOnMenuItemClickListener(MenuUtils.getPlaylistPopupMenuClickListener(getContext(), playlist, null));
                                    menu.show();
                                }
                            };

                            return Stream.of(playlists)
                                    .sorted((a, b) -> ComparisonUtils.compare(a.name, b.name))
                                    .sorted((a, b) -> ComparisonUtils.compareInt(a.type, b.type))
                                    .map(playlist -> {
                                        PlaylistView playlistView = new PlaylistView(playlist);
                                        playlistView.setListener(listener);
                                        return (ViewModel) playlistView;
                                    })
                                    .toList();
                        })
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(items -> {
                            if (items.isEmpty()) {
                                adapter.setItems(Collections.singletonList(new EmptyView(R.string.empty_playlist)));
                            } else {
                                adapter.setItems(items);
                            }
                        }, error -> LogUtils.logException(TAG, "Error refreshing adapter", error));
            }
        });
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}
