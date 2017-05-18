package com.simplecity.amp_library.ui.settings;

import android.content.Intent;

import com.afollestad.materialdialogs.MaterialDialog;

public interface SettingsView {

    void showChangelog(MaterialDialog dialog);

    void openStoreLink(Intent intent);

    void showUpgradeDialog(MaterialDialog dialog);
}
