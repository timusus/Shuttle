package com.simplecity.amp_library.ui.presenters;

import android.app.Activity;

import com.simplecity.amp_library.ui.dialog.UpgradeDialog;
import com.simplecity.amp_library.ui.views.PurchaseView;

public class PurchasePresenter<V extends PurchaseView> extends Presenter<V> {

    private Activity activity;

    public PurchasePresenter(Activity activity) {
        this.activity = activity;
    }

    public void upgradeClicked() {
        PurchaseView purchaseView = getView();
        if (purchaseView != null) {
            purchaseView.showUpgradeDialog(UpgradeDialog.getUpgradeDialog(activity));
        }
    }
}