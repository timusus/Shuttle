package com.simplecity.amp_library.ui.fragments;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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
import com.simplecity.amp_library.ui.modelviews.BreadcrumbsView;
import com.simplecity.amp_library.ui.modelviews.FolderView;
import com.simplecity.amp_library.ui.views.BreadcrumbItem;
import com.simplecity.amp_library.utils.FileBrowser;
import com.simplecity.amp_library.utils.FileHelper;
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
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import test.com.androidnavigation.fragment.BackPressListener;

public class FolderFragment extends BaseFragment implements
        BreadcrumbListener,
        BackPressListener,
        FolderView.ClickListener {

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

    boolean isInActionMode = false;

    String currentDir;

    FileBrowser fileBrowser;

    boolean showCheckboxes;

    List<String> paths = new ArrayList<>();

    boolean showBreadcrumbsInList;

    private ActionMode actionMode;

    ActionMode.Callback actionModeCallback;

    private CompositeSubscription subscriptions;

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

        subscriptions = new CompositeSubscription();

        setHasOptionsMenu(true);

        adapter = new ViewModelAdapter();

        fileBrowser = new FileBrowser();

        if (savedInstanceState != null) {
            currentDir = savedInstanceState.getString(ARG_CURRENT_DIR);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_folder_browser, container, false);

        ButterKnife.bind(this, rootView);

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

        toolbar.setOnClickListener(v -> getNavigationController().popViewController());

        recyclerView.setRecyclerListener(new RecyclerListener());
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (currentDir == null) {
            subscriptions.add(Observable.fromCallable(() -> {
                if (!TextUtils.isEmpty(currentDir)) {
                    return new File(currentDir);
                } else {
                    return fileBrowser.getInitialDir();
                }
            }).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::changeDir));
        }

        getNavigationController().addBackPressListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        subscriptions.clear();

        getNavigationController().removeBackPressListener(this);
    }

    @Override
    public void onDestroyView() {

        if (actionMode != null) {
            actionMode.finish();
            actionMode = null;
        }
        actionModeCallback = null;

        super.onDestroyView();
    }

    @Override
    public void onDestroy() {

        if (actionMode != null) {
            actionMode.finish();
            actionMode = null;
        }
        actionModeCallback = null;

        super.onDestroy();
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

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sort_files_default:
                SettingsManager.getInstance().setFolderBrowserFilesSortOrder(SortManager.SortFiles.DEFAULT);
                reload();
                getActivity().supportInvalidateOptionsMenu();
                return true;
            case R.id.sort_files_filename:
                SettingsManager.getInstance().setFolderBrowserFilesSortOrder(SortManager.SortFiles.FILE_NAME);
                reload();
                getActivity().supportInvalidateOptionsMenu();
                return true;
            case R.id.sort_files_size:
                SettingsManager.getInstance().setFolderBrowserFilesSortOrder(SortManager.SortFiles.SIZE);
                reload();
                getActivity().supportInvalidateOptionsMenu();
                return true;
            case R.id.sort_files_artist_name:
                SettingsManager.getInstance().setFolderBrowserFilesSortOrder(SortManager.SortFiles.ARTIST_NAME);
                reload();
                getActivity().supportInvalidateOptionsMenu();
                return true;
            case R.id.sort_files_album_name:
                SettingsManager.getInstance().setFolderBrowserFilesSortOrder(SortManager.SortFiles.ALBUM_NAME);
                reload();
                getActivity().supportInvalidateOptionsMenu();
                return true;
            case R.id.sort_files_track_name:
                SettingsManager.getInstance().setFolderBrowserFilesSortOrder(SortManager.SortFiles.TRACK_NAME);
                reload();
                getActivity().supportInvalidateOptionsMenu();
                return true;
            case R.id.files_ascending:
                SettingsManager.getInstance().setFolderBrowserFilesAscending(!item.isChecked());
                reload();
                getActivity().supportInvalidateOptionsMenu();
                return true;
            case R.id.sort_folder_count:
                SettingsManager.getInstance().setFolderBrowserFoldersSortOrder(SortManager.SortFolders.COUNT);
                reload();
                getActivity().supportInvalidateOptionsMenu();
                return true;
            case R.id.sort_folder_default:
                SettingsManager.getInstance().setFolderBrowserFoldersSortOrder(SortManager.SortFolders.DEFAULT);
                reload();
                getActivity().supportInvalidateOptionsMenu();
                return true;
            case R.id.folders_ascending:
                SettingsManager.getInstance().setFolderBrowserFoldersAscending(!item.isChecked());
                reload();
                getActivity().supportInvalidateOptionsMenu();
                return true;

            case R.id.whitelist:
                actionMode = recyclerView.startActionMode(getActionModeCallback());
                isInActionMode = true;
                updateWhitelist();
                showCheckboxes(true);
                break;

            case R.id.show_filenames:
                SettingsManager.getInstance().setFolderBrowserShowFileNames(!item.isChecked());
                adapter.notifyItemRangeChanged(0, adapter.getItemCount());
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBreadcrumbItemClick(BreadcrumbItem item) {
        changeDir(new File(item.getItemPath()));
    }

//    @Override
//    public void onCheckedChange(FolderView folderView, boolean isChecked) {
//
//        folderView.setChecked(isChecked);
//
//        if (isChecked) {
//            if (!paths.contains(folderView.baseFileObject.path)) {
//                paths.add(folderView.baseFileObject.path);
//            }
//        } else {
//            if (paths.contains(folderView.baseFileObject.path)) {
//                paths.remove(folderView.baseFileObject.path);
//            }
//        }
//    }

    public void changeDir(File newDir) {

        subscriptions.add(Observable.fromCallable(() -> {

            final String path = FileHelper.getPath(newDir);

            if (TextUtils.isEmpty(path)) {
                return new ArrayList<BaseFileObject>();
            }

            currentDir = path;

            return fileBrowser.loadDir(new File(path));
        })
                .map(baseFileObjects -> {
                    List<ViewModel> items = Stream.of(baseFileObjects)
                            .map(baseFileObject -> {
                                FolderView folderView = new FolderView(baseFileObject);
                                folderView.setClickListener(this);
                                folderView.setChecked(showCheckboxes);
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
                })
                .subscribeOn(Schedulers.io())
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
                }));
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
    public void onFileObjectClick(BaseFileObject fileObject) {
        if (!isInActionMode) {
            if (fileObject.fileType == FileType.FILE) {
                FileHelper.getSongList(new File(fileObject.path), false, true)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(songs -> {
                            int index = -1;
                            for (int i = 0, songsSize = songs.size(); i < songsSize; i++) {
                                Song song = songs.get(i);
                                if (song.path.contains(fileObject.path)) {
                                    index = i;
                                    break;
                                }
                            }
                            MusicUtils.playAll(songs, index, (String message) -> {
                                if (isAdded() && getContext() != null) {
                                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                                }
                            });
                        });
            } else {
                changeDir(new File(fileObject.path));
            }
        } else if (fileObject.fileType != FileType.FILE) {
            changeDir(new File(fileObject.path));
        }
    }

    @Override
    public void onFileObjectOverflowClick(View v, BaseFileObject fileObject) {
        PopupMenu menu = new PopupMenu(getActivity(), v);
        MenuUtils.setupFolderMenu(getContext(), menu, fileObject);
        menu.setOnMenuItemClickListener(MenuUtils.getFolderMenuClickListener(
                getContext(),
                fileObject, taggerDialog -> taggerDialog.show(getFragmentManager()),
                () -> IntStream.range(0, adapter.getItemCount())
                        .filter(i -> adapter.items.get(i) instanceof FolderView)
                        .filter(i -> ((FolderView) adapter.items.get(i)).baseFileObject == fileObject)
                        .findFirst()
                        .ifPresent(i -> adapter.notifyItemChanged(i)),
                () -> IntStream.range(0, adapter.getItemCount())
                        .filter(i -> adapter.items.get(i) instanceof FolderView)
                        .filter(i -> ((FolderView) adapter.items.get(i)).baseFileObject == fileObject)
                        .findFirst()
                        .ifPresent(i -> adapter.notifyItemRemoved(i))));
        menu.show();
    }

    public ActionMode.Callback getActionModeCallback() {
        if (actionModeCallback == null) {
            actionModeCallback = new ActionMode.Callback() {
                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    isInActionMode = true;
                    MenuInflater inflater = getActivity().getMenuInflater();
                    inflater.inflate(R.menu.menu_save_whitelist, menu);
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.menu_save:
                            WhitelistHelper.deleteAllFolders();
                            WhitelistHelper.addToWhitelist(paths);
                            showCheckboxes(false);
                            adapter.notifyDataSetChanged();
                            mode.finish();
                            return true;
                    }
                    return false;
                }

                @Override
                public void onDestroyActionMode(ActionMode actionMode) {
                    isInActionMode = false;
                    actionModeCallback = null;
                    showCheckboxes(false);
                    adapter.notifyDataSetChanged();
                }
            };
        }
        return actionModeCallback;
    }

    public void showCheckboxes(boolean show) {

        showCheckboxes = show;

        List<ViewModel> folderViews = Stream.of(adapter.items)
                .filter(adaptableItem -> adaptableItem instanceof FolderView)
                .collect(Collectors.toList());

        for (ViewModel viewModel : folderViews) {
            ((FolderView) viewModel).setShowCheckboxes(showCheckboxes);
            adapter.notifyItemChanged(adapter.items.indexOf(viewModel));
        }
    }

    public void changeBreadcrumbPath() {

        List<ViewModel> breadcrumbViews = Stream.of(adapter.items)
                .filter(adaptableItem -> adaptableItem instanceof BreadcrumbsView)
                .collect(Collectors.toList());

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
                            .collect(Collectors.toList()));

                    if (showCheckboxes) {
                        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
                    }
                });
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}