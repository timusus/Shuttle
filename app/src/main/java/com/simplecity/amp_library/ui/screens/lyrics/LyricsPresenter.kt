package com.simplecity.amp_library.ui.screens.lyrics

import android.content.Context
import android.content.IntentFilter
import android.net.Uri
import android.provider.MediaStore
import android.text.TextUtils
import com.cantrowitz.rxbroadcast.RxBroadcast
import com.simplecity.amp_library.ShuttleApplication
import com.simplecity.amp_library.model.Query
import com.simplecity.amp_library.playback.MediaManager
import com.simplecity.amp_library.playback.constants.InternalIntents
import com.simplecity.amp_library.sql.SqlUtils
import com.simplecity.amp_library.ui.common.Presenter
import com.simplecity.amp_library.utils.LogUtils
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.TagException
import java.io.File
import java.io.IOException
import java.util.concurrent.Callable
import javax.inject.Inject

class LyricsPresenter @Inject
constructor(
    private val application: ShuttleApplication,
    private val mediaManager: MediaManager
) : Presenter<LyricsView>() {

    override fun bindView(view: LyricsView) {
        super.bindView(view)

        updateLyrics(application)

        addDisposable(
            RxBroadcast.fromBroadcast(application, IntentFilter(InternalIntents.META_CHANGED))
                .toFlowable(BackpressureStrategy.LATEST)
                .subscribe(
                    { _ -> updateLyrics(application) },
                    { error -> LogUtils.logException(TAG, "Error receiving meta changed", error) }
                )
        )
    }

    fun downloadOrLaunchQuickLyric() {
        val lyricsView = view
        if (lyricsView != null) {
            if (QuickLyricUtils.isQLInstalled(application)) {
                val song = mediaManager.song
                if (song != null) {
                    lyricsView.launchQuickLyric(song)
                }
            } else {
                lyricsView.downloadQuickLyric()
            }
        }
    }

    fun showQuickLyricInfoDialog() {
        val lyricsView = view
        lyricsView?.showQuickLyricInfoDialog()
    }

    private fun updateLyrics(context: Context) {
        addDisposable(
            Observable.fromCallable(Callable {
                var lyrics = ""
                var path = mediaManager.filePath

                if (TextUtils.isEmpty(path)) {
                    return@Callable lyrics
                }

                if (path!!.startsWith("content://")) {
                    val query = Query.Builder()
                        .uri(Uri.parse(path))
                        .projection(arrayOf(MediaStore.Audio.Media.DATA))
                        .build()

                    val cursor = SqlUtils.createQuery(application, query)
                    if (cursor != null) {
                        try {
                            val colIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                            if (cursor.moveToFirst()) {
                                path = cursor.getString(colIndex)
                            }
                        } finally {
                            cursor.close()
                        }
                    }
                }

                val file = File(path)
                if (file.exists()) {
                    try {
                        val audioFile = AudioFileIO.read(file)
                        if (audioFile != null) {
                            val tag = audioFile.tag
                            if (tag != null) {
                                val tagLyrics = tag.getFirst(FieldKey.LYRICS)
                                if (tagLyrics != null && tagLyrics.isNotEmpty()) {
                                    lyrics = tagLyrics.replace("\r", "\n")
                                }
                            }
                        }
                    } catch (ignored: CannotReadException) {
                    } catch (ignored: IOException) {
                    } catch (ignored: TagException) {
                    } catch (ignored: ReadOnlyFileException) {
                    } catch (ignored: InvalidAudioFrameException) {
                    } catch (ignored: UnsupportedOperationException) {
                    }
                }
                lyrics
            })
                .subscribe(
                    { lyrics ->
                        val lyricsView = view
                        if (lyricsView != null) {
                            lyricsView.updateLyrics(lyrics)
                            lyricsView.showNoLyricsView(TextUtils.isEmpty(lyrics))
                            lyricsView.showQuickLyricInfoButton(!QuickLyricUtils.isQLInstalled(context))
                        }
                    },
                    { error -> LogUtils.logException(TAG, "Error getting lyrics", error) })
        )
    }

    companion object {

        private const val TAG = "LyricsPresenter"
    }
}