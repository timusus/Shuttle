/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.libraries.cast.companionlibrary.cast.tracks.ui;

import com.google.android.gms.cast.MediaTrack;
import com.google.android.libraries.cast.companionlibrary.R;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * An {@link android.widget.ArrayAdapter} for presenting tracks.
 */
public class TracksListAdapter extends ArrayAdapter<MediaTrack>
        implements View.OnClickListener {

    private final List<MediaTrack> mTracks;
    private final Context mContext;
    private int mSelectedPosition = -1;

    public TracksListAdapter(Context context, int resource, List<MediaTrack> tracks,
            int activePosition) {
        super(context, resource);
        this.mContext = context;
        mTracks = new ArrayList<>();
        mTracks.addAll(tracks);
        mSelectedPosition = activePosition;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                    Activity.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.tracks_row_layout, parent, false);

            holder = new Holder((TextView) convertView.findViewById(R.id.text),
                    (RadioButton) convertView.findViewById(R.id.radio));
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }
        holder.radio.setTag(position);
        holder.radio.setChecked(mSelectedPosition == position);
        convertView.setOnClickListener(this);
        holder.label.setText(mTracks.get(position).getName());
        return convertView;
    }

    @Override
    public int getCount() {
        return mTracks == null ? 0 : mTracks.size();
    }

    @Override
    public void onClick(View v) {
        Holder holder = (Holder) v.getTag();
        mSelectedPosition = (Integer) holder.radio.getTag();
        notifyDataSetChanged();
    }

    private class Holder {

        private final TextView label;
        private final RadioButton radio;

        private Holder(TextView label, RadioButton radio) {
            this.label = label;
            this.radio = radio;
        }
    }

    public MediaTrack getSelectedTrack() {
        if (mSelectedPosition >= 0) {
            return mTracks.get(mSelectedPosition);
        }
        return null;
    }

}
