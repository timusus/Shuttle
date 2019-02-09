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
import com.simplecity.amp_library.ShuttleApplication
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.utils.LogUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class SongInfoDialog : DialogFragment() {

    private lateinit var song: Song

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        song = arguments!!.getSerializable(ARG_SONG) as Song
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        @SuppressLint("InflateParams")
        val view = (context!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(R.layout.dialog_song_info, null)

        val titleView = view.findViewById<View>(R.id.title)
        val titleKey = titleView.findViewById<TextView>(R.id.key)
        titleKey.setText(R.string.song_title)
        val titleValue = titleView.findViewById<TextView>(R.id.value)
        titleValue.text = song.name

        val trackNumberView = view.findViewById<View>(R.id.track_number)
        val trackNumberKey = trackNumberView.findViewById<TextView>(R.id.key)
        trackNumberKey.setText(R.string.track_number)
        val trackNumberValue = trackNumberView.findViewById<TextView>(R.id.value)
        trackNumberValue.text = song.trackNumberLabel.toString()

        val artistView = view.findViewById<View>(artist)
        val artistKey = artistView.findViewById<TextView>(R.id.key)
        artistKey.setText(R.string.artist_title)
        val artistValue = artistView.findViewById<TextView>(R.id.value)
        artistValue.text = song.artistName

        val albumView = view.findViewById<View>(album)
        val albumKey = albumView.findViewById<TextView>(R.id.key)
        albumKey.setText(R.string.album_title)
        val albumValue = albumView.findViewById<TextView>(R.id.value)
        albumValue.text = song.albumName

        val genreView = view.findViewById<View>(R.id.genre)
        val genreKey = genreView.findViewById<TextView>(R.id.key)
        genreKey.setText(R.string.genre_title)
        val genreValue = genreView.findViewById<TextView>(R.id.value)
        song.getGenre(context!!.applicationContext as ShuttleApplication)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ genre -> genreValue.text = genre.name },
                { error -> LogUtils.logException(TAG, "Error getting genre", error) })

        val albumArtistView = view.findViewById<View>(R.id.album_artist)
        val albumArtistKey = albumArtistView.findViewById<TextView>(R.id.key)
        albumArtistKey.setText(R.string.album_artist_title)
        val albumArtistValue = albumArtistView.findViewById<TextView>(R.id.value)
        albumArtistValue.text = song.albumArtistName

        val durationView = view.findViewById<View>(R.id.duration)
        val durationKey = durationView.findViewById<TextView>(R.id.key)
        durationKey.setText(R.string.sort_song_duration)
        val durationValue = durationView.findViewById<TextView>(R.id.value)
        durationValue.text = song.getDurationLabel(context)

        val pathView = view.findViewById<View>(R.id.path)
        val pathKey = pathView.findViewById<TextView>(R.id.key)
        pathKey.setText(R.string.song_info_path)
        val pathValue = pathView.findViewById<TextView>(R.id.value)
        pathValue.text = song.path

        val discNumberView = view.findViewById<View>(R.id.disc_number)
        val discNumberKey = discNumberView.findViewById<TextView>(R.id.key)
        discNumberKey.setText(R.string.disc_number)
        val discNumberValue = discNumberView.findViewById<TextView>(R.id.value)
        discNumberValue.text = song.discNumberLabel.toString()

        val fileSizeView = view.findViewById<View>(R.id.file_size)
        val fileSizeKey = fileSizeView.findViewById<TextView>(R.id.key)
        fileSizeKey.setText(R.string.song_info_file_size)
        val fileSizeValue = fileSizeView.findViewById<TextView>(R.id.value)
        fileSizeValue.text = song.fileSizeLabel

        val formatView = view.findViewById<View>(R.id.format)
        val formatKey = formatView.findViewById<TextView>(R.id.key)
        formatKey.setText(R.string.song_info_format)
        val formatValue = formatView.findViewById<TextView>(R.id.value)
        formatValue.text = song.formatLabel

        val bitrateView = view.findViewById<View>(R.id.bitrate)
        val bitrateKey = bitrateView.findViewById<TextView>(R.id.key)
        bitrateKey.setText(R.string.song_info_bitrate)
        val bitrateValue = bitrateView.findViewById<TextView>(R.id.value)
        bitrateValue.text = song.getBitrateLabel(context)

        val samplingRateView = view.findViewById<View>(R.id.sample_rate)
        val samplingRateKey = samplingRateView.findViewById<TextView>(R.id.key)
        samplingRateKey.setText(R.string.song_info_sample_Rate)
        val samplingRateValue = samplingRateView.findViewById<TextView>(R.id.value)
        samplingRateValue.text = song.getSampleRateLabel(context)

        val playCountView = view.findViewById<View>(R.id.play_count)
        val playCountKey = playCountView.findViewById<TextView>(R.id.key)
        playCountKey.setText(R.string.song_info_play_count)
        val playCountValue = playCountView.findViewById<TextView>(R.id.value)

        Observable.fromCallable { song.getPlayCount(context) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ playCount -> playCountValue.text = playCount.toString() },
                { error -> LogUtils.logException(TAG, "Error getting play count", error) })

        return MaterialDialog.Builder(context!!)
            .title(context!!.getString(R.string.dialog_song_info_title))
            .customView(view, false)
            .negativeText(R.string.close)
            .build()
    }

    fun show(fragmentManager: FragmentManager) {
        show(fragmentManager, TAG)
    }

    companion object {

        private const val TAG = "SongInfoDialog"

        private const val ARG_SONG = "song"

        fun newInstance(song: Song): SongInfoDialog {
            val args = Bundle()
            args.putSerializable(ARG_SONG, song)
            val fragment = SongInfoDialog()
            fragment.arguments = args
            return fragment
        }
    }
}
