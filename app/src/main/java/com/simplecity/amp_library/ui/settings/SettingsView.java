package com.simplecity.amp_library.ui.settings;

import android.content.Intent;
import android.support.annotation.StringRes;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.color.ColorChooserDialog;

public interface SettingsView {

    // Support

    void showChangelog(MaterialDialog dialog);

    void openStoreLink(Intent intent);

    void showUpgradeDialog(MaterialDialog dialog);

    void showUpgradeSuccessDialog(MaterialDialog dialog);

    void showUpgradeFailedToast(@StringRes int messageResId);

    void showRestorePurchasesMessage(@StringRes int messageResId);

    // Display

    void showTabChooserDialog(MaterialDialog dialog);

    void showDefaultPageDialog(MaterialDialog dialog);

    // Themes

    void showBaseThemeDialog(MaterialDialog dialog);

    void showPrimaryColorDialog(ColorChooserDialog dialog);

    void showAccentColorDialog(ColorChooserDialog dialog);

    // Artwork

    void showDownloadArtworkDialog(MaterialDialog dialog);

    void showDeleteArtworkDialog(MaterialDialog dialog);

    void showArtworkPreferenceChangeDialog(MaterialDialog dialog);

    // Scrobbling
    void launchDownloadScrobblerIntent(Intent intent);

    // Blacklist/Whitelist

    void showBlacklistDialog(MaterialDialog dialog);

    void showWhitelistDialog(MaterialDialog dialog);
}
