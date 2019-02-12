package com.simplecity.amp_library.ui.screens.lyrics

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import com.simplecity.amp_library.R
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.utils.ViewUtils
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class LyricsDialog : DialogFragment(), LyricsView {

    @Inject lateinit var lyricsPresenter: LyricsPresenter

    private var lyricsTextView: TextView? = null

    private var noLyricsView: View? = null

    private var quickLyricInfo: View? = null

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        @SuppressLint("InflateParams")
        val customView = LayoutInflater.from(context).inflate(R.layout.dialog_lyrics, null)

        lyricsTextView = customView.findViewById(R.id.text1)

        noLyricsView = customView.findViewById(R.id.noLyricsView)

        val quickLyricButton = customView.findViewById<Button>(R.id.quickLyricButton)
        quickLyricButton.text = QuickLyricUtils.getSpannedString(context!!)
        quickLyricButton.setOnClickListener { lyricsPresenter.downloadOrLaunchQuickLyric() }

        quickLyricInfo = customView.findViewById(R.id.quickLyricInfo)
        quickLyricInfo!!.setOnClickListener { lyricsPresenter.showQuickLyricInfoDialog() }

        val quickLyricsLayout = customView.findViewById<View>(R.id.quickLyricLayout)
        if (!QuickLyricUtils.canDownloadQuickLyric(context)) {
            quickLyricsLayout.visibility = View.GONE
        }

        lyricsPresenter.bindView(this)

        return MaterialDialog.Builder(context!!)
            .customView(customView, false)
            .title(R.string.lyrics)
            .negativeText(R.string.close)
            .build()
    }

    override fun updateLyrics(lyrics: String?) {
        lyricsTextView!!.text = lyrics
    }

    override fun showNoLyricsView(show: Boolean) {
        if (show) {
            ViewUtils.fadeOut(lyricsTextView) {
                if (noLyricsView!!.visibility == View.GONE) {
                    ViewUtils.fadeIn(noLyricsView!!, null)
                }
            }
        } else {
            ViewUtils.fadeOut(noLyricsView) {
                if (lyricsTextView!!.visibility == View.GONE) {
                    ViewUtils.fadeIn(lyricsTextView!!, null)
                }
            }
        }
    }

    override fun showQuickLyricInfoButton(show: Boolean) {
        quickLyricInfo!!.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun launchQuickLyric(song: Song) {
        QuickLyricUtils.getLyricsFor(context!!, song)
    }

    override fun downloadQuickLyric() {
        try {
            startActivity(QuickLyricUtils.getQuickLyricIntent())
        } catch (ignored: ActivityNotFoundException) {
            // If the user doesn't have the play store on their device
        }
    }

    override fun showQuickLyricInfoDialog() {
        MaterialDialog.Builder(context!!)
            .iconRes(R.drawable.quicklyric)
            .title(R.string.quicklyric)
            .content(context!!.getString(R.string.quicklyric_info))
            .positiveText(R.string.download)
            .onPositive { _, _ -> lyricsPresenter.downloadOrLaunchQuickLyric() }
            .negativeText(R.string.close)
            .show()
    }

    fun show(fragmentManager: FragmentManager) {
        show(fragmentManager, TAG)
    }

    companion object {

        private const val TAG = "LyricsDialog"

        fun newInstance() = LyricsDialog()
    }
}