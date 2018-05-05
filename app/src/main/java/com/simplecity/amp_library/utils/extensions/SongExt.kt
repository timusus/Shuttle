package com.simplecity.amp_library.utils.extensions

import android.content.Context
import android.content.Intent
import android.support.v4.content.FileProvider
import com.simplecity.amp_library.R
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.utils.LogUtils
import java.io.File

const val TAG = "SongExtensions"

fun Song.share(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_SEND).setType("audio/*")
        val uri = FileProvider.getUriForFile(context, context.applicationContext.packageName + ".provider", File(path))
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_via)))
    } catch (e: IllegalArgumentException) {
        LogUtils.logException(TAG, "Failed to share track", e)
    }
}

fun Song.delete(): Boolean {

    if (path == null) return false

    var success = false

    val file = File(path)
    if (file.exists()) {
        success = file.delete()
    }

    return success
}

