package com.simplecity.amp_library.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import com.simplecity.amp_library.BuildConfig;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.ui.common.Presenter;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ShuttleUtils;
import javax.inject.Inject;

public class SupportPresenter extends Presenter<SupportView> {

    private ShuttleApplication application;

    private SettingsManager settingsManager;

    @Inject
    public SupportPresenter(ShuttleApplication application, SettingsManager settingsManager) {
        this.application = application;
        this.settingsManager = settingsManager;
    }

    @Override
    public void bindView(@NonNull SupportView view) {
        super.bindView(view);

        setAppVersion();
    }

    private void setAppVersion() {
        SupportView supportView = getView();
        if (supportView != null) {
            supportView.setVersion("Shuttle Music Player " + BuildConfig.VERSION_NAME + (ShuttleUtils.isUpgraded(application, settingsManager) ? " (Upgraded)" : " (Free)"));
        }
    }

    public void faqClicked() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("http://www.shuttlemusicplayer.com/#faq"));
        SupportView supportView = getView();
        if (supportView != null) {
            supportView.showFaq(intent);
        }
    }

    public void helpClicked() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://discordapp.com/channels/499448243491569673"));
        SupportView supportView = getView();
        if (supportView != null) {
            supportView.showHelp(intent);
        }
    }

    public void rateClicked() {

        settingsManager.setHasRated();

        SupportView supportView = getView();
        if (supportView != null) {
            Intent intent = ShuttleUtils.getShuttleStoreIntent(application.getPackageName());
            supportView.showRate(intent);
        }
    }
}