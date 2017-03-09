package com.simplecity.amp_library.ui.activities;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.jp.wasabeef.glide.transformations.BlurTransformation;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.MusicService;
import com.simplecity.amp_library.tagger.TaggerDialog;
import com.simplecity.amp_library.ui.fragments.LyricsFragment;
import com.simplecity.amp_library.ui.fragments.QueueFragment;
import com.simplecity.amp_library.ui.fragments.QueuePagerFragment;
import com.simplecity.amp_library.ui.views.RepeatingImageButton;
import com.simplecity.amp_library.ui.views.SizableSeekBar;
import com.simplecity.amp_library.utils.ColorUtils;
import com.simplecity.amp_library.utils.DialogUtils;
import com.simplecity.amp_library.utils.DrawableUtils;
import com.simplecity.amp_library.utils.MusicServiceConnectionUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.SleepTimer;
import com.simplecity.amp_library.utils.StringUtils;
import com.simplecity.amp_library.utils.ThemeUtils;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;

public class PlayerActivity extends BaseCastActivity implements
        View.OnClickListener,
        SeekBar.OnSeekBarChangeListener,
        MusicUtils.Defs,
        ServiceConnection {

    private static final String TAG = "PlayerActivity";

    private static final int REFRESH = 1;

    private BroadcastReceiver mStatusListener;

    private boolean mIsPaused = false;
    private long mStartSeekPos = 0;
    private long mLastShortSeekEventTime;
    private long mLastSeekEventTime;
    private TimeHandler mTimeHandler;

    private static final String QUEUE_FRAGMENT = "queue_fragment";
    private static final String QUEUE_PAGER_FRAGMENT = "queue_pager_fragment";
    private static final String LYRICS_FRAGMENT = "lyrics_fragment";

    private ImageButton mPauseButton;
    private ImageButton mShuffleButton;
    private ImageButton mRepeatButton;
    private RepeatingImageButton mNextButton;
    private RepeatingImageButton mPrevButton;

    private SizableSeekBar mSeekBar;

    private TextView mArtist;
    private TextView mTrack;
    private TextView mAlbum;
    private TextView mCurrentTime;
    private TextView mTotalTime;
    private TextView mQueuePosition;

    private ImageView mBackgroundImage;

    private View mTextViewContainer;
    private View mButtonContainer;

    private Toolbar mToolbar;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onCreate(Bundle savedInstanceState) {

        ThemeUtils.setTheme(this);

        if (!ShuttleUtils.hasLollipop() && ShuttleUtils.hasKitKat()) {
            getWindow().setFlags(FLAG_TRANSLUCENT_STATUS, FLAG_TRANSLUCENT_STATUS);
        }
        if (ShuttleUtils.hasLollipop()) {
            if (!ShuttleUtils.hasKitKat()) {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            } else {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
                getWindow().setStatusBarColor(Color.TRANSPARENT);
            }
        }

        if (SettingsManager.getInstance().canTintNavBar()) {
            getWindow().setNavigationBarColor(ColorUtils.getPrimaryColorDark(this));
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_player);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(null);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ThemeUtils.themeActionBar(this);
        ThemeUtils.themeStatusBar(this, null);

        if (!ShuttleUtils.isTablet() && ShuttleUtils.isLandscape()) {
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ColorUtils.getPrimaryColor()));
        } else {
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.parseColor("#20000000")));
        }

        mTimeHandler = new TimeHandler(this);

        mTrack = (TextView) findViewById(R.id.text1);
        mAlbum = (TextView) findViewById(R.id.text2);
        mArtist = (TextView) findViewById(R.id.text3);
        mCurrentTime = (TextView) findViewById(R.id.current_time);
        mTotalTime = (TextView) findViewById(R.id.total_time);
        mQueuePosition = (TextView) findViewById(R.id.queue_position);
        mQueuePosition = (TextView) findViewById(R.id.queue_position);

        mPauseButton = (ImageButton) findViewById(R.id.play);
        mPauseButton.setOnClickListener(this);
        mNextButton = (RepeatingImageButton) findViewById(R.id.next);
        mNextButton.setOnClickListener(this);
        mNextButton.setRepeatListener(mFastForwardListener);
        mPrevButton = (RepeatingImageButton) findViewById(R.id.prev);
        mPrevButton.setOnClickListener(this);
        mPrevButton.setRepeatListener(mRewindListener);
        mRepeatButton = (ImageButton) findViewById(R.id.repeat);
        mRepeatButton.setOnClickListener(this);
        mShuffleButton = (ImageButton) findViewById(R.id.shuffle);
        mShuffleButton.setOnClickListener(this);

        mTextViewContainer = findViewById(R.id.textContainer);
        mButtonContainer = findViewById(R.id.button_container);

        mSeekBar = (SizableSeekBar) findViewById(R.id.seekbar);
        mSeekBar.setMax(1000);
        mSeekBar.setOnSeekBarChangeListener(this);

        mBackgroundImage = (ImageView) findViewById(R.id.background);

        themeUIComponents();

        if (savedInstanceState == null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setCustomAnimations(R.anim.abc_fade_in, R.anim.abc_fade_out);

            ft.replace(R.id.main_container, new QueuePagerFragment(), QUEUE_PAGER_FRAGMENT);

            if (ShuttleUtils.isTablet()) {
                ft.replace(R.id.queue_container, QueueFragment.newInstance(), QUEUE_FRAGMENT);
            }

            ft.commit();
        }

        Glide.with(this)
                .load(MusicUtils.getAlbumArtist())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .bitmapTransform(new BlurTransformation(this))
                .override(500, 500)
                .into(mBackgroundImage);
    }

    @Override
    protected void onPause() {

        mTimeHandler.removeMessages(REFRESH);

        super.onPause();
    }

    @Override
    protected void onStop() {
        unregisterReceiver(mStatusListener);

        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();

        mStatusListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (action != null) {
                    switch (action) {
                        case MusicService.InternalIntents.META_CHANGED:
                            updateTrackInfo();
                            setPauseButtonImage();
                            queueNextRefresh(1);
                            break;
                        case MusicService.InternalIntents.PLAY_STATE_CHANGED:
                            setPauseButtonImage();
                            break;
                        case MusicService.InternalIntents.REPEAT_CHANGED:
                            setRepeatButtonImage();
                            break;
                        case MusicService.InternalIntents.SHUFFLE_CHANGED:
                            setShuffleButtonImage();
                            break;
                    }
                }
            }
        };

        final IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.InternalIntents.PLAY_STATE_CHANGED);
        filter.addAction(MusicService.InternalIntents.META_CHANGED);
        filter.addAction(MusicService.InternalIntents.SHUFFLE_CHANGED);
        filter.addAction(MusicService.InternalIntents.REPEAT_CHANGED);
        registerReceiver(mStatusListener, new IntentFilter(filter));
    }

    @Override
    public void onResume() {
        super.onResume();

        final long next = refreshCurrentTime();
        queueNextRefresh(next);
        updateTrackInfo();
        setPauseButtonImage();
        setShuffleButtonImage();
        setRepeatButtonImage();

    }

    @Override
    protected void onDestroy() {

        mIsPaused = false;
        mTimeHandler.removeMessages(REFRESH);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_player_activity, menu);

        if (!ShuttleUtils.isUpgraded()) {
            menu.findItem(R.id.media_route_menu_item).setVisible(false);
        } else {
            if (mCastManager != null) {
                mCastManager.addMediaRouterButton(menu, R.id.media_route_menu_item);
            }
        }

        menu.add(0, OPTIONS, OPTIONS, R.string.settings);

        menu.add(0, EQUALIZER, 2, R.string.equalizer);

        menu.add(0, TIMER, TIMER, R.string.timer);

        if (ShuttleUtils.isUpgraded()) {
            menu.add(0, TAGGER, 4, R.string.edit_tags);
        }

        SubMenu sub = menu.addSubMenu(0, ADD_TO_PLAYLIST, 5, R.string.save_as_playlist);
        PlaylistUtils.makePlaylistMenu(this, sub, 0);

        menu.add(0, CLEAR_QUEUE, 6, R.string.clear_queue);

        menu.add(0, VIEW_INFO, 7, R.string.song_info);

        menu.add(0, DELETE_ITEM, 8, R.string.delete_item);

        if (ShuttleUtils.isTablet()) {
            if (menu.findItem(R.id.menu_list) != null) {
                menu.removeItem(R.id.menu_list);
            }
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        PlaylistUtils.isFavorite(this, MusicUtils.getSong())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(isFavorite -> {
                    if (isFavorite) {
                        final MenuItem favItem = menu.findItem(R.id.menu_favorite);
                        int[] attrs = new int[]{R.attr.btn_fav_pressed /* index 0 */};
                        TypedArray ta = obtainStyledAttributes(attrs);
                        if (ta != null) {
                            Drawable drawableFromTheme = ta.getDrawable(0 /* index */);
                            ta.recycle();
                            if (favItem != null) {
                                favItem.setIcon(drawableFromTheme);
                            }
                        }
                    }
                });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        if (item.getItemId() == R.id.menu_favorite) {
            PlaylistUtils.toggleFavorite(this);
            supportInvalidateOptionsMenu();
            return true;
        }

        if (item.getItemId() == R.id.menu_list) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setCustomAnimations(R.anim.abc_fade_in, R.anim.abc_fade_out, R.anim.abc_fade_in, R.anim.abc_fade_out);

            //Remove the lyrics fragment
            Fragment lyricsFragment = getSupportFragmentManager().findFragmentByTag(LYRICS_FRAGMENT);
            if (lyricsFragment != null) {
                ft.remove(lyricsFragment);
            }

            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.queue_container);
            if (fragment instanceof QueueFragment) {
                ft.remove(getSupportFragmentManager().findFragmentByTag(QUEUE_FRAGMENT));
            } else {
                ft.add(R.id.queue_container, QueueFragment.newInstance(), QUEUE_FRAGMENT);
            }

            ft.commit();
            return true;
        }

        switch (item.getItemId()) {
            case EQUALIZER: {
                final Intent equalizerIntent = new Intent(this, EqualizerActivity.class);
                startActivity(equalizerIntent);
                return true;
            }
            case OPTIONS: {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            case TIMER: {
                SleepTimer.createTimer(this, MusicUtils.getTimerActive(), MusicUtils.getTimeRemaining());
                return true;
            }
            case DELETE_ITEM: {
                new DialogUtils.DeleteDialogBuilder()
                        .context(this)
                        .singleMessageId(R.string.delete_song_desc)
                        .multipleMessage(R.string.delete_song_desc_multiple)
                        .itemNames(Collections.singletonList(MusicUtils.getSongName()))
                        .songsToDelete(Observable.just(Collections.singletonList(MusicUtils.getSong())))
                        .build()
                        .show();
                return true;
            }
            case NEW_PLAYLIST: {
                PlaylistUtils.createPlaylistDialog(this, MusicUtils.getQueue());
                return true;
            }
            case PLAYLIST_SELECTED: {
                List<Song> songs = MusicUtils.getQueue();
                Playlist playlist = (Playlist) item.getIntent().getSerializableExtra(ShuttleUtils.ARG_PLAYLIST);
                PlaylistUtils.addToPlaylist(this, playlist, songs);
                return true;
            }
            case CLEAR_QUEUE: {
                MusicUtils.clearQueue();
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                return true;
            }
            case TAGGER: {
                TaggerDialog.newInstance(MusicUtils.getSong())
                        .show(getSupportFragmentManager());
                return true;
            }
            case VIEW_INFO: {
                DialogUtils.showSongInfoDialog(this, MusicUtils.getSong());
                return true;
            }
        }

        if (item.getItemId() == R.id.menu_share) {
            String path = MusicUtils.getFilePath();
            if (!TextUtils.isEmpty(path)) {
                DialogUtils.showShareDialog(PlayerActivity.this, MusicUtils.getSong());
            }
            return true;
        }

        return false;
    }

    public void themeUIComponents() {

        if (mPauseButton != null) {
            mPauseButton.setImageDrawable(DrawableUtils.getColoredStateListDrawableWithThemeColor(this, mPauseButton.getDrawable(), ThemeUtils.WHITE));
        }
        if (mNextButton != null) {
            mNextButton.setImageDrawable(DrawableUtils.getColoredStateListDrawableWithThemeColor(this, mNextButton.getDrawable(), ThemeUtils.WHITE));
        }
        if (mPrevButton != null) {
            mPrevButton.setImageDrawable(DrawableUtils.getColoredStateListDrawableWithThemeColor(this, mPrevButton.getDrawable(), ThemeUtils.WHITE));
        }
        if (mSeekBar != null) {
            ThemeUtils.themeSeekBar(this, mSeekBar, true);
        }
        if (mTextViewContainer != null) {
            mTextViewContainer.setBackgroundColor(ColorUtils.getPrimaryColorDark(this));
        }
        if (mButtonContainer != null) {
            mButtonContainer.setBackgroundColor(ColorUtils.getPrimaryColor());
        }

        setShuffleButtonImage();
        setRepeatButtonImage();
    }

    @Override
    public void onClick(View view) {
        if (view == mPauseButton) {
            doPauseResume();

        } else if (view == mNextButton) {
            MusicUtils.next();

        } else if (view == mPrevButton) {
            MusicUtils.previous(true);

        } else if (view == mRepeatButton) {
            cycleRepeat();
        } else if (view == mShuffleButton) {
            toggleShuffle();
        }
    }

    private boolean mFromTouch = false;
    private long mPosOverride = -1;

    @Override
    public void onStartTrackingTouch(SeekBar bar) {
        mLastSeekEventTime = 0;
        mFromTouch = true;
        mCurrentTime.setVisibility(View.VISIBLE);
    }

    @Override
    public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {

        if (!fromUser) {
            return;
        }

        final long now = SystemClock.elapsedRealtime();
        if (now - mLastSeekEventTime > 250) {
            mLastSeekEventTime = now;
            mLastShortSeekEventTime = now;
            mPosOverride = MusicUtils.getDuration() * progress / 1000;
            MusicUtils.seekTo(mPosOverride);
            if (!mFromTouch) {
                mPosOverride = -1;
            }
        } else if (now - mLastShortSeekEventTime > 5) {
            mLastShortSeekEventTime = now;
            mPosOverride = MusicUtils.getDuration() * progress / 1000;
            refreshCurrentTimeText(mPosOverride);
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar bar) {
        if (mPosOverride != -1) {
            MusicUtils.seekTo(mPosOverride);
        }
        mPosOverride = -1;
        mFromTouch = false;
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        super.onServiceConnected(componentName, iBinder);
        updateTrackInfo();
        setPauseButtonImage();

        QueuePagerFragment queuePagerFragment = (QueuePagerFragment) getSupportFragmentManager().findFragmentByTag(QUEUE_PAGER_FRAGMENT);
        if (queuePagerFragment != null) {
            queuePagerFragment.resetAdapter();
            queuePagerFragment.updateQueuePosition();
        }
    }

    @Override
    public void themeColorChanged() {
        themeUIComponents();
    }

    private static final class TimeHandler extends Handler {

        private final WeakReference<PlayerActivity> mNowPlayingActivity;

        public TimeHandler(final PlayerActivity playingActivity) {
            mNowPlayingActivity = new WeakReference<>(playingActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REFRESH:
                    PlayerActivity playerActivity = mNowPlayingActivity.get();
                    if (playerActivity != null) {
                        final long next = playerActivity.refreshCurrentTime();
                        playerActivity.queueNextRefresh(next);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public void queueNextRefresh(long delay) {
        if (!mIsPaused) {
            final Message message = mTimeHandler.obtainMessage(REFRESH);
            mTimeHandler.removeMessages(REFRESH);
            mTimeHandler.sendMessageDelayed(message, delay);
        }
    }

    /**
     * Method refreshCurrentTimeText.
     *
     * @param pos the {@link long} position of the current track}
     */
    private void refreshCurrentTimeText(final long pos) {
        mCurrentTime.setText(StringUtils.makeTimeString(this, pos / 1000));
    }

    /**
     * Method refreshNow.
     *
     * @return long
     */
    long refreshCurrentTime() {

        try {
            final long pos = mPosOverride < 0 ? MusicUtils.getPosition() : mPosOverride;
            if (pos >= 0 && MusicUtils.getDuration() > 0) {
                refreshCurrentTimeText(pos);
                final int progress = (int) (1000 * pos / MusicUtils.getDuration());
                mSeekBar.setProgress(progress);

                if (mFromTouch) {
                    return 500;
                } else if (MusicUtils.isPlaying()) {
                    mCurrentTime.setVisibility(View.VISIBLE);
                } else {
                    // blink the counter
                    final int vis = mCurrentTime.getVisibility();
                    mCurrentTime.setVisibility(vis == View.INVISIBLE ? View.VISIBLE
                            : View.INVISIBLE);
                    return 500;
                }
            } else {
                mCurrentTime.setText("--:--");
                mSeekBar.setProgress(1000);
            }
            // calculate the number of milliseconds until the next full second,
            // so
            // the counter can be updated at just the right time
            final long remaining = 1000 - pos % 1000;
            // approximate how often we would need to refresh the slider to
            // move it smoothly
            int width = mSeekBar.getWidth();
            if (width == 0) {
                width = 320;
            }
            final long smoothrefreshtime = MusicUtils.getDuration() / width;
            if (smoothrefreshtime > remaining) {
                return remaining;
            }
            if (smoothrefreshtime < 20) {
                return 20;
            }
            return smoothrefreshtime;
        } catch (final Exception ignored) {

        }
        return 500;
    }

    public void updateTrackInfo() {

        String totalTime = StringUtils.makeTimeString(this, MusicUtils.getDuration() / 1000);
        String trackName = MusicUtils.getSongName();
        String albumName = MusicUtils.getAlbumName();
        String artistName = MusicUtils.getAlbumArtistName();

        String currentQueuePos = String.valueOf(MusicUtils.getQueuePosition() + 1);
        String queueLength = String.valueOf(MusicUtils.getQueue().size());

        if (totalTime != null && totalTime.length() != 0) {
            mTotalTime.setText(" / " + totalTime);
        }

        if (trackName != null && trackName.length() != 0) {
            mTrack.setText(trackName);
            mTrack.setSelected(true);
        }

        if (albumName != null && artistName != null && albumName.length() != 0 && artistName.length() != 0) {
            if (mArtist == null) {
                mAlbum.setText(artistName + " - " + albumName);
            } else {
                mAlbum.setText(albumName);
                mArtist.setText(artistName);
            }
        }

        mQueuePosition.setText(currentQueuePos + " / " + queueLength);

        queueNextRefresh(1);

        supportInvalidateOptionsMenu();

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main_container);
        if (fragment != null && fragment instanceof LyricsFragment) {
            ((LyricsFragment) fragment).updateLyrics();
        }

        Glide.with(this)
                .load(MusicUtils.getAlbumArtist())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .bitmapTransform(new BlurTransformation(this))
                .override(500, 500)
                .into(mBackgroundImage);
    }

    private final RepeatingImageButton.RepeatListener mRewindListener = (v, howlong, repcnt) -> scanBackward(repcnt, howlong);

    private final RepeatingImageButton.RepeatListener mFastForwardListener = (v, howlong, repcnt) -> scanForward(repcnt, howlong);

    public void scanForward(final int repcnt, long delta) {
        if (repcnt == 0) {
            mStartSeekPos = MusicUtils.getPosition();
            mLastSeekEventTime = 0;
        } else {
            if (delta < 5000) {
                // seek at 10x speed for the first 5 seconds
                delta = delta * 10;
            } else {
                // seek at 40x after that
                delta = 50000 + (delta - 5000) * 40;
            }
            long newpos = mStartSeekPos + delta;
            final long duration = MusicUtils.getDuration();
            if (newpos >= duration) {
                // move to next track
                MusicUtils.next();
                mStartSeekPos -= duration; // is OK to go negative
                newpos -= duration;
            }
            if (delta - mLastSeekEventTime > 250 || repcnt < 0) {
                MusicUtils.seekTo(newpos);
                mLastSeekEventTime = delta;
            }
            if (repcnt >= 0) {
                mPosOverride = newpos;
            } else {
                mPosOverride = -1;
            }
            refreshCurrentTime();
        }
    }

    public void scanBackward(final int repcnt, long delta) {
        if (repcnt == 0) {
            mStartSeekPos = MusicUtils.getPosition();
            mLastSeekEventTime = 0;
        } else {
            if (delta < 5000) {
                // seek at 10x speed for the first 5 seconds
                delta = delta * 10;
            } else {
                // seek at 40x after that
                delta = 50000 + (delta - 5000) * 40;
            }
            long newpos = mStartSeekPos - delta;
            if (newpos < 0) {
                // move to previous track
                MusicUtils.previous(true);
                final long duration = MusicUtils.getDuration();
                mStartSeekPos += duration;
                newpos += duration;
            }
            if (delta - mLastSeekEventTime > 250 || repcnt < 0) {
                MusicUtils.seekTo(newpos);
                mLastSeekEventTime = delta;
            }
            if (repcnt >= 0) {
                mPosOverride = newpos;
            } else {
                mPosOverride = -1;
            }
            refreshCurrentTime();
        }
    }

    public void setShuffleButtonImage() {
        if (mShuffleButton == null) {
            return;
        }

        switch (MusicUtils.getShuffleMode()) {

            case MusicService.ShuffleMode.OFF:
                mShuffleButton.setImageDrawable(DrawableUtils.getWhiteDrawable(this, R.drawable.ic_shuffle_white));
                break;

            default:
                mShuffleButton.setImageDrawable(DrawableUtils.getColoredAccentDrawableNonWhite(this, getResources().getDrawable(R.drawable.ic_shuffle_white)));
                break;
        }
    }

    public void setPauseButtonImage() {
        if (mPauseButton == null) {
            return;
        }
        if (MusicServiceConnectionUtils.sServiceBinder != null && MusicUtils.isPlaying()) {
            mPauseButton.setImageDrawable(DrawableUtils.getColoredStateListDrawableWithThemeColor(this, R.drawable.ic_pause_white, ThemeUtils.WHITE));
        } else {
            mPauseButton.setImageDrawable(DrawableUtils.getColoredStateListDrawableWithThemeColor(this, R.drawable.ic_play_white, ThemeUtils.WHITE));
        }
    }

    public void setRepeatButtonImage() {
        if (mRepeatButton == null) {
            return;
        }
        switch (MusicUtils.getRepeatMode()) {

            case MusicService.RepeatMode.ALL:
                mRepeatButton.setImageDrawable(DrawableUtils.getColoredAccentDrawableNonWhite(this, getResources().getDrawable(R.drawable.ic_repeat_white)));
                break;

            case MusicService.RepeatMode.ONE:
                mRepeatButton.setImageDrawable(DrawableUtils.getColoredAccentDrawableNonWhite(this, getResources().getDrawable(R.drawable.ic_repeat_one_white)));
                break;

            default:
                mRepeatButton.setImageDrawable(DrawableUtils.getWhiteDrawable(this, R.drawable.ic_repeat_white));
                break;
        }
    }

    private void doPauseResume() {
        MusicUtils.playOrPause();
        setPauseButtonImage();
    }

    private void cycleRepeat() {
        MusicUtils.cycleRepeat();
        setRepeatButtonImage();
    }

    private void toggleShuffle() {
        MusicUtils.toggleShuffleMode();
        setRepeatButtonImage();
        setShuffleButtonImage();
    }

    public void toggleLyrics() {

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.abc_fade_in, R.anim.abc_fade_out);

        if (!ShuttleUtils.isTablet()) {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.queue_container);
            if (fragment instanceof LyricsFragment) {
                ft.remove(fragment);
            } else {
                ft.replace(R.id.queue_container, new LyricsFragment(), LYRICS_FRAGMENT);
            }
            ft.commit();
            return;
        }

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main_container);
        if (fragment instanceof LyricsFragment) {
            ft.remove(fragment);
        } else {
            ft.add(R.id.main_container, new LyricsFragment(), LYRICS_FRAGMENT);
        }
        ft.commit();
    }

    public void setToolbarBackroundColor(int color) {
        mToolbar.setBackgroundColor(color);
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}
