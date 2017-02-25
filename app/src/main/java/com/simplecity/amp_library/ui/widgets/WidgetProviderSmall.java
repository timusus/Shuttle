package com.simplecity.amp_library.ui.widgets;

import android.content.Context;
import android.content.res.Resources;
import android.os.Environment;
import android.view.View;
import android.widget.RemoteViews;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.playback.MusicService;
import com.simplecity.amp_library.utils.ColorUtils;
import com.simplecity.amp_library.utils.DrawableUtils;

public class WidgetProviderSmall extends BaseWidgetProvider {

    private static final String TAG = "MusicAppWidgetProvider";

    public static final String ARG_SMALL_LAYOUT_ID = "widget_small_layout_id_";

    public static final String CMDAPPWIDGETUPDATE = "appwidgetupdate_small";

    private static WidgetProviderSmall sInstance;

    public static synchronized WidgetProviderSmall getInstance() {
        if (sInstance == null) {
            sInstance = new WidgetProviderSmall();
        }
        return sInstance;
    }

    @Override
    public String getUpdateCommandString() {
        return CMDAPPWIDGETUPDATE;
    }

    @Override
    public String getLayoutIdString() {
        return ARG_SMALL_LAYOUT_ID;
    }

    @Override
    public int getWidgetLayoutId() {
        return R.layout.widget_layout_small;
    }

    @Override
    public int getRootViewId() {
        return R.id.widget_layout_small;
    }

    protected void initialiseWidget(Context context, int appWidgetId) {
        final Resources res = context.getResources();
        final RemoteViews views = new RemoteViews(context.getPackageName(), mLayoutId);

        views.setViewVisibility(R.id.text1, View.GONE);
        views.setTextViewText(R.id.text2, res.getText(R.string.widget_initial_text));

        int textColor = mPrefs.getInt(ARG_WIDGET_TEXT_COLOR + appWidgetId, context.getResources().getColor(R.color.white));
        views.setImageViewResource(R.id.next_button, R.drawable.ic_skip_white);
        views.setImageViewResource(R.id.prev_button, R.drawable.ic_prev_white);
        views.setTextColor(R.id.text2, textColor);
        views.setTextColor(R.id.text1, textColor);

        int backgroundColor = mPrefs.getInt(ARG_WIDGET_BACKGROUND_COLOR + appWidgetId, ColorUtils.adjustAlpha(context.getResources().getColor(R.color.white), 35 / 255f));
        views.setInt(R.id.widget_layout_small, "setBackgroundColor", backgroundColor);
        int colorFilter = mPrefs.getInt(ARG_WIDGET_COLOR_FILTER + appWidgetId, -1);
        if (colorFilter != -1) {
            views.setInt(R.id.album_art, "setColorFilter", colorFilter);
        }
        boolean showAlbumArt = mPrefs.getBoolean(ARG_WIDGET_SHOW_ARTWORK + appWidgetId, true);
        if (!showAlbumArt) {
            views.setViewVisibility(R.id.album_art, View.GONE);
        }

        setupButtons(context, views, appWidgetId, getRootViewId());
        pushUpdate(context, appWidgetId, views);
    }

    public void update(MusicService service, int[] appWidgetIds, boolean updateArtwork) {

        if (appWidgetIds == null) {
            return;
        }

        for (int appWidgetId : appWidgetIds) {

            boolean showAlbumArt = mPrefs.getBoolean(ARG_WIDGET_SHOW_ARTWORK + appWidgetId, true);

            mLayoutId = mPrefs.getInt(ARG_SMALL_LAYOUT_ID + appWidgetId, R.layout.widget_layout_small);

            final Resources res = service.getResources();
            final RemoteViews views = new RemoteViews(service.getPackageName(), mLayoutId);

            CharSequence titleName = service.getSongName();
            CharSequence artistName = service.getAlbumArtistName();
            CharSequence errorState = null;

            // Format title string with track number, or show SD card message
            String status = Environment.getExternalStorageState();
            if (status.equals(Environment.MEDIA_SHARED) || status.equals(Environment.MEDIA_UNMOUNTED)) {
                if (android.os.Environment.isExternalStorageRemovable()) {
                    errorState = res.getText(R.string.sdcard_busy_title);
                } else {
                    errorState = res.getText(R.string.sdcard_busy_title_nosdcard);
                }
            } else if (status.equals(Environment.MEDIA_REMOVED)) {
                if (android.os.Environment.isExternalStorageRemovable()) {
                    errorState = res.getText(R.string.sdcard_missing_title);
                } else {
                    errorState = res.getText(R.string.sdcard_missing_title_nosdcard);
                }
            } else if (titleName == null) {
                errorState = res.getText(R.string.emptyplaylist);
            }

            if (errorState != null) {
                // Show error state to user
                views.setViewVisibility(R.id.text1, View.GONE);
                views.setTextViewText(R.id.text2, errorState);

            } else {
                // No error, so show normal titles
                views.setViewVisibility(R.id.text1, View.VISIBLE);
                views.setTextViewText(R.id.text1, titleName);
                views.setTextViewText(R.id.text2, artistName);
            }

            boolean invertIcons = mPrefs.getBoolean(ARG_WIDGET_INVERT_ICONS + appWidgetId, false);

            // Set correct drawable for pause state
            final boolean isPlaying = service.isPlaying();
            if (isPlaying) {
                if (invertIcons) {
                    views.setImageViewBitmap(R.id.play_button, DrawableUtils.getBlackBitmap(service, R.drawable.ic_pause_white));
                } else {
                    views.setImageViewResource(R.id.play_button, R.drawable.ic_pause_white);
                }
            } else {
                if (invertIcons) {
                    views.setImageViewBitmap(R.id.play_button, DrawableUtils.getBlackBitmap(service, R.drawable.ic_play_white));
                } else {
                    views.setImageViewResource(R.id.play_button, R.drawable.ic_play_white);
                }
            }

            switch (service.getShuffleMode()) {
                case MusicService.ShuffleMode.OFF:
                    if (invertIcons) {
                        views.setImageViewBitmap(R.id.shuffle_button, DrawableUtils.getBlackBitmap(service, R.drawable.ic_shuffle_white));
                    } else {
                        views.setImageViewResource(R.id.shuffle_button, R.drawable.ic_shuffle_white);
                    }
                    break;
                default:
                    views.setImageViewBitmap(R.id.shuffle_button, DrawableUtils.getColoredBitmap(service, R.drawable.ic_shuffle_white));
                    break;
            }

            switch (service.getRepeatMode()) {
                case MusicService.RepeatMode.ALL:
                    views.setImageViewBitmap(R.id.repeat_button, DrawableUtils.getColoredBitmap(service, R.drawable.ic_repeat_white));
                    break;
                case MusicService.RepeatMode.ONE:
                    views.setImageViewBitmap(R.id.repeat_button, DrawableUtils.getColoredBitmap(service, R.drawable.ic_repeat_one_white));
                    break;
                default:
                    if (invertIcons) {
                        views.setImageViewBitmap(R.id.repeat_button, DrawableUtils.getBlackBitmap(service, R.drawable.ic_repeat_white));
                    } else {
                        views.setImageViewResource(R.id.repeat_button, R.drawable.ic_repeat_white);
                    }
                    break;
            }

            int textColor = mPrefs.getInt(ARG_WIDGET_TEXT_COLOR + appWidgetId, service.getResources().getColor(R.color.white));
            if (invertIcons) {
                views.setImageViewBitmap(R.id.next_button, DrawableUtils.getBlackBitmap(service, R.drawable.ic_skip_white));
                views.setImageViewBitmap(R.id.prev_button, DrawableUtils.getBlackBitmap(service, R.drawable.ic_prev_white));
            } else {
                views.setImageViewResource(R.id.next_button, R.drawable.ic_skip_white);
                views.setImageViewResource(R.id.prev_button, R.drawable.ic_prev_white);
            }

            views.setTextColor(R.id.text2, textColor);
            views.setTextColor(R.id.text1, textColor);

            int backgroundColor = mPrefs.getInt(ARG_WIDGET_BACKGROUND_COLOR + appWidgetId, ColorUtils.adjustAlpha(service.getResources().getColor(R.color.white), 35 / 255f));
            views.setInt(R.id.widget_layout_small, "setBackgroundColor", backgroundColor);

            setupButtons(service, views, appWidgetId, getRootViewId());

            if (!showAlbumArt) {
                views.setViewVisibility(R.id.album_art, View.GONE);
            }

            views.setImageViewResource(R.id.album_art, R.drawable.ic_placeholder_light_medium);

            pushUpdate(service, appWidgetId, views);
        }
    }
}