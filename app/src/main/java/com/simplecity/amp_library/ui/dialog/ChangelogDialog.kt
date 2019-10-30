package com.simplecity.amp_library.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.CheckBox
import android.widget.ProgressBar
import com.afollestad.aesthetic.Aesthetic
import com.afollestad.materialdialogs.MaterialDialog
import com.simplecity.amp_library.R
import com.simplecity.amp_library.utils.SettingsManager
import com.simplecity.amp_library.utils.ViewUtils
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class ChangelogDialog : DialogFragment() {

    @Inject lateinit var settingsManager: SettingsManager

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        @SuppressLint("InflateParams")
        val customView = LayoutInflater.from(context).inflate(R.layout.dialog_changelog, null)

        val webView = customView.findViewById<WebView>(R.id.webView)
        webView.setBackgroundColor(ContextCompat.getColor(context!!, android.R.color.transparent))

        val checkBox = customView.findViewById<CheckBox>(R.id.checkbox)
        checkBox.isChecked = settingsManager.showChangelogOnLaunch
        checkBox.setOnCheckedChangeListener { buttonView, isChecked -> settingsManager.showChangelogOnLaunch = isChecked }

        val progressBar = customView.findViewById<ProgressBar>(R.id.progress)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)

                ViewUtils.fadeOut(progressBar) { ViewUtils.fadeIn(webView, null) }
            }

            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                if (intent.resolveActivity(context!!.packageManager) != null) {
                    context?.startActivity(intent)
                    return true
                }
                return false
            }
        }

        Aesthetic.get(context)
            .isDark
            .take(1)
            .subscribe { isDark -> webView.loadUrl(if (isDark) "file:///android_asset/web/info_dark.html" else "file:///android_asset/web/info.html") }

        return MaterialDialog.Builder(context!!)
            .title(R.string.pref_title_changelog)
            .customView(customView, false)
            .negativeText(R.string.close)
            .build()
    }

    fun show(fragmentManager: FragmentManager) {
        show(fragmentManager, TAG)
    }

    companion object {
        private const val TAG = "ChangelogDialog"

        fun newInstance() = ChangelogDialog()
    }
}