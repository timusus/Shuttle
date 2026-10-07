package com.simplecity.amp_library.ui.dialog;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.utils.DialogUtils;
import java.text.DateFormat;
import java.util.Date;

/**
 * One-off message shown on the first launch after the final legacy update. The shown flag is set by the caller when
 * the dialog is displayed, so recreation (rotation) restores the fragment without showing it a second time.
 */
public class DeveloperMessageDialog extends DialogFragment {

    private static final String TAG = "DeveloperMessageDialog";

    /**
     * Sale end time (epoch millis). Owner sets this before release; 0 hides the line.
     */
    public static final long SALE_END_EPOCH_MILLIS = 0L;

    private static final String REFERRER = "utm_source%3Dshuttle_legacy%26utm_medium%3Ddialog";
    private static final String PACKAGE_MUSIC = "com.simplecityapps.shuttle";
    private static final String PACKAGE_PODCASTS = "com.simplecityapps.shuttle.podcasts";

    public void show(FragmentManager fragmentManager) {
        show(fragmentManager, TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = requireContext();

        StringBuilder message = new StringBuilder(getString(R.string.dev_message_body_intro));
        if (SALE_END_EPOCH_MILLIS > 0 && System.currentTimeMillis() < SALE_END_EPOCH_MILLIS) {
            String date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(SALE_END_EPOCH_MILLIS));
            message.append("\n\n").append(getString(R.string.dev_message_sale, date));
        }
        message.append("\n\n").append(getString(R.string.dev_message_signoff));

        // MaterialDialog's content area scrolls when it doesn't fit.
        return DialogUtils.getBuilder(context)
                .title(R.string.dev_message_title)
                .content(message.toString())
                .positiveText(R.string.dev_message_button_music)
                .onPositive((dialog, which) -> openStore(PACKAGE_MUSIC))
                .neutralText(R.string.dev_message_button_podcasts)
                .onNeutral((dialog, which) -> openStore(PACKAGE_PODCASTS))
                .negativeText(R.string.dev_message_button_dismiss)
                .build();
    }

    private void openStore(String packageName) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        String query = "id=" + packageName + "&referrer=" + REFERRER;
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?" + query)));
        } catch (ActivityNotFoundException e) {
            try {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?" + query)));
            } catch (ActivityNotFoundException ignored) {
                // No store or browser installed.
            }
        }
    }
}
