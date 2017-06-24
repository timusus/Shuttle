package com.simplecity.amp_library.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.utils.DialogUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class ShareDialog {

    private ShareDialog() {
        //no instance
    }

    public static void showShareDialog(final Context context, final Song song) {

        if (song == null) {
            Toast.makeText(context, context.getString(R.string.share_failed), Toast.LENGTH_LONG).show();
            return;
        }

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_listview, null);

        final ListView listView = view.findViewById(android.R.id.list);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(context, R.layout.dialog_list_item);
        arrayAdapter.add(context.getString(R.string.share_option_song_info));
        arrayAdapter.add(context.getString(R.string.share_option_audio_file));
        listView.setAdapter(arrayAdapter);

        MaterialDialog.Builder builder = DialogUtils.getBuilder(context)
                .title(context.getString(R.string.share_dialog_title))
                .customView(view, false);

        final Dialog dialog = builder.show();
        builder.negativeText(R.string.close);

        listView.setOnItemClickListener((adapterView, view1, i, l) -> {
            switch (i) {
                case 0:
                    // Use the compress method on the Bitmap object to write image to the OutputStream
                    Glide.with(context)
                            .load(song)
                            .asBitmap()
                            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                            .into(new SimpleTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                                    Intent sendIntent = new Intent();
                                    sendIntent.setType("text/plain");
                                    FileOutputStream fileOutputStream = null;
                                    try {
                                        File file = new File(context.getFilesDir() + "/share_image.jpg");
                                        fileOutputStream = new FileOutputStream(file);
                                        if (resource != null) {
                                            resource.compress(Bitmap.CompressFormat.JPEG, 80, fileOutputStream);
                                            sendIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", file));
                                            sendIntent.setType("image/jpeg");
                                        }
                                    } catch (FileNotFoundException ignored) {

                                    } finally {
                                        try {
                                            if (fileOutputStream != null) {
                                                fileOutputStream.close();
                                            }
                                        } catch (IOException ignored) {

                                        }
                                    }

                                    sendIntent.setAction(Intent.ACTION_SEND);
                                    sendIntent.putExtra(Intent.EXTRA_TEXT, "#NowPlaying " + song.artistName + " - " + song.name + "\n\n" + "#Shuttle");
                                    context.startActivity(Intent.createChooser(sendIntent, "Share current song via: "));
                                    dialog.dismiss();
                                }
                            });
                    break;

                case 1:
                    song.share(context);
                    dialog.dismiss();
                    break;
            }
        });
    }
}
