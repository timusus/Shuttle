package com.simplecity.amp_library.ui.screens.playlist.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ContentValues
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.simplecity.amp_library.R
import com.simplecity.amp_library.model.Playlist

class RenamePlaylistDialog : DialogFragment() {

    private var playlist: Playlist? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        playlist = arguments!!.getSerializable(ARG_PLAYLIST) as Playlist

        @SuppressLint("InflateParams")
        val customView = LayoutInflater.from(context).inflate(R.layout.dialog_playlist, null)
        val editText = customView.findViewById<EditText>(R.id.editText)
        editText.setText(playlist!!.name)

        val builder = MaterialDialog.Builder(context!!)
            .title(R.string.create_playlist_create_text_prompt)
            .customView(customView, false)
            .positiveText(R.string.save)
            .onPositive { materialDialog, dialogAction ->
                val name = editText.text.toString()
                if (name.isNotEmpty()) {
                    val resolver = context!!.contentResolver
                    val values = ContentValues(1)
                    values.put(MediaStore.Audio.Playlists.NAME, name)
                    resolver.update(
                        MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                        values,
                        MediaStore.Audio.Playlists._ID + "=?",
                        arrayOf(java.lang.Long.valueOf(playlist!!.id).toString())
                    )
                    playlist!!.name = name
                    Toast.makeText(context, R.string.playlist_renamed_message, Toast.LENGTH_SHORT).show()
                }
            }
            .negativeText(R.string.cancel)

        val dialog = builder.build()

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // check if playlist with current name exists already, and warn the user if so.
                setSaveButton(dialog, playlist, editText.text.toString())
            }

            override fun afterTextChanged(s: Editable) {}
        }

        editText.addTextChangedListener(textWatcher)

        return dialog
    }

    private fun setSaveButton(dialog: MaterialDialog, playlist: Playlist?, typedName: String) {
        if (typedName.trim { it <= ' ' }.isEmpty()) {
            val button = dialog.getActionButton(DialogAction.POSITIVE)
            if (button != null) {
                button.isEnabled = false
            }
        } else {
            val button = dialog.getActionButton(DialogAction.POSITIVE)
            if (button != null) {
                button.isEnabled = true
            }
            if (playlist!!.id >= 0 && playlist.name != typedName) {
                button?.setText(R.string.create_playlist_overwrite_text)
            } else {
                button?.setText(R.string.create_playlist_create_text)
            }
        }
    }

    fun show(fragmentManager: FragmentManager) {
        show(fragmentManager, TAG)
    }

    companion object {

        const val TAG = "RenamePlaylistDialog"

        private const val ARG_PLAYLIST = "playlist"

        fun newInstance(playlist: Playlist): RenamePlaylistDialog {
            val args = Bundle()
            args.putSerializable(ARG_PLAYLIST, playlist)
            val fragment = RenamePlaylistDialog()
            fragment.arguments = args
            return fragment
        }
    }
}