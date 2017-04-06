package com.simplecity.amp_library.search;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.afollestad.materialdialogs.MaterialDialog;
import com.simplecity.amp_library.model.AdaptableItem;
import com.simplecity.amp_library.tagger.TaggerDialog;

import java.util.List;

import rx.Subscription;

public interface SearchView {

    void setLoading(boolean loading);

    void setEmpty(boolean empty);

    Subscription setItems(@NonNull List<AdaptableItem> items);

    void setFilterFuzzyChecked(boolean checked);

    void setFilterArtistsChecked(boolean checked);

    void setFilterAlbumsChecked(boolean checked);

    void showEmptyPlaylistToast();

    void showTaggerDialog(@NonNull TaggerDialog taggerDialog);

    void showDeleteDialog(@NonNull MaterialDialog deleteDialog);

    void finish(int resultCode, @Nullable Intent data);

    void finish();
}