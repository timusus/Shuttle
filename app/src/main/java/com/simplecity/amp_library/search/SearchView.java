package com.simplecity.amp_library.search;

import android.support.annotation.NonNull;

import com.afollestad.materialdialogs.MaterialDialog;
import com.simplecity.amp_library.tagger.TaggerDialog;
import com.simplecityapps.recycler_adapter.model.ViewModel;

import java.util.List;

import rx.Subscription;

public interface SearchView {

    void setLoading(boolean loading);

    void setEmpty(boolean empty);

    Subscription setItems(@NonNull List<ViewModel> items);

    void setFilterFuzzyChecked(boolean checked);

    void setFilterArtistsChecked(boolean checked);

    void setFilterAlbumsChecked(boolean checked);

    void showToast(String message);

    void showTaggerDialog(@NonNull TaggerDialog taggerDialog);

    void showDeleteDialog(@NonNull MaterialDialog deleteDialog);
}