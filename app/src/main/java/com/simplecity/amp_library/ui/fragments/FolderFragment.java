package com.simplecity.amp_library.ui.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.graphics.drawable.DrawableCompat;
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
import android.widget.LinearLayout;
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
import com.simplecity.amp_library.model.AdaptableItem;
import com.simplecity.amp_library.model.BaseFileObject;
import com.simplecity.amp_library.model.FileObject;
import com.simplecity.amp_library.model.FolderObject;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.sql.databases.WhitelistHelper;
import com.simplecity.amp_library.tagger.TaggerDialog;
import com.simplecity.amp_library.ui.activities.MainActivity;
import com.simplecity.amp_library.ui.adapters.FolderAdapter;
import com.simplecity.amp_library.ui.modelviews.BreadcrumbsView;
import com.simplecity.amp_library.ui.modelviews.FolderView;
import com.simplecity.amp_library.ui.views.BreadcrumbItem;
import com.simplecity.amp_library.ui.views.CustomEditText;
import com.simplecity.amp_library.utils.ActionBarUtils;
import com.simplecity.amp_library.utils.ColorUtils;
import com.simplecity.amp_library.utils.CustomMediaScanner;
import com.simplecity.amp_library.utils.DialogUtils;
import com.simplecity.amp_library.utils.DrawableUtils;
import com.simplecity.amp_library.utils.FileBrowser;
import com.simplecity.amp_library.utils.FileHelper;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.SortManager;
import com.simplecity.amp_library.utils.ThemeUtils;
import com.simplecity.amp_library.utils.ViewUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class FolderFragment extends BaseFragment implements
        MusicUtils.Defs,
        BreadcrumbListener,
        BackPressListener,
        FolderAdapter.Listener {

    private static final String TAG = "FolderFragment";

    private static final String ARG_PAGE_TITLE = "page_title";

    private static final String ARG_CURRENT_DIR = "current_dir";

    static final int FRAGMENT_GROUPID = FOLDER_FRAGMENT_GROUP_ID;

    private View mRootView;

    private RecyclerView mRecyclerView;

    FolderAdapter mAdapter;

    private Toolbar mToolbar;

    private View mDummyToolbar;
    private View mDummyStatusBar;

    boolean mInActionMode = false;

    String mCurrentDir;

    private SharedPreferences mPrefs;

    Breadcrumb mBreadcrumb;

    private SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferenceChangeListener;

    FileBrowser mFileBrowser;

    boolean mShowCheckboxes;

    List<String> paths = new ArrayList<>();

    boolean mShowBreadcrumbsInList;

    private ActionMode mActionMode;

    ActionMode.Callback mActionModeCallback;

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

        if (context instanceof MainActivity) {
            ((MainActivity) context).setOnBackPressedListener(this);
            if (!(getParentFragment() != null && getParentFragment() instanceof MainFragment)) {
                ((MainActivity) context).onSectionAttached(getString(R.string.folders_title));
            }
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        mSharedPreferenceChangeListener = (sharedPreferences, key) -> {
            if (key.equals("pref_theme_highlight_color") || key.equals("pref_theme_accent_color") || key.equals("pref_theme_white_accent")) {
                themeUIComponents();
            }
        };

        setHasOptionsMenu(true);

        mPrefs.registerOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);

        mAdapter = new FolderAdapter();
        mAdapter.setListener(this);

        mFileBrowser = new FileBrowser();

        ShuttleUtils.execute(new AsyncTask<Void, Void, File>() {
            @Override
            protected File doInBackground(Void... params) {
                File currentDir = null;
                if (savedInstanceState != null) {
                    String path = savedInstanceState.getString(ARG_CURRENT_DIR);
                    if (path != null) {
                        currentDir = new File(path);
                    }
                } else {
                    currentDir = mFileBrowser.getInitialDir();
                }
                return currentDir;
            }

            @Override
            protected void onPostExecute(File file) {
                super.onPostExecute(file);
                changeDir(file);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (mRootView == null) {

            mRootView = inflater.inflate(R.layout.fragment_folder_browser, container, false);

            mToolbar = (Toolbar) mRootView.findViewById(R.id.toolbar);

            mDummyToolbar = mRootView.findViewById(R.id.dummyToolbar);
            mDummyStatusBar = mRootView.findViewById(R.id.dummyStatusBar);

            //We need to set the dummy status bar height.
            if (ShuttleUtils.hasKitKat()) {
                LinearLayout.LayoutParams statusBarParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) ActionBarUtils.getStatusBarHeight(getActivity()));
                mDummyStatusBar.setLayoutParams(statusBarParams);
            } else {
                mDummyStatusBar.setVisibility(View.GONE);
            }

            if (getParentFragment() == null || !(getParentFragment() instanceof MainFragment)) {
                mShowBreadcrumbsInList = false;
                mBreadcrumb = (Breadcrumb) mRootView.findViewById(R.id.breadcrumb_view);
                mBreadcrumb.setTextColor(Color.WHITE);
                mBreadcrumb.addBreadcrumbListener(this);
                if (!TextUtils.isEmpty(mCurrentDir)) {
                    mBreadcrumb.changeBreadcrumbPath(mCurrentDir);
                }
                if (ShuttleUtils.hasKitKat()) {
                    mDummyStatusBar.setVisibility(View.VISIBLE);
                }
                mDummyToolbar.setVisibility(View.VISIBLE);
            } else {
                mShowBreadcrumbsInList = true;
                changeBreadcrumbPath();
                mToolbar.setVisibility(View.GONE);
                if (ShuttleUtils.hasKitKat()) {
                    mDummyStatusBar.setVisibility(View.GONE);
                }
                mDummyToolbar.setVisibility(View.GONE);
            }

            mRecyclerView = (RecyclerView) mRootView.findViewById(android.R.id.list);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            mRecyclerView.setAdapter(mAdapter);

            themeUIComponents();
        }
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        changeBreadcrumbPath();
        if (mBreadcrumb != null) {
            mBreadcrumb.changeBreadcrumbPath(mCurrentDir);
        }
    }

    @Override
    public void onDestroyView() {

        if (mActionMode != null) {
            mActionMode.finish();
            mActionMode = null;
        }
        mActionModeCallback = null;

        super.onDestroyView();
    }

    @Override
    public void onDestroy() {

        mPrefs.unregisterOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);

        if (mActionMode != null) {
            mActionMode.finish();
            mActionMode = null;
        }
        mActionModeCallback = null;

        super.onDestroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setOnBackPressedListener(null);
        }
    }

    private void themeUIComponents() {

        if (mDummyStatusBar != null) {
            //noinspection ResourceAsColor
            mDummyStatusBar.setBackgroundColor(ShuttleUtils.hasLollipop() ? ColorUtils.getPrimaryColorDark(getContext()) : ColorUtils.getPrimaryColor());
        }

        if (mDummyToolbar != null) {
            mDummyToolbar.setBackgroundColor(ColorUtils.getPrimaryColor());
        }

        if (mToolbar != null) {
            if (getParentFragment() != null && getParentFragment() instanceof MainFragment) {
                mToolbar.setBackgroundColor(Color.TRANSPARENT);
            } else {
                mToolbar.setBackgroundColor(ColorUtils.getPrimaryColor());
            }
        }

        mAdapter.notifyItemRangeChanged(0, mAdapter.getItemCount());

        ThemeUtils.themeRecyclerView(mRecyclerView);

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                ThemeUtils.themeRecyclerView(recyclerView);
                super.onScrollStateChanged(recyclerView, newState);
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(ARG_CURRENT_DIR, mCurrentDir);
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
                mActionMode = mRecyclerView.startActionMode(getActionModeCallback());
                mInActionMode = true;
                updateWhitelist();
                showCheckboxes(true);
                break;

            case R.id.show_filenames:
                SettingsManager.getInstance().setFolderBrowserShowFileNames(!item.isChecked());
                mAdapter.notifyItemRangeChanged(0, mAdapter.getItemCount());
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBreadcrumbItemClick(BreadcrumbItem item) {
        changeDir(new File(item.getItemPath()));
    }

    @Override
    public void onCheckedChange(FolderView folderView, boolean isChecked) {

        folderView.setChecked(isChecked);

        if (isChecked) {
            if (!paths.contains(folderView.baseFileObject.path)) {
                paths.add(folderView.baseFileObject.path);
            }
        } else {
            if (paths.contains(folderView.baseFileObject.path)) {
                paths.remove(folderView.baseFileObject.path);
            }
        }
    }

    public void changeDir(File newDir) {

        final String path = FileHelper.getPath(newDir);

        if (TextUtils.isEmpty(path)) {
            return;
        }

        ShuttleUtils.execute(new AsyncTask<Void, Void, List<BaseFileObject>>() {

            private final File file = new File(path);

            @Override
            protected List<BaseFileObject> doInBackground(Void... params) {
                return mFileBrowser.loadDir(file);
            }

            @Override
            protected void onPostExecute(List<BaseFileObject> fileObjects) {
                super.onPostExecute(fileObjects);

                List<AdaptableItem> items = Stream.of(fileObjects)
                        .map(baseFileObject -> {
                            FolderView folderView = new FolderView(baseFileObject);
                            folderView.setChecked(mShowCheckboxes);
                            return folderView;
                        })
                        .collect(Collectors.toList());

                if (mShowBreadcrumbsInList) {
                    BreadcrumbsView breadcrumbsView = new BreadcrumbsView(mCurrentDir);
                    breadcrumbsView.setBreadcrumbsPath(mCurrentDir);
                    items.add(0, breadcrumbsView);
                }

                if (mAdapter != null) {
                    mAdapter.setItems(items);
                }

                mCurrentDir = path;

                if (mBreadcrumb != null) {
                    mBreadcrumb.changeBreadcrumbPath(mCurrentDir);
                }
                if (mAdapter != null) {
                    changeBreadcrumbPath();
                }

            }
        });

    }

    public void reload() {
        if (mCurrentDir != null) {
            changeDir(new File(mCurrentDir));
        }
    }

    @Override
    public boolean onBackPressed() {

        if (mFileBrowser.getCurrentDir() != null && mFileBrowser.getRootDir() != null && mFileBrowser.getCurrentDir().compareTo(mFileBrowser.getRootDir()) != 0) {
            File parent = mFileBrowser.getCurrentDir().getParentFile();
            changeDir(parent);
            return true;
        }
        return false;
    }

    @Override
    public void onItemClick(View v, int position, BaseFileObject fileObject) {

        if (!mInActionMode) {

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
                            MusicUtils.playAll(songs, index, () -> {
                                final String message = getContext().getString(R.string.emptyplaylist);
                                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
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
    public void onOverflowClick(View v, int position, BaseFileObject fileObject) {

        PopupMenu menu = new PopupMenu(getActivity(), v);

        if (fileObject.fileType == FileType.FILE) {

            //Play this song next
            menu.getMenu().add(FRAGMENT_GROUPID, PLAY_NEXT, 4, R.string.play_next);

            //Tag editor
            if (ShuttleUtils.isUpgraded()) {
                menu.getMenu().add(FRAGMENT_GROUPID, TAGGER, 5, R.string.edit_tags);
            }

            //Set this song as the ringtone
            menu.getMenu().add(FRAGMENT_GROUPID, USE_AS_RINGTONE, 6, R.string.ringtone_menu);


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
        SubMenu sub = menu.getMenu().addSubMenu(FRAGMENT_GROUPID, ADD_TO_PLAYLIST, 2, R.string.add_to_playlist);
        PlaylistUtils.makePlaylistMenu(getActivity(), sub, FRAGMENT_GROUPID);

        //Add to queue
        menu.getMenu().add(FRAGMENT_GROUPID, QUEUE, 3, R.string.add_to_queue);

        menu.getMenu().add(FRAGMENT_GROUPID, RESCAN, 4, R.string.scan_file);

        menu.setOnMenuItemClickListener(item -> {

            switch (item.getItemId()) {

                case TAGGER:
                    FileHelper.getSong(new File(fileObject.path))
                            .subscribeOn(Schedulers.io())
                            .subscribe(song -> TaggerDialog.newInstance(song).show(getFragmentManager()));
                    return true;

                case QUEUE:
                    FileHelper.getSongList(new File(fileObject.path), true, false)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(songs -> MusicUtils.addToQueue(getActivity(), songs));
                    return true;

                case DELETE_ITEM:
                    MaterialDialog.Builder builder = DialogUtils.getBuilder(getActivity())
                            .title(R.string.delete_item)
                            .icon(DrawableUtils.getBlackDrawable(getActivity(), R.drawable.ic_dialog_alert));
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
                                    mAdapter.removeItem(position);
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
                    final CustomEditText editText = (CustomEditText) customView.findViewById(R.id.editText);
                    ThemeUtils.themeEditText(editText);
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
                                        mAdapter.notifyDataSetChanged();
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
                    FileHelper.getSong(new File(fileObject.path))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(song -> ShuttleUtils.setRingtone(getContext(), song));
                    return true;
                case PLAY_NEXT:
                    FileHelper.getSongList(new File(fileObject.path), false, false)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(songs -> MusicUtils.playNext(getActivity(), songs));

                    return true;
                case PLAY_SELECTION:
                    final ProgressDialog progressDialog = ProgressDialog.show(getActivity(), "", getString(R.string.gathering_songs), false);
                    FileHelper.getSongList(new File(fileObject.path), true, fileObject.fileType == FileType.FILE)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(songs -> {
                                MusicUtils.playAll(songs, 0, () -> {
                                    final String message = getContext().getString(R.string.emptyplaylist);
                                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                                });

                                if (isAdded() && progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }
                            });
                    return true;
                case NEW_PLAYLIST:
                    List<BaseFileObject> fileObjects = new ArrayList<>();
                    fileObjects.add(fileObject);
                    PlaylistUtils.createFileObjectPlaylistDialog(getActivity(), fileObjects);
                    return true;
                case PLAYLIST_SELECTED:
                    final Playlist playlist = (Playlist) item.getIntent().getSerializableExtra(ShuttleUtils.ARG_PLAYLIST);
                    FileHelper.getSongList(new File(fileObject.path), true, false)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(songs -> PlaylistUtils.addToPlaylist(getContext(), playlist, songs));

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
                        DrawableCompat.setTint(DrawableCompat.wrap(indeterminateProgress.getIndeterminateDrawable()), ColorUtils.getAccentColor());

                        ProgressBar horizontalProgress = (ProgressBar) view.findViewById(R.id.horizontalProgress);
                        DrawableCompat.setTint(DrawableCompat.wrap(horizontalProgress.getProgressDrawable()), ColorUtils.getAccentColor());

                        MaterialDialog dialog = DialogUtils.getBuilder(getContext())
                                .title(R.string.scanning)
                                .customView(view, false)
                                .negativeText(R.string.close)
                                .show();

                        FileHelper.getSongList(new File(fileObject.path), true, false)
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
                                });
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
        if (mActionModeCallback == null) {
            mActionModeCallback = new ActionMode.Callback() {
                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    ThemeUtils.themeContextualActionBar(getActivity());
                    mInActionMode = true;
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
                            mAdapter.notifyDataSetChanged();
                            mode.finish();
                            return true;
                    }
                    return false;
                }

                @Override
                public void onDestroyActionMode(ActionMode actionMode) {
                    mInActionMode = false;
                    mActionModeCallback = null;
                    showCheckboxes(false);
                    mAdapter.notifyDataSetChanged();
                }
            };
        }
        return mActionModeCallback;
    }

    public void showCheckboxes(boolean show) {

        mShowCheckboxes = show;

        List<AdaptableItem> folderViews = Stream.of(mAdapter.items)
                .filter(adaptableItem -> adaptableItem instanceof FolderView)
                .collect(Collectors.toList());

        for (AdaptableItem adaptableItem : folderViews) {
            ((FolderView) adaptableItem).setShowCheckboxes(mShowCheckboxes);
            mAdapter.notifyItemChanged(mAdapter.items.indexOf(adaptableItem));
        }
    }

    public void changeBreadcrumbPath() {

        List<AdaptableItem> breadcrumbViews = Stream.of(mAdapter.items)
                .filter(adaptableItem -> adaptableItem instanceof BreadcrumbsView)
                .collect(Collectors.toList());

        for (AdaptableItem adaptableItem : breadcrumbViews) {
            ((BreadcrumbsView) adaptableItem).setBreadcrumbsPath(mCurrentDir);
            mAdapter.notifyItemChanged(mAdapter.items.indexOf(adaptableItem));
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

                    if (mShowCheckboxes) {
                        mAdapter.notifyItemRangeChanged(0, mAdapter.getItemCount());
                    }
                });
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}