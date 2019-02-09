package com.simplecity.amp_library.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import com.simplecity.amp_library.R
import com.simplecity.amp_library.R.id.album
import com.simplecity.amp_library.R.id.artist
import com.simplecity.amp_library.model.FileObject
import com.simplecity.amp_library.utils.FileHelper

class FileInfoDialog : DialogFragment() {

    private var fileObject: FileObject? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        fileObject = arguments!!.getSerializable(ARG_FILE_OBJECT) as FileObject
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        @SuppressLint("InflateParams")
        val view = (context!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(R.layout.dialog_song_info, null)

        val titleView = view.findViewById<View>(R.id.title)
        val titleKey = titleView.findViewById<TextView>(R.id.key)
        titleKey.setText(R.string.song_title)
        val titleValue = titleView.findViewById<TextView>(R.id.value)
        titleValue.text = fileObject!!.tagInfo.trackName

        val trackNumberView = view.findViewById<View>(R.id.track_number)
        val trackNumberKey = trackNumberView.findViewById<TextView>(R.id.key)
        trackNumberKey.setText(R.string.track_number)
        val trackNumberValue = trackNumberView.findViewById<TextView>(R.id.value)
        if (fileObject!!.tagInfo.trackTotal != 0) {
            trackNumberValue.text = String.format(context!!.getString(R.string.track_count), fileObject!!.tagInfo.trackNumber.toString(), fileObject!!.tagInfo.trackTotal.toString())
        } else {
            trackNumberValue.text = fileObject!!.tagInfo.trackNumber.toString()
        }

        val artistView = view.findViewById<View>(artist)
        val artistKey = artistView.findViewById<TextView>(R.id.key)
        artistKey.setText(R.string.artist_title)
        val artistValue = artistView.findViewById<TextView>(R.id.value)
        artistValue.text = fileObject!!.tagInfo.artistName

        val albumView = view.findViewById<View>(album)
        val albumKey = albumView.findViewById<TextView>(R.id.key)
        albumKey.setText(R.string.album_title)
        val albumValue = albumView.findViewById<TextView>(R.id.value)
        albumValue.text = fileObject!!.tagInfo.albumName

        val genreView = view.findViewById<View>(R.id.genre)
        val genreKey = genreView.findViewById<TextView>(R.id.key)
        genreKey.setText(R.string.genre_title)
        val genreValue = genreView.findViewById<TextView>(R.id.value)
        genreValue.text = fileObject!!.tagInfo.genre

        val albumArtistView = view.findViewById<View>(R.id.album_artist)
        val albumArtistKey = albumArtistView.findViewById<TextView>(R.id.key)
        albumArtistKey.setText(R.string.album_artist_title)
        val albumArtistValue = albumArtistView.findViewById<TextView>(R.id.value)
        albumArtistValue.text = fileObject!!.tagInfo.albumArtistName

        val durationView = view.findViewById<View>(R.id.duration)
        val durationKey = durationView.findViewById<TextView>(R.id.key)
        durationKey.setText(R.string.sort_song_duration)
        val durationValue = durationView.findViewById<TextView>(R.id.value)
        durationValue.text = fileObject!!.getTimeString(context)

        val pathView = view.findViewById<View>(R.id.path)
        val pathKey = pathView.findViewById<TextView>(R.id.key)
        pathKey.setText(R.string.song_info_path)
        val pathValue = pathView.findViewById<TextView>(R.id.value)
        pathValue.text = fileObject!!.path + "/" + fileObject!!.name + "." + fileObject!!.extension

        val discNumberView = view.findViewById<View>(R.id.disc_number)
        val discNumberKey = discNumberView.findViewById<TextView>(R.id.key)
        discNumberKey.setText(R.string.disc_number)
        val discNumberValue = discNumberView.findViewById<TextView>(R.id.value)
        if (fileObject!!.tagInfo.discTotal != 0) {
            discNumberValue.text = String.format(context!!.getString(R.string.track_count), fileObject!!.tagInfo.discNumber.toString(), fileObject!!.tagInfo.discTotal.toString())
        } else {
            discNumberValue.text = fileObject!!.tagInfo.discNumber.toString()
        }

        val fileSizeView = view.findViewById<View>(R.id.file_size)
        val fileSizeKey = fileSizeView.findViewById<TextView>(R.id.key)
        fileSizeKey.setText(R.string.song_info_file_size)
        val fileSizeValue = fileSizeView.findViewById<TextView>(R.id.value)
        fileSizeValue.text = FileHelper.getHumanReadableSize(fileObject!!.size)

        val formatView = view.findViewById<View>(R.id.format)
        val formatKey = formatView.findViewById<TextView>(R.id.key)
        formatKey.setText(R.string.song_info_format)
        val formatValue = formatView.findViewById<TextView>(R.id.value)
        formatValue.text = fileObject!!.tagInfo.format

        val bitrateView = view.findViewById<View>(R.id.bitrate)
        val bitrateKey = bitrateView.findViewById<TextView>(R.id.key)
        bitrateKey.setText(R.string.song_info_bitrate)
        val bitrateValue = bitrateView.findViewById<TextView>(R.id.value)
        bitrateValue.text = fileObject!!.tagInfo.bitrate + context!!.getString(R.string.song_info_bitrate_suffix)

        val samplingRateView = view.findViewById<View>(R.id.sample_rate)
        val samplingRateKey = samplingRateView.findViewById<TextView>(R.id.key)
        samplingRateKey.setText(R.string.song_info_sample_Rate)
        val samplingRateValue = samplingRateView.findViewById<TextView>(R.id.value)
        samplingRateValue.text = (fileObject!!.tagInfo.sampleRate / 1000).toString() + context!!.getString(R.string.song_info_sample_rate_suffix)

        val playCountView = view.findViewById<View>(R.id.play_count)
        playCountView.visibility = View.GONE

        return MaterialDialog.Builder(context!!)
            .title(context!!.getString(R.string.dialog_song_info_title))
            .customView(view, false)
            .negativeText(R.string.close)
            .show()
    }

    fun show(fragmentManager: FragmentManager) {
        show(fragmentManager, TAG)
    }

    companion object {

        private const val TAG = "FileInfoDialog"

        private const val ARG_FILE_OBJECT = "file"

        fun newInstance(fileObject: FileObject): FileInfoDialog {
            val args = Bundle()
            args.putSerializable(ARG_FILE_OBJECT, fileObject)
            val fragment = FileInfoDialog()
            fragment.arguments = args
            return fragment
        }
    }
}
