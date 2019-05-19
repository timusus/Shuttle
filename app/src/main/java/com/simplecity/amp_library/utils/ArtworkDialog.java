package com.simplecity.amp_library.utils;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import com.afollestad.materialdialogs.MaterialDialog;
import com.annimon.stream.Stream;
import com.mlsdev.rximagepicker.RxImageConverters;
import com.mlsdev.rximagepicker.RxImagePicker;
import com.mlsdev.rximagepicker.Sources;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.model.ArtworkModel;
import com.simplecity.amp_library.model.ArtworkProvider;
import com.simplecity.amp_library.model.UserSelectedArtwork;
import com.simplecity.amp_library.sql.databases.CustomArtworkTable;
import com.simplecity.amp_library.ui.modelviews.ArtworkLoadingView;
import com.simplecity.amp_library.ui.modelviews.ArtworkView;
import com.simplecity.amp_library.ui.views.recyclerview.SpacesItemDecoration;
import com.simplecityapps.recycler_adapter.adapter.ViewModelAdapter;
import com.simplecityapps.recycler_adapter.model.ViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.RecyclerListener;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ArtworkDialog {

    private static final String TAG = "ArtworkDialog";

    private ArtworkDialog() {

    }

    public static MaterialDialog build(Context context, ArtworkProvider artworkProvider) {

        @SuppressLint("InflateParams")
        View customView = LayoutInflater.from(context).inflate(R.layout.dialog_artwork, null);

        ViewModelAdapter adapter = new ViewModelAdapter();

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        RecyclerView recyclerView = customView.findViewById(R.id.recyclerView);
        recyclerView.addItemDecoration(new SpacesItemDecoration(16));
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(0);
        recyclerView.setRecyclerListener(new RecyclerListener());

        adapter.items.add(0, new ArtworkLoadingView());
        adapter.notifyDataSetChanged();
        recyclerView.setAdapter(adapter);

        ArtworkView.GlideListener glideListener = artworkView -> {
            int index = adapter.items.indexOf(artworkView);
            if (index != -1) {
                adapter.removeItem(index);
            }
        };

        List<ViewModel> viewModels = new ArrayList<>();

        UserSelectedArtwork userSelectedArtwork = ((ShuttleApplication) context.getApplicationContext()).userSelectedArtwork.get(artworkProvider.getArtworkKey());
        if (userSelectedArtwork != null) {
            File file = null;
            if (userSelectedArtwork.path != null) {
                file = new File(userSelectedArtwork.path);
            }
            ArtworkView artworkView = new ArtworkView(userSelectedArtwork.type, artworkProvider, glideListener, file, true);
            artworkView.setSelected(true);
            viewModels.add(artworkView);
        }

        if (userSelectedArtwork == null || userSelectedArtwork.type != ArtworkProvider.Type.MEDIA_STORE) {
            viewModels.add(new ArtworkView(ArtworkProvider.Type.MEDIA_STORE, artworkProvider, glideListener));
        }
        if (userSelectedArtwork == null || userSelectedArtwork.type != ArtworkProvider.Type.TAG) {
            viewModels.add(new ArtworkView(ArtworkProvider.Type.TAG, artworkProvider, glideListener));
        }
        if (userSelectedArtwork == null || userSelectedArtwork.type != ArtworkProvider.Type.REMOTE) {
            viewModels.add(new ArtworkView(ArtworkProvider.Type.REMOTE, artworkProvider, glideListener));
        }

        //Dummy Folder ArtworkView - will be replaced or removed depending on availability of folder images
        ArtworkView folderView = new ArtworkView(ArtworkProvider.Type.FOLDER, null, null);
        viewModels.add(folderView);

        ArtworkView.ClickListener listener = artworkView -> {
            Stream.of(viewModels)
                    .filter(viewModel -> viewModel instanceof ArtworkView)
                    .forEachIndexed((i, viewModel) -> ((ArtworkView) viewModel).setSelected(viewModel == artworkView));
            adapter.notifyItemRangeChanged(0, adapter.getItemCount(), 0);
        };

        Stream.of(viewModels)
                .filter(viewModel -> viewModel instanceof ArtworkView)
                .forEach(viewModel -> ((ArtworkView) viewModel).setListener(listener));

        adapter.setItems(viewModels);

        Observable.fromCallable(artworkProvider::getFolderArtworkFiles)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(files -> {
                    adapter.removeItem(adapter.items.indexOf(folderView));
                    if (files != null) {
                        Stream.of(files)
                                .filter(file -> userSelectedArtwork == null || !file.getPath().equals(userSelectedArtwork.path))
                                .forEach(file ->
                                        adapter.addItem(new ArtworkView(ArtworkProvider.Type.FOLDER, artworkProvider, glideListener, file, false)));
                    }
                }, error -> LogUtils.logException(TAG, "Error getting artwork files", error));

        return new MaterialDialog.Builder(context)
                .title(R.string.artwork_edit)
                .customView(customView, false)
                .autoDismiss(false)
                .positiveText(context.getString(R.string.save))
                .onPositive((dialog, which) -> {
                    ArtworkView checkedView = ArtworkDialog.getCheckedView(adapter.items);
                    if (checkedView != null) {
                        ArtworkModel artworkModel = checkedView.getItem();
                        ContentValues values = new ContentValues();
                        values.put(CustomArtworkTable.COLUMN_KEY, artworkProvider.getArtworkKey());
                        values.put(CustomArtworkTable.COLUMN_TYPE, artworkModel.type);
                        values.put(CustomArtworkTable.COLUMN_PATH, artworkModel.file == null ? null : artworkModel.file.getPath());
                        context.getContentResolver().insert(CustomArtworkTable.URI, values);

                        ((ShuttleApplication) context.getApplicationContext()).userSelectedArtwork.put(artworkProvider.getArtworkKey(),
                                new UserSelectedArtwork(artworkModel.type, artworkModel.file == null ? null : artworkModel.file.getPath()));
                    } else {
                        context.getContentResolver().delete(CustomArtworkTable.URI, CustomArtworkTable.COLUMN_KEY + "='" + artworkProvider.getArtworkKey().replaceAll("'", "\''") + "'", null);
                        ((ShuttleApplication) context.getApplicationContext()).userSelectedArtwork.remove(artworkProvider.getArtworkKey());
                    }
                    dialog.dismiss();
                })
                .negativeText(context.getString(R.string.close))
                .onNegative((dialog, which) -> dialog.dismiss())
                .neutralText(context.getString(R.string.artwork_gallery))
                .onNeutral((dialog, which) -> RxImagePicker.with(context)
                        .requestImage(Sources.GALLERY)
                        .flatMap(uri -> {

                            // The directory will be shuttle/custom_artwork/key_hashcode/currentSystemTime.artwork
                            // We want the directory to be based on the key, so we can delete old artwork, and the
                            // filename to be unique, because it's used for Glide caching.
                            File dir = new File(context.getFilesDir() + "/shuttle/custom_artwork/" + artworkProvider.getArtworkKey().hashCode() + "/");

                            // Create dir if necessary
                            if (!dir.exists()) {
                                dir.mkdirs();
                            } else {
                                // Delete any existing artwork for this key.
                                if (dir.isDirectory()) {
                                    String[] children = dir.list();
                                    for (String child : children) {
                                        new File(dir, child).delete();
                                    }
                                }
                            }

                            File file = new File(dir.getPath() + System.currentTimeMillis() + ".artwork");

                            try {
                                file.createNewFile();
                                if (file.exists()) {
                                    return RxImageConverters.uriToFile(context, uri, file);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            return null;
                        })
                        .filter(file -> file != null && file.exists())
                        .subscribe(file -> {
                            // If we've already got user-selected artwork in the adapter, remove it.
                            if (adapter.getItemCount() != 0) {
                                File aFile = ((ArtworkView) adapter.items.get(0)).file;
                                if (aFile != null && aFile.getPath().contains(artworkProvider.getArtworkKey())) {
                                    adapter.removeItem(0);
                                }
                            }

                            ArtworkView artworkView = new ArtworkView(ArtworkProvider.Type.FOLDER, artworkProvider, glideListener, file, true);
                            artworkView.setSelected(true);
                            adapter.addItem(0, artworkView);
                            recyclerView.scrollToPosition(0);
                        }, error -> LogUtils.logException(TAG, "Error picking from gallery", error)))
                .cancelable(false)
                .build();
    }

    @Nullable
    public static ArtworkView getCheckedView(List<ViewModel> viewModels) {
        return (ArtworkView) Stream.of(viewModels)
                .filter(viewModel -> viewModel instanceof ArtworkView && ((ArtworkView) viewModel).isSelected())
                .findFirst()
                .orElse(null);
    }
}