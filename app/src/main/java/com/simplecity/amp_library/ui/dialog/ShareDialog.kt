package com.simplecity.amp_library.ui.dialog

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v4.content.FileProvider
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import com.simplecity.amp_library.R
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.utils.extensions.share
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class ShareDialog : DialogFragment() {

    private lateinit var song: Song

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        song = arguments!!.getSerializable(ARG_SONG) as Song
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialDialog.Builder(context!!)
            .title(R.string.share_dialog_title)
            .items(context!!.getString(R.string.share_option_song_info), context!!.getString(R.string.share_option_audio_file))
            .itemsCallback { _, _, i, _ ->
                when (i) {
                    0 -> {
                        val context = context
                        // Use the compress method on the Bitmap object to write image to the OutputStream
                        Glide.with(context)
                            .load(song)
                            .asBitmap()
                            .priority(Priority.IMMEDIATE)
                            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                            .into(object : SimpleTarget<Bitmap>() {
                                override fun onResourceReady(resource: Bitmap?, glideAnimation: GlideAnimation<in Bitmap>) {
                                    val sendIntent = Intent()
                                    sendIntent.type = "text/plain"
                                    var fileOutputStream: FileOutputStream? = null
                                    try {
                                        val file = File(context!!.filesDir.toString() + "/share_image.jpg")
                                        fileOutputStream = FileOutputStream(file)
                                        if (resource != null) {
                                            resource.compress(Bitmap.CompressFormat.JPEG, 80, fileOutputStream)
                                            sendIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, context.applicationContext.packageName + ".provider", file))
                                            sendIntent.type = "image/jpeg"
                                        }
                                    } catch (ignored: FileNotFoundException) {

                                    } finally {
                                        try {
                                            fileOutputStream?.close()
                                        } catch (ignored: IOException) {

                                        }
                                    }

                                    sendIntent.action = Intent.ACTION_SEND
                                    sendIntent.putExtra(Intent.EXTRA_TEXT, "#NowPlaying " + song.artistName + " - " + song.name + "\n\n" + "#Shuttle")
                                    context!!.startActivity(Intent.createChooser(sendIntent, "Share current song via: "))
                                }
                            })
                    }
                    1 -> song.share(context!!)
                }
            }
            .negativeText(R.string.close)
            .build()
    }

    fun show(fragmentManager: FragmentManager) {
        show(fragmentManager, TAG)
    }

    companion object {

        private const val TAG = "ShareDialog"

        private const val ARG_SONG = "song"

        fun newInstance(song: Song): ShareDialog {
            val args = Bundle()
            args.putSerializable(ARG_SONG, song)
            val fragment = ShareDialog()
            fragment.arguments = args
            return fragment
        }
    }
}
