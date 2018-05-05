package com.simplecity.amp_library.utils.menu.playlist;

import android.support.annotation.StringRes;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.playback.MediaManager;
import com.simplecity.amp_library.rx.UnsafeAction;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.menu.MenuUtils;
import java.util.Collections;

public class PlaylistMenuUtils {

    public interface Callbacks {

        void showToast(String message);

        void showToast(@StringRes int messageResId);

        void showWeekSelectorDialog();

        void showRenamePlaylistDialog(Playlist playlist);

        void showCreateM3uPlaylistDialog(Playlist playlist);

        void playNext(Playlist playlist);

        void showDeleteConfirmationDialog(Playlist playlist, UnsafeAction onDelete);

        void onPlaylistDeleted();
    }

    private PlaylistMenuUtils() {
        //no instance
    }

    public static void edit(Callbacks callbacks, Playlist playlist) {
        if (playlist.id == PlaylistUtils.PlaylistIds.RECENTLY_ADDED_PLAYLIST) {
            callbacks.showWeekSelectorDialog();
        }
    }

    public static void rename(Callbacks callbacks, Playlist playlist) {
        callbacks.showRenamePlaylistDialog(playlist);
    }

    public static void export(Callbacks callbacks, Playlist playlist) {
        callbacks.showCreateM3uPlaylistDialog(playlist);
    }

    public static void clear(Playlist playlist) {
        playlist.clear();
    }

    public static void setupPlaylistMenu(PopupMenu menu, Playlist playlist) {
        menu.inflate(R.menu.menu_playlist);

        if (!playlist.canDelete) {
            menu.getMenu().findItem(R.id.deletePlaylist).setVisible(false);
        }

        if (!playlist.canClear) {
            menu.getMenu().findItem(R.id.clearPlaylist).setVisible(false);
        }

        if (playlist.id != PlaylistUtils.PlaylistIds.RECENTLY_ADDED_PLAYLIST) {
            menu.getMenu().findItem(R.id.editPlaylist).setVisible(false);
        }

        if (!playlist.canRename) {
            menu.getMenu().findItem(R.id.renamePlaylist).setVisible(false);
        }

        if (playlist.id == PlaylistUtils.PlaylistIds.MOST_PLAYED_PLAYLIST) {
            menu.getMenu().findItem(R.id.exportPlaylist).setVisible(false);
        }
    }

    public static void setupPlaylistMenu(Toolbar toolbar, Playlist playlist) {
        toolbar.inflateMenu(R.menu.menu_playlist);

        if (!playlist.canDelete) {
            toolbar.getMenu().findItem(R.id.deletePlaylist).setVisible(false);
        }

        if (!playlist.canClear) {
            toolbar.getMenu().findItem(R.id.clearPlaylist).setVisible(false);
        }

        if (playlist.id != PlaylistUtils.PlaylistIds.RECENTLY_ADDED_PLAYLIST) {
            toolbar.getMenu().findItem(R.id.editPlaylist).setVisible(false);
        }

        if (!playlist.canRename) {
            toolbar.getMenu().findItem(R.id.renamePlaylist).setVisible(false);
        }

        if (playlist.id == PlaylistUtils.PlaylistIds.MOST_PLAYED_PLAYLIST) {
            toolbar.getMenu().findItem(R.id.exportPlaylist).setVisible(false);
        }
    }

    public static PopupMenu.OnMenuItemClickListener getPlaylistPopupMenuClickListener(MediaManager mediaManager, Playlist playlist, Callbacks callbacks) {
        return item -> handleMenuItemClicks(item, mediaManager, playlist, callbacks);
    }

    public static boolean handleMenuItemClicks(MenuItem menuItem, MediaManager mediaManager, Playlist playlist, Callbacks callbacks) {
        switch (menuItem.getItemId()) {
            case R.id.playPlaylist:
                MenuUtils.play(mediaManager, playlist.getSongsObservable().first(Collections.emptyList()), callbacks::showToast);
                return true;
            case R.id.playNext:
                callbacks.playNext(playlist);
                return true;
            case R.id.deletePlaylist:
                callbacks.showDeleteConfirmationDialog(playlist, () -> {
                    playlist.delete();
                    callbacks.onPlaylistDeleted();
                });
                return true;
            case R.id.editPlaylist:
                edit(callbacks, playlist);
                return true;
            case R.id.renamePlaylist:
                rename(callbacks, playlist);
                return true;
            case R.id.exportPlaylist:
                export(callbacks, playlist);
                return true;
            case R.id.clearPlaylist:
                clear(playlist);
                return true;
        }
        return false;
    }

    public static class CallbacksAdapter implements Callbacks {

        @Override
        public void showToast(String message) {

        }

        @Override
        public void showToast(int messageResId) {

        }

        @Override
        public void showWeekSelectorDialog() {

        }

        @Override
        public void showRenamePlaylistDialog(Playlist playlist) {

        }

        @Override
        public void showCreateM3uPlaylistDialog(Playlist playlist) {

        }

        @Override
        public void playNext(Playlist playlist) {

        }

        @Override
        public void showDeleteConfirmationDialog(Playlist playlist, UnsafeAction onDelete) {

        }

        @Override
        public void onPlaylistDeleted() {

        }
    }
}
