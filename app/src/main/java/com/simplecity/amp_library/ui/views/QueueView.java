package com.simplecity.amp_library.ui.views;

import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.tagger.TaggerDialog;
import com.simplecity.amp_library.ui.dialog.DeleteDialog;
import java.util.List;

public interface QueueView {

    void setData(List<Song> items, int position);

    void updateQueuePosition(int position, boolean fromUser);

    void showToast(String message, int duration);

    void showTaggerDialog(TaggerDialog taggerDialog);

    void showDeleteDialog(DeleteDialog deleteDialog);

    void onRemovedFromQueue(int position);

    void showUpgradeDialog();
}
