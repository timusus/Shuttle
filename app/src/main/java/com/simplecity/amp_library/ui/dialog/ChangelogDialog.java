package com.simplecity.amp_library.ui.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.ProgressBar;

import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.materialdialogs.MaterialDialog;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.utils.DialogUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ViewUtils;

public class ChangelogDialog {

    private ChangelogDialog() {
        //no instance
    }

    public static MaterialDialog getChangelogDialog(Context context) {

        @SuppressLint("InflateParams")
        View customView = LayoutInflater.from(context).inflate(R.layout.dialog_changelog, null);

        WebView webView = customView.findViewById(R.id.webView);
        webView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));

        CheckBox checkBox = customView.findViewById(R.id.checkbox);
        checkBox.setChecked(SettingsManager.getInstance().getShowChangelogOnLaunch());
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> SettingsManager.getInstance().setShowChangelogOnLaunch(isChecked));

        ProgressBar progressBar = customView.findViewById(R.id.progress);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                ViewUtils.fadeOut(progressBar, ()
                        -> ViewUtils.fadeIn(webView, null));
            }
        });

        Aesthetic.get(context)
                .isDark()
                .take(1)
                .subscribe(isDark -> webView.loadUrl(isDark ? "file:///android_asset/web/info_dark.html" : "file:///android_asset/web/info.html"));

        return DialogUtils.getBuilder(context)
                .title(R.string.pref_title_changelog)
                .customView(customView, false)
                .negativeText(R.string.close)
                .build();
    }
}