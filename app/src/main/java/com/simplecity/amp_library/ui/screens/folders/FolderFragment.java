package com.simplecity.amp_library.ui.screens.folders;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.aesthetic.ViewBackgroundAction;
import com.annimon.stream.Collectors;
import com.annimon.stream.IntStream;
import com.annimon.stream.Stream;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.data.Repository;
import com.simplecity.amp_library.interfaces.Breadcrumb;
import com.simplecity.amp_library.interfaces.BreadcrumbListener;
import com.simplecity.amp_library.interfaces.FileType;
import com.simplecity.amp_library.model.BaseFileObject;
import com.simplecity.amp_library.model.InclExclItem;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.ui.common.BaseFragment;
import com.simplecity.amp_library.ui.dialog.SongInfoDialog;
import com.simplecity.amp_library.ui.modelviews.BreadcrumbsView;
import com.simplecity.amp_library.ui.modelviews.FolderView;
import com.simplecity.amp_library.ui.modelviews.SelectableViewModel;
import com.simplecity.amp_library.ui.screens.drawer.DrawerLockManager;
import com.simplecity.amp_library.ui.screens.tagger.TaggerDialog;
import com.simplecity.amp_library.ui.views.BreadcrumbItem;
import com.simplecity.amp_library.ui.views.ContextualToolbar;
import com.simplecity.amp_library.ui.views.ThemedStatusBarView;
import com.simplecity.amp_library.utils.AnalyticsManager;
import com.simplecity.amp_library.utils.ContextualToolbarHelper;
import com.simplecity.amp_library.utils.FileBrowser;
import com.simplecity.amp_library.utils.FileHelper;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.RingtoneManager;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.extensions.SongExtKt;
import com.simplecity.amp_library.utils.menu.MenuUtils;
import com.simplecity.amp_library.utils.menu.folder.FolderMenuUtils;
import com.simplecity.amp_library.utils.playlists.PlaylistManager;
import com.simplecity.amp_library.utils.playlists.PlaylistMenuHelper;
import com.simplecity.amp_library.utils.sorting.SortManager;
import com.simplecityapps.recycler_adapter.adapter.ViewModelAdapter;
import com.simplecityapps.recycler_adapter.model.ViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.RecyclerListener;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function3;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import test.com.androidnavigation.fragment.BackPressListener;

import static com.afollestad.aesthetic.Rx.distinctToMainThread;
import static com.afollestad.aesthetic.Rx.onErrorLogAndRethrow;

public class FolderFragment extends BaseFragment implements
        BreadcrumbListener,
        BackPressListener,
        FolderView.ClickListener,
        Toolbar.OnMenuItemClickListener,
        DrawerLockManager.DrawerLock {

    private static final String TAG = "FolderFragment";

    private static final String ARG_CURRENT_DIR = "current_dir";

    private static final String ARG_DISPLAYED_IN_TABS = "displayed_in_tabs";

    private static final String ARG_TITLE = "title";

    ViewModelAdapter adapter;

    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.breadcrumb_view)
    Breadcrumb breadcrumb;

    @BindView(R.id.contextualToolbar)
    ContextualToolbar contextualToolbar;

    @BindView(R.id.app_bar)
    AppBarLayout appBarLayout;

    @BindView(R.id.statusBarView)
    ThemedStatusBarView statusBarView;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    String currentDir;

    boolean displayedInTabs = false;

    FileBrowser fileBrowser;

    boolean showBreadcrumbsInList;

    private boolean isShowingWhitelist;
    private boolean isShowingBlacklist;

    private CompositeDisposable disposables;

    private ContextualToolbarHelper<BaseFileObject> contextualToolbarHelper;

    @Nullable
    private BreadcrumbsView breadcrumbsView;

    private Unbinder unbinder;

    @Nullable
    private Disposable setItemsDisposable;

    @Inject
    Repository.BlacklistRepository blacklistRepository;

    @Inject
    Repository.WhitelistRepository whitelistRepository;

    @Inject
    Repository.SongsRepository songsRepository;

    @Inject
    Repository.PlaylistsRepository playlistsRepository;

    @Inject
    SettingsManager settingsManager;

    @Inject
    AnalyticsManager analyticsManager;

    @Inject
    RingtoneManager ringtoneManager;

    @Inject
    PlaylistManager playlistManager;

    @Inject
    PlaylistMenuHelper playlistMenuHelper;

    public static FolderFragment newInstance(String title, boolean isDisplayedInTabs) {
        FolderFragment fragment = new FolderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putBoolean(ARG_DISPLAYED_IN_TABS, isDisplayedInTabs);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        disposables = new CompositeDisposable();

        adapter = new ViewModelAdapter();

        fileBrowser = new FileBrowser(settingsManager);

        if (savedInstanceState != null) {
            currentDir = savedInstanceState.getString(ARG_CURRENT_DIR);
        }

        displayedInTabs = getArguments().getBoolean(ARG_DISPLAYED_IN_TABS);

        if (displayedInTabs) {
            setHasOptionsMenu(true);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_folder_browser, container, false);

        unbinder = ButterKnife.bind(this, rootView);

        if (displayedInTabs) {
            breadcrumbsView = new BreadcrumbsView(currentDir);
            showBreadcrumbsInList = true;
            changeBreadcrumbPath();
            appBarLayout.setVisibility(View.GONE);
            statusBarView.setVisibility(View.GONE);
        } else {
            showBreadcrumbsInList = false;
            breadcrumb.addBreadcrumbListener(this);
            if (!TextUtils.isEmpty(currentDir)) {
                breadcrumb.changeBreadcrumbPath(currentDir);
            }
        }

        if (!displayedInTabs) {
            toolbar.inflateMenu(R.menu.menu_folders);
            toolbar.setNavigationOnClickListener(v -> getNavigationController().popViewController());
            toolbar.setOnMenuItemClickListener(this);
            updateMenuItems(toolbar.getMenu());
        }

        recyclerView.setRecyclerListener(new RecyclerListener());
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);

        compositeDisposable.add(Aesthetic.get(getContext())
                .colorPrimary()
                .compose(distinctToMainThread())
                .subscribe(color -> ViewBackgroundAction.create(appBarLayout).accept(color), onErrorLogAndRethrow()));

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (currentDir == null) {
            disposables.add(Observable.fromCallable(() -> {
                        if (!TextUtils.isEmpty(currentDir)) {
                            return new File(currentDir);
                        } else {
                            return fileBrowser.getInitialDir();
                        }
                    }).subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    this::changeDir,
                                    error -> LogUtils.logException(TAG, "Error in onResume", error))
            );
        }

        getNavigationController().addBackPressListener(this);

        if (!displayedInTabs) {
            DrawerLockManager.getInstance().addDrawerLock(this);
        }

        if (isVisible()) {
            setupContextualToolbar();
        }
    }

    @Override
    public void onPause() {
        disposables.clear();

        getNavigationController().removeBackPressListener(this);

        if (!displayedInTabs) {
            DrawerLockManager.getInstance().removeDrawerLock(this);
        }

        super.onPause();
    }

    @Override
    public void onDestroyView() {
        compositeDisposable.clear();
        if (setItemsDisposable != null) {
            setItemsDisposable.dispose();
        }
        unbinder.unbind();
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(ARG_CURRENT_DIR, currentDir);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_folders, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        updateMenuItems(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return this.onMenuItemClick(item);
    }

    private void updateMenuItems() {
        if (displayedInTabs) {
            getActivity().invalidateOptionsMenu();
        } else {
            updateMenuItems(toolbar.getMenu());
        }
    }

    private void updateMenuItems(Menu menu) {

        switch (settingsManager.getFolderBrowserFilesSortOrder()) {
            case SortManager.SortFiles.DEFAULT:
                menu.findItem(R.id.sort_files_default).setChecked(true);
                break;
            case SortManager.SortFiles.FILE_NAME:
                menu.findItem(R.id.sort_files_filename).setChecked(true);
                break;
            case SortManager.SortFiles.SIZE:
                menu.findItem(R.id.sort_files_size).setChecked(true);
                break;
            case SortManager.SortFiles.ARTIST_NAME:
                menu.findItem(R.id.sort_files_artist_name).setChecked(true);
                break;
            case SortManager.SortFiles.ALBUM_NAME:
                menu.findItem(R.id.sort_files_album_name).setChecked(true);
                break;
            case SortManager.SortFiles.TRACK_NAME:
                menu.findItem(R.id.sort_files_track_name).setChecked(true);
                break;
        }

        switch (settingsManager.getFolderBrowserFoldersSortOrder()) {
            case SortManager.SortFolders.DEFAULT:
                menu.findItem(R.id.sort_folder_default).setChecked(true);
                break;
            case SortManager.SortFolders.COUNT:
                menu.findItem(R.id.sort_folder_count).setChecked(true);
                break;
        }

        menu.findItem(R.id.folder_home_dir).setIcon(fileBrowser.getHomeDirIcon());
        menu.findItem(R.id.folder_home_dir).setTitle(fileBrowser.getHomeDirTitle());
        menu.findItem(R.id.show_filenames).setChecked(settingsManager.getFolderBrowserShowFileNames());
        menu.findItem(R.id.files_ascending).setChecked(settingsManager.getFolderBrowserFilesAscending());
        menu.findItem(R.id.folders_ascending).setChecked(settingsManager.getFolderBrowserFoldersAscending());
    }

    @Override
    public void onBreadcrumbItemClick(BreadcrumbItem item) {
        changeDir(new File(item.getItemPath()));
    }

    @SuppressLint("CheckResult")
    public void changeDir(File newDir) {
        disposables.add(Single.zip(
                whitelistRepository.getWhitelistItems(songsRepository).first(Collections.emptyList()),
                blacklistRepository.getBlacklistItems(songsRepository).first(Collections.emptyList()),
                Single.fromCallable(() -> {
                    final String path = FileHelper.getPath(newDir);
                    if (TextUtils.isEmpty(path)) {
                        return new ArrayList<>();
                    }
                    currentDir = path;
                    return fileBrowser.loadDir(new File(path));
                }),
                (Function3<List<InclExclItem>, List<InclExclItem>, List<BaseFileObject>, List<ViewModel>>) (whitelist, blacklist, baseFileObjects) -> {
                    List<ViewModel> items = Stream.of(baseFileObjects)
                            .map(baseFileObject -> {

                                // Look for an existing FolderView wrapping the BaseFileObject, we'll reuse it if it exists.
                                FolderView folderView = (FolderView) Stream.of(adapter.items)
                                        .filter(viewModel -> viewModel instanceof FolderView && (((FolderView) viewModel).baseFileObject.equals(baseFileObject)))
                                        .findFirst()
                                        .orElse(null);

                                if (folderView == null) {
                                    folderView = new FolderView(baseFileObject, whitelistRepository, blacklistRepository, settingsManager,
                                            Stream.of(whitelist).anyMatch(inclExclItem -> inclExclItem.path.equals(baseFileObject.path)),
                                            Stream.of(blacklist).anyMatch(inclExclItem -> inclExclItem.path.equals(baseFileObject.path)));
                                    folderView.setShowWhitelist(isShowingWhitelist);
                                    folderView.setShowBlacklist(isShowingBlacklist);
                                    folderView.setClickListener(FolderFragment.this);
                                }

                                return folderView;
                            })
                            .collect(Collectors.toList());

                    if (showBreadcrumbsInList && breadcrumbsView != null) {
                        breadcrumbsView.setBreadcrumbsPath(currentDir);
                        breadcrumbsView.setListener(FolderFragment.this);
                        items.add(0, breadcrumbsView);
                    }
                    return items;
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        adaptableItems -> {
                            if (adapter != null) {
                                analyticsManager.dropBreadcrumb(TAG, "setItems()");
                                setItemsDisposable = adapter.setItems(adaptableItems);
                            }
                            if (breadcrumb != null) {
                                breadcrumb.changeBreadcrumbPath(currentDir);
                            }
                            if (adapter != null) {
                                changeBreadcrumbPath();
                            }
                            updateMenuItems();
                        },
                        error -> LogUtils.logException(TAG, "Error changing dir", error))
        );
    }

    public void reload() {
        if (currentDir != null) {
            changeDir(new File(currentDir));
        }
    }

    @Override
    public boolean consumeBackPress() {
        if (getUserVisibleHint()) {
            final File currDir = fileBrowser.getCurrentDir();
            final File homeDir = fileBrowser.getHomeDir();
            if (currDir != null && homeDir != null && currDir.compareTo(homeDir) != 0) {
                changeDir(currDir.getParentFile());
                return true;
            }
        }
        return false;
    }

    @SuppressLint("CheckResult")
    @Override
    public void onFileObjectClick(int position, FolderView folderView) {
        if (contextualToolbarHelper != null && !contextualToolbarHelper.handleClick(folderView, folderView.baseFileObject)) {
            if (folderView.baseFileObject.fileType == FileType.FILE) {
                FileHelper.getSongList(songsRepository, new File(folderView.baseFileObject.path), false, true)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                songs -> {
                                    int index = -1;
                                    for (int i = 0, songsSize = songs.size(); i < songsSize; i++) {
                                        Song song = songs.get(i);
                                        if (song.path.contains(folderView.baseFileObject.path)) {
                                            index = i;
                                            break;
                                        }
                                    }
                                    mediaManager.playAll(songs, index, true, () -> {
                                        if (isAdded() && getContext() != null) {
                                            // Todo: Show playback failed toast
                                        }
                                        return Unit.INSTANCE;
                                    });
                                },
                                error -> LogUtils.logException(TAG, "Error playing all", error));
            } else {
                changeDir(new File(folderView.baseFileObject.path));
            }
        } else if (folderView.baseFileObject.fileType != FileType.FILE) {
            changeDir(new File(folderView.baseFileObject.path));
        }
    }

    @Override
    public void onFileObjectOverflowClick(View v, FolderView folderView) {
        PopupMenu menu = new PopupMenu(getActivity(), v);
        FolderMenuUtils.INSTANCE.setupFolderMenu(menu, folderView.baseFileObject, playlistMenuHelper);
        menu.setOnMenuItemClickListener(FolderMenuUtils.INSTANCE.getFolderMenuClickListener(this, mediaManager, songsRepository, folderView, playlistManager, callbacks));
        menu.show();
    }

    @Override
    public void onFileObjectCheckboxClick(CheckBox checkBox, FolderView folderView) {

    }

    public void changeBreadcrumbPath() {
        if (breadcrumbsView != null) {
            breadcrumbsView.setBreadcrumbsPath(currentDir);
            adapter.notifyItemChanged(adapter.items.indexOf(breadcrumbsView), 0);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            setupContextualToolbar();
        } else {
            if (contextualToolbarHelper != null) {
                contextualToolbarHelper.finish();
            }
        }
    }

    private void setupContextualToolbar() {
        if (contextualToolbar != null) {

            contextualToolbar.getMenu().clear();
            contextualToolbar.inflateMenu(R.menu.context_menu_folders);

            contextualToolbarHelper = new ContextualToolbarHelper<>(getContext(), contextualToolbar, new ContextualToolbarHelper.Callback() {
                @Override
                public void notifyItemChanged(SelectableViewModel viewModel) {
                    int index = adapter.items.indexOf(viewModel);
                    if (index >= 0) {
                        adapter.notifyItemChanged(index, 0);
                    }
                }

                @Override
                public void notifyDatasetChanged() {
                    adapter.notifyItemRangeChanged(0, adapter.items.size(), 0);
                }
            });

            contextualToolbarHelper.setCanChangeTitle(false);

            contextualToolbar.setOnMenuItemClickListener(menuItem -> {
                switch (menuItem.getItemId()) {
                    case R.id.done:
                        contextualToolbarHelper.finish();
                        showWhitelist(false);
                        showBlacklist(false);
                        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
                        return true;
                }
                return false;
            });
        }
    }

    private void showWhitelist(boolean show) {
        isShowingWhitelist = show;
        if (isShowingWhitelist) {
            isShowingBlacklist = false;
        }
        Stream.of(adapter.items)
                .filter(viewModel -> viewModel instanceof FolderView)
                .forEach(viewModel -> ((FolderView) viewModel).setShowWhitelist(show));
        adapter.notifyItemRangeChanged(0, adapter.getItemCount(), 0);
        contextualToolbar.setTitle(R.string.whitelist_title);
    }

    private void showBlacklist(boolean show) {
        isShowingBlacklist = show;
        if (isShowingBlacklist) {
            isShowingWhitelist = false;
        }
        Stream.of(adapter.items)
                .filter(viewModel -> viewModel instanceof FolderView)
                .forEach(viewModel -> ((FolderView) viewModel).setShowBlacklist(show));
        adapter.notifyItemRangeChanged(0, adapter.getItemCount(), 0);
        contextualToolbar.setTitle(R.string.blacklist_title);
    }

    @Override
    protected String screenName() {
        return TAG;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.folder_home_dir:
                if (fileBrowser.atHomeDirectory()) {
                    fileBrowser.clearHomeDir();
                    updateMenuItems();
                } else if (fileBrowser.hasHomeDir()) {
                    changeDir(fileBrowser.getHomeDir());
                } else {
                    fileBrowser.setHomeDir();
                    updateMenuItems();
                }
                return true;
            case R.id.sort_files_default:
                settingsManager.setFolderBrowserFilesSortOrder(SortManager.SortFiles.DEFAULT);
                reload();
                updateMenuItems();
                return true;
            case R.id.sort_files_filename:
                settingsManager.setFolderBrowserFilesSortOrder(SortManager.SortFiles.FILE_NAME);
                reload();
                updateMenuItems();
                return true;
            case R.id.sort_files_size:
                settingsManager.setFolderBrowserFilesSortOrder(SortManager.SortFiles.SIZE);
                reload();
                updateMenuItems();
                return true;
            case R.id.sort_files_artist_name:
                settingsManager.setFolderBrowserFilesSortOrder(SortManager.SortFiles.ARTIST_NAME);
                reload();
                updateMenuItems();
                return true;
            case R.id.sort_files_album_name:
                settingsManager.setFolderBrowserFilesSortOrder(SortManager.SortFiles.ALBUM_NAME);
                reload();
                updateMenuItems();
                return true;
            case R.id.sort_files_track_name:
                settingsManager.setFolderBrowserFilesSortOrder(SortManager.SortFiles.TRACK_NAME);
                reload();
                updateMenuItems();
                return true;
            case R.id.files_ascending:
                settingsManager.setFolderBrowserFilesAscending(!menuItem.isChecked());
                reload();
                updateMenuItems();
                return true;
            case R.id.sort_folder_count:
                settingsManager.setFolderBrowserFoldersSortOrder(SortManager.SortFolders.COUNT);
                reload();
                updateMenuItems();
                return true;
            case R.id.sort_folder_default:
                settingsManager.setFolderBrowserFoldersSortOrder(SortManager.SortFolders.DEFAULT);
                reload();
                updateMenuItems();
                return true;
            case R.id.folders_ascending:
                settingsManager.setFolderBrowserFoldersAscending(!menuItem.isChecked());
                reload();
                getActivity().invalidateOptionsMenu();
                return true;
            case R.id.whitelist:
                contextualToolbarHelper.start();
                showWhitelist(true);
                return true;
            case R.id.blacklist:
                contextualToolbarHelper.start();
                showBlacklist(true);
                return true;
            case R.id.show_filenames:
                settingsManager.setFolderBrowserShowFileNames(!menuItem.isChecked());
                adapter.notifyItemRangeChanged(0, adapter.getItemCount(), 0);
                updateMenuItems();
                return true;
        }
        return false;
    }

    FolderMenuUtils.Callbacks callbacks = new FolderMenuUtils.Callbacks() {

        @Override
        public void onSongsAddedToQueue(int numSongs) {
            Toast.makeText(getContext(), getContext().getResources().getQuantityString(R.plurals.NNNtrackstoqueue, numSongs, numSongs), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPlaybackFailed() {
            // Todo: Improve error message
            Toast.makeText(getContext(), R.string.emptyplaylist, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void showToast(String message) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }

        @Override
        public void showToast(int messageResId) {
            Toast.makeText(getContext(), messageResId, Toast.LENGTH_LONG).show();
        }

        @Override
        public void shareSong(Song song) {
            SongExtKt.share(song, getContext());
        }

        @Override
        public void setRingtone(Song song) {
            if (RingtoneManager.Companion.requiresDialog(getContext())) {
                RingtoneManager.Companion.getDialog(getContext()).show();
            } else {
                ringtoneManager.setRingtone(song, () -> {
                    Toast.makeText(getContext(), R.string.ringtone_set_new, Toast.LENGTH_SHORT).show();
                    return Unit.INSTANCE;
                });
            }
        }

        @Override
        public void showSongInfo(Song song) {
            SongInfoDialog.Companion.newInstance(song).show(getChildFragmentManager());
        }

        @Override
        public void onPlaylistItemsInserted() {

        }

        @Override
        public void showTagEditor(Song song) {
            TaggerDialog.newInstance(song).show(getChildFragmentManager());
        }

        @Override
        public void onFileNameChanged(FolderView folderView) {
            IntStream.range(0, adapter.getItemCount())
                    .filter(i -> adapter.items.get(i) == folderView)
                    .findFirst()
                    .ifPresent(i -> adapter.notifyItemChanged(i));
        }

        @Override
        public void onFileDeleted(FolderView folderView) {
            IntStream.range(0, adapter.getItemCount())
                    .filter(i -> adapter.items.get(i) == folderView)
                    .findFirst()
                    .ifPresent(i -> adapter.notifyItemRemoved(i));
        }

        @Override
        public void playNext(Single<List<Song>> songsSingle) {
            mediaManager.playNext(songsSingle, message -> {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                return Unit.INSTANCE;
            });
        }

        @Override
        public void blacklist(@NotNull Song song) {
            MenuUtils.blacklist(blacklistRepository, song);
        }

        @Override
        public void blacklist(@NotNull Single<List<Song>> songsSingle) {
            MenuUtils.blacklist(blacklistRepository, songsSingle);
        }

        @Override
        public void whitelist(@NotNull Song song) {
            MenuUtils.whitelist(whitelistRepository, song);
        }

        @Override
        public void whitelist(@NotNull Single<List<Song>> songsSingle) {
            MenuUtils.whitelist(whitelistRepository, songsSingle);
        }
    };
}
