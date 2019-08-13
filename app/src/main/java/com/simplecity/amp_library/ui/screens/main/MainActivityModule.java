package com.simplecity.amp_library.ui.screens.main;

import android.support.v7.app.AppCompatActivity;
import com.simplecity.amp_library.billing.BillingManager;
import com.simplecity.amp_library.di.app.activity.ActivityModule;
import com.simplecity.amp_library.di.app.activity.ActivityScope;
import com.simplecity.amp_library.di.app.activity.fragment.DialogFragmentModule;
import com.simplecity.amp_library.di.app.activity.fragment.FragmentModule;
import com.simplecity.amp_library.di.app.activity.fragment.FragmentScope;
import com.simplecity.amp_library.saf.SafManager;
import com.simplecity.amp_library.ui.common.EqualizerModule;
import com.simplecity.amp_library.ui.dialog.ChangelogDialog;
import com.simplecity.amp_library.ui.dialog.DeleteDialog;
import com.simplecity.amp_library.ui.dialog.InclExclDialog;
import com.simplecity.amp_library.ui.dialog.UpgradeDialog;
import com.simplecity.amp_library.ui.dialog.UpgradeNagDialog;
import com.simplecity.amp_library.ui.dialog.WeekSelectorDialog;
import com.simplecity.amp_library.ui.screens.album.detail.AlbumDetailFragment;
import com.simplecity.amp_library.ui.screens.album.detail.AlbumsDetailFragmentModule;
import com.simplecity.amp_library.ui.screens.album.list.AlbumListFragment;
import com.simplecity.amp_library.ui.screens.album.list.AlbumListFragmentModule;
import com.simplecity.amp_library.ui.screens.artist.detail.ArtistDetailFragment;
import com.simplecity.amp_library.ui.screens.artist.detail.ArtistsDetailFragmentModule;
import com.simplecity.amp_library.ui.screens.artist.list.AlbumArtistListFragment;
import com.simplecity.amp_library.ui.screens.artist.list.AlbumArtistListFragmentModule;
import com.simplecity.amp_library.ui.screens.drawer.DrawerFragment;
import com.simplecity.amp_library.ui.screens.drawer.DrawerFragmentModule;
import com.simplecity.amp_library.ui.screens.equalizer.EqualizerFragment;
import com.simplecity.amp_library.ui.screens.folders.FolderFragment;
import com.simplecity.amp_library.ui.screens.folders.FolderFragmentModule;
import com.simplecity.amp_library.ui.screens.genre.detail.GenreDetailFragment;
import com.simplecity.amp_library.ui.screens.genre.detail.GenreDetailFragmentModule;
import com.simplecity.amp_library.ui.screens.genre.list.GenreListFragment;
import com.simplecity.amp_library.ui.screens.genre.list.GenreListFragmentModule;
import com.simplecity.amp_library.ui.screens.lyrics.LyricsDialog;
import com.simplecity.amp_library.ui.screens.miniplayer.MiniPlayerFragment;
import com.simplecity.amp_library.ui.screens.nowplaying.PlayerFragment;
import com.simplecity.amp_library.ui.screens.nowplaying.PlayerFragmentModule;
import com.simplecity.amp_library.ui.screens.playlist.detail.PlaylistDetailFragment;
import com.simplecity.amp_library.ui.screens.playlist.detail.PlaylistDetailFragmentModule;
import com.simplecity.amp_library.ui.screens.playlist.dialog.CreatePlaylistDialog;
import com.simplecity.amp_library.ui.screens.playlist.dialog.DeletePlaylistConfirmationDialog;
import com.simplecity.amp_library.ui.screens.playlist.dialog.M3uPlaylistDialog;
import com.simplecity.amp_library.ui.screens.playlist.list.PlaylistListFragment;
import com.simplecity.amp_library.ui.screens.playlist.list.PlaylistListFragmentModule;
import com.simplecity.amp_library.ui.screens.queue.QueueFragment;
import com.simplecity.amp_library.ui.screens.queue.QueueFragmentModule;
import com.simplecity.amp_library.ui.screens.queue.pager.QueuePagerFragment;
import com.simplecity.amp_library.ui.screens.queue.pager.QueuePagerFragmentModule;
import com.simplecity.amp_library.ui.screens.search.SearchFragment;
import com.simplecity.amp_library.ui.screens.search.SearchFragmentModule;
import com.simplecity.amp_library.ui.screens.songs.list.SongListFragment;
import com.simplecity.amp_library.ui.screens.songs.list.SongsListFragmentModule;
import com.simplecity.amp_library.ui.screens.suggested.SuggestedFragment;
import com.simplecity.amp_library.ui.screens.suggested.SuggestedFragmentModule;
import com.simplecity.amp_library.ui.screens.tagger.TaggerDialog;
import com.simplecity.amp_library.ui.settings.SettingsFragmentModule;
import com.simplecity.amp_library.ui.settings.SettingsParentFragment;
import com.simplecity.amp_library.ui.settings.SettingsParentFragmentModule;
import com.simplecity.amp_library.ui.settings.TabChooserDialog;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;

@Module(includes = ActivityModule.class)
public abstract class MainActivityModule {

    @Binds
    @ActivityScope
    abstract AppCompatActivity appCompatActivity(MainActivity mainActivity);

    @Provides
    static BillingManager.BillingUpdatesListener provideBillingUpdatesListener(MainActivity mainActivity) {
        return mainActivity;
    }

    @FragmentScope
    @ContributesAndroidInjector(modules = LibraryFragmentModule.class)
    abstract LibraryController libraryControllerInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = DrawerFragmentModule.class)
    abstract DrawerFragment drawerFragmentInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = MainControllerModule.class)
    abstract MainController mainControllerInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = AlbumArtistListFragmentModule.class)
    abstract AlbumArtistListFragment artistsFragmentInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = AlbumListFragmentModule.class)
    abstract AlbumListFragment albumsFragmentInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = SongsListFragmentModule.class)
    abstract SongListFragment songsFragmentInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = PlayerFragmentModule.class)
    abstract PlayerFragment playerFragmentInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = QueueFragmentModule.class)
    abstract QueueFragment queueFragmentInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = QueuePagerFragmentModule.class)
    abstract QueuePagerFragment queuePagerFragmentInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = FragmentModule.class)
    abstract MiniPlayerFragment miniPlayerFragmentInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = SuggestedFragmentModule.class)
    abstract SuggestedFragment suggestedFragmentInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = FolderFragmentModule.class)
    abstract FolderFragment folderFragmentInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = AlbumsDetailFragmentModule.class)
    abstract AlbumDetailFragment albumDetailFragmentInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = ArtistsDetailFragmentModule.class)
    abstract ArtistDetailFragment artistDetailFragmentInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = PlaylistListFragmentModule.class)
    abstract PlaylistListFragment playlistFragmentInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = PlaylistDetailFragmentModule.class)
    abstract PlaylistDetailFragment playlistDetailFragmentInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = GenreListFragmentModule.class)
    abstract GenreListFragment genresFragmentInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = GenreDetailFragmentModule.class)
    abstract GenreDetailFragment genreDetailFragmentInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = SettingsParentFragmentModule.class)
    abstract SettingsParentFragment settingsParentFragmentInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = SettingsFragmentModule.class)
    abstract SettingsParentFragment.SettingsFragment settingsFragmentInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = SearchFragmentModule.class)
    abstract SearchFragment searchFragmentInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = EqualizerModule.class)
    abstract EqualizerFragment equalizerFragmentInjector();

    // Dialog fragments

    @FragmentScope
    @ContributesAndroidInjector(modules = DialogFragmentModule.class)
    abstract CreatePlaylistDialog createPlaylistdialogInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = DialogFragmentModule.class)
    abstract DeletePlaylistConfirmationDialog deletePlaylistConfirmationDialogInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = DialogFragmentModule.class)
    abstract M3uPlaylistDialog m3uPlaylistDialogInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = DialogFragmentModule.class)
    abstract TaggerDialog taggerDialogInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = DialogFragmentModule.class)
    abstract DeleteDialog deleteDialogInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = DialogFragmentModule.class)
    abstract InclExclDialog inclExclDialogInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = DialogFragmentModule.class)
    abstract UpgradeDialog upgradeDialogInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = DialogFragmentModule.class)
    abstract UpgradeNagDialog UpgradeNagDialogInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = DialogFragmentModule.class)
    abstract WeekSelectorDialog weekSelectorDialogInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = DialogFragmentModule.class)
    abstract ChangelogDialog changelogDialogInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = DialogFragmentModule.class)
    abstract TabChooserDialog tabChooseerDialogInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = DialogFragmentModule.class)
    abstract LyricsDialog lyricsDialogInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = DialogFragmentModule.class)
    abstract SafManager.SafDialog safDialogInjector();
}