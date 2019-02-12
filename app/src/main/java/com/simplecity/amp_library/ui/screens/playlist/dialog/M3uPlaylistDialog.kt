package com.simplecity.amp_library.ui.screens.playlist.dialog

import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.util.Log
import android.widget.Toast
import com.simplecity.amp_library.R
import com.simplecity.amp_library.data.SongsRepository
import com.simplecity.amp_library.di.app.activity.fragment.FragmentModule
import com.simplecity.amp_library.di.app.activity.fragment.FragmentScope
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.utils.LogUtils
import dagger.Binds
import dagger.Module
import dagger.android.support.AndroidSupportInjection
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.FileWriter
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named

class M3uPlaylistDialog : DialogFragment() {

    private lateinit var playlist: Playlist

    @Inject lateinit var songsRepository: SongsRepository

    private var disposable: Disposable? = null

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        playlist = arguments!!.getSerializable(ARG_PLAYLIST) as Playlist

        val progressDialog = ProgressDialog(context)
        progressDialog.isIndeterminate = true
        progressDialog.setTitle(R.string.saving_playlist)

        disposable = songsRepository.getSongs(playlist)
            .first(emptyList())
            .map(Function<List<Song>, File> { songs ->
                if (!songs.isEmpty()) {

                    var playlistFile: File? = null

                    if (Environment.getExternalStorageDirectory().canWrite()) {
                        val root = File(Environment.getExternalStorageDirectory(), "Playlists/Export/")
                        if (!root.exists()) {
                            root.mkdirs()
                        }

                        val noMedia = File(root, ".nomedia")
                        if (!noMedia.exists()) {
                            try {
                                noMedia.createNewFile()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }

                        val name = playlist.name.replace("[^a-zA-Z0-9.-]".toRegex(), "_")

                        playlistFile = File(root, "$name.m3u")

                        var i = 0
                        while (playlistFile!!.exists()) {
                            i++
                            playlistFile = File(root, "$name$i.m3u")
                        }

                        try {
                            val fileWriter = FileWriter(playlistFile)
                            val body = StringBuilder()
                            body.append("#EXTM3U\n")

                            for (song in songs) {
                                body.append("#EXTINF:")
                                    .append(song.duration / 1000)
                                    .append(",")
                                    .append(song.name)
                                    .append(" - ")
                                    .append(song.artistName)
                                    .append("\n")
                                    //Todo: Use relative paths instead of absolute
                                    .append(song.path)
                                    .append("\n")
                            }
                            fileWriter.append(body)
                            fileWriter.flush()
                            fileWriter.close()
                        } catch (e: IOException) {
                            Log.e(TAG, "Failed to write file: $e")
                        }

                    }
                    return@Function playlistFile
                }
                null
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { file ->
                    progressDialog.dismiss()
                    if (file != null) {
                        Toast.makeText(context, String.format(context!!.getString(R.string.playlist_saved), file.path), Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, R.string.playlist_save_failed, Toast.LENGTH_SHORT).show()
                    }
                },
                { error -> LogUtils.logException(TAG, "Error saving m3u playlist", error) }
            )

        return progressDialog
    }

    override fun onDestroyView() {
        disposable!!.dispose()
        super.onDestroyView()
    }

    fun show(fragmentManager: FragmentManager) {
        show(fragmentManager, TAG)
    }

    companion object {

        private const val TAG = "M3uPlaylistDialog"

        private const val ARG_PLAYLIST = "playlist"

        fun newInstance(playlist: Playlist): M3uPlaylistDialog {
            val args = Bundle()
            args.putSerializable(ARG_PLAYLIST, playlist)
            val fragment = M3uPlaylistDialog()
            fragment.arguments = args
            return fragment
        }
    }
}

@Module(includes = arrayOf(FragmentModule::class))
abstract class M3uDialogFragmentModule {

    @Binds
    @Named(FragmentModule.FRAGMENT)
    @FragmentScope
    internal abstract fun fragment(m3uPlaylistDialog: M3uPlaylistDialog): Fragment
}