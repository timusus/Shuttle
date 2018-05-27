package com.simplecity.amp_library.utils.menu.song;

import android.content.Context;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.view.SubMenu;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.MediaManager;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.menu.MenuUtils;
import io.reactivex.Single;
import java.util.Collections;
import java.util.List;

public class SongMenuUtils {

    public interface Callbacks {

        void onPlaylistItemsInserted();

        void onQueueItemInserted(String message);

        void onSongsRemoved(Single<List<Song>> songsSingle);

        void onSongRemoved(int position, Song song);

        void playNext(Single<List<Song>> songsSingle);

        void showBiographyDialog(Song song);

        void showDeleteDialog(Single<List<Song>> songsSingle);

        void showDeleteDialog(Song song);

        void showTagEditor(Song song);

        void showToast(String message);

        void shareSong(Song song);

        void setRingtone(Song song);
    }

    public static void setupSongMenu(PopupMenu menu, boolean showRemoveButton) {
        menu.inflate(R.menu.menu_song);

        if (!showRemoveButton) {
            menu.getMenu().findItem(R.id.remove).setVisible(false);
        }

        // Add playlist menu
        SubMenu sub = menu.getMenu().findItem(R.id.addToPlaylist).getSubMenu();
        PlaylistUtils.createPlaylistMenu(sub);
    }

    public static Toolbar.OnMenuItemClickListener getQueueMenuClickListener(Context context, Single<List<Song>> songsSingle, Callbacks callbacks) {
        return item -> {
            switch (item.getItemId()) {
                case MediaManager.NEW_PLAYLIST:
                    MenuUtils.newPlaylist(context, songsSingle, callbacks::onPlaylistItemsInserted);
                    return true;
                case MediaManager.PLAYLIST_SELECTED:
                    MenuUtils.addToPlaylist(context, item, songsSingle, callbacks::onPlaylistItemsInserted);
                    return true;
                case R.id.delete:
                    callbacks.showDeleteDialog(songsSingle);
                    return true;
                case R.id.queue_remove:
                    callbacks.onSongsRemoved(songsSingle);
                    return true;
            }
            return false;
        };
    }

    public static Toolbar.OnMenuItemClickListener getSongMenuClickListener(Context context, MediaManager mediaManager, Single<List<Song>> songsSingle, Callbacks callbacks) {
        return item -> {
            switch (item.getItemId()) {
                case MediaManager.NEW_PLAYLIST:
                    MenuUtils.newPlaylist(context, songsSingle, callbacks::onPlaylistItemsInserted);
                    return true;
                case MediaManager.PLAYLIST_SELECTED:
                    MenuUtils.addToPlaylist(context, item, songsSingle, callbacks::onPlaylistItemsInserted);
                    return true;
                case R.id.playNext:
                    callbacks.playNext(songsSingle);
                    return true;
                case R.id.addToQueue:
                    MenuUtils.addToQueue(mediaManager, songsSingle, callbacks::showToast);
                    return true;
                case R.id.blacklist:
                    MenuUtils.blacklist(songsSingle);
                    return true;
                case R.id.delete:
                    callbacks.showDeleteDialog(songsSingle);
                    return true;
            }
            return false;
        };
    }

    public static PopupMenu.OnMenuItemClickListener getSongMenuClickListener(Context context, MediaManager mediaManager, int position, Song song, Callbacks callbacks) {
        return item -> {
            switch (item.getItemId()) {
                case R.id.playNext:
                    MenuUtils.playNext(mediaManager, song, callbacks::showToast);
                    return true;
                case MediaManager.NEW_PLAYLIST:
                    MenuUtils.newPlaylist(context, Collections.singletonList(song), callbacks::onPlaylistItemsInserted);
                    return true;
                case MediaManager.PLAYLIST_SELECTED:
                    MenuUtils.addToPlaylist(context, item, Collections.singletonList(song), callbacks::onPlaylistItemsInserted);
                    return true;
                case R.id.addToQueue:
                    MenuUtils.addToQueue(mediaManager, Collections.singletonList(song), callbacks::onQueueItemInserted);
                    return true;
                case R.id.editTags:
                    callbacks.showTagEditor(song);
                    return true;
                case R.id.share:
                    callbacks.shareSong(song);
                    return true;
                case R.id.ringtone:
                    callbacks.setRingtone(song);
                    return true;
                case R.id.songInfo:
                    callbacks.showBiographyDialog(song);
                    return true;
                case R.id.blacklist:
                    MenuUtils.blacklist(song);
                    return true;
                case R.id.delete:
                    callbacks.showDeleteDialog(song);
                    return true;
                case R.id.remove:
                    callbacks.onSongRemoved(position, song);
                    return true;
            }
            return false;
        };
    }

    public static class CallbacksAdapter implements Callbacks {

        @Override
        public void onPlaylistItemsInserted() {

        }

        @Override
        public void onQueueItemInserted(String message) {

        }

        @Override
        public void onSongsRemoved(Single<List<Song>> songsSingle) {

        }

        @Override
        public void onSongRemoved(int position, Song song) {

        }

        @Override
        public void playNext(Single<List<Song>> songsSingle) {

        }

        @Override
        public void showBiographyDialog(Song song) {

        }

        @Override
        public void showDeleteDialog(Single<List<Song>> songsSingle) {

        }

        @Override
        public void showDeleteDialog(Song song) {

        }

        @Override
        public void showTagEditor(Song song) {

        }

        @Override
        public void showToast(String message) {

        }

        @Override
        public void shareSong(Song song) {

        }

        @Override
        public void setRingtone(Song song) {

        }
    }
}