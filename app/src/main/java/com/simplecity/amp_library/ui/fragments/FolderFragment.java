package com.simplecity.amp_library.ui.fragments;

import android.app.ProgressDialog;
import android.content.Context;
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
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.interfaces.BackPressListener;
import com.simplecity.amp_library.interfaces.Breadcrumb;
import com.simplecity.amp_library.interfaces.BreadcrumbListener;
import com.simplecity.amp_library.interfaces.FileType;
import com.simplecity.amp_library.model.BaseFileObject;
import com.simplecity.amp_library.model.FileObject;
import com.simplecity.amp_library.model.FolderObject;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.sql.databases.WhitelistHelper;
import com.simplecity.amp_library.tagger.TaggerDialog;
import com.simplecity.amp_library.ui.modelviews.BreadcrumbsView;
import com.simplecity.amp_library.ui.modelviews.FolderView;
import com.simplecity.amp_library.ui.views.BreadcrumbItem;
import com.simplecity.amp_library.utils.CustomMediaScanner;
import com.simplecity.amp_library.utils.DialogUtils;
import com.simplecity.amp_library.utils.FileBrowser;
import com.simplecity.amp_library.utils.FileHelper;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.SortManager;
import com.simplecity.amp_library.utils.ViewUtils;
import com.simplecityapps.recycler_adapter.adapter.ViewModelAdapter;
import com.simplecityapps.recycler_adapter.model.ViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.RecyclerListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class FolderFragment extends BaseFragment implements
        MusicUtils.Defs,
        BreadcrumbListener,
        BackPressListener,
        FolderView.ClickListener {

    private static final String TAG = "FolderFragment";

    private static final String ARG_PAGE_TITLE = "page_title";

    private static final String ARG_CURRENT_DIR = "current_dir";

    static final int FRAGMENT_GROUPID = FOLDER_FRAGMENT_GROUP_ID;

    ViewModelAdapter adapter;

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
    public void onAttach(Context context) {
        super.onAttach(context);

        //Todo:

//        if (context instanceof MainActivity2) {
//            ((MainActivity2) context).setOnBackPressedListener(this);
//            if (!(getParentFragment() != null && getParentFragment() instanceof MainFragment)) {
//                ((MainActivity2) context).onSectionAttached(getString(R.string.folders_title));
//            }
//        }
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
//            breadcrumb.setTextColor(Color.WHITE);
//            breadcrumb.addBreadcrumbListener(this);
//            if (!TextUtils.isEmpty(currentDir)) {
//                breadcrumb.changeBreadcrumbPath(currentDir);
//            }
//        } else {
//            showBreadcrumbsInList = true;
//            changeBreadcrumbPath();
//            toolbar.setVisibility(View.GONE);
//        }

        toolbar.setOnClickListener(v -> getActivity().onBackPressed());

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
    }

    @Override
    public void onPause() {
        super.onPause();

        subscriptions.clear();
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
    public void onDetach() {
        super.onDetach();

        //Todo:
//        if (getActivity() instanceof MainActivity2) {
//            ((MainActivity2) getActivity()).setOnBackPressedListener(null);
//        }
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
    public boolean onBackPressed() {

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

        if (fileObject.fileType == FileType.FILE) {


            if (FileHelper.canReadWrite(new File(fileObject.path))) {
                //Rename File
                menu.getMenu().add(FRAGMENT_GROUPID, RENAME, 7, R.string.rename_file);
                //Delete File
                menu.getMenu().add(FRAGMENT_GROUPID, DELETE_ITEM, 8, R.string.delete_item);
            }

            menu.getMenu().add(FRAGMENT_GROUPID, VIEW_INFO, 9, R.string.song_info);

        } else {

            //Play all files in this dir
            menu.getMenu().add(FRAGMENT_GROUPID, PLAY_SELECTION, 0, R.string.play_selection);

            //Set this directory as initial directory
            menu.getMenu().add(FRAGMENT_GROUPID, SET_INITIAL_DIR, 4, R.string.set_initial_dir);

            if (FileHelper.canReadWrite(new File(fileObject.path))) {
                //Rename dir
                menu.getMenu().add(FRAGMENT_GROUPID, RENAME, 5, R.string.rename_folder);
                //Delete dir
                menu.getMenu().add(FRAGMENT_GROUPID, DELETE_ITEM, 6, R.string.delete_item);
            }
        }

        //Bring up the add to playlist menu
        SubMenu sub = menu.getMenu().addSubMenu(FRAGMENT_GROUPID, ADD_TO_PLAYLIST, 2, R.string.menu_playlist);
        PlaylistUtils.makePlaylistMenu(getActivity(), sub);

        //Add to queue
        menu.getMenu().add(FRAGMENT_GROUPID, QUEUE, 3, R.string.menu_queue);

        menu.getMenu().add(FRAGMENT_GROUPID, RESCAN, 4, R.string.scan_file);

        menu.setOnMenuItemClickListener(item -> {

            switch (item.getItemId()) {

                case TAGGER:
                    subscriptions.add(FileHelper.getSong(new File(fileObject.path))
                            .subscribeOn(Schedulers.io())
                            .subscribe(song -> TaggerDialog.newInstance(song).show(getFragmentManager())));
                    return true;

                case QUEUE:
                    subscriptions.add(FileHelper.getSongList(new File(fileObject.path), true, false)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(songs -> MusicUtils.addToQueue(songs, message ->
                                    Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show())));
                    return true;

                case DELETE_ITEM:
                    MaterialDialog.Builder builder = DialogUtils.getBuilder(getActivity())
                            .title(R.string.delete_item)
                            .iconRes(R.drawable.ic_dialog_alert);
                    if (fileObject.fileType == FileType.FILE) {
                        builder.content(String.format(getResources().getString(
                                R.string.delete_file_confirmation_dialog), fileObject.name));
                    } else {
                        builder.content(String.format(getResources().getString(
                                R.string.delete_folder_confirmation_dialog), fileObject.path));
                    }
                    builder.positiveText(R.string.button_ok)
                            .onPositive((materialDialog, dialogAction) -> {
                                if (FileHelper.deleteFile(new File(fileObject.path))) {
                                    adapter.removeItem(Stream.of(adapter.items)
                                            .filter(folderView -> folderView instanceof FolderView &&
                                                    (((FolderView) folderView).baseFileObject.equals(fileObject)))
                                            .findFirst()
                                            .orElse(null));
                                    CustomMediaScanner.scanFiles(Collections.singletonList(fileObject.path), null);
                                } else {
                                    Toast.makeText(getActivity(),
                                            fileObject.fileType == FileType.FOLDER ? R.string.delete_folder_failed : R.string.delete_file_failed,
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                    builder.negativeText(R.string.cancel)
                            .show();
                    return true;

                case RENAME:

                    View customView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_rename, null);
                    final EditText editText = (EditText) customView.findViewById(R.id.editText);
                    editText.setText(fileObject.name);

                    builder = DialogUtils.getBuilder(getActivity());
                    if (fileObject.fileType == FileType.FILE) {
                        builder.title(R.string.rename_file);
                    } else {
                        builder.title(R.string.rename_folder);
                    }

                    builder.customView(customView, false);
                    builder.positiveText(R.string.save)
                            .onPositive((materialDialog, dialogAction) -> {
                                if (editText.getText() != null) {
                                    if (FileHelper.renameFile(getActivity(), fileObject, editText.getText().toString())) {
                                        adapter.notifyDataSetChanged();
                                    } else {
                                        Toast.makeText(getActivity(),
                                                fileObject.fileType == FileType.FOLDER ? R.string.rename_folder_failed : R.string.rename_file_failed,
                                                Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                    builder.negativeText(R.string.cancel)
                            .show();
                    return true;
                case USE_AS_RINGTONE:
                    subscriptions.add(FileHelper.getSong(new File(fileObject.path))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(song -> ShuttleUtils.setRingtone(getContext(), song)));
                    return true;
                case PLAY_NEXT:
                    subscriptions.add(FileHelper.getSongList(new File(fileObject.path), false, false)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(songs -> MusicUtils.playNext(songs, message ->
                                    Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show())));

                    return true;
                case PLAY_SELECTION:
                    final ProgressDialog progressDialog = ProgressDialog.show(getActivity(), "", getString(R.string.gathering_songs), false);
                    subscriptions.add(FileHelper.getSongList(new File(fileObject.path), true, fileObject.fileType == FileType.FILE)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(songs -> {
                                MusicUtils.playAll(songs, 0, (String message) ->
                                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());

                                if (isAdded() && progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }
                            }));
                    return true;
                case NEW_PLAYLIST:
                    List<BaseFileObject> fileObjects = new ArrayList<>();
                    fileObjects.add(fileObject);
                    PlaylistUtils.createFileObjectPlaylistDialog(getActivity(), fileObjects);
                    return true;
                case PLAYLIST_SELECTED:
                    final Playlist playlist = (Playlist) item.getIntent().getSerializableExtra(ShuttleUtils.ARG_PLAYLIST);
                    subscriptions.add(FileHelper.getSongList(new File(fileObject.path), true, false)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(songs -> PlaylistUtils.addToPlaylist(getContext(), playlist, songs)));

                    return true;
                case SET_INITIAL_DIR:
                    SettingsManager.getInstance().setFolderBrowserInitialDir(fileObject.path);
                    Toast.makeText(getActivity(),
                            fileObject.path + getResources().getString(R.string.initial_dir_set_message),
                            Toast.LENGTH_SHORT).show();
                    return true;
                case RESCAN:

                    if (fileObject instanceof FolderObject) {

                        //Todo:
                        // Abstract this away to DialogUtils or somewhere else, where it can be reused
                        // by anyone else who wants to run a scan (like the Tagger)
                        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_progress, null);
                        TextView pathsTextView = (TextView) view.findViewById(R.id.paths);
                        pathsTextView.setText(fileObject.path);

                        ProgressBar indeterminateProgress = (ProgressBar) view.findViewById(R.id.indeterminateProgress);
                        ProgressBar horizontalProgress = (ProgressBar) view.findViewById(R.id.horizontalProgress);

                        MaterialDialog dialog = DialogUtils.getBuilder(getContext())
                                .title(R.string.scanning)
                                .customView(view, false)
                                .negativeText(R.string.close)
                                .show();

                        subscriptions.add(FileHelper.getSongList(new File(fileObject.path), true, false)
                                .map(songs -> Stream.of(songs)
                                        .map(song -> song.path)
                                        .collect(Collectors.toList()))
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(paths -> {
                                    ViewUtils.fadeOut(indeterminateProgress, null);
                                    ViewUtils.fadeIn(horizontalProgress, null);
                                    horizontalProgress.setMax(paths.size());

                                    CustomMediaScanner.scanFiles(paths, new CustomMediaScanner.ScanCompletionListener() {
                                        @Override
                                        public void onPathScanned(String path) {
                                            horizontalProgress.setProgress(horizontalProgress.getProgress() + 1);
                                            pathsTextView.setText(path);
                                        }

                                        @Override
                                        public void onScanCompleted() {
                                            if (isAdded() && dialog.isShowing()) {
                                                dialog.dismiss();
                                            }
                                        }
                                    });
                                }));
                    } else {
                        CustomMediaScanner.scanFiles(Collections.singletonList(fileObject.path), new CustomMediaScanner.ScanCompletionListener() {
                            @Override
                            public void onPathScanned(String path) {

                            }

                            @Override
                            public void onScanCompleted() {
                                Toast.makeText(getContext(), R.string.scan_complete, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                    return true;
                case VIEW_INFO:
                    DialogUtils.showFileInfoDialog(getActivity(), (FileObject) fileObject);
                    break;
            }
            return false;
        });
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