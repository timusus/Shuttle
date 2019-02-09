package com.simplecity.amp_library.format;

import android.content.Context;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;
import com.afollestad.aesthetic.Aesthetic;
import javax.inject.Inject;

/**
 * Highlights the text in a text field
 */
public class PrefixHighlighter {

    private final int mPrefixHighlightColor;

    private ForegroundColorSpan mPrefixColorSpan;

    @Inject
    public PrefixHighlighter(Context context) {
        mPrefixHighlightColor = Aesthetic.get(context).colorAccent().blockingFirst();
    }

    /**
     * Sets the text on the given {@link TextView}, highlighting the word that
     * matches the given prefix
     *
     * @param view The {@link TextView} on which to set the text
     * @param prefix The prefix to look for
     */
    public void setText(TextView view, char[] prefix) {
        setText(view, view.getText().toString(), prefix);
    }

    /**
     * Sets the text on the given {@link TextView}, highlighting the word that
     * matches the given prefix
     *
     * @param view The {@link TextView} on which to set the text
     * @param text The string to use as the text
     * @param prefix The prefix to look for
     */
    public void setText(TextView view, String text, char[] prefix) {
        if (view == null) {
            return;
        }
        if ((prefix == null || prefix.length == 0)) {
            view.setText(text);
        } else if (!TextUtils.isEmpty(text)) {
            view.setText(apply(text, prefix));
        }
    }

    /**
     * Returns a {@link CharSequence} which highlights the given prefix if found
     * in the given text
     *
     * @param text the text to which to apply the highlight
     * @param prefix the prefix to look for
     */
    private CharSequence apply(CharSequence text, char[] prefix) {
        final int index = indexOfWordPrefix(text, prefix);
        if (index != -1) {
            if (mPrefixColorSpan == null) {
                mPrefixColorSpan = new ForegroundColorSpan(mPrefixHighlightColor);
            }
            final SpannableString result = new SpannableString(text);
            result.setSpan(mPrefixColorSpan, index, index + prefix.length, 0);
            return result;
        } else {
            return text;
        }
    }

    /**
     * Finds the index of the first word that starts with the given prefix. If
     * not found, returns -1
     *
     * @param text the text in which to search for the prefix
     * @param prefix the text to find, in upper case letters
     */
    private static int indexOfWordPrefix(CharSequence text, char[] prefix) {
        if (TextUtils.isEmpty(text) || prefix == null) {
            return -1;
        }

        final int tlen = text.length();
        final int plen = prefix.length;

        if (plen == 0 || tlen < plen) {
            return -1;
        }

        int i = 0;
        while (i < tlen) {
            // Skip non-word characters
            while (i < tlen && !Character.isLetterOrDigit(text.charAt(i))) {
                i++;
            }

            if (i + plen > tlen) {
                return -1;
            }

            //  Compare the prefixes
            int j;
            for (j = 0; j < plen; j++) {
                if (Character.toUpperCase(text.charAt(i + j)) != prefix[j]) {
                    break;
                }
            }
            if (j == plen) {
                return i;
            }

            // Skip this word
            while (i < tlen && Character.isLetterOrDigit(text.charAt(i))) {
                i++;
            }
        }
        return -1;
    }
}