package com.simplecity.amp_library.ui.views;

import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.tagger.TaggerDialog;
import com.simplecity.amp_library.ui.dialog.DeleteDialog;
import com.simplecity.amp_library.ui.modelviews.SongView;
import com.simplecityapps.recycler_adapter.model.ViewModel;

import java.util.List;

public interface QueueView {

    void loadData(List<ViewModel> items, int position);

    void updateQueuePosition(int position, boolean fromUser);

    void showToast(String message, int duration);

    void startDrag(SongView.ViewHolder holder);

    void showTaggerDialog(TaggerDialog taggerDialog);

    void showDeleteDialog(DeleteDialog deleteDialog);

    void removeFromQueue(int position);

    void removeFromQueue(List<Song> songs);

    void moveQueueItem(int from, int to);

    void showUpgradeDialog();
}
