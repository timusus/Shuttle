package com.simplecity.amp_library.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.XmlRes;
import android.support.v4.app.Fragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.materialdialogs.MaterialDialog;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.dagger.module.FragmentModule;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import test.com.androidnavigation.base.Controller;
import test.com.androidnavigation.base.NavigationController;
import test.com.androidnavigation.fragment.BaseController;
import test.com.androidnavigation.fragment.BaseNavigationController;
import test.com.androidnavigation.fragment.FragmentInfo;

public class SettingsParentFragment extends BaseNavigationController {

    public static String ARG_PREFERENCE_RESOURCE = "preference_resource";
    public static String ARG_TITLE = "title";

    @BindView(R.id.toolbar) Toolbar toolbar;

    @XmlRes int preferenceResource;
    @StringRes int titleResId;

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

        ButterKnife.bind(this, rootView);

        toolbar.setTitle(titleResId);
        toolbar.setNavigationOnClickListener(v -> getActivity().onBackPressed());

        return rootView;
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements
            Controller,
            SupportView,
            SettingsView {

        @XmlRes int preferenceResource;

        @Inject SupportPresenter supportPresenter;
        @Inject SettingsPresenter settingsPresenter;

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
            super.onCreate(savedInstanceState);

            ShuttleApplication.getInstance().getAppComponent()
                    .plus(new FragmentModule(this))
                    .inject(this);

            // Support Preferences

            Preference faqPreference = findPreference("pref_faq");
            if (faqPreference != null) {
                faqPreference.setOnPreferenceClickListener(preference -> {
                    supportPresenter.faqClicked();
                    return true;
                });
            }

            Preference helpPreference = findPreference("pref_help");
            if (helpPreference != null) {
                helpPreference.setOnPreferenceClickListener(preference -> {
                    supportPresenter.helpClicked();
                    return true;
                });
            }

            Preference ratePreference = findPreference("pref_rate");
            if (ratePreference != null) {
                ratePreference.setOnPreferenceClickListener(preference -> {
                    supportPresenter.rateClicked();
                    return true;
                });
            }
        }

        @Override
        public void onResume() {
            super.onResume();

            supportPresenter.bindView(this);
            settingsPresenter.bindView(this);
        }

        @Override
        public void onPause() {
            super.onPause();

            supportPresenter.unbindView(this);
            settingsPresenter.unbindView(this);
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            switch (preference.getKey()) {
                case "pref_display":
                    getNavigationController().pushViewController(SettingsFragment.newInstance(R.xml.settings_display), "DisplaySettings");
                    break;
                case "pref_themes":
                    getNavigationController().pushViewController(SettingsFragment.newInstance(R.xml.settings_themes), "DisplaySettings");
                    break;
                case "pref_artwork":
                    getNavigationController().pushViewController(SettingsFragment.newInstance(R.xml.settings_artwork), "DisplaySettings");
                    break;
                case "pref_headset":
                    getNavigationController().pushViewController(SettingsFragment.newInstance(R.xml.settings_headset), "DisplaySettings");
                    break;
                case "pref_scrobbling":
                    getNavigationController().pushViewController(SettingsFragment.newInstance(R.xml.settings_scrobbling), "DisplaySettings");
                    break;
                case "pref_blacklist":
                    getNavigationController().pushViewController(SettingsFragment.newInstance(R.xml.settings_blacklist), "DisplaySettings");
                    break;
                case "pref_about":
                    settingsPresenter.changelogClicked(getContext());
                    break;
                case "pref_upgrade":
                    settingsPresenter.upgradeClicked(getContext());
                    break;
            }
            return true;
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

        @Override
        public void showChangelog(MaterialDialog dialog) {
            dialog.show();
        }

        @Override
        public void openStoreLink(Intent intent) {
            startActivity(intent);
        }

        @Override
        public void showUpgradeDialog(MaterialDialog dialog) {
            dialog.show();
        }
    }
}