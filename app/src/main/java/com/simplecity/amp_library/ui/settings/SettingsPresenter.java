package com.simplecity.amp_library.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.color.ColorChooserDialog;
import com.annimon.stream.IntPair;
import com.annimon.stream.Stream;
import com.bumptech.glide.Glide;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.billing.BillingManager;
import com.simplecity.amp_library.model.CategoryItem;
import com.simplecity.amp_library.services.ArtworkDownloadService;
import com.simplecity.amp_library.ui.common.PurchasePresenter;
import com.simplecity.amp_library.utils.AnalyticsManager;
import com.simplecity.amp_library.utils.ColorPalette;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ShuttleUtils;
import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;
import java.util.List;
import javax.inject.Inject;

public class SettingsPresenter extends PurchasePresenter<SettingsView> {

    private ShuttleApplication application;
    private BillingManager billingManager;
    private AnalyticsManager analyticsManager;
    private SettingsManager settingsManager;

    @Inject
    public SettingsPresenter(
            ShuttleApplication application,
            BillingManager billingManager,
            AnalyticsManager analyticsManager,
            SettingsManager settingsManager
    ) {

        super();

        this.application = application;
        this.billingManager = billingManager;
        this.analyticsManager = analyticsManager;
        this.settingsManager = settingsManager;
    }

    // Support Preferences

    void changelogClicked() {
        analyticsManager.logChangelogViewed();

        SettingsView settingsView = getView();
        if (settingsView != null) {
            settingsView.showChangelog();
        }
    }

    public void restorePurchasesClicked() {
        billingManager.restorePurchases();
    }

    // Display

    public void chooseTabsClicked() {
        SettingsView settingsView = getView();
        if (settingsView != null) {
            settingsView.showTabChooserDialog();
        }
    }

    public void chooseDefaultPageClicked(Context context) {
        SettingsView settingsView = getView();
        if (settingsView != null) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            List<CategoryItem> categoryItems = Stream.of(CategoryItem.getCategoryItems(sharedPreferences))
                    .filter(categoryItem -> categoryItem.isChecked)
                    .toList();

            int defaultPageType = settingsManager.getDefaultPageType();
            int defaultPage = Math.min(Stream.of(categoryItems)
                    .indexed()
                    .filter(categoryItemIntPair -> categoryItemIntPair.getSecond().type == defaultPageType)
                    .map(IntPair::getFirst)
                    .findFirst()
                    .orElse(1), categoryItems.size());

            settingsView.showDefaultPageDialog(
                    new MaterialDialog.Builder(context)
                            .title(R.string.pref_title_default_page)
                            .items(Stream.of(categoryItems)
                                    .map(categoryItem -> context.getString(categoryItem.getTitleResId()))
                                    .toList())
                            .itemsCallbackSingleChoice(defaultPage, (dialog, itemView, which, text) -> {
                                settingsManager.setDefaultPageType(categoryItems.get(which).type);
                                return false;
                            })
                            .build());
        }
    }

    // Themes

    public void baseThemeClicked(Context context) {
        SettingsView settingsView = getView();
        if (settingsView != null) {
            settingsView.showBaseThemeDialog(new MaterialDialog.Builder(context)
                    .title(R.string.pref_title_base_theme)
                    .items(R.array.baseThemeArray)
                    .itemsCallback((materialDialog, view, i, charSequence) -> changeBaseTheme(context, i))
                    .build());
        }
    }

    private void changeBaseTheme(Context context, int i) {
        int theme = R.style.AppTheme_Light;
        boolean isDark = false;
        switch (i) {
            case 0:
                //Light
                theme = R.style.AppTheme_Light;
                isDark = false;
                break;
            case 1:
                //Dark
                theme = R.style.AppTheme;
                isDark = true;
                break;
            case 2:
                //Black
                theme = R.style.AppTheme_Black;
                isDark = true;
                break;
        }

        Aesthetic.get(context)
                .activityTheme(theme)
                .isDark(isDark)
                .apply();
    }

    public void primaryColorClicked(Context context) {
        SettingsView settingsView = getView();
        if (settingsView != null) {
            settingsView.showPrimaryColorDialog(
                    new ColorChooserDialog.Builder(context, R.string.pref_title_theme_pick_color)
                            .customColors(ColorPalette.getPrimaryColors(context, settingsManager), ColorPalette.getPrimaryColorsSub(context, settingsManager))
                            .allowUserColorInput(ShuttleUtils.isUpgraded(application, settingsManager))
                            .allowUserColorInputAlpha(false)
                            .dynamicButtonColor(false)
                            .preselect(Aesthetic.get(context).colorPrimary().blockingFirst())
                            .build());
        }
    }

    public void changePrimaryColor(Context context, int color) {
        Aesthetic.get(context)
                .colorPrimary(color)
                .colorStatusBarAuto()
                .apply();

        settingsManager.storePrimaryColor(color);
    }

    public void accentColorClicked(Context context) {
        SettingsView settingsView = getView();
        if (settingsView != null) {
            settingsView.showAccentColorDialog(
                    new ColorChooserDialog.Builder(context, R.string.pref_title_theme_pick_accent_color)
                            .accentMode(true)
                            .allowUserColorInput(true)
                            .allowUserColorInputAlpha(false)
                            .dynamicButtonColor(false)
                            .preselect(Aesthetic.get(context).colorAccent().blockingFirst())
                            .build());
        }
    }

    public void changeAccentColor(Context context, int color) {
        Aesthetic.get(context)
                .colorAccent(color)
                .apply();

        settingsManager.storeAccentColor(color);
    }

    public void tintNavBarClicked(Context context, boolean tintNavBar) {
        Aesthetic.get(context)
                .colorNavigationBarAuto(tintNavBar)
                .apply();
    }

    public void usePaletteClicked(Context context, boolean usePalette) {
        // If we're not using palette any more, set the primary color back to default
        if (!usePalette) {
            int storedPrimaryColor = settingsManager.getPrimaryColor();
            int storedAccentColor = settingsManager.getAccentColor();

            Aesthetic.get(context)
                    .colorPrimary(storedPrimaryColor == -1 ? ContextCompat.getColor(context, R.color.md_blue_500) : storedPrimaryColor)
                    .colorAccent(storedAccentColor == -1 ? ContextCompat.getColor(context, R.color.md_amber_300) : storedAccentColor)
                    .colorStatusBarAuto()
                    .colorNavigationBarAuto(settingsManager.getTintNavBar())
                    .apply();
        }
    }

    public void usePaletteNowPlayingOnlyClicked(Context context, boolean usePaletteNowPlayingOnly) {
        // If we're only using palette for 'now playing', set the primary color back to default
        if (usePaletteNowPlayingOnly) {
            int storedPrimaryColor = settingsManager.getPrimaryColor();
            int storedAccentColor = settingsManager.getAccentColor();

            Aesthetic.get(context)
                    .colorPrimary(storedPrimaryColor == -1 ? ContextCompat.getColor(context, R.color.md_blue_500) : storedPrimaryColor)
                    .colorAccent(storedAccentColor == -1 ? ContextCompat.getColor(context, R.color.md_amber_300) : storedAccentColor)
                    .colorStatusBarAuto()
                    .colorNavigationBarAuto(settingsManager.getTintNavBar())
                    .apply();
        }
    }

    // Artwork

    public void downloadArtworkClicked(Context context) {
        SettingsView settingsView = getView();
        if (settingsView != null) {
            settingsView.showDownloadArtworkDialog(
                    new MaterialDialog.Builder(context)
                            .title(R.string.pref_title_download_artwork)
                            .content(R.string.pref_warning_download_artwork)
                            .positiveText(R.string.download)
                            .onPositive((dialog, which) -> downloadArtwork(context))
                            .negativeText(R.string.cancel)
                            .build());
        }
    }

    private void downloadArtwork(Context context) {
        Intent intent = new Intent(context, ArtworkDownloadService.class);
        context.startService(intent);
    }

    public void deleteArtworkClicked(Context context) {
        SettingsView settingsView = getView();
        if (settingsView != null) {
            settingsView.showDeleteArtworkDialog(
                    new MaterialDialog.Builder(context)
                            .title(R.string.pref_title_delete_artwork)
                            .iconRes(R.drawable.ic_warning_24dp)
                            .content(R.string.delete_artwork_confirmation_dialog)
                            .positiveText(R.string.button_ok)
                            .onPositive((materialDialog, dialogAction) -> deleteArtwork())
                            .negativeText(R.string.cancel)
                            .build());
        }
    }

    private void deleteArtwork() {
        //Clear Glide' mem & disk cache

        Glide.get(application).clearMemory();

        Completable.fromAction(() -> Glide.get(application).clearDiskCache())
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    public void changeArtworkPreferenceClicked(Context context) {
        SettingsView settingsView = getView();
        if (settingsView != null) {
            settingsView.showArtworkPreferenceChangeDialog(
                    new MaterialDialog.Builder(context)
                            .title(R.string.pref_title_delete_artwork)
                            .content(R.string.pref_summary_change_artwork_source)
                            .positiveText(R.string.pref_button_remove_artwork)
                            .onPositive((dialog1, which) -> deleteArtwork())
                            .negativeText(R.string.close)
                            .show());
        }
    }

    // Headset/Bluetooth

    // Scrobbling

    public void downloadScrobblerClicked() {
        SettingsView settingsView = getView();
        if (settingsView != null) {
            settingsView.launchDownloadScrobblerIntent(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.adam.aslfms")));
        }
    }

    public void viewBlacklistClicked() {
        SettingsView settingsView = getView();
        if (settingsView != null) {
            settingsView.showBlacklistDialog();
        }
    }

    public void viewWhitelistClicked() {
        SettingsView settingsView = getView();
        if (settingsView != null) {
            settingsView.showWhitelistDialog();
        }
    }
}