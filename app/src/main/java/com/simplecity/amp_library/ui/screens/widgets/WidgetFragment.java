package com.simplecity.amp_library.ui.screens.widgets;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ui.widgets.WidgetConfigureActivityExtraLarge;
import com.simplecity.amp_library.ui.widgets.WidgetConfigureActivityLarge;
import com.simplecity.amp_library.ui.widgets.WidgetConfigureActivityMedium;
import com.simplecity.amp_library.ui.widgets.WidgetConfigureActivitySmall;
import com.simplecity.amp_library.ui.common.BaseFragment;
import dagger.android.support.AndroidSupportInjection;

public class WidgetFragment extends BaseFragment {

    private static final String TAG = "WidgetFragment";

    private static final String ARG_WIDGET_LAYOUT_ID = "widget_layout_id";

    int mWidgetLayoutResId;

    /**
     * Empty constructor as per the fragment docs
     */
    public WidgetFragment() {
    }

    @Override
    public void onAttachFragment(Fragment childFragment) {
        AndroidSupportInjection.inject(this);
        super.onAttachFragment(childFragment);
    }

    /**
     * Creates a new instance of the {@link WidgetFragment}
     *
     * @param widgetLayoutResId the id of the layout to use for the widget
     * @return a new instance of {@link WidgetFragment}
     */
    public static WidgetFragment newInstance(int widgetLayoutResId) {
        WidgetFragment fragment = new WidgetFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_WIDGET_LAYOUT_ID, widgetLayoutResId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mWidgetLayoutResId = getArguments().getInt(ARG_WIDGET_LAYOUT_ID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_widget, container, false);

        FrameLayout frameLayout = (FrameLayout) inflater.inflate(mWidgetLayoutResId, null);
        if (mWidgetLayoutResId == R.layout.widget_layout_extra_large) {
            frameLayout.setScaleX(.75f);
            frameLayout.setScaleY(.75f);
        }

        float height;
        float width;
        FrameLayout.LayoutParams layoutParams = null;
        if (mWidgetLayoutResId == R.layout.widget_layout_medium || mWidgetLayoutResId == R.layout.widget_layout_medium_alt) {
            height = getActivity().getResources().getDimension(R.dimen.widget_medium_height);
            layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, (int) height);
        }
        if (mWidgetLayoutResId == R.layout.widget_layout_large || mWidgetLayoutResId == R.layout.widget_layout_large_alt) {
            height = getActivity().getResources().getDimension(R.dimen.widget_large_height);
            layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, (int) height);
        }
        if (mWidgetLayoutResId == R.layout.widget_layout_small) {
            height = getActivity().getResources().getDimension(R.dimen.widget_small_height);
            width = getActivity().getResources().getDimension(R.dimen.widget_small_width);
            layoutParams = new FrameLayout.LayoutParams((int) width, (int) height);
        }

        FrameLayout frameLayout1 = rootView.findViewById(R.id.frame);

        if (layoutParams != null) {
            frameLayout1.addView(frameLayout, layoutParams);
        } else {
            frameLayout1.addView(frameLayout);
        }

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getActivity() instanceof WidgetConfigureActivitySmall) {
            ((WidgetConfigureActivitySmall) getActivity()).updateWidgetUI();
        } else if (getActivity() instanceof WidgetConfigureActivityMedium) {
            ((WidgetConfigureActivityMedium) getActivity()).updateWidgetUI();
        } else if (getActivity() instanceof WidgetConfigureActivityLarge) {
            ((WidgetConfigureActivityLarge) getActivity()).updateWidgetUI();
        } else if (getActivity() instanceof WidgetConfigureActivityExtraLarge) {
            ((WidgetConfigureActivityExtraLarge) getActivity()).updateWidgetUI();
        }
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}
