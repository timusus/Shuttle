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

/**
 * One-off message shown on the first launch after the final legacy update. The shown flag is set by the caller when
 * the dialog is displayed, so recreation (rotation) restores the fragment without showing it a second time.
 */
public class DeveloperMessageDialog extends DialogFragment {

    private static final String TAG = "DeveloperMessageDialog";

    private static final String ARG_TITLE = "title";
    private static final String ARG_BODY = "body";

    private static final String REFERRER = "utm_source%3Dshuttle_legacy%26utm_medium%3Ddialog";
    private static final String PACKAGE_MUSIC = "com.simplecityapps.shuttle";

    public static DeveloperMessageDialog newInstance(DeveloperMessageConfig config) {
        DeveloperMessageDialog dialog = new DeveloperMessageDialog();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, DeveloperMessageConfig.clean(config.title, DeveloperMessageConfig.MAX_TITLE));
        args.putString(ARG_BODY, DeveloperMessageConfig.clean(config.body, DeveloperMessageConfig.MAX_BODY));
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
        String title = args.getString(ARG_TITLE);
        String body = args.getString(ARG_BODY);

        // MaterialDialog's content area scrolls when it doesn't fit.
        return DialogUtils.getBuilder(context)
                .title(title)
                .content(body)
                .positiveText(R.string.dev_message_button_music)
                .onPositive((dialog, which) -> openStore(PACKAGE_MUSIC))
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
