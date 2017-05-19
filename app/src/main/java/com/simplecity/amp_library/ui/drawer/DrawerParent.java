package com.simplecity.amp_library.ui.drawer;

import android.animation.ObjectAnimator;
import android.graphics.LightingColorFilter;
import android.graphics.PorterDuff;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.bignerdranch.expandablerecyclerview.ParentViewHolder;
import com.bignerdranch.expandablerecyclerview.model.Parent;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.utils.ColorUtils;
import com.simplecity.amp_library.utils.DrawableUtils;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.ThemeUtils;
import com.simplecity.amp_library.utils.TypefaceManager;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DrawerParent implements Parent<DrawerChild> {

    static DrawerParent libraryParent = new DrawerParent(DrawerParent.Type.LIBRARY, R.string.library_title, R.drawable.ic_library_white, DrawerEventRelay.librarySelectedEvent, true);
    static DrawerParent folderParent = new DrawerParent(DrawerParent.Type.FOLDERS, R.string.folders_title, R.drawable.ic_folders_many_white, DrawerEventRelay.foldersSelectedEvent, true);
    static DrawerParent equalizerParent = new DrawerParent(Type.EQUALIZER, R.string.equalizer, R.drawable.ic_equalizer_24dp, DrawerEventRelay.equalizerSelectedEvent, false);
    static DrawerParent settingsParent = new DrawerParent(DrawerParent.Type.SETTINGS, R.string.settings, R.drawable.ic_action_settings, DrawerEventRelay.settingsSelectedEvent, false);
    static DrawerParent supportParent = new DrawerParent(DrawerParent.Type.SUPPORT, R.string.pref_title_support, R.drawable.ic_settings_help, DrawerEventRelay.supportSelectedEvent, false);
    static DrawerParent playlistsParent = new DrawerParent(DrawerParent.Type.PLAYLISTS, R.string.playlists_title, R.drawable.ic_action_toggle_queue, null, true);

    public @interface Type {
        int LIBRARY = 0;
        int FOLDERS = 1;
        int PLAYLISTS = 2;
        int EQUALIZER = 3;
        int SETTINGS = 4;
        int SUPPORT = 5;
    }

    boolean selectable = true;

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

    @Nullable DrawerEventRelay.DrawerEvent drawerEvent;

    @StringRes private int titleResId;

    @DrawableRes private int iconResId;

    List<DrawerChild> children = new ArrayList<>();

    boolean isSelected;

    DrawerParent(@DrawerParent.Type int type, int titleResId, int iconResId, @Nullable DrawerEventRelay.DrawerEvent drawerEvent, boolean selectable) {
        this.type = type;
        this.titleResId = titleResId;
        this.iconResId = iconResId;
        this.drawerEvent = drawerEvent;
        this.selectable = selectable;
    }

    @Override
    public List<DrawerChild> getChildList() {
        return children;
    }

    @Override
    public boolean isInitiallyExpanded() {
        return false;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    void onClick() {
        if (listener != null && type != Type.PLAYLISTS) {
            listener.onClick(this);
        }
    }

    public void bindView(ParentHolder holder) {

        holder.bind(this);

        int imageResourceId = holder.isExpanded() ? R.drawable.ic_arrow_up : R.drawable.ic_arrow_down;
        holder.expandableIcon.setImageDrawable(holder.itemView.getResources().getDrawable(imageResourceId));
        holder.expandableIcon.setVisibility(getChildList().isEmpty() ? View.GONE : View.VISIBLE);

        if (iconResId != -1) {
            holder.icon.setImageDrawable(DrawableUtils.themeLightOrDark(holder.itemView.getContext(), holder.itemView.getResources().getDrawable(iconResId)));
            holder.icon.setVisibility(View.VISIBLE);
        } else {
            holder.icon.setVisibility(View.GONE);
        }

        if (titleResId != -1) {
            holder.lineOne.setText(holder.itemView.getResources().getString(titleResId));
            holder.lineOne.setTypeface(TypefaceManager.getInstance().getTypeface(TypefaceManager.SANS_SERIF_MEDIUM));
        }

        if (isSelected) {
            holder.itemView.setActivated(true);
            if (ColorUtils.isPrimaryColorLowContrast(holder.itemView.getContext())) {
                holder.lineOne.setTextColor(ColorUtils.getAccentColor());
                holder.icon.setColorFilter(ColorUtils.getAccentColor(), PorterDuff.Mode.MULTIPLY);
            } else {
                holder.lineOne.setTextColor(ColorUtils.getPrimaryColor());
                holder.icon.setColorFilter(ColorUtils.getPrimaryColor(), PorterDuff.Mode.MULTIPLY);
            }
        } else {
            holder.itemView.setActivated(false);
            holder.lineOne.setTextColor(ColorUtils.getTextColorPrimary());
            holder.icon.setColorFilter(new LightingColorFilter(ThemeUtils.getBaseColor(holder.itemView.getContext()), 0));
            holder.icon.setAlpha(0.6f);
        }

        if (type == DrawerParent.Type.FOLDERS && !ShuttleUtils.isUpgraded()) {
            holder.itemView.setAlpha(0.4f);
            holder.itemView.setEnabled(false);
        } else {
            holder.itemView.setEnabled(true);
            holder.itemView.setAlpha(1.0f);
        }

        if (type == DrawerParent.Type.PLAYLISTS) {
            holder.itemView.setAlpha(getChildList().isEmpty() ? 0.4f : 1.0f);
            holder.itemView.setEnabled(!getChildList().isEmpty());
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