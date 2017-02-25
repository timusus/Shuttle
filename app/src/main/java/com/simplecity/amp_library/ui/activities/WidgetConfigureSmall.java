package com.simplecity.amp_library.ui.activities;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ui.widgets.WidgetProviderSmall;

public class WidgetConfigureSmall extends BaseWidgetConfigure {

    private static final String TAG = "WidgetConfigureSmall";

    @Override
    int[] getWidgetLayouts() {
        return new int[]{R.layout.widget_layout_small};
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
