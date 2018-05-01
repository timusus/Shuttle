package com.simplecity.amp_library.ui.dialog;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.utils.extensions.SongExtKt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class ShareDialog {

    private ShareDialog() {
        //no instance
    }

    public static MaterialDialog getDialog(Context context, @NonNull Song song) {

        return new MaterialDialog.Builder(context)
                .title(R.string.share_dialog_title)
                .items(context.getString(R.string.share_option_song_info), context.getString(R.string.share_option_audio_file))
                .itemsCallback((materialDialog, view, i, charSequence) -> {
                    switch (i) {
                        case 0:
                            shareSongInfo(context, song);
                            break;
                        case 1:
                            SongExtKt.share(song, context);
                            break;
                    }
                })
                .negativeText(R.string.close)
                .build();
    }

    static void shareSongInfo(Context context, Song song) {
        // Use the compress method on the Bitmap object to write image to the OutputStream
        RequestOptions options = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .priority(Priority.IMMEDIATE);
        Glide.with(context)
                .asBitmap()
                .load(song)
                .apply(options)
                .into(new SimpleTarget<Bitmap>() {

            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
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
            }
        });
    }
}
