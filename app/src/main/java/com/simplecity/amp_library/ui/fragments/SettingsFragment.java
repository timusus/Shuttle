package com.simplecity.amp_library.ui.fragments;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.content.IntentCompat;
import android.support.v4.preference.PreferenceFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.simplecity.amp_library.BuildConfig;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.model.CategoryItem;
import com.simplecity.amp_library.services.ArtworkDownloadService;
import com.simplecity.amp_library.sql.databases.BlacklistHelper;
import com.simplecity.amp_library.sql.databases.WhitelistHelper;
import com.simplecity.amp_library.ui.activities.MainActivity;
import com.simplecity.amp_library.ui.activities.SettingsActivity;
import com.simplecity.amp_library.ui.adapters.TabsAdapter;
import com.simplecity.amp_library.ui.recyclerview.ItemTouchHelperCallback;
import com.simplecity.amp_library.utils.AnalyticsManager;
import com.simplecity.amp_library.utils.ColorPalette;
import com.simplecity.amp_library.utils.DialogUtils;
import com.simplecity.amp_library.utils.DrawableUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.ResourceUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.ThemeUtils;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SettingsFragment extends PreferenceFragment {

    private static final String TAG = "SettingsFragment";

    private static final String PREF_RES_ID = "pref_res_id";
    private int mPrefResId;

    private SharedPreferences mPrefs;
    private SharedPreferences.OnSharedPreferenceChangeListener mListener;
    private SystemBarTintManager mTintManager;

    public SettingsFragment() {

    }

    public static SettingsFragment newInstance(int preferenceResId) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putInt(PREF_RES_ID, preferenceResId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActivity().supportInvalidateOptionsMenu();
        mTintManager = new SystemBarTintManager(getActivity());

        if (getArguments() != null) {
            mPrefResId = getArguments().getInt(PREF_RES_ID);
        } else {
            mPrefResId = R.xml.settings_headers;
        }

        // Load the preferences from an XML resource
        addPreferencesFromResource(mPrefResId);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        final Preference chooseTabsPreference = findPreference("pref_tab_chooser");
        if (chooseTabsPreference != null) {
            chooseTabsPreference.setOnPreferenceClickListener(preference -> {

                RecyclerView recyclerView = (RecyclerView) LayoutInflater.from(getContext()).inflate(R.layout.dialog_tab_chooser, null);
                recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

                TabsAdapter tabsAdapter = new TabsAdapter(getContext());

                ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelperCallback(tabsAdapter::moveItem, (fromPosition, toPosition) -> tabsAdapter.updatePreferences(), () -> {}));

                itemTouchHelper.attachToRecyclerView(recyclerView);

                tabsAdapter.setListener(new TabsAdapter.TabListener() {
                    @Override
                    public void onItemClick(View v, int position, CategoryItem categoryItem) {
                        categoryItem.setChecked(!categoryItem.isChecked());
                        com.simplecity.amp_library.utils.AnalyticsManager.logTabVisibilityChanged(categoryItem.isChecked(), categoryItem.title);
                        tabsAdapter.notifyItemChanged(position);
                        tabsAdapter.updatePreferences();
                    }

                    @Override
                    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
                        itemTouchHelper.startDrag(viewHolder);
                    }
                });
                recyclerView.setAdapter(tabsAdapter);

                DialogUtils.getBuilder(getContext())
                        .title(R.string.pref_title_choose_tabs)
                        .customView(recyclerView, false)
                        .positiveText(R.string.button_done)
                        .onPositive((materialDialog, dialogAction) -> {
                            DialogUtils.createRestartDialog(getActivity());
                            materialDialog.dismiss();
                        })
                        .show();

                return true;
            });
        }

        final Preference colorPickerPreference = findPreference("pref_theme_highlight_color");
        if (colorPickerPreference != null) {
            colorPickerPreference.setOnPreferenceClickListener(preference -> {
                int selectedColor = mPrefs.getInt("pref_theme_highlight_color", -1);
                DialogUtils.showColorPickerDialog(this, selectedColor, color ->
                        mPrefs.edit().putInt("pref_theme_highlight_color", color).apply());
                return true;
            });
        }

        final Preference accentPickerPreference = findPreference("pref_theme_accent_color");
        if (accentPickerPreference != null) {
            accentPickerPreference.setOnPreferenceClickListener(preference -> {
                int selectedColor = mPrefs.getInt("pref_theme_accent_color", -1);
                DialogUtils.showColorPickerDialog(this, selectedColor,
                        ColorPalette.getAccentColors(), ColorPalette.getAccentColorsSub(), color ->
                                mPrefs.edit().putInt("pref_theme_accent_color", color).apply());
                return true;
            });
        }

        mListener = (sharedPreferences, key) -> {
            if (key.equals("pref_theme_highlight_color") || key.equals("pref_theme_accent_color") || key.equals("pref_theme_white_accent")) {
                ThemeUtils.setTheme(getActivity());
                ThemeUtils.themeActionBar((SettingsActivity) getActivity());
                ThemeUtils.themeStatusBar(getActivity(), mTintManager);
                getListView().invalidate();
                themeUIElements();

                for (int i = 0, size = getListView().getChildCount(); i < size; i++) {
                    View view = getListView().getChildAt(i);
                    ThemeUtils.updateThemableViews(view);
                }

            }
            if (key.equals("pref_theme_base") || key.equals("pref_default_page")) {
                DialogUtils.createRestartDialog(getActivity());
            }
        };

        final Preference restartPreference = findPreference("pref_restart");
        if (restartPreference != null) {
            restartPreference.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(getActivity(), MainActivity.class);
                ComponentName componentNAme = intent.getComponent();
                Intent mainIntent = IntentCompat.makeRestartActivityTask(componentNAme);
                startActivity(mainIntent);
                return true;
            });
        }

        final CheckBoxPreference showLockscreenArtworkPreference = (CheckBoxPreference) findPreference(SettingsManager.KEY_SHOW_LOCKSCREEN_ARTWORK);
        if (showLockscreenArtworkPreference != null) {
            showLockscreenArtworkPreference.setOnPreferenceClickListener(preference -> {
                MusicUtils.toggleLockscreenArtwork();
                return false;
            });
        }

        final Preference downloadArtworkPreference = findPreference("pref_download_artwork");
        if (downloadArtworkPreference != null) {
            downloadArtworkPreference.setOnPreferenceClickListener(preference -> {
                DialogUtils.showDownloadWarningDialog(getActivity(), (materialDialog, dialogAction) ->
                {
                    Intent intent = new Intent(getContext(), ArtworkDownloadService.class);
                    ShuttleApplication.getInstance().startService(intent);
                });
                return true;
            });
        }

        final Preference deleteArtworkPreference = findPreference("pref_delete_artwork");
        if (deleteArtworkPreference != null) {
            deleteArtworkPreference.setOnPreferenceClickListener(preference -> {
                DialogUtils.getBuilder(getActivity())
                        .title(getString(R.string.pref_title_delete_artwork))
                        .icon(DrawableUtils.themeLightOrDark(getActivity(), getResources().getDrawable(R.drawable.ic_dialog_alert)))
                        .content(getString(R.string.delete_artwork_confirmation_dialog))
                        .positiveText(getString(R.string.button_ok))
                        .onPositive((materialDialog, dialogAction) -> {

                            //Clear Glide's mem cache
                            Glide.get(getContext())
                                    .clearMemory();

                            //Clear Glide' disk cache
                            ShuttleUtils.execute(new AsyncTask<Void, Void, Void>() {
                                @Override
                                protected Void doInBackground(Void... params) {
                                    Glide.get(getContext())
                                            .clearDiskCache();
                                    return null;
                                }
                            });
                        })
                        .negativeText(getString(R.string.cancel))
                        .show();
                return true;
            });
        }

        final Preference downloadSimpleLastFmScrobbler = findPreference("pref_download_simple_lastfm_scrobbler");
        if (downloadSimpleLastFmScrobbler != null) {
            if (ShuttleUtils.isAmazonBuild()) {
                PreferenceGroup preferenceGroup = (PreferenceGroup) findPreference("pref_key_simple_lastfm_scrobble_settings");
                if (preferenceGroup != null) {
                    preferenceGroup.removePreference(downloadSimpleLastFmScrobbler);
                }
            } else {
                downloadSimpleLastFmScrobbler.setIntent(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=com.adam.aslfms")));
            }
        }

        final Preference about = findPreference("pref_about");
        if (about != null) {
            about.setOnPreferenceClickListener(preference -> {

                View customView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_changelog, null);
                WebView webView = (WebView) customView.findViewById(R.id.webView);
                int themeType = ThemeUtils.getThemeType(getActivity());
                webView.setBackgroundColor(getResources().getColor(android.R.color.transparent));

                if (themeType == ThemeUtils.ThemeType.TYPE_LIGHT || themeType == ThemeUtils.ThemeType.TYPE_SOLID_LIGHT) {
                    webView.loadUrl("file:///android_asset/web/info.html");
                } else {
                    webView.loadUrl("file:///android_asset/web/info_dark.html");
                }

                DialogUtils.getBuilder(getActivity())
                        .title(R.string.pref_title_about)
                        .customView(customView, false)
                        .negativeText(R.string.close)
                        .show();

                AnalyticsManager.logChangelogViewed();

                return true;
            });
        }

        final Preference upgrade = findPreference("pref_upgrade");
        if (upgrade != null) {
            if (ShuttleUtils.isUpgraded()) {
                SettingsFragment.this.getPreferenceScreen().removePreference(upgrade);
            }
            upgrade.setOnPreferenceClickListener(preference -> {
                DialogUtils.showUpgradeDialog(getActivity(),
                        (materialDialog, dialogAction) -> {
                            if (ShuttleUtils.isAmazonBuild()) {
                                ShuttleUtils.openShuttleLink(getActivity(), "com.simplecity.amp_pro");
                            } else {
                                AnalyticsManager.logUpgrade(AnalyticsManager.UpgradeType.UPGRADE);
                                ((SettingsActivity) getActivity()).purchasePremiumUpgrade();
                            }
                        });
                return true;
            });
        }

        final Preference viewBlacklist = findPreference("pref_blacklist_view");
        if (viewBlacklist != null) {
            viewBlacklist.setOnPreferenceClickListener(preference -> {
                DialogUtils.showBlacklistDialog(getActivity());
                return true;
            });
        }

        final Preference viewWhitelist = findPreference("pref_whitelist_view");
        if (viewWhitelist != null) {
            viewWhitelist.setOnPreferenceClickListener(preference -> {
                DialogUtils.showWhitelistDialog(getActivity());
                return true;
            });
        }

        final Preference clearBlacklist = findPreference("pref_blacklist_clear");
        if (clearBlacklist != null) {
            clearBlacklist.setOnPreferenceClickListener(preference -> {
                BlacklistHelper.deleteAllSongs();
                Toast.makeText(getActivity(), R.string.blacklist_deleted, Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        final Preference clearWhitelist = findPreference("pref_whitelist_clear");
        if (clearWhitelist != null) {
            clearWhitelist.setOnPreferenceClickListener(preference -> {
                WhitelistHelper.deleteAllFolders();
                Toast.makeText(getActivity(), R.string.whitelist_deleted, Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        final CheckBoxPreference ignoreEmbeddedArtwork = (CheckBoxPreference) findPreference(SettingsManager.KEY_IGNORE_EMBEDDED_ARTWORK);
        if (ignoreEmbeddedArtwork != null) {
            ignoreEmbeddedArtwork.setOnPreferenceChangeListener((preference, newValue) -> {
                showArtworkPreferenceDialog();
                return true;
            });
        }

        final CheckBoxPreference ignoreFolderArtwork = (CheckBoxPreference) findPreference(SettingsManager.KEY_IGNORE_FOLDER_ARTWORK);
        if (ignoreFolderArtwork != null) {
            ignoreFolderArtwork.setOnPreferenceChangeListener((preference, newValue) -> {
                showArtworkPreferenceDialog();
                return true;
            });
        }

        final CheckBoxPreference preferEmbeddedArtwork = (CheckBoxPreference) findPreference(SettingsManager.KEY_PREFER_EMBEDDED_ARTWORK);
        if (preferEmbeddedArtwork != null) {
            preferEmbeddedArtwork.setOnPreferenceChangeListener((preference, newValue) -> {
                showArtworkPreferenceDialog();
                return true;
            });
        }

        final CheckBoxPreference ignoreMediaStoreArtwork = (CheckBoxPreference) findPreference(SettingsManager.KEY_IGNORE_MEDIASTORE_ART);
        if (ignoreMediaStoreArtwork != null) {
            ignoreMediaStoreArtwork.setOnPreferenceChangeListener((preference, newValue) -> {
                showArtworkPreferenceDialog();
                return true;
            });
        }

        final CheckBoxPreference preferLastFmArtwork = (CheckBoxPreference) findPreference(SettingsManager.KEY_PREFER_LAST_FM);
        if (preferLastFmArtwork != null) {
            preferLastFmArtwork.setOnPreferenceChangeListener((preference, newValue) -> {
                showArtworkPreferenceDialog();
                return true;
            });
        }

        final Preference restorePurchases = findPreference("pref_restore_purchases");
        if (ShuttleUtils.isAmazonBuild() || ShuttleUtils.isUpgraded()) {
            PreferenceGroup preferenceGroup = (PreferenceGroup) findPreference("support_group");
            if (preferenceGroup != null) {
                preferenceGroup.removePreference(restorePurchases);
            }
        } else if (restorePurchases != null) {
            restorePurchases.setOnPreferenceClickListener(preference -> {
                ((SettingsActivity) getActivity()).restorePurchases();
                return true;
            });
        }

        final Preference versionPreference = findPreference("pref_version");
        if (versionPreference != null) {
            versionPreference.setSummary("Shuttle Music Player " + BuildConfig.VERSION_NAME + (ShuttleUtils.isUpgraded() ? " (Upgraded)" : " (Free)"));
        }

        final Preference faqPreference = findPreference("pref_faq");
        if (faqPreference != null) {
            faqPreference.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("http://www.shuttlemusicplayer.com/#faq"));
                startActivity(intent);
                return true;
            });
        }

        final Preference gplusPreference = findPreference("pref_gplus");
        if (gplusPreference != null) {
            gplusPreference.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://plus.google.com/communities/112365043563095486408"));
                startActivity(intent);
                return true;
            });
        }

        final Preference ratePreference = findPreference("pref_rate");
        if (ratePreference != null) {
            ratePreference.setOnPreferenceClickListener(preference -> {
                final String appPackageName = getActivity().getPackageName();
                ShuttleUtils.openShuttleLink(getActivity(), appPackageName);
                SettingsManager.getInstance().setHasRated();
                return true;
            });
        }

        final CheckBoxPreference openOnClickPreference = (CheckBoxPreference) findPreference("pref_open_now_playing_on_click");
        if (openOnClickPreference != null) {
            if (!ShuttleUtils.isTablet()) {
                PreferenceGroup preferenceGroup = (PreferenceGroup) findPreference("display_group");
                if (preferenceGroup != null) {
                    preferenceGroup.removePreference(openOnClickPreference);
                }
            }
        }
    }

    private Dialog showArtworkPreferenceDialog() {
        return DialogUtils.getBuilder(getContext())
                .title(R.string.pref_title_delete_artwork)
                .content(R.string.pref_summary_change_artwork_source)
                .positiveText(R.string.pref_button_remove_artwork)
                .onPositive((dialog1, which) -> {
                            Glide.get(getContext()).clearMemory();
                            ShuttleUtils.execute(new AsyncTask<Void, Void, Void>() {
                                @Override
                                protected Void doInBackground(Void... params) {
                                    Glide.get(getContext()).clearDiskCache();
                                    return null;
                                }
                            });
                        }
                )
                .negativeText(R.string.close)
                .show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view != null) {
            Drawable drawable = DrawableUtils.getBackgroundDrawable(getActivity());
            view.setBackground(drawable);
        }
        themeUIElements();

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (mPrefResId == R.xml.settings_headers) {
            if (getListView() != null && getListView().getDivider() != null) {
                Drawable drawable = getListView().getDivider();
                InsetDrawable divider = new InsetDrawable(drawable, ResourceUtils.toPixels(72), 0, 0, 0);
                getListView().setDivider(divider);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        mPrefs.registerOnSharedPreferenceChangeListener(mListener);
    }

    @Override
    public void onStop() {

        if (mListener != null) {
            mPrefs.unregisterOnSharedPreferenceChangeListener(mListener);
        }

        super.onStop();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference
            preference) {

        if (preference == null || preference.getKey() == null) {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        if (preference.getKey().equals("pref_display")) {
            ((SettingsActivity) getActivity()).swapSettingsFragment(R.xml.settings_display);
        } else if (preference.getKey().equals("pref_themes")) {
            ((SettingsActivity) getActivity()).swapSettingsFragment(R.xml.settings_themes);
        } else if (preference.getKey().equals("pref_artwork")) {
            ((SettingsActivity) getActivity()).swapSettingsFragment(R.xml.settings_artwork);
        } else if (preference.getKey().equals("pref_blacklist")) {
            ((SettingsActivity) getActivity()).swapSettingsFragment(R.xml.settings_blacklist);
        } else if (preference.getKey().equals("pref_headset")) {
            ((SettingsActivity) getActivity()).swapSettingsFragment(R.xml.settings_headset);
        } else if (preference.getKey().equals("pref_scrobbling")) {
            ((SettingsActivity) getActivity()).swapSettingsFragment(R.xml.settings_scrobbling);
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public void themeUIElements() {
        if (isAdded()) {
            PreferenceScreen displayPreference = (PreferenceScreen) findPreference("pref_display");
            if (displayPreference != null) {
                displayPreference.setIcon(DrawableUtils.getColoredAccentDrawable(getActivity(), getResources().getDrawable(R.drawable.ic_settings_display), false));
            }
            PreferenceScreen themesPreference = (PreferenceScreen) findPreference("pref_themes");
            if (themesPreference != null) {
                themesPreference.setIcon(DrawableUtils.getColoredAccentDrawable(getActivity(), getResources().getDrawable(R.drawable.ic_settings_themes), false));
            }
            PreferenceScreen artworkPreference = (PreferenceScreen) findPreference("pref_artwork");
            if (artworkPreference != null) {
                artworkPreference.setIcon(DrawableUtils.getColoredAccentDrawable(getActivity(), getResources().getDrawable(R.drawable.ic_settings_artwork), false));
            }
            PreferenceScreen headsetPreference = (PreferenceScreen) findPreference("pref_headset");
            if (headsetPreference != null) {
                headsetPreference.setIcon(DrawableUtils.getColoredAccentDrawable(getActivity(), getResources().getDrawable(R.drawable.ic_settings_headset), false));
            }
            PreferenceScreen scrobblingPreference = (PreferenceScreen) findPreference("pref_scrobbling");
            if (scrobblingPreference != null) {
                scrobblingPreference.setIcon(DrawableUtils.getColoredAccentDrawable(getActivity(), getResources().getDrawable(R.drawable.ic_settings_scrobbling), false));
            }
            PreferenceScreen blacklistPreference = (PreferenceScreen) findPreference("pref_blacklist");
            if (blacklistPreference != null) {
                blacklistPreference.setIcon(DrawableUtils.getColoredAccentDrawable(getActivity(), getResources().getDrawable(R.drawable.ic_settings_blacklist), false));
            }
            Preference aboutPreference = findPreference("pref_about");
            if (aboutPreference != null) {
                aboutPreference.setIcon(DrawableUtils.getColoredAccentDrawable(getActivity(), getResources().getDrawable(R.drawable.ic_settings_about), false));
            }
            Preference upgradePreference = findPreference("pref_upgrade");
            if (upgradePreference != null) {
                upgradePreference.setIcon(DrawableUtils.getColoredAccentDrawable(getActivity(), getResources().getDrawable(R.drawable.ic_settings_purchase), false));
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getActivity() != null && ((SettingsActivity) getActivity()).getSupportActionBar() != null) {
            ((SettingsActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
}
