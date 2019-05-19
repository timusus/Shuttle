package com.simplecity.amp_library.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.XmlRes;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.aesthetic.Rx;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.color.ColorChooserDialog;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.billing.BillingManager;
import com.simplecity.amp_library.model.InclExclItem;
import com.simplecity.amp_library.ui.dialog.ChangelogDialog;
import com.simplecity.amp_library.ui.dialog.InclExclDialog;
import com.simplecity.amp_library.ui.dialog.UpgradeDialog;
import com.simplecity.amp_library.ui.screens.drawer.DrawerLockManager;
import com.simplecity.amp_library.ui.screens.drawer.MiniPlayerLockManager;
import com.simplecity.amp_library.utils.AnalyticsManager;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ShuttleUtils;
import dagger.android.support.AndroidSupportInjection;
import io.reactivex.disposables.Disposable;
import javax.inject.Inject;
import test.com.androidnavigation.base.Controller;
import test.com.androidnavigation.base.NavigationController;
import test.com.androidnavigation.fragment.BaseController;
import test.com.androidnavigation.fragment.BaseNavigationController;
import test.com.androidnavigation.fragment.FragmentInfo;

public class SettingsParentFragment extends BaseNavigationController implements
        DrawerLockManager.DrawerLock,
        MiniPlayerLockManager.MiniPlayerLock {

    public static String ARG_PREFERENCE_RESOURCE = "preference_resource";
    public static String ARG_TITLE = "title";

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @XmlRes
    int preferenceResource;

    @StringRes
    int titleResId;

    private Unbinder unbinder;

    public static SettingsParentFragment newInstance(@XmlRes int preferenceResource, @StringRes int titleResId) {
        Bundle args = new Bundle();
        args.putInt(ARG_PREFERENCE_RESOURCE, preferenceResource);
        args.putInt(ARG_TITLE, titleResId);
        SettingsParentFragment fragment = new SettingsParentFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public SettingsParentFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        titleResId = getArguments().getInt(ARG_TITLE);
        preferenceResource = getArguments().getInt(ARG_PREFERENCE_RESOURCE);
    }

    @Override
    public FragmentInfo getRootViewControllerInfo() {
        return SettingsFragment.getFragmentInfo(preferenceResource);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_settings, container, false);

        unbinder = ButterKnife.bind(this, rootView);

        toolbar.setTitle(titleResId);
        toolbar.setNavigationOnClickListener(v -> getActivity().onBackPressed());

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        DrawerLockManager.getInstance().addDrawerLock(this);
        MiniPlayerLockManager.getInstance().addMiniPlayerLock(this);
    }

    @Override
    public void onPause() {
        DrawerLockManager.getInstance().removeDrawerLock(this);
        MiniPlayerLockManager.getInstance().removeMiniPlayerLock(this);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        unbinder.unbind();
        super.onDestroyView();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements
            Controller,
            SupportView,
            SettingsView,
            ColorChooserDialog.ColorCallback {

        @XmlRes
        int preferenceResource;

        @Inject
        SupportPresenter supportPresenter;

        @Inject
        SettingsPresenter settingsPresenter;

        @Inject
        BillingManager billingManager;

        @Inject
        AnalyticsManager analyticsManager;

        @Inject
        SettingsManager settingsManager;

        private ColorChooserDialog primaryColorDialog;
        private ColorChooserDialog accentColorDialog;

        private Disposable aestheticDisposable;

        public static FragmentInfo getFragmentInfo(@XmlRes int preferenceResource) {
            Bundle args = new Bundle();
            args.putInt(ARG_PREFERENCE_RESOURCE, preferenceResource);
            return new FragmentInfo(SettingsFragment.class, args, "settingsRoot");
        }

        public static SettingsFragment newInstance(@XmlRes int preferenceResource) {
            Bundle args = new Bundle();
            args.putInt(ARG_PREFERENCE_RESOURCE, preferenceResource);
            SettingsFragment settingsFragment = new SettingsFragment();
            settingsFragment.setArguments(args);
            return settingsFragment;
        }

        public SettingsFragment() {
        }

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);

            preferenceResource = getArguments().getInt(ARG_PREFERENCE_RESOURCE);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(preferenceResource);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            AndroidSupportInjection.inject(this);
            super.onCreate(savedInstanceState);

            // Support Preferences

            Preference changelogPreference = findPreference(SettingsManager.KEY_PREF_CHANGELOG);
            if (changelogPreference != null) {
                changelogPreference.setOnPreferenceClickListener(preference -> {
                    settingsPresenter.changelogClicked();
                    return true;
                });
            }

            Preference faqPreference = findPreference(SettingsManager.KEY_PREF_FAQ);
            if (faqPreference != null) {
                faqPreference.setOnPreferenceClickListener(preference -> {
                    supportPresenter.faqClicked();
                    return true;
                });
            }

            Preference helpPreference = findPreference(SettingsManager.KEY_PREF_HELP);
            if (helpPreference != null) {
                helpPreference.setOnPreferenceClickListener(preference -> {
                    supportPresenter.helpClicked();
                    return true;
                });
            }

            Preference ratePreference = findPreference(SettingsManager.KEY_PREF_RATE);
            if (ratePreference != null) {
                ratePreference.setOnPreferenceClickListener(preference -> {
                    supportPresenter.rateClicked();
                    return true;
                });
            }

            Preference restorePurchasesPreference = findPreference(SettingsManager.KEY_PREF_RESTORE_PURCHASES);
            if (restorePurchasesPreference != null) {
                if (ShuttleUtils.isAmazonBuild() || ShuttleUtils.isUpgraded((ShuttleApplication) getContext().getApplicationContext(), settingsManager)) {
                    restorePurchasesPreference.setVisible(false);
                }
                restorePurchasesPreference.setOnPreferenceClickListener(preference -> {
                    settingsPresenter.restorePurchasesClicked();
                    return true;
                });
            }

            // Display

            Preference chooseTabsPreference = findPreference(SettingsManager.KEY_PREF_TAB_CHOOSER);
            if (chooseTabsPreference != null) {
                chooseTabsPreference.setOnPreferenceClickListener(preference -> {
                    settingsPresenter.chooseTabsClicked();
                    return true;
                });
            }

            Preference defaultPagePreference = findPreference(SettingsManager.KEY_PREF_DEFAULT_PAGE);
            if (defaultPagePreference != null) {
                defaultPagePreference.setOnPreferenceClickListener(preference -> {
                    settingsPresenter.chooseDefaultPageClicked(getContext());
                    return true;
                });
            }

            // Themes

            Preference baseThemePreference = findPreference(SettingsManager.KEY_PREF_THEME_BASE);
            if (baseThemePreference != null) {
                baseThemePreference.setOnPreferenceClickListener(preference -> {
                    settingsPresenter.baseThemeClicked(getContext());
                    return true;
                });
            }

            Preference primaryColorPreference = findPreference(SettingsManager.KEY_PREF_PRIMARY_COLOR);
            if (primaryColorPreference != null) {
                primaryColorPreference.setOnPreferenceClickListener(preference -> {
                    settingsPresenter.primaryColorClicked(getContext());
                    return true;
                });
            }

            Preference accentColorColorPreference = findPreference(SettingsManager.KEY_PREF_ACCENT_COLOR);
            if (accentColorColorPreference != null) {
                accentColorColorPreference.setOnPreferenceClickListener(preference -> {
                    settingsPresenter.accentColorClicked(getContext());
                    return true;
                });
            }

            SwitchPreferenceCompat tintNavBarColorPreference = (SwitchPreferenceCompat) findPreference(SettingsManager.KEY_PREF_NAV_BAR);
            if (tintNavBarColorPreference != null) {
                tintNavBarColorPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                    settingsPresenter.tintNavBarClicked(getContext(), (Boolean) newValue);
                    return true;
                });
            }

            SwitchPreferenceCompat usePalettePreference = (SwitchPreferenceCompat) findPreference(SettingsManager.KEY_PREF_PALETTE);
            if (usePalettePreference != null) {
                usePalettePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                    settingsPresenter.usePaletteClicked(getContext(), (Boolean) newValue);
                    return true;
                });
            }

            SwitchPreferenceCompat usePaletteNowPlayingOnlyPreference = (SwitchPreferenceCompat) findPreference(SettingsManager.KEY_PREF_PALETTE_NOW_PLAYING_ONLY);
            if (usePaletteNowPlayingOnlyPreference != null) {
                usePaletteNowPlayingOnlyPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                    settingsPresenter.usePaletteNowPlayingOnlyClicked(getContext(), (Boolean) newValue);
                    return true;
                });
            }

            // Artwork

            Preference downloadArtworkPreference = findPreference(SettingsManager.KEY_PREF_DOWNLOAD_ARTWORK);
            if (downloadArtworkPreference != null) {
                downloadArtworkPreference.setOnPreferenceClickListener(preference -> {
                    settingsPresenter.downloadArtworkClicked(getContext());
                    return true;
                });
            }

            Preference deleteArtworkPreference = findPreference(SettingsManager.KEY_PREF_DELETE_ARTWORK);
            if (deleteArtworkPreference != null) {
                deleteArtworkPreference.setOnPreferenceClickListener(preference -> {
                    settingsPresenter.deleteArtworkClicked(getContext());
                    return true;
                });
            }

            SwitchPreferenceCompat ignoreEmbeddedArtworkPreference = (SwitchPreferenceCompat) findPreference(SettingsManager.KEY_IGNORE_EMBEDDED_ARTWORK);
            if (ignoreEmbeddedArtworkPreference != null) {
                ignoreEmbeddedArtworkPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                    settingsPresenter.changeArtworkPreferenceClicked(getContext());
                    return true;
                });
            }

            SwitchPreferenceCompat ignoreFolderArtworkPreference = (SwitchPreferenceCompat) findPreference(SettingsManager.KEY_IGNORE_FOLDER_ARTWORK);
            if (ignoreFolderArtworkPreference != null) {
                ignoreFolderArtworkPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                    settingsPresenter.changeArtworkPreferenceClicked(getContext());
                    return true;
                });
            }

            SwitchPreferenceCompat preferEmbeddedArtworkPreference = (SwitchPreferenceCompat) findPreference(SettingsManager.KEY_PREFER_EMBEDDED_ARTWORK);
            if (preferEmbeddedArtworkPreference != null) {
                preferEmbeddedArtworkPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                    settingsPresenter.changeArtworkPreferenceClicked(getContext());
                    return true;
                });
            }

            SwitchPreferenceCompat ignoreMediaStoreArtworkPreference = (SwitchPreferenceCompat) findPreference(SettingsManager.KEY_IGNORE_MEDIASTORE_ART);
            if (ignoreMediaStoreArtworkPreference != null) {
                ignoreMediaStoreArtworkPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                    settingsPresenter.changeArtworkPreferenceClicked(getContext());
                    return true;
                });
            }

            // Headset/Bluetooth

            // Scrobbling

            Preference downloadScrobblerPreference = findPreference(SettingsManager.KEY_PREF_DOWNLOAD_SCROBBLER);
            if (downloadScrobblerPreference != null) {
                if (ShuttleUtils.isAmazonBuild()) {
                    // Amazon don't allow links to the Play Store
                    downloadScrobblerPreference.setVisible(false);
                }
                downloadScrobblerPreference.setOnPreferenceClickListener(preference -> {
                    settingsPresenter.downloadScrobblerClicked();
                    return true;
                });
            }

            // Whitelist/Blacklist

            Preference viewBlacklistPreference = findPreference(SettingsManager.KEY_PREF_BLACKLIST);
            if (viewBlacklistPreference != null) {
                viewBlacklistPreference.setOnPreferenceClickListener(preference -> {
                    settingsPresenter.viewBlacklistClicked();
                    return true;
                });
            }

            Preference viewWhitelistPreference = findPreference(SettingsManager.KEY_PREF_WHITELIST);
            if (viewWhitelistPreference != null) {
                viewWhitelistPreference.setOnPreferenceClickListener(preference -> {
                    settingsPresenter.viewWhitelistClicked();
                    return true;
                });
            }

            // Upgrade preference
            Preference upgradePreference = findPreference(SettingsManager.KEY_PREF_UPGRADE);
            if (upgradePreference != null) {
                if (ShuttleUtils.isUpgraded((ShuttleApplication) getContext().getApplicationContext(), settingsManager)) {
                    upgradePreference.setVisible(false);
                }
            }
        }

        @Override
        public void onResume() {
            super.onResume();

            supportPresenter.bindView(this);
            settingsPresenter.bindView(this);

            aestheticDisposable = Aesthetic.get(getContext()).colorAccent()
                    .compose(Rx.distinctToMainThread())
                    .subscribe(this::invalidateColors);
        }

        @Override
        public void onPause() {
            supportPresenter.unbindView(this);
            settingsPresenter.unbindView(this);

            aestheticDisposable.dispose();

            super.onPause();
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            if (preference.getKey() != null) {
                switch (preference.getKey()) {
                    case "pref_display":
                        getNavigationController().pushViewController(SettingsFragment.newInstance(R.xml.settings_display), "DisplaySettings");
                        break;
                    case "pref_themes":
                        getNavigationController().pushViewController(SettingsFragment.newInstance(R.xml.settings_themes), "ThemeSettings");
                        break;
                    case "pref_artwork":
                        getNavigationController().pushViewController(SettingsFragment.newInstance(R.xml.settings_artwork), "ArtworkSettings");
                        break;
                    case "pref_playback":
                        getNavigationController().pushViewController(SettingsFragment.newInstance(R.xml.settings_playback), "PlaybackSettings");
                        break;
                    case "pref_headset":
                        getNavigationController().pushViewController(SettingsFragment.newInstance(R.xml.settings_headset), "HeadsetSettings");
                        break;
                    case "pref_scrobbling":
                        getNavigationController().pushViewController(SettingsFragment.newInstance(R.xml.settings_scrobbling), "ScrobblingSettings");
                        break;
                    case "pref_blacklist":
                        getNavigationController().pushViewController(SettingsFragment.newInstance(R.xml.settings_blacklist), "BlacklistSettings");
                        break;
                    case "pref_upgrade":
                        settingsPresenter.upgradeClicked();
                        break;
                }
            }
            return true;
        }

        void invalidateColors(int color) {
            int preferenceCount = getPreferenceScreen().getPreferenceCount();
            for (int i = 0; i < preferenceCount; i++) {
                tintPreferenceIcon(getPreferenceScreen().getPreference(i), color);
            }
        }

        void tintPreferenceIcon(Preference preference, int color) {
            if (preference != null) {
                Drawable icon = preference.getIcon();
                if (icon != null) {
                    icon = DrawableCompat.wrap(icon);
                    DrawableCompat.setTint(icon, color);
                    preference.setIcon(icon);
                }
            }
        }

        @Override
        public void onColorSelection(@NonNull ColorChooserDialog dialog, int selectedColor) {
            if (dialog == primaryColorDialog) {
                settingsPresenter.changePrimaryColor(getContext(), selectedColor);
            } else if (dialog == accentColorDialog) {
                settingsPresenter.changeAccentColor(getContext(), selectedColor);
            }
        }

        @Override
        public void onColorChooserDismissed(@NonNull ColorChooserDialog dialog) {

        }

        // Support View

        @Override
        public void setVersion(String version) {
            final Preference versionPreference = findPreference("pref_version");
            if (versionPreference != null) {
                versionPreference.setSummary(version);
            }
        }

        @Override
        public void showFaq(Intent intent) {
            startActivity(intent);
        }

        @Override
        public void showHelp(Intent intent) {
            startActivity(intent);
        }

        @Override
        public void showRate(Intent intent) {
            startActivity(intent);
        }

        @NonNull
        @Override
        public NavigationController<Fragment> getNavigationController() {
            return BaseController.findNavigationController(this);
        }

        // Settings View

        // Support

        @Override
        public void showChangelog() {
            ChangelogDialog.Companion.newInstance().show(getChildFragmentManager());
        }

        @Override
        public void showUpgradeDialog() {
            new UpgradeDialog().show(getChildFragmentManager());
        }

        @Override
        public void showRestorePurchasesMessage(int messageResId) {
            Toast.makeText(getContext(), messageResId, Toast.LENGTH_LONG).show();
        }

        @Override
        public void showTabChooserDialog() {
            new TabChooserDialog().show(getChildFragmentManager());
        }

        @Override
        public void showDefaultPageDialog(MaterialDialog dialog) {
            dialog.show();
        }

        // Themes

        @Override
        public void showBaseThemeDialog(MaterialDialog dialog) {
            dialog.show();
        }

        @Override
        public void showPrimaryColorDialog(ColorChooserDialog dialog) {
            primaryColorDialog = dialog.show(getChildFragmentManager());
        }

        @Override
        public void showAccentColorDialog(ColorChooserDialog dialog) {
            accentColorDialog = dialog.show(getChildFragmentManager());
        }

        // Artwork

        @Override
        public void showDownloadArtworkDialog(MaterialDialog dialog) {
            dialog.show();
        }

        @Override
        public void showDeleteArtworkDialog(MaterialDialog dialog) {
            dialog.show();
        }

        @Override
        public void showArtworkPreferenceChangeDialog(MaterialDialog dialog) {
            dialog.show();
        }

        // Scrobbling

        @Override
        public void launchDownloadScrobblerIntent(Intent intent) {
            startActivity(intent);
        }

        // Blacklist/Whitelist

        @Override
        public void showBlacklistDialog() {
            InclExclDialog.Companion.newInstance(InclExclItem.Type.EXCLUDE).show(getChildFragmentManager());
        }

        @Override
        public void showWhitelistDialog() {
            InclExclDialog.Companion.newInstance(InclExclItem.Type.INCLUDE).show(getChildFragmentManager());
        }
    }
}