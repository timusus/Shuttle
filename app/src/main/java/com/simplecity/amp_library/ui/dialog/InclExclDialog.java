package com.simplecity.amp_library.ui.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.annimon.stream.Stream;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.InclExclItem;
import com.simplecity.amp_library.sql.databases.InclExclHelper;
import com.simplecity.amp_library.ui.modelviews.EmptyView;
import com.simplecity.amp_library.ui.modelviews.InclExclView;
import com.simplecity.amp_library.utils.DataManager;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecityapps.recycler_adapter.adapter.ViewModelAdapter;
import com.simplecityapps.recycler_adapter.model.ViewModel;

import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class InclExclDialog {

    private static final String TAG = "InclExclDialog";

    private InclExclDialog() {
    }

    public static MaterialDialog getDialog(Context context, @InclExclItem.Type int type) {

        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_incl_excl, null);

        final MaterialDialog.Builder builder = new MaterialDialog.Builder(context)
                .title(getTitleResId(type))
                .customView(view, false)
                .positiveText(R.string.close)
                .negativeText(R.string.pref_title_clear_whitelist)
                .onNegative((materialDialog, dialogAction) -> {
                    InclExclHelper.deleteAllItems(type);
                    Toast.makeText(context, getItemsDeletedResId(type), Toast.LENGTH_SHORT).show();
                });

        final MaterialDialog dialog = builder.build();

        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        final ViewModelAdapter inclExclAdapter = new ViewModelAdapter();

        recyclerView.setAdapter(inclExclAdapter);

        InclExclView.ClickListener listener = inclExclView -> {
            InclExclHelper.deleteInclExclItem(inclExclView.inclExclItem);
            if (inclExclAdapter.items.size() == 0) {
                dialog.dismiss();
            }
        };

        getItems(type, listener)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(inclExclViews -> {
                    if (inclExclViews.size() == 0) {
                        inclExclAdapter.setItems(Collections.singletonList(new EmptyView(getItemsEmptyResId(type))));
                    } else {
                        inclExclAdapter.setItems(inclExclViews);
                    }
                }, error -> LogUtils.logException(TAG, "Error setting incl/excl items", error));

        return dialog;
    }

    private static int getTitleResId(@InclExclItem.Type int type) {
        return type == InclExclItem.Type.INCLUDE ? R.string.whitelist_title : R.string.blacklist_title;
    }

    private static int getItemsEmptyResId(@InclExclItem.Type int type) {
        return type == InclExclItem.Type.INCLUDE ? R.string.whitelist_empty : R.string.blacklist_empty;
    }

    private static int getItemsDeletedResId(@InclExclItem.Type int type) {
        return type == InclExclItem.Type.INCLUDE ? R.string.whitelist_deleted : R.string.blacklist_deleted;
    }

    private static Observable<List<ViewModel>> getItems(@InclExclItem.Type int type, InclExclView.ClickListener listener) {

        Observable<List<InclExclItem>> items = type == InclExclItem.Type.INCLUDE ? DataManager.getInstance().getIncludeItems() : DataManager.getInstance().getExcludeItems();

        return items.map(inclExclItems -> Stream.of(inclExclItems)
                .map(inclExclItem -> {
                    InclExclView inclExclView = new InclExclView(inclExclItem);
                    inclExclView.setClickListener(listener);
                    return (ViewModel) inclExclView;
                })
                .toList());
    }
}