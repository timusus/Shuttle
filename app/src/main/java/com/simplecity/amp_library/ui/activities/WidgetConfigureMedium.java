package com.simplecity.amp_library.ui.activities;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ui.widgets.WidgetProviderMedium;

public class WidgetConfigureMedium extends BaseWidgetConfigure {

    private static final String TAG = "WidgetConfigureMedium";

    @Override
    int[] getWidgetLayouts() {
        return new int[]{R.layout.widget_layout_medium, R.layout.widget_layout_medium_alt};
    }

    @Override
    String getLayoutIdString() {
        return WidgetProviderMedium.ARG_MEDIUM_LAYOUT_ID;
    }

    @Override
    String getUpdateCommandString() {
        return WidgetProviderMedium.CMDAPPWIDGETUPDATE;
    }

    @Override
    int getRootViewId() {
        return R.id.widget_layout_medium;
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}
