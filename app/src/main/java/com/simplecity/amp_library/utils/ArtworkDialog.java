package com.simplecity.amp_library.utils;

import android.content.ContentValues;
import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.annimon.stream.Stream;
import com.mlsdev.rximagepicker.RxImageConverters;
import com.mlsdev.rximagepicker.RxImagePicker;
import com.mlsdev.rximagepicker.Sources;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.model.AdaptableItem;
import com.simplecity.amp_library.model.ArtworkModel;
import com.simplecity.amp_library.model.ArtworkProvider;
import com.simplecity.amp_library.model.UserSelectedArtwork;
import com.simplecity.amp_library.sql.databases.CustomArtworkTable;
import com.simplecity.amp_library.ui.adapters.ItemAdapter;
import com.simplecity.amp_library.ui.modelviews.ArtworkView;
import com.simplecity.amp_library.ui.modelviews.LoadingView;
import com.simplecity.amp_library.ui.recyclerview.SpacesItemDecoration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class ArtworkDialog {

    private static final String TAG = "ArtworkDialog";

    private ArtworkDialog() {

    }

    public static void showDialog(Context context, ArtworkProvider artworkProvider) {

        View customView = LayoutInflater.from(context).inflate(R.layout.dialog_artwork, null);

        ArtworkAdapter artworkAdapter = new ArtworkAdapter();

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        RecyclerView recyclerView = (RecyclerView) customView.findViewById(R.id.recyclerView);
        recyclerView.addItemDecoration(new SpacesItemDecoration(16));
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(0);
        recyclerView.setRecyclerListener(holder -> {
            if (holder.getAdapterPosition() != -1) {
                artworkAdapter.items.get(holder.getAdapterPosition()).recycle(holder);
            }
        });

        artworkAdapter.items.add(0, new LoadingView());
        artworkAdapter.notifyDataSetChanged();
        recyclerView.setAdapter(artworkAdapter);

        ArtworkView.GlideListener glideListener = artworkView -> {
            int index = artworkAdapter.items.indexOf(artworkView);
            if (index != -1) {
                artworkAdapter.removeItem(index);
            }
        };

        List<AdaptableItem> adaptableItems = new ArrayList<>();

        UserSelectedArtwork userSelectedArtwork = ShuttleApplication.getInstance().userSelectedArtwork.get(artworkProvider.getArtworkKey());
        if (userSelectedArtwork != null) {
            File file = null;
            if (userSelectedArtwork.path != null) {
                file = new File(userSelectedArtwork.path);
            }
            ArtworkView artworkView = new ArtworkView(userSelectedArtwork.type, artworkProvider, glideListener, file, true);
            artworkView.setSelected(true);
            adaptableItems.add(artworkView);
        }

        if (userSelectedArtwork == null || userSelectedArtwork.type != ArtworkProvider.Type.MEDIA_STORE) {
            adaptableItems.add(new ArtworkView(ArtworkProvider.Type.MEDIA_STORE, artworkProvider, glideListener));
        }
        if (userSelectedArtwork == null || userSelectedArtwork.type != ArtworkProvider.Type.TAG) {
            adaptableItems.add(new ArtworkView(ArtworkProvider.Type.TAG, artworkProvider, glideListener));
        }
        if (userSelectedArtwork == null || userSelectedArtwork.type != ArtworkProvider.Type.LAST_FM) {
            adaptableItems.add(new ArtworkView(ArtworkProvider.Type.LAST_FM, artworkProvider, glideListener));
        }
        if (userSelectedArtwork == null || userSelectedArtwork.type != ArtworkProvider.Type.ITUNES) {
            adaptableItems.add(new ArtworkView(ArtworkProvider.Type.ITUNES, artworkProvider, glideListener));
        }

        //Dummy Folder ArtworkView - will be replaced or removed depending on availability of folder images
        ArtworkView folderView = new ArtworkView(ArtworkProvider.Type.FOLDER, null, null);
        adaptableItems.add(folderView);

        artworkAdapter.setItems(adaptableItems);

        Observable.fromCallable(artworkProvider::getFolderArtworkFiles)
                .subscribeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(files -> {
                    artworkAdapter.removeItem(artworkAdapter.items.indexOf(folderView));
                    if (files != null) {
                        Stream.of(files).filter(file -> userSelectedArtwork == null || !file.getPath().equals(userSelectedArtwork.path)).forEach(file -> {
                            artworkAdapter.addItem(new ArtworkView(ArtworkProvider.Type.FOLDER, artworkProvider, glideListener, file, false));
                        });
                    }
                }, error -> {
                    Log.e(TAG, "Error getting folder artwork files.. " + error.toString());
                });

        DialogUtils.getBuilder(context)
                .title(context.getString(R.string.artwork_edit))
                .customView(customView, false)
                .autoDismiss(false)
                .positiveText(context.getString(R.string.save))
                .onPositive((dialog, which) -> {
                    if (artworkAdapter.checkedItem != null) {
                        ContentValues values = new ContentValues();
                        values.put(CustomArtworkTable.COLUMN_KEY, artworkProvider.getArtworkKey());
                        values.put(CustomArtworkTable.COLUMN_TYPE, artworkAdapter.checkedItem.type);
                        values.put(CustomArtworkTable.COLUMN_PATH, artworkAdapter.checkedItem.file == null ? null : artworkAdapter.checkedItem.file.getPath());
                        context.getContentResolver().insert(CustomArtworkTable.URI, values);

                        ShuttleApplication.getInstance().userSelectedArtwork.put(artworkProvider.getArtworkKey(), new UserSelectedArtwork(artworkAdapter.checkedItem.type, artworkAdapter.checkedItem.file == null ? null : artworkAdapter.checkedItem.file.getPath()));
                    } else {
                        context.getContentResolver().delete(CustomArtworkTable.URI, CustomArtworkTable.COLUMN_KEY + "='" + artworkProvider.getArtworkKey().replaceAll("'", "\''") + "'", null);
                        ShuttleApplication.getInstance().userSelectedArtwork.remove(artworkProvider.getArtworkKey());
                    }
                    dialog.dismiss();
                })
                .negativeText(context.getString(R.string.close))
                .onNegative((dialog, which) -> dialog.dismiss())
                .neutralText(context.getString(R.string.artwork_gallery))
                .onNeutral((dialog, which) -> RxImagePicker.with(context)
                        .requestImage(Sources.GALLERY)
                        .flatMap(uri -> {

                            //The directory will be shuttle/custom_artwork/key_hashcode/currenSystemTime.artwork
                            //We want the directory to be based on the key, so we can delete old artwork, and the
                            //filename to be unique, because it's used for Glide caching.
                            File dir = new File(ShuttleApplication.getInstance().getFilesDir() + "/shuttle/custom_artwork/" + artworkProvider.getArtworkKey().hashCode() + "/");

                            //Create dir if necessary
                            if (!dir.exists()) {
                                dir.mkdirs();
                            } else {
                                //Delete any existing artwork for this key.
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

                            //If we've already got user-selected artwork in the adapter, remove it.
                            if (artworkAdapter.getItemCount() != 0) {
                                File aFile = ((ArtworkView) artworkAdapter.items.get(0)).file;
                                if (aFile != null && aFile.getPath().contains(artworkProvider.getArtworkKey())) {
                                    artworkAdapter.removeItem(0);
                                }
                            }

                            ArtworkView artworkView = new ArtworkView(ArtworkProvider.Type.FOLDER, artworkProvider, glideListener, file, true);
                            artworkAdapter.addItem(0, artworkView);
                            artworkAdapter.selectItem(0);
                            recyclerView.scrollToPosition(0);
                        }))
                .cancelable(false)
                .show();
    }

    private static class ArtworkAdapter extends ItemAdapter {

        ArtworkModel checkedItem;

        ArtworkAdapter() {
        }

        @Override
        protected void attachListeners(RecyclerView.ViewHolder viewHolder) {
            super.attachListeners(viewHolder);

            if (viewHolder instanceof ArtworkView.ViewHolder) {
                viewHolder.itemView.setOnClickListener(v -> {
                    if (viewHolder.getAdapterPosition() != -1) {
                        selectItem(viewHolder.getAdapterPosition());
                    }
                });
            }
        }

        void selectItem(int position) {

            AdaptableItem adaptableItem = items.get(position);
            if (!(adaptableItem instanceof ArtworkView)) {
                return;
            }

            ArtworkView artworkView = (ArtworkView) adaptableItem;

            int previouslySelectedItem = -1;

            for (int i = 0, itemsSize = items.size(); i < itemsSize; i++) {
                AdaptableItem item = items.get(i);
                if (item instanceof ArtworkView && ((ArtworkView) item).isSelected()) {
                    previouslySelectedItem = i;
                    break;
                }
            }

            if (previouslySelectedItem == -1 || previouslySelectedItem == position) {
                artworkView.setSelected(!artworkView.isSelected());
                notifyItemChanged(position);
            } else {
                artworkView.setSelected(true);
                ((ArtworkView) items.get(previouslySelectedItem)).setSelected(false);
                notifyItemChanged(previouslySelectedItem);
                notifyItemChanged(position);
            }

            Stream.of(items)
                    .filter(item -> item instanceof ArtworkView && ((ArtworkView) item).isSelected())
                    .forEach(item -> checkedItem = (ArtworkModel) item.getItem());

            notifyDataSetChanged();
        }
    }
}