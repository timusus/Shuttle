package com.simplecity.amp_library.ui.drawer;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.annimon.stream.Stream;
import com.bignerdranch.expandablerecyclerview.model.Parent;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.dagger.module.FragmentModule;
import com.simplecity.amp_library.glide.utils.GlideUtils;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.ui.fragments.BaseFragment;
import com.simplecity.amp_library.ui.presenters.PlayerPresenter;
import com.simplecity.amp_library.ui.views.CircleImageView;
import com.simplecity.amp_library.ui.views.PlayerViewAdapter;
import com.simplecityapps.recycler_adapter.recyclerview.ChildAttachStateChangeListener;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DrawerFragment extends BaseFragment implements
        DrawerView,
        View.OnCreateContextMenuListener,
        DrawerParent.ClickListener {

    private static final String TAG = "DrawerFragment";

    private static final String STATE_SELECTED_DRAWER_PARENT = "selected_drawer_parent";

    private static final String STATE_SELECTED_PLAYLIST = "selected_drawer_playlist";

    private DrawerAdapter adapter;

    private DrawerParent playlistDrawerParent;

    private View rootView;

    @BindView(R.id.line1)
    TextView trackNameView;

    @BindView(R.id.line2)
    TextView artistNameView;

    @BindView(R.id.placeholder_text)
    TextView placeholderText;

    @BindView(R.id.background_image)
    ImageView backgroundImage;

    @BindView(R.id.artist_image)
    CircleImageView artistImage;

    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;

    @DrawerParent.Type
    private int selectedDrawerParent = DrawerParent.Type.LIBRARY;

    private Playlist currentSelectedPlaylist = null;

    @Inject PlayerPresenter playerPresenter;

    @Inject DrawerPresenter drawerPresenter;

    private RequestManager requestManager;

    private Drawable backgroundPlaceholder;

    public DrawerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ShuttleApplication.getInstance().getAppComponent()
                .plus(new FragmentModule(this))
                .inject(this);

        if (savedInstanceState != null) {
            selectedDrawerParent = savedInstanceState.getInt(STATE_SELECTED_DRAWER_PARENT, DrawerParent.Type.LIBRARY);
            currentSelectedPlaylist = (Playlist) savedInstanceState.get(STATE_SELECTED_PLAYLIST);
        }

        requestManager = Glide.with(this);

        backgroundPlaceholder = DrawableCompat.wrap(getResources().getDrawable(R.drawable.ic_drawer_header_placeholder));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_drawer, container, false);

        ButterKnife.bind(this, rootView);

        playlistDrawerParent = DrawerParent.playlistsParent;

        List<Parent<DrawerChild>> drawerParents = new ArrayList<>();
        drawerParents.add(DrawerParent.libraryParent);
        drawerParents.add(DrawerParent.folderParent);
        drawerParents.add(playlistDrawerParent);
        drawerParents.add(new DrawerDivider());
        drawerParents.add(DrawerParent.sleepTimerParent);
        drawerParents.add(DrawerParent.equalizerParent);
        drawerParents.add(DrawerParent.settingsParent);
        drawerParents.add(DrawerParent.supportParent);

        Stream.of(drawerParents)
                .filter(parent -> parent instanceof DrawerParent)
                .forEach(parent -> ((DrawerParent) parent).setListener(this));

        adapter = new DrawerAdapter(drawerParents);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.addOnChildAttachStateChangeListener(new ChildAttachStateChangeListener(recyclerView));

        setDrawerItemSelected(selectedDrawerParent);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        drawerPresenter.bindView(this);
        playerPresenter.bindView(playerViewAdapter);
    }

    @Override
    public void onPause() {
        drawerPresenter.unbindView(this);
        playerPresenter.unbindView(playerViewAdapter);
        super.onPause();
    }

    @Override
    public void onClick(DrawerParent drawerParent) {
        drawerPresenter.onDrawerItemClicked(drawerParent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(STATE_SELECTED_DRAWER_PARENT, selectedDrawerParent);
        outState.putSerializable(STATE_SELECTED_PLAYLIST, currentSelectedPlaylist);
    }

    @Override
    public void setItems(List<DrawerChild> drawerChildren) {

        int parentPosition = adapter.getParentList().indexOf(playlistDrawerParent);

        int prevItemCount = playlistDrawerParent.children.size();
        playlistDrawerParent.children.clear();
        adapter.notifyChildRangeRemoved(parentPosition, 0, prevItemCount);

        playlistDrawerParent.children.addAll(drawerChildren);
        adapter.notifyChildRangeInserted(parentPosition, 0, drawerChildren.size());

        adapter.notifyParentChanged(parentPosition);
    }

    @Override
    public void closeDrawer() {
        DrawerLayout drawerLayout = getParentDrawerLayout(rootView);
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(Gravity.START);
        }
    }

    @Override
    public void setDrawerItemSelected(@DrawerParent.Type int type) {
        Stream.of(adapter.getParentList())
                .filter(parent -> parent instanceof DrawerParent)
                .map(parent -> ((DrawerParent) parent))
                .forEach(drawerParent -> drawerParent.setSelected(drawerParent.type == type));

        adapter.notifyParentRangeChanged(0, adapter.getParentList().size());
    }

    PlayerViewAdapter playerViewAdapter = new PlayerViewAdapter() {
        @Override
        public void trackInfoChanged(@Nullable Song song) {

            if (song == null) {
                return;
            }

            trackNameView.setText(song.name);
            artistNameView.setText(String.format("%s - %s", song.albumArtistName, song.albumName));
            placeholderText.setText(R.string.app_name);

            requestManager.load(song)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .error(backgroundPlaceholder)
                    .into(backgroundImage);

            requestManager.load(song.getAlbumArtist())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(GlideUtils.getMediumPlaceHolderResId())
                    .into(artistImage);

            if (song.name == null || (song.albumName == null && song.albumArtistName == null)) {
                placeholderText.setVisibility(View.VISIBLE);
                trackNameView.setVisibility(View.GONE);
                artistNameView.setVisibility(View.GONE);
            } else {
                placeholderText.setVisibility(View.GONE);
                trackNameView.setVisibility(View.VISIBLE);
                artistNameView.setVisibility(View.VISIBLE);
            }
        }
    };

    @Nullable
    public static DrawerLayout getParentDrawerLayout(@Nullable View v) {
        if (v == null) return null;

        if (v instanceof DrawerLayout) {
            return (DrawerLayout) v;
        }

        if (v.getParent() instanceof View) {
            return getParentDrawerLayout((View) v.getParent());
        }

        return null;
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}