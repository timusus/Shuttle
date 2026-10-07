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
import com.simplecity.amp_library.http.DeveloperMessageConfig;
import com.simplecity.amp_library.utils.DialogUtils;
import java.text.DateFormat;
import java.util.Date;

/**
 * One-off message shown on the first launch after the final legacy update. The shown flag is set by the caller when
 * the dialog is displayed, so recreation (rotation) restores the fragment without showing it a second time.
 */
public class DeveloperMessageDialog extends DialogFragment {

    private static final String TAG = "DeveloperMessageDialog";

    private static final String ARG_SALE_END_EPOCH_MILLIS = "sale_end_epoch_millis";

    private static final String ARG_TITLE = "title";
    private static final String ARG_BODY = "body";
    private static final String ARG_SALE_TEXT = "sale_text";

    private static final String REFERRER = "utm_source%3Dshuttle_legacy%26utm_medium%3Ddialog";
    private static final String PACKAGE_MUSIC = "com.simplecityapps.shuttle";
    private static final String PACKAGE_PODCASTS = "com.simplecityapps.shuttle.podcasts";

    /**
     * The sale line shows only while now is before the config's sale end (0 hides it). Blank title, body or sale text
     * fall back to the bundled strings.
     */
    public static DeveloperMessageDialog newInstance(DeveloperMessageConfig config) {
        DeveloperMessageDialog dialog = new DeveloperMessageDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_SALE_END_EPOCH_MILLIS, config.saleEndEpochMillis);
        args.putString(ARG_TITLE, DeveloperMessageConfig.clean(config.title, DeveloperMessageConfig.MAX_TITLE));
        args.putString(ARG_BODY, DeveloperMessageConfig.clean(config.body, DeveloperMessageConfig.MAX_BODY));
        args.putString(ARG_SALE_TEXT, DeveloperMessageConfig.clean(config.saleText, DeveloperMessageConfig.MAX_SALE_TEXT));
        dialog.setArguments(args);
        return dialog;
    }

    public void show(FragmentManager fragmentManager) {
        show(fragmentManager, TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = requireContext();

        Bundle args = getArguments() != null ? getArguments() : Bundle.EMPTY;
        long saleEnd = args.getLong(ARG_SALE_END_EPOCH_MILLIS, 0);
        String title = args.getString(ARG_TITLE);
        String body = args.getString(ARG_BODY);
        String saleText = args.getString(ARG_SALE_TEXT);

        StringBuilder message = new StringBuilder(body != null ? body : getString(R.string.dev_message_body_intro));
        if (saleEnd > 0 && System.currentTimeMillis() < saleEnd) {
            String date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(saleEnd));
            message.append("\n\n").append(saleText != null ? saleText.replace("{date}", date) : getString(R.string.dev_message_sale, date));
        }
        message.append("\n\n").append(getString(R.string.dev_message_signoff));

        // MaterialDialog's content area scrolls when it doesn't fit.
        return DialogUtils.getBuilder(context)
                .title(title != null ? title : getString(R.string.dev_message_title))
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
