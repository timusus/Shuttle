package com.simplecity.amp_library.utils.menu.album;

import android.content.Context;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.view.SubMenu;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.MediaManager;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.extensions.AlbumExt;
import com.simplecity.amp_library.utils.menu.MenuUtils;
import io.reactivex.Single;
import java.util.List;

public class AlbumMenuUtils {

    public interface Callbacks {

        void onPlaylistItemsInserted();

        void onQueueItemsInserted(String message);

        void playNext(Single<List<Song>> songsSingle);

        void showTagEditor(Album album);

        void showDeleteDialog(Album album);

        void showDeleteDialog(List<Album> albums);

        void showDeleteDialog(Single<List<Album>> albums);

        void showAlbumInfo(Album album);

        void showArtworkChooser(Album album);

        void showUpgradeDialog();

        void showToast(String message);
    }

    private AlbumMenuUtils() {
        //no instance
    }

    public static void setupAlbumMenu(PopupMenu menu) {
        menu.inflate(R.menu.menu_album);

        // Add playlist menu
        SubMenu sub = menu.getMenu().findItem(R.id.addToPlaylist).getSubMenu();
        PlaylistUtils.createPlaylistMenu(sub);
    }

    public static Toolbar.OnMenuItemClickListener getAlbumMenuClickListener(Context context, MediaManager mediaManager, Single<List<Album>> selectedAlbums, Callbacks callbacks) {
        return item -> {
            switch (item.getItemId()) {
                case MediaManager.NEW_PLAYLIST:
                    MenuUtils.newPlaylist(context, AlbumExt.INSTANCE.getSongsForAlbums(selectedAlbums), callbacks::onPlaylistItemsInserted);
                    return true;
                case MediaManager.PLAYLIST_SELECTED:
                    MenuUtils.addToPlaylist(context, item, AlbumExt.INSTANCE.getSongsForAlbums(selectedAlbums), callbacks::onPlaylistItemsInserted);
                    return true;
                case R.id.playNext:
                    callbacks.playNext(AlbumExt.INSTANCE.getSongsForAlbums(selectedAlbums));
                    return true;
                case R.id.addToQueue:
                    MenuUtils.addToQueue(mediaManager, AlbumExt.INSTANCE.getSongsForAlbums(selectedAlbums), callbacks::onQueueItemsInserted);
                    return true;
                case R.id.delete:
                    callbacks.showDeleteDialog(selectedAlbums);
                    return true;
            }
            return false;
        };
    }

    public static PopupMenu.OnMenuItemClickListener getAlbumMenuClickListener(Context context, MediaManager mediaManager, Album album, Callbacks callbacks) {
        return item -> {
            switch (item.getItemId()) {
                case R.id.play:
                    MenuUtils.play(mediaManager, AlbumExt.INSTANCE.getSongsForAlbum(album), callbacks::showToast);
                    return true;
                case R.id.playNext:
                    callbacks.playNext(AlbumExt.INSTANCE.getSongsForAlbum(album));
                    return true;
                case MediaManager.NEW_PLAYLIST:
                    MenuUtils.newPlaylist(context, AlbumExt.INSTANCE.getSongsForAlbum(album), callbacks::onPlaylistItemsInserted);
                    return true;
                case MediaManager.PLAYLIST_SELECTED:
                    MenuUtils.addToPlaylist(context, item, AlbumExt.INSTANCE.getSongsForAlbum(album), callbacks::onPlaylistItemsInserted);
                    return true;
                case R.id.addToQueue:
                    MenuUtils.addToQueue(mediaManager, AlbumExt.INSTANCE.getSongsForAlbum(album), callbacks::onQueueItemsInserted);
                    return true;
                case R.id.editTags:
                    if (ShuttleUtils.isUpgraded()) {
                        callbacks.showTagEditor(album);
                    } else {
                        callbacks.showUpgradeDialog();
                    }
                    return true;
                case R.id.info:
                    callbacks.showAlbumInfo(album);
                    return true;
                case R.id.artwork:
                    callbacks.showArtworkChooser(album);
                    return true;
                case R.id.blacklist:
                    MenuUtils.blacklist(AlbumExt.INSTANCE.getSongsForAlbum(album));
                    return true;
                case R.id.delete:
                    callbacks.showDeleteDialog(album);
                    return true;
            }
            return false;
        };
    }
}