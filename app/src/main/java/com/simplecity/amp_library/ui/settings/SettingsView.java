package com.simplecity.amp_library.ui.settings;

import android.content.Intent;
import android.support.annotation.StringRes;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.color.ColorChooserDialog;
import com.simplecity.amp_library.ui.views.PurchaseView;

public interface SettingsView extends PurchaseView {

    // Support

    void showChangelog();

    void showRestorePurchasesMessage(@StringRes int messageResId);

    // Display

    void showTabChooserDialog();

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

    void showBlacklistDialog();

    void showWhitelistDialog();
}
