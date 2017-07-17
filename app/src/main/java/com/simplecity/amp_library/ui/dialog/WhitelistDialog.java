package com.simplecity.amp_library.ui.dialog;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.annimon.stream.Stream;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.sql.databases.WhitelistHelper;
import com.simplecity.amp_library.ui.modelviews.EmptyView;
import com.simplecity.amp_library.ui.modelviews.WhitelistView;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecityapps.recycler_adapter.adapter.ViewModelAdapter;
import com.simplecityapps.recycler_adapter.model.ViewModel;

import io.reactivex.android.schedulers.AndroidSchedulers;

public class WhitelistDialog {

    private static final String TAG = "WhitelistDialog";

    private WhitelistDialog() {
    }

    public static MaterialDialog getDialog(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_blacklist, null);

        final MaterialDialog.Builder builder = new MaterialDialog.Builder(context)
                .title(R.string.whitelist_title)
                .customView(view, false)
                .positiveText(R.string.close)
                .negativeText(R.string.pref_title_clear_whitelist)
                .onNegative((materialDialog, dialogAction) -> {
                    WhitelistHelper.deleteAllFolders();
                    Toast.makeText(context, R.string.whitelist_deleted, Toast.LENGTH_SHORT).show();
                });

        final MaterialDialog dialog = builder.build();

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        final ViewModelAdapter whitelistAdapter = new ViewModelAdapter();

        recyclerView.setAdapter(whitelistAdapter);

        WhitelistView.ClickListener listener = whiteListView -> {
            WhitelistHelper.deleteFolder(whiteListView.whitelistFolder);
            if (whitelistAdapter.items.size() == 0) {
                dialog.dismiss();
            }
        };

        WhitelistHelper.getWhitelistFolders()
                .map(whitelistFolders -> Stream.of(whitelistFolders)
                        .map(folder -> {
                            WhitelistView whitelistView = new WhitelistView(folder);
                            whitelistView.setClickListener(listener);
                            return (ViewModel) whitelistView;
                        })
                        .toList())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(whitelistViews -> {
                    if (whitelistViews.size() == 0) {
                        whitelistAdapter.addItem(0, new EmptyView(R.string.whitelist_empty));
                    } else {
                        whitelistAdapter.setItems(whitelistViews);
                    }
                }, error -> LogUtils.logException(TAG, "Error setting whitelist items", error));

        return dialog;
    }
}