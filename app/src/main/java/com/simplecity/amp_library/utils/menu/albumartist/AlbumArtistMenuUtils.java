package com.simplecity.amp_library.utils.menu.albumartist;

import android.content.Context;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.MediaManager;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.Operators;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.extensions.AlbumArtistExt;
import com.simplecity.amp_library.utils.menu.MenuUtils;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import java.util.List;

public class AlbumArtistMenuUtils {

    public interface Callbacks {

        void onPlaylistItemsInserted();

        void onQueueItemsInserted(String message);

        void playNext(Single<List<Song>> songsSingle);

        void showTagEditor(AlbumArtist albumArtist);

        void showDeleteDialog(AlbumArtist albumArtist);

        void showDeleteDialog(List<AlbumArtist> albumArtists);

        void showDeleteDialog(Single<List<AlbumArtist>> albumArtists);

        void showAlbumArtistInfo(AlbumArtist albumArtist);

        void showArtworkChooser(AlbumArtist albumArtist);

        void showUpgradeDialog();

        void showToast(String message);
    }

    private AlbumArtistMenuUtils() {
        //no instance
    }

    public static Toolbar.OnMenuItemClickListener getAlbumArtistMenuClickListener(Context context, MediaManager mediaManager, Single<List<AlbumArtist>> selectedAlbumArtists, Callbacks callbacks) {
        return item -> {
            switch (item.getItemId()) {
                case MediaManager.NEW_PLAYLIST:
                    MenuUtils.newPlaylist(context, AlbumArtistExt.INSTANCE.getSongsForAlbumArtists(selectedAlbumArtists), callbacks::onPlaylistItemsInserted);
                    return true;
                case MediaManager.PLAYLIST_SELECTED:
                    MenuUtils.addToPlaylist(context, item, AlbumArtistExt.INSTANCE.getSongsForAlbumArtists(selectedAlbumArtists), callbacks::onPlaylistItemsInserted);
                    return true;
                case R.id.playNext:
                    callbacks.playNext(AlbumArtistExt.INSTANCE.getSongsForAlbumArtists(selectedAlbumArtists));
                    return true;
                case R.id.addToQueue:
                    MenuUtils.addToQueue(mediaManager, AlbumArtistExt.INSTANCE.getSongsForAlbumArtists(selectedAlbumArtists), callbacks::onQueueItemsInserted);
                    return true;
                case R.id.delete:
                    callbacks.showDeleteDialog(selectedAlbumArtists);
                    return true;
            }
            return false;
        };
    }

    public static PopupMenu.OnMenuItemClickListener getAlbumArtistClickListener(Context context, MediaManager mediaManager, AlbumArtist albumArtist, Callbacks callbacks) {
        return item -> {
            switch (item.getItemId()) {
                case R.id.play:
                    MenuUtils.play(mediaManager, AlbumArtistExt.INSTANCE.getSongsForAlbumArtist(albumArtist), callbacks::showToast);
                    return true;
                case R.id.playNext:
                    callbacks.playNext(AlbumArtistExt.INSTANCE.getSongsForAlbumArtist(albumArtist));
                    AlbumArtistExt.INSTANCE.getSongsForAlbumArtist(albumArtist)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(songs -> MenuUtils.playNext(mediaManager, songs, callbacks::showToast));
                    return true;
                case R.id.albumShuffle:
                    MenuUtils.play(mediaManager, AlbumArtistExt.INSTANCE.getSongsForAlbumArtist(albumArtist).map(Operators::albumShuffleSongs), callbacks::showToast);
                    return true;
                case MediaManager.NEW_PLAYLIST:
                    MenuUtils.newPlaylist(context, AlbumArtistExt.INSTANCE.getSongsForAlbumArtist(albumArtist), callbacks::onPlaylistItemsInserted);
                    return true;
                case MediaManager.PLAYLIST_SELECTED:
                    MenuUtils.addToPlaylist(context, item, AlbumArtistExt.INSTANCE.getSongsForAlbumArtist(albumArtist), callbacks::onPlaylistItemsInserted);
                    return true;
                case R.id.addToQueue:
                    MenuUtils.addToQueue(mediaManager, AlbumArtistExt.INSTANCE.getSongsForAlbumArtist(albumArtist), callbacks::onQueueItemsInserted);
                    return true;
                case R.id.editTags:
                    if (ShuttleUtils.isUpgraded()) {
                        callbacks.showTagEditor(albumArtist);
                    } else {
                        callbacks.showUpgradeDialog();
                    }
                    return true;
                case R.id.info:
                    callbacks.showAlbumArtistInfo(albumArtist);
                    return true;
                case R.id.artwork:
                    callbacks.showArtworkChooser(albumArtist);
                    return true;
                case R.id.blacklist:
                    MenuUtils.blacklist(AlbumArtistExt.INSTANCE.getSongsForAlbumArtist(albumArtist));
                    return true;
                case R.id.delete:
                    callbacks.showDeleteDialog(albumArtist);
                    return true;
            }
            return false;
        };
    }
}
