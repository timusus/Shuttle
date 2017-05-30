package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.util.Log;
import android.view.ViewGroup;

public class CustomMediaRouteActionProvider extends MediaRouteActionProvider {
    private static final String TAG = "MediaRteActProvider";

    private MediaRouteSelector selector = MediaRouteSelector.EMPTY;
    private CustomMediaRouteButton button;

    public CustomMediaRouteActionProvider(Context context) {
        super(context);
    }

    /**
     * Called when the media route button is being created.
     */
    @SuppressWarnings("deprecation")
    @Override
    public CustomMediaRouteButton onCreateMediaRouteButton() {
        if (button != null) {
            Log.e(TAG, "onCreateMediaRouteButton: This ActionProvider is already associated "
                    + "with a menu item. Don't reuse MediaRouteActionProvider instances!  "
                    + "Abandoning the old button...");
        }

        button = new CustomMediaRouteButton(getContext());
        button.setRouteSelector(selector);
        button.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.FILL_PARENT));
        return button;
    }

}