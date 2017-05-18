package com.simplecity.amp_library.ui.settings;

import android.content.Context;
import android.content.Intent;

import com.afollestad.materialdialogs.MaterialDialog;
import com.simplecity.amp_library.ui.presenters.Presenter;
import com.simplecity.amp_library.utils.AnalyticsManager;
import com.simplecity.amp_library.utils.DialogUtils;
import com.simplecity.amp_library.utils.ShuttleUtils;

import javax.inject.Inject;

public class SettingsPresenter extends Presenter<SettingsView> {

    @Inject
    public SettingsPresenter() {
    }

    void changelogClicked(Context context) {
        AnalyticsManager.logChangelogViewed();

        SettingsView settingsView = getView();
        if (settingsView != null) {
            settingsView.showChangelog(DialogUtils.getChangelogDialog(context));
        }
    }

    void upgradeClicked(Context context) {
        MaterialDialog dialog = DialogUtils.getUpgradeDialog(context,
                (materialDialog, dialogAction) -> {
                    if (ShuttleUtils.isAmazonBuild()) {
                        openPaidStoreLink();
                    } else {
                        purchaseUpgrade();
                    }
                });

        SettingsView settingsView = getView();
        if (settingsView != null) {
            settingsView.showUpgradeDialog(dialog);
        }
    }

    public void openPaidStoreLink() {
        SettingsView settingsView = getView();
        if (settingsView != null) {
            Intent intent = ShuttleUtils.getShuttleStoreIntent("com.simplecity.amp_pro");
            settingsView.openStoreLink(intent);
        }
    }

    public void purchaseUpgrade() {
        AnalyticsManager.logUpgrade(AnalyticsManager.UpgradeType.UPGRADE);

        //Todo: Begin upgrade flow
    }

}