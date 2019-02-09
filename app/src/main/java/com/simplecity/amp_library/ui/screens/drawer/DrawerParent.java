package com.simplecity.amp_library.ui.screens.drawer;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.afollestad.aesthetic.Aesthetic;
import com.bignerdranch.expandablerecyclerview.ParentViewHolder;
import com.bignerdranch.expandablerecyclerview.model.Parent;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.StringUtils;
import com.simplecity.amp_library.utils.TypefaceManager;
import java.util.ArrayList;
import java.util.List;

public class DrawerParent implements Parent<DrawerChild> {

    private static final String TAG = "DrawerParent";

    static DrawerParent getLibraryParent(SettingsManager settingsManager) {
        return new DrawerParent(
                Type.LIBRARY,
                R.string.library_title,
                R.drawable.ic_library_music_24dp,
                NavigationEventRelay.librarySelectedEvent,
                true,
                settingsManager);
    }

    static DrawerParent getPlaylistsParent(SettingsManager settingsManager) {
        return new DrawerParent(
                Type.PLAYLISTS,
                R.string.playlists_title,
                R.drawable.ic_queue_music_24dp,
                null,
                true,
                settingsManager);
    }

    static DrawerParent getSleepTimerParent(SettingsManager settingsManager) {
        return new DrawerParent(
                Type.SLEEP_TIMER,
                R.string.sleep_timer,
                R.drawable.ic_sleep_24dp,
                NavigationEventRelay.sleepTimerSelectedEvent,
                false,
                settingsManager);
    }

    static DrawerParent getEqualizerParent(SettingsManager settingsManager) {
        return new DrawerParent(
                Type.EQUALIZER,
                R.string.equalizer,
                R.drawable.ic_equalizer_24dp,
                NavigationEventRelay.equalizerSelectedEvent,
                false,
                settingsManager);
    }

    static DrawerParent getSettingsParent(SettingsManager settingsManager) {
        return new DrawerParent(
                Type.SETTINGS,
                R.string.settings,
                R.drawable.ic_settings_24dp,
                NavigationEventRelay.settingsSelectedEvent,
                false,
                settingsManager);
    }

    static DrawerParent getSupportParent(SettingsManager settingsManager) {
        return new DrawerParent(
                Type.SUPPORT,
                R.string.pref_title_support,
                R.drawable.ic_help_24dp,
                NavigationEventRelay.supportSelectedEvent,
                false,
                settingsManager);
    }

    static DrawerParent getFolderParent(Context context, SettingsManager settingsManager) {
        return new DrawerParent(
                DrawerParent.Type.FOLDERS,
                R.string.folders_title,
                R.drawable.ic_folder_multiple_24dp,
                NavigationEventRelay.getFoldersSelectedEvent((ShuttleApplication) context.getApplicationContext(), settingsManager),
                true,
                settingsManager
        ) {
            @Override
            public boolean isSelectable() {
                return ShuttleUtils.isUpgraded((ShuttleApplication) context.getApplicationContext(), settingsManager);
            }
        };
    }

    public @interface Type {
        int LIBRARY = 0;
        int FOLDERS = 1;
        int PLAYLISTS = 2;
        int SLEEP_TIMER = 3;
        int EQUALIZER = 4;
        int SETTINGS = 5;
        int SUPPORT = 6;
    }

    private boolean selectable = true;

    public interface ClickListener {
        void onClick(DrawerParent drawerParent);
    }

    @Nullable
    private ClickListener listener;

    public void setListener(@Nullable ClickListener listener) {
        this.listener = listener;
    }

    @DrawerParent.Type
    public int type;

    @Nullable
    NavigationEventRelay.NavigationEvent navigationEvent;

    @StringRes
    private int titleResId;

    @DrawableRes
    private int iconResId;

    List<DrawerChild> children = new ArrayList<>();

    private boolean isSelected;

    private boolean timerActive = false;
    private long timeRemaining = 0L;

    private SettingsManager settingsManager;

    DrawerParent(
            @DrawerParent.Type int type,
            int titleResId,
            int iconResId, @
            Nullable NavigationEventRelay.NavigationEvent navigationEvent,
            boolean selectable,
            SettingsManager settingsManager
    ) {
        this.type = type;
        this.titleResId = titleResId;
        this.iconResId = iconResId;
        this.navigationEvent = navigationEvent;
        this.selectable = selectable;
        this.settingsManager = settingsManager;
    }

    @Override
    public List<DrawerChild> getChildList() {
        return children;
    }

    @Override
    public boolean isInitiallyExpanded() {
        return false;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public void setTimerActive(boolean timerActive) {
        this.timerActive = timerActive;
    }

    public void setTimeRemaining(long timeRemaining) {
        this.timeRemaining = timeRemaining;
    }

    public boolean isSelectable() {
        return selectable;
    }

    void onClick() {
        if (listener != null && type != Type.PLAYLISTS) {
            listener.onClick(this);
        }
    }

    public void bindView(ParentHolder holder) {

        holder.bind(this);

        Drawable arrowDrawable = DrawableCompat.wrap(ContextCompat.getDrawable(holder.itemView.getContext(), holder.isExpanded() ? R.drawable.ic_arrow_up_24dp : R.drawable.ic_arrow_down_24dp));
        DrawableCompat.setTint(arrowDrawable, Aesthetic.get(holder.itemView.getContext()).textColorSecondary().blockingFirst());
        holder.expandableIcon.setImageDrawable(arrowDrawable);

        holder.expandableIcon.setVisibility(getChildList().isEmpty() ? View.GONE : View.VISIBLE);

        holder.icon.setImageResource(iconResId);
        if (iconResId != -1) {
            holder.icon.setVisibility(View.VISIBLE);
        } else {
            holder.icon.setVisibility(View.GONE);
        }

        if (titleResId != -1) {
            holder.lineOne.setText(holder.itemView.getResources().getString(titleResId));
            holder.lineOne.setTypeface(TypefaceManager.getInstance().getTypeface(holder.itemView.getContext(), TypefaceManager.SANS_SERIF_MEDIUM));
        }

        if (isSelected) {
            holder.itemView.setActivated(true);
        } else {
            holder.itemView.setActivated(false);
            holder.icon.setAlpha(0.6f);
        }

        if (type == DrawerParent.Type.FOLDERS && !ShuttleUtils.isUpgraded((ShuttleApplication) holder.itemView.getContext().getApplicationContext(), settingsManager)) {
            holder.itemView.setAlpha(0.4f);
        } else {
            holder.itemView.setAlpha(1.0f);
        }

        if (type == DrawerParent.Type.PLAYLISTS) {
            holder.itemView.setAlpha(getChildList().isEmpty() ? 0.4f : 1.0f);
            holder.itemView.setEnabled(!getChildList().isEmpty());
        }

        if (type == Type.SLEEP_TIMER) {
            holder.timeRemaining.setVisibility(timerActive ? View.VISIBLE : View.GONE);
            holder.timeRemaining.setText(StringUtils.makeTimeString(holder.itemView.getContext(), timeRemaining));
        } else {
            holder.timeRemaining.setVisibility(View.GONE);
        }
    }

    static class ParentHolder extends ParentViewHolder {

        private DrawerParent drawerParent;

        @BindView(R.id.icon)
        ImageView icon;

        @BindView(R.id.line_one)
        TextView lineOne;

        @BindView(R.id.expandable_icon)
        ImageView expandableIcon;

        @BindView(R.id.timeRemaining)
        TextView timeRemaining;

        private ObjectAnimator objectAnimator;

        ParentHolder(@NonNull View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }

        void bind(DrawerParent drawerParent) {
            this.drawerParent = drawerParent;
        }

        @Override
        public void onExpansionToggled(boolean expanded) {
            super.onExpansionToggled(expanded);

            if (objectAnimator != null) {
                objectAnimator.cancel();
            }

            objectAnimator = ObjectAnimator.ofFloat(expandableIcon, View.ROTATION,
                    expanded ? expandableIcon.getRotation() : expandableIcon.getRotation(),
                    expanded ? 0f : -180f);
            objectAnimator.setDuration(250);
            objectAnimator.setStartDelay(expanded ? 100 : 0);
            objectAnimator.setInterpolator(new DecelerateInterpolator(1.2f));
            objectAnimator.start();
        }

        @Override
        public void onClick(View v) {
            super.onClick(v);

            drawerParent.onClick();
        }
    }
}