package com.simplecity.amp_library.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.playback.MusicService;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.PlaylistUtils;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class ShortcutTrampolineActivity extends AppCompatActivity {

    private static final String TAG = "ShortcutTrampolineActiv";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String action = getIntent().getAction();
        switch (action) {
            case MusicService.ShortcutCommands.PLAY:
            case MusicService.ShortcutCommands.SHUFFLE_ALL:
                Intent intent = new Intent(this, MusicService.class);
                intent.setAction(action);
                startService(intent);
                finish();
                break;
            case MusicService.ShortcutCommands.FOLDERS:
                intent = new Intent(this, MainActivity.class);
                intent.setAction(action);
                startActivity(intent);
                finish();
                break;
            case MusicService.ShortcutCommands.PLAYLIST:
                intent = new Intent(this, MainActivity.class);
                intent.setAction(action);
                Playlist.favoritesPlaylist()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(playlist -> {
                            intent.putExtra(PlaylistUtils.ARG_PLAYLIST, playlist);
                            startActivity(intent);
                            finish();
                        }, error -> LogUtils.logException(TAG, "Error starting activity", error));
                break;
        }
    }
}
