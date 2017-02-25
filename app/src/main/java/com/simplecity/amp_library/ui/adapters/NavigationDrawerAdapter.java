package com.simplecity.amp_library.ui.adapters;

import android.graphics.LightingColorFilter;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.interfaces.DrawerListCallbacks;
import com.simplecity.amp_library.model.DrawerGroupItem;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.ui.views.AnimatedExpandableListView;
import com.simplecity.amp_library.utils.ColorUtils;
import com.simplecity.amp_library.utils.DrawableUtils;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.ThemeUtils;
import com.simplecity.amp_library.utils.TypefaceManager;

import java.util.ArrayList;
import java.util.List;

public class NavigationDrawerAdapter extends AnimatedExpandableListView.AnimatedExpandableListAdapter {

    private static final String TAG = "NavigationDrawerAdapter";

    List<DrawerGroupItem> mDrawerGroupItems = new ArrayList<>();

    DrawerListCallbacks mDrawerListCallbacks;

    private DrawerGroupItem mSelectedDrawerGroupItem;

    private Playlist mSelectedPlaylist;

    public NavigationDrawerAdapter() {
        DrawerGroupItem library = new DrawerGroupItem(DrawerGroupItem.Type.LIBRARY, R.string.library_title, R.drawable.ic_library_white);
        DrawerGroupItem folders = new DrawerGroupItem(DrawerGroupItem.Type.FOLDERS, R.string.folders_title, R.drawable.ic_folders_many_white);
        DrawerGroupItem playlists = new DrawerGroupItem(DrawerGroupItem.Type.PLAYLISTS, R.string.playlists_title, R.drawable.ic_action_toggle_queue);
        DrawerGroupItem settings = new DrawerGroupItem(DrawerGroupItem.Type.SETTINGS, R.string.settings, R.drawable.ic_action_settings);
        DrawerGroupItem support = new DrawerGroupItem(DrawerGroupItem.Type.SUPPORT, R.string.pref_title_support, R.drawable.ic_settings_help);
        DrawerGroupItem divider = new DrawerGroupItem(DrawerGroupItem.Type.DIVIDER, -1, -1);

        mDrawerGroupItems.add(library);
        mDrawerGroupItems.add(folders);
        mDrawerGroupItems.add(playlists);
        mDrawerGroupItems.add(divider);
        mDrawerGroupItems.add(settings);
        mDrawerGroupItems.add(support);

        mSelectedDrawerGroupItem = library;
    }

    public void setListCallbacks(DrawerListCallbacks drawerListCallbacks) {
        mDrawerListCallbacks = drawerListCallbacks;
    }

    public void setPlaylistData(List<Playlist> playlists) {
        for (DrawerGroupItem groupItem : mDrawerGroupItems) {
            if (groupItem.type == DrawerGroupItem.Type.PLAYLISTS) {
                groupItem.children.clear();
                groupItem.addChildren(playlists);
                break;
            }
        }
        notifyDataSetChanged();
    }

    public void setSelectedItem(DrawerGroupItem drawerGroupItem, Playlist playlist) {
        mSelectedDrawerGroupItem = drawerGroupItem;
        mSelectedPlaylist = playlist;
        notifyDataSetChanged();
    }

    public void clearPlaylistData() {
        for (DrawerGroupItem groupItem : mDrawerGroupItems) {
            if (groupItem.type == DrawerGroupItem.Type.PLAYLISTS) {
                groupItem.children.clear();
                break;
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getGroupCount() {
        return mDrawerGroupItems.size();
    }

    @Override
    public DrawerGroupItem getGroup(int groupPosition) {
        return mDrawerGroupItems.get(groupPosition);
    }

    @Override
    public Playlist getChild(int groupPosition, int childPosition) {
        return mDrawerGroupItems.get(groupPosition).children.get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return 0;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        final GroupViewHolder viewHolder;

        DrawerGroupItem groupItem = getGroup(groupPosition);

        if (groupItem.type == DrawerGroupItem.Type.DIVIDER) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_drawer_divider, parent, false);
        } else {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_drawer, parent, false);
            viewHolder = new GroupViewHolder(convertView);

            viewHolder.position = groupPosition;

            convertView.setClickable(groupItem.getChildCount() == 0);

            int imageResourceId = isExpanded ? R.drawable.ic_arrow_up : R.drawable.ic_arrow_down;
            viewHolder.expandableIcon.setImageDrawable(parent.getResources().getDrawable(imageResourceId));
            viewHolder.expandableIcon.setVisibility(groupItem.getChildCount() == 0 ? View.GONE : View.VISIBLE);
            if (groupItem.iconResId != -1) {
                viewHolder.icon.setImageDrawable(DrawableUtils.themeLightOrDark(parent.getContext(), parent.getResources().getDrawable(groupItem.iconResId)));
                viewHolder.icon.setVisibility(View.VISIBLE);
            } else {
                viewHolder.icon.setVisibility(View.GONE);
            }
            viewHolder.lineOne.setText(parent.getResources().getString(groupItem.titleResId));
            viewHolder.lineOne.setTypeface(TypefaceManager.getInstance().getTypeface(TypefaceManager.SANS_SERIF_MEDIUM));

            if (mSelectedDrawerGroupItem != null && groupItem.type == mSelectedDrawerGroupItem.type) {
                viewHolder.itemView.setActivated(true);
                if (ColorUtils.isPrimaryColorLowContrast(parent.getContext())) {
                    viewHolder.lineOne.setTextColor(ColorUtils.getAccentColor());
                    viewHolder.icon.setColorFilter(ColorUtils.getAccentColor(), PorterDuff.Mode.MULTIPLY);
                } else {
                    viewHolder.lineOne.setTextColor(ColorUtils.getPrimaryColor());
                    viewHolder.icon.setColorFilter(ColorUtils.getPrimaryColor(), PorterDuff.Mode.MULTIPLY);
                }
            } else {
                viewHolder.itemView.setActivated(false);
                viewHolder.lineOne.setTextColor(ColorUtils.getTextColorPrimary());
                viewHolder.icon.setColorFilter(new LightingColorFilter(ThemeUtils.getBaseColor(parent.getContext()), 0));
                viewHolder.icon.setAlpha(0.6f);
            }

            if (groupItem.type == DrawerGroupItem.Type.FOLDERS && !ShuttleUtils.isUpgraded()) {
                viewHolder.itemView.setAlpha(0.4f);
            } else {
                viewHolder.itemView.setAlpha(1.0f);
            }

            if (groupItem.type == DrawerGroupItem.Type.PLAYLISTS) {
                viewHolder.itemView.setAlpha(groupItem.children.isEmpty() ? 0.4f : 1.0f);
                viewHolder.itemView.setEnabled(!groupItem.children.isEmpty());
            }
        }

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public View getRealChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        final ChildViewHolder viewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_drawer, parent, false);
            viewHolder = new ChildViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ChildViewHolder) convertView.getTag();
        }

        DrawerGroupItem groupItem = getGroup(groupPosition);
        Playlist playlist = groupItem.children.get(childPosition);

        if (playlist != null && mSelectedPlaylist != null) {
            if (playlist.name.equals(mSelectedPlaylist.name)) {
                if (ColorUtils.isPrimaryColorLowContrast(parent.getContext())) {
                    viewHolder.lineOne.setTextColor(ColorUtils.getAccentColor());
                } else {
                    viewHolder.lineOne.setTextColor(ColorUtils.getPrimaryColor());
                }
            } else {
                viewHolder.lineOne.setTextColor(ColorUtils.getTextColorPrimary());
            }
        }

        convertView.setClickable(true);
        viewHolder.groupPosition = groupPosition;
        viewHolder.childPosition = childPosition;
        viewHolder.expandableIcon.setVisibility(View.GONE);
        viewHolder.lineOne.setText(playlist.name);
        viewHolder.lineOne.setAlpha(0.54f);

        return convertView;
    }

    @Override
    public int getRealChildrenCount(int groupPosition) {
        return mDrawerGroupItems.get(groupPosition).children.size();
    }

    private class GroupViewHolder implements
            View.OnClickListener {

        int position;
        View itemView;
        ImageView icon;
        ImageView expandableIcon;
        TextView lineOne;

        public GroupViewHolder(final View itemView) {
            this.itemView = itemView;
            icon = (ImageView) itemView.findViewById(R.id.icon);
            lineOne = (TextView) itemView.findViewById(R.id.line_one);
            expandableIcon = (ImageView) itemView.findViewById(R.id.expandable_icon);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (v == itemView) {
                if (mDrawerListCallbacks != null) {
                    mDrawerListCallbacks.onDrawerItemClick(mDrawerGroupItems.get(position));
                }
            }
        }
    }

    private class ChildViewHolder implements
            View.OnClickListener {

        int groupPosition;
        int childPosition;
        View itemView;
        ImageView icon;
        ImageView expandableIcon;
        TextView lineOne;
        ImageButton overFlow;

        public ChildViewHolder(final View itemView) {
            this.itemView = itemView;
            icon = (ImageView) itemView.findViewById(R.id.icon);
            icon.setVisibility(View.INVISIBLE);
            lineOne = (TextView) itemView.findViewById(R.id.line_one);
            expandableIcon = (ImageView) itemView.findViewById(R.id.expandable_icon);
            overFlow = (ImageButton) itemView.findViewById(R.id.btn_overflow);
            overFlow.setVisibility(View.VISIBLE);
            overFlow.setImageDrawable(DrawableUtils.getColoredStateListDrawable(overFlow.getContext(), R.drawable.ic_overflow_white));
            overFlow.setOnClickListener(this);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (v == itemView) {
                if (mDrawerListCallbacks != null) {
                    mDrawerListCallbacks.onPlaylistItemClick(mDrawerGroupItems.get(groupPosition), mDrawerGroupItems.get(groupPosition).children.get(childPosition));
                }
            } else if (v == overFlow) {
                if (mDrawerListCallbacks != null) {
                    mDrawerListCallbacks.onOverflowButtonClick(v, mDrawerGroupItems.get(groupPosition).children.get(childPosition));
                }
            }
        }
    }
}
