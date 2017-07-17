package com.simplecity.amp_library.ui.fragments;

import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.aesthetic.ViewBackgroundAction;
import com.annimon.stream.Collectors;
import com.annimon.stream.IntStream;
import com.annimon.stream.Stream;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.interfaces.Breadcrumb;
import com.simplecity.amp_library.interfaces.BreadcrumbListener;
import com.simplecity.amp_library.interfaces.FileType;
import com.simplecity.amp_library.model.BaseFileObject;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.sql.databases.WhitelistHelper;
import com.simplecity.amp_library.ui.drawer.DrawerLockManager;
import com.simplecity.amp_library.ui.modelviews.BreadcrumbsView;
import com.simplecity.amp_library.ui.modelviews.FolderView;
import com.simplecity.amp_library.ui.views.BreadcrumbItem;
import com.simplecity.amp_library.ui.views.ContextualToolbar;
import com.simplecity.amp_library.utils.ContextualToolbarHelper;
import com.simplecity.amp_library.utils.FileBrowser;
import com.simplecity.amp_library.utils.FileHelper;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.MenuUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.SortManager;
import com.simplecityapps.recycler_adapter.adapter.ViewModelAdapter;
import com.simplecityapps.recycler_adapter.model.ViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.RecyclerListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
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

    private static final String ARG_PAGE_TITLE = "page_title";

    private static final String ARG_CURRENT_DIR = "current_dir";

    private ViewModelAdapter adapter;

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

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    boolean isInActionMode = false;

    String currentDir;

    FileBrowser fileBrowser;

    boolean showCheckboxes;

    List<String> paths = new ArrayList<>();

    boolean showBreadcrumbsInList;

    private CompositeDisposable disposables;

    private ContextualToolbarHelper<BaseFileObject> contextualToolbarHelper;

    private Unbinder unbinder;

    public FolderFragment() {
    }

    public static FolderFragment newInstance(String pageTitle) {
        FolderFragment fragment = new FolderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PAGE_TITLE, pageTitle);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        disposables = new CompositeDisposable();

        adapter = new ViewModelAdapter();

        fileBrowser = new FileBrowser();

        if (savedInstanceState != null) {
            currentDir = savedInstanceState.getString(ARG_CURRENT_DIR);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_folder_browser, container, false);

        unbinder = ButterKnife.bind(this, rootView);

//        if (getParentFragment() == null) {
        showBreadcrumbsInList = false;
        breadcrumb.addBreadcrumbListener(this);
        if (!TextUtils.isEmpty(currentDir)) {
            breadcrumb.changeBreadcrumbPath(currentDir);
        }
//        } else {
//            showBreadcrumbsInList = true;
//            changeBreadcrumbPath();
//            toolbar.setVisibility(View.GONE);
//        }

        toolbar.inflateMenu(R.menu.menu_sort_folders);
        toolbar.setNavigationOnClickListener(v -> getNavigationController().popViewController());
        toolbar.setOnMenuItemClickListener(this);
        updateMenuItems(toolbar.getMenu());

        recyclerView.setRecyclerListener(new RecyclerListener());
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);

        Aesthetic.get()
                .colorPrimary()
                .take(1)
                .subscribe(color -> ViewBackgroundAction.create(appBarLayout)
                        .accept(color), onErrorLogAndRethrow());

        compositeDisposable.add(Aesthetic.get()
                .colorPrimary()
                .compose(distinctToMainThread())
                .subscribe(color -> ViewBackgroundAction.create(appBarLayout)
                        .accept(color), onErrorLogAndRethrow()));

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
                    .subscribe(this::changeDir,
                            error -> LogUtils.logException(TAG, "Error in onResume", error)));
        }

        getNavigationController().addBackPressListener(this);

        DrawerLockManager.getInstance().addDrawerLock(this);

        if (isVisible()) {
            setupContextualToolbar();
        }
    }

    @Override
    public void onPause() {
        disposables.clear();

        getNavigationController().removeBackPressListener(this);

        DrawerLockManager.getInstance().removeDrawerLock(this);

        super.onPause();
    }

    @Override
    public void onDestroyView() {
        compositeDisposable.clear();
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
        inflater.inflate(R.menu.menu_sort_folders, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void updateMenuItems(Menu menu) {

        switch (SettingsManager.getInstance().getFolderBrowserFilesSortOrder()) {
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

        switch (SettingsManager.getInstance().getFolderBrowserFoldersSortOrder()) {
            case SortManager.SortFolders.DEFAULT:
                menu.findItem(R.id.sort_folder_default).setChecked(true);
                break;
            case SortManager.SortFolders.COUNT:
                menu.findItem(R.id.sort_folder_count).setChecked(true);
                break;
        }

        menu.findItem(R.id.show_filenames).setChecked(SettingsManager.getInstance().getFolderBrowserShowFileNames());
        menu.findItem(R.id.files_ascending).setChecked(SettingsManager.getInstance().getFolderBrowserFilesAscending());
        menu.findItem(R.id.folders_ascending).setChecked(SettingsManager.getInstance().getFolderBrowserFoldersAscending());
    }

    @Override
    public void onBreadcrumbItemClick(BreadcrumbItem item) {
        changeDir(new File(item.getItemPath()));
    }

    public void changeDir(File newDir) {

        disposables.add(Observable.fromCallable(() -> {

            final String path = FileHelper.getPath(newDir);

            if (TextUtils.isEmpty(path)) {
                return new ArrayList<BaseFileObject>();
            }

            currentDir = path;

            return fileBrowser.loadDir(new File(path));
        }).map(baseFileObjects -> {
            List<ViewModel> items = Stream.of(baseFileObjects)
                    .map(baseFileObject -> {

                        // Look for an existing FolderView wrapping the BAseFileObject, we'll reuse it if it exists.
                        FolderView folderView = (FolderView) Stream.of(adapter.items)
                                .filter(viewModel -> viewModel instanceof FolderView && (((FolderView) viewModel).baseFileObject.equals(baseFileObject)))
                                .findFirst()
                                .orElse(null);

                        if (folderView == null) {
                            folderView = new FolderView(baseFileObject);
                            folderView.setClickListener(this);
                        }

                        return folderView;
                    })
                    .collect(Collectors.toList());

            if (showBreadcrumbsInList) {
                BreadcrumbsView breadcrumbsView = new BreadcrumbsView(currentDir);
                breadcrumbsView.setBreadcrumbsPath(currentDir);
                breadcrumbsView.setListener(this);
                items.add(0, breadcrumbsView);
            }
            return items;
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(adaptableItems -> {
                    if (adapter != null) {
                        adapter.setItems(adaptableItems);
                    }
                    if (breadcrumb != null) {
                        breadcrumb.changeBreadcrumbPath(currentDir);
                    }
                    if (adapter != null) {
                        changeBreadcrumbPath();
                    }
                }, error -> LogUtils.logException(TAG, "Error changing dir", error)));
    }

    public void reload() {
        if (currentDir != null) {
            changeDir(new File(currentDir));
        }
    }

    @Override
    public boolean consumeBackPress() {
        if (fileBrowser.getCurrentDir() != null && fileBrowser.getRootDir() != null && fileBrowser.getCurrentDir().compareTo(fileBrowser.getRootDir()) != 0) {
            File parent = fileBrowser.getCurrentDir().getParentFile();
            changeDir(parent);
            return true;
        }
        return false;
    }

    @Override
    public void onFileObjectClick(int position, FolderView folderView) {
        if (!isInActionMode) {
            if (folderView.baseFileObject.fileType == FileType.FILE) {
                FileHelper.getSongList(new File(folderView.baseFileObject.path), false, true)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(songs -> {
                            int index = -1;
                            for (int i = 0, songsSize = songs.size(); i < songsSize; i++) {
                                Song song = songs.get(i);
                                if (song.path.contains(folderView.baseFileObject.path)) {
                                    index = i;
                                    break;
                                }
                            }
                            MusicUtils.playAll(songs, index, (String message) -> {
                                if (isAdded() && getContext() != null) {
                                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }, error -> LogUtils.logException(TAG, "Error playing all", error));
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
        MenuUtils.setupFolderMenu(getContext(), menu, folderView.baseFileObject);
        menu.setOnMenuItemClickListener(MenuUtils.getFolderMenuClickListener(
                getContext(),
                folderView.baseFileObject, taggerDialog -> taggerDialog.show(getFragmentManager()),
                () -> IntStream.range(0, adapter.getItemCount())
                        .filter(i -> adapter.items.get(i) == folderView)
                        .findFirst()
                        .ifPresent(i -> adapter.notifyItemChanged(i)),
                () -> IntStream.range(0, adapter.getItemCount())
                        .filter(i -> adapter.items.get(i) == folderView)
                        .findFirst()
                        .ifPresent(i -> adapter.notifyItemRemoved(i))));
        menu.show();
    }

    @Override
    public void onFileObjectCheckboxClick(View v, FolderView folderView) {
        Log.i(TAG, "Clicked.. Selected: " + folderView.isSelected());
    }

    public void changeBreadcrumbPath() {

        List<ViewModel> breadcrumbViews = Stream.of(adapter.items)
                .filter(adaptableItem -> adaptableItem instanceof BreadcrumbsView)
                .toList();

        for (ViewModel viewModel : breadcrumbViews) {
            ((BreadcrumbsView) viewModel).setBreadcrumbsPath(currentDir);
            adapter.notifyItemChanged(adapter.items.indexOf(viewModel));
        }
    }

    /**
     * Retrieves all folders from the whitelist, and adds them to our 'pathlist'
     * so the appropriate checkboxes can be checked.
     */
    public void updateWhitelist() {
        WhitelistHelper.getWhitelistFolders()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(whitelistFolders -> {
                    paths.clear();
                    paths.addAll(Stream.of(whitelistFolders)
                            .map(whitelistFolder -> whitelistFolder.folder)
                            .toList());

                    if (showCheckboxes) {
                        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
                    }
                }, error -> LogUtils.logException(TAG, "Error updating whitelist", error));
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

            contextualToolbarHelper = new ContextualToolbarHelper<>(contextualToolbar, new ContextualToolbarHelper.Callback() {
                @Override
                public void notifyItemChanged(int position) {
                    adapter.notifyItemChanged(position, 0);
                }

                @Override
                public void notifyDatasetChanged() {
                    adapter.notifyItemRangeChanged(0, adapter.items.size(), 0);
                }
            });

            contextualToolbar.setOnMenuItemClickListener(menuItem -> {
                switch (menuItem.getItemId()) {
                    case R.id.menu_save:
//                            WhitelistHelper.deleteAllFolders();
//                            WhitelistHelper.addToWhitelist(paths);
//                            showCheckboxes(false);
//                            adapter.notifyDataSetChanged();
//                            mode.finish();
                        return true;
                }
                return false;
            });
        }
    }

    @Override
    protected String screenName() {
        return TAG;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.sort_files_default:
                SettingsManager.getInstance().setFolderBrowserFilesSortOrder(SortManager.SortFiles.DEFAULT);
                reload();
                updateMenuItems(toolbar.getMenu());
                return true;
            case R.id.sort_files_filename:
                SettingsManager.getInstance().setFolderBrowserFilesSortOrder(SortManager.SortFiles.FILE_NAME);
                reload();
                updateMenuItems(toolbar.getMenu());
                return true;
            case R.id.sort_files_size:
                SettingsManager.getInstance().setFolderBrowserFilesSortOrder(SortManager.SortFiles.SIZE);
                reload();
                updateMenuItems(toolbar.getMenu());
                return true;
            case R.id.sort_files_artist_name:
                SettingsManager.getInstance().setFolderBrowserFilesSortOrder(SortManager.SortFiles.ARTIST_NAME);
                reload();
                updateMenuItems(toolbar.getMenu());
                return true;
            case R.id.sort_files_album_name:
                SettingsManager.getInstance().setFolderBrowserFilesSortOrder(SortManager.SortFiles.ALBUM_NAME);
                reload();
                updateMenuItems(toolbar.getMenu());
                return true;
            case R.id.sort_files_track_name:
                SettingsManager.getInstance().setFolderBrowserFilesSortOrder(SortManager.SortFiles.TRACK_NAME);
                reload();
                updateMenuItems(toolbar.getMenu());
                return true;
            case R.id.files_ascending:
                SettingsManager.getInstance().setFolderBrowserFilesAscending(!menuItem.isChecked());
                reload();
                updateMenuItems(toolbar.getMenu());
                return true;
            case R.id.sort_folder_count:
                SettingsManager.getInstance().setFolderBrowserFoldersSortOrder(SortManager.SortFolders.COUNT);
                reload();
                updateMenuItems(toolbar.getMenu());
                return true;
            case R.id.sort_folder_default:
                SettingsManager.getInstance().setFolderBrowserFoldersSortOrder(SortManager.SortFolders.DEFAULT);
                reload();
                updateMenuItems(toolbar.getMenu());
                return true;
            case R.id.folders_ascending:
                SettingsManager.getInstance().setFolderBrowserFoldersAscending(!menuItem.isChecked());
                reload();
                getActivity().supportInvalidateOptionsMenu();
                return true;
            case R.id.whitelist:
                contextualToolbarHelper.start();

                Stream.of(adapter.items)
                        .filter(viewModel -> viewModel instanceof FolderView)
                        .forEach(viewModel -> ((FolderView) viewModel).setShowCheckboxes(true));

                adapter.notifyItemRangeChanged(0, adapter.getItemCount(), 0);

                WhitelistHelper.getWhitelistFolders()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(whitelistFolders -> {
                                    Stream.of(adapter.items)
                                            .filter(viewModel -> viewModel instanceof FolderView)
                                            .filter(viewModel -> whitelistFolders.contains(((FolderView) viewModel).baseFileObject.path))
                                            .forEach(viewModel -> ((FolderView) viewModel).setSelected(true));

                                    adapter.notifyItemRangeChanged(0, adapter.getItemCount(), 0);
                                }
                        );
                return true;
            case R.id.show_filenames:
                SettingsManager.getInstance().setFolderBrowserShowFileNames(!menuItem.isChecked());
                adapter.notifyItemRangeChanged(0, adapter.getItemCount(), 0);
                updateMenuItems(toolbar.getMenu());
                return true;

        }
        return false;
    }
}