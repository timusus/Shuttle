package com.simplecity.amp_library.ui.common;

import com.simplecity.amp_library.ui.common.Presenter;
import com.simplecity.amp_library.ui.views.PurchaseView;

public class PurchasePresenter<V extends PurchaseView> extends Presenter<V> {

    public void upgradeClicked() {
        PurchaseView purchaseView = getView();
        if (purchaseView != null) {
            purchaseView.showUpgradeDialog();
        }
    }
}