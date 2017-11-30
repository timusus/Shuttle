package com.simplecity.amp_library.ui.presenters;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;

import com.simplecity.amp_library.lifecycle.LifecycleProvider;
import com.simplecity.amp_library.lifecycle.LifecycleProviderHelper;
import com.simplecity.amp_library.ui.dialog.UpgradeDialog;
import com.simplecity.amp_library.ui.views.PurchaseView;

public class PurchasePresenter<V extends PurchaseView> extends Presenter<V> {

    private Activity activity;
    protected LifecycleProvider lifecycleProvider;

    public PurchasePresenter(Activity activity) {
        this.activity = activity;
        if (activity instanceof LifecycleProvider) {
            lifecycleProvider = ((LifecycleProvider) activity);
        } else {
            lifecycleProvider = new LifecycleProviderHelper(((AppCompatActivity) activity));
        }
    }

    public void upgradeClicked() {
        PurchaseView purchaseView = getView();
        if (purchaseView != null) {
            purchaseView.showUpgradeDialog(UpgradeDialog.getUpgradeDialog(activity));
        }
    }
}