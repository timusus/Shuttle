package com.simplecity.amp_library.ui.widgets;

import com.simplecity.amp_library.R;

public class WidgetConfigureActivitySmall extends BaseWidgetConfigureActivity {

    private static final String TAG = "WidgetConfigureActivitySmall";

    @Override
    int[] getWidgetLayouts() {
        return new int[] { R.layout.widget_layout_small };
    }

    @Override
    String getLayoutIdString() {
        return WidgetProviderSmall.ARG_SMALL_LAYOUT_ID;
    }

    @Override
    String getUpdateCommandString() {
        return WidgetProviderSmall.CMDAPPWIDGETUPDATE;
    }

    @Override
    int getRootViewId() {
        return R.id.widget_layout_small;
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}
