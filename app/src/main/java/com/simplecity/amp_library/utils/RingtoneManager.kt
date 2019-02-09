package com.simplecity.amp_library.utils

import android.app.AlertDialog
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.BaseColumns
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.simplecity.amp_library.R
import com.simplecity.amp_library.model.Query
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.sql.SqlUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.Callable
import javax.inject.Inject

class RingtoneManager @Inject constructor() {

    @Inject lateinit var context: Context

    fun setRingtone(song: Song): Disposable? {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(context)) {
                AlertDialog.Builder(context)
                    .setTitle(R.string.dialog_title_set_ringtone)
                    .setMessage(R.string.dialog_message_set_ringtone)
                    .setPositiveButton(R.string.button_ok) { dialog, which ->
                        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                        intent.data = Uri.parse("package:" + context.applicationContext.packageName)
                        context.startActivity(intent)
                    }.setNegativeButton(R.string.cancel, null)
                    .show()
                return null
            }
        }

        return Observable.fromCallable(Callable {
            var success = false

            val resolver = context.contentResolver
            // Set the flag in the database to mark this as a ringtone
            val ringUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
            try {
                val values = ContentValues(2)
                values.put(MediaStore.Audio.AudioColumns.IS_RINGTONE, "1")
                values.put(MediaStore.Audio.AudioColumns.IS_ALARM, "1")
                if (ringUri != null) {
                    resolver.update(ringUri, values, null, null)
                }
            } catch (ex: UnsupportedOperationException) {
                // most likely the card just got unmounted
                Log.e(TAG, "couldn't set ringtone flag for song $song")
                return@Callable false
            }

            val query = Query.Builder()
                .uri(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
                .projection(arrayOf(BaseColumns._ID, MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.TITLE))
                .selection(BaseColumns._ID + "=" + song.id)
                .build()

            SqlUtils.createQuery(context, query)?.use { cursor ->
                if (cursor.count == 1) {
                    // Set the system setting to make this the current ringtone
                    cursor.moveToFirst()
                    if (ringUri != null) {
                        Settings.System.putString(resolver, Settings.System.RINGTONE, ringUri.toString())
                    }
                    success = true
                }
            }
            success
        })
            .map { success -> if (success) context.getString(R.string.ringtone_set, song.name) else context.getString(R.string.ringtone_set_failed) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show() },
                { error -> LogUtils.logException(TAG, "Error setting ringtone", error) }
            )
    }

    companion object {
        private const val TAG = "RingtoneManager"
    }
}
