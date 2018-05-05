package com.simplecity.amp_library.utils;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import com.simplecity.amp_library.R;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Locale;
import java.util.regex.Pattern;

public class StringUtils {

    private static final String TAG = "StringUtils";

    private static StringBuilder sFormatBuilder = new StringBuilder();

    private static Formatter sFormatter = new Formatter(sFormatBuilder, Locale.getDefault());

    private static Pattern pattern = Pattern.compile("^(?i)\\s*(?:the |an |a )|(?:, the|, an|, a)\\s*$|[\\[\\]()!?.,']");

    private StringUtils() {

    }

    /**
     * Method makeTimeString.
     * <p>
     * Todo: Move to StringUtils or somewhere else
     *
     * @param context Context
     * @param secs long
     * @return String
     */
    public static String makeTimeString(@NonNull Context context, long secs) {
        sFormatBuilder.setLength(0);
        //return (secs < 0 ? "- " : "") + (Math.abs(secs) < 3600 ? makeShortTimeString(context, Math.abs(secs)) : makeLongTimeString(context, Math.abs(secs)));
        return Math.abs(secs) < 3600 ? makeShortTimeString(context, secs) : makeLongTimeString(context, secs);
    }

    private static String makeLongTimeString(@NonNull Context context, long secs) {
        return makeTimeString(context.getString(R.string.durationformatlong), secs);
    }

    private static String makeShortTimeString(@NonNull Context context, long secs) {
        return makeTimeString(context.getString(R.string.durationformatshort), secs);
    }

    private static String makeTimeString(String formatString, long secs) {
        long absSeconds = Math.abs(secs);
        sFormatBuilder.setLength(0);
        return sFormatter.format(formatString,
                secs < 0 ? "- " : "",
                absSeconds / 3600,
                absSeconds / 60,
                absSeconds / 60 % 60,
                absSeconds,
                absSeconds % 60)
                .toString();
    }

    /**
     * Method makeSubfoldersLabel.
     *
     * @param context context
     * @param numSubfolders the number of subFolders for this folder
     * @param numSubfiles the number of subFiles for this folder
     * @return a label in the vein of "5 folders | 3 files"
     */
    public static String makeSubfoldersLabel(Context context, int numSubfolders, int numSubfiles) {

        final StringBuilder string = new StringBuilder();

        final Resources r = context.getResources();

        if (numSubfolders != 0) {
            if (numSubfolders == 1) {
                string.append(context.getString(R.string.onefolder));
            } else {
                final String f = r.getQuantityText(R.plurals.Nfolders, numSubfolders)
                        .toString();
                sFormatBuilder.setLength(0);
                sFormatter.format(f, numSubfolders);
                string.append(sFormatBuilder);
            }
        }

        if (numSubfiles > 0 && numSubfolders > 0) {
            string.append(" | ");
        }

        if (numSubfiles != 0) {
            if (numSubfiles == 1) {
                string.append(context.getString(R.string.onesong));
            } else {
                final String f = r.getQuantityText(R.plurals.Nsongs, numSubfiles).toString();
                sFormatBuilder.setLength(0);
                sFormatter.format(f, numSubfiles);
                string.append(sFormatBuilder);
            }
        }

        if (numSubfiles == 0 && numSubfolders == 0) {
            string.append("-");
        }

        return string.toString();
    }

    public static String makeAlbumAndSongsLabel(Context context, int numalbums, int numsongs) {

        final StringBuilder stringBuilder = new StringBuilder();
        final Resources r = context.getResources();

        String f;
        if (numalbums > 0) {
            f = r.getQuantityText(R.plurals.Nalbums, numalbums).toString();
            sFormatBuilder.setLength(0);
            sFormatter.format(f, numalbums);
            stringBuilder.append(sFormatBuilder);
        }

        if (numalbums > 0 && numsongs > 0) {
            stringBuilder.append(" | ");
        }
        if (numsongs == 1) {
            stringBuilder.append(context.getString(R.string.onesong));
        } else if (numsongs > 0) {
            f = r.getQuantityText(R.plurals.Nsongs, numsongs).toString();
            sFormatBuilder.setLength(0);
            sFormatter.format(f, numsongs);
            stringBuilder.append(sFormatBuilder);
        }
        return stringBuilder.toString();
    }

    public static String makeAlbumsLabel(Context context, int numAlbums) {
        final StringBuilder stringBuilder = new StringBuilder();
        String formatString = context.getResources().getQuantityText(R.plurals.Nalbums, numAlbums).toString();
        sFormatBuilder.setLength(0);
        sFormatter.format(formatString, numAlbums);
        stringBuilder.append(sFormatBuilder);

        return stringBuilder.toString();
    }

    public static String makeSongsLabel(Context context, int numSongs) {
        final StringBuilder stringBuilder = new StringBuilder();
        String formatString = context.getResources().getQuantityText(R.plurals.Nsongs, numSongs).toString();
        sFormatBuilder.setLength(0);
        sFormatter.format(formatString, numSongs);
        stringBuilder.append(sFormatBuilder);

        return stringBuilder.toString();
    }

    public static String makeYearLabel(Context context, int year) {

        if (year <= 0) {
            return context.getResources().getString(R.string.unknown_year);
        }

        return String.format("%s", year);
    }

    public static String makeSongsAndTimeLabel(Context context, int numSongs, long secs) {
        return context.getResources().getString(R.string.songs_time_label, makeSongsLabel(context, numSongs), makeLongTimeString(context, secs));
    }

    /**
     * Converts a name to a "key" that can be used for grouping, sorting
     * and searching.
     * The rules that govern this conversion are:
     * - remove 'special' characters like ()[]'!?.,
     * - remove leading/trailing spaces
     * - convert everything to lowercase
     * - remove leading "the ", "an " and "a "
     * - remove trailing ", the|an|a"
     * - remove accents. This step leaves us with CollationKey data,
     * which is not human readable
     *
     * @param name The artist or album name to convert
     * @return The "key" for the given name.
     */
    public static String keyFor(String name) {

        if (!TextUtils.isEmpty(name)) {
            name = pattern.matcher(name)
                    .replaceAll("")
                    .trim()
                    .toLowerCase();
        } else {
            name = "";
        }

        return name;
    }

    /**
     * @return true if String s1 contains String s2, ignoring case.
     */
    public static boolean containsIgnoreCase(String s1, String s2) {
        return s1.toLowerCase().contains(s2.toLowerCase());
    }

    /**
     * Find the Jaro Winkler Similarity which indicates the similarity score between two Strings.
     * <p>
     * Note: This method splits the {@param first} string at whitespaces, and returns the best Jaro-Winkler
     * score between {@param second} and the 'split' strings.
     */
    public static double getAdjustedJaroWinklerSimilarity(@Nullable String first, @Nullable String second) {

        if (TextUtils.isEmpty(first) || TextUtils.isEmpty(second)) {
            return 0;
        }

        String[] split = first.split("\\s");
        if (split.length > 1) {
            double score = 0;
            for (String str : split) {
                double curScore = getJaroWinklerSimilarity(str, second);
                if (curScore > score) {
                    score = curScore;
                }
            }
            //Make sure we do a normal (non-adjusted) test as well, in case that comes out as our best match.
            return Math.max(getJaroWinklerSimilarity(first, second), score);
        } else {
            return getJaroWinklerSimilarity(first, second);
        }
    }

    /**
     * <p>Find the Jaro Winkler Similarity which indicates the similarity score between two Strings.</p>
     * <p>
     * <p>The Jaro measure is the weighted sum of percentage of matched characters from each file and transposed characters.
     * Winkler increased this measure for matching initial characters.</p>
     * <p>
     * <p>This implementation is based on the Jaro Winkler similarity algorithm
     * from <a href="http://en.wikipedia.org/wiki/Jaro%E2%80%93Winkler_distance">http://en.wikipedia.org/wiki/Jaro%E2%80%93Winkler_distance</a>.</p>
     * <p>
     *
     * @param first the first String, must not be null
     * @param second the second String, must not be null
     * @return result similarity
     */
    public static double getJaroWinklerSimilarity(@NonNull String first, @NonNull String second) {

        final double DEFAULT_SCALING_FACTOR = 0.1;

        first = first.toLowerCase();
        second = second.toLowerCase();
        first = Normalizer.normalize(first, Normalizer.Form.NFD);
        second = Normalizer.normalize(second, Normalizer.Form.NFD);

        final int[] mtp = matches(first, second);
        final double m = mtp[0];
        if (m == 0) {
            return 0D;
        }
        final double j = ((m / first.length() + m / second.length() + (m - mtp[1]) / m)) / 3;
        final double jw = j < 0.7D ? j : j + Math.min(DEFAULT_SCALING_FACTOR, 1D / mtp[3]) * mtp[2] * (1D - j);
        return Math.round(jw * 100.0D) / 100.0D;
    }

    private static int[] matches(final CharSequence first, final CharSequence second) {
        CharSequence max, min;
        if (first.length() > second.length()) {
            max = first;
            min = second;
        } else {
            max = second;
            min = first;
        }
        final int range = Math.max(max.length() / 2 - 1, 0);
        final int[] matchIndexes = new int[min.length()];
        Arrays.fill(matchIndexes, -1);
        final boolean[] matchFlags = new boolean[max.length()];
        int matches = 0;
        for (int mi = 0; mi < min.length(); mi++) {
            final char c1 = min.charAt(mi);
            for (int xi = Math.max(mi - range, 0), xn = Math.min(mi + range + 1, max.length()); xi < xn; xi++) {
                if (!matchFlags[xi] && c1 == max.charAt(xi)) {
                    matchIndexes[mi] = xi;
                    matchFlags[xi] = true;
                    matches++;
                    break;
                }
            }
        }
        final char[] ms1 = new char[matches];
        final char[] ms2 = new char[matches];
        for (int i = 0, si = 0; i < min.length(); i++) {
            if (matchIndexes[i] != -1) {
                ms1[si] = min.charAt(i);
                si++;
            }
        }
        for (int i = 0, si = 0; i < max.length(); i++) {
            if (matchFlags[i]) {
                ms2[si] = max.charAt(i);
                si++;
            }
        }
        int transpositions = 0;
        for (int mi = 0; mi < ms1.length; mi++) {
            if (ms1[mi] != ms2[mi]) {
                transpositions++;
            }
        }
        int prefix = 0;
        for (int mi = 0; mi < min.length(); mi++) {
            if (first.charAt(mi) == second.charAt(mi)) {
                prefix++;
            } else {
                break;
            }
        }
        return new int[] { matches, transpositions / 2, prefix, max.length() };
    }

    public static int parseInt(@Nullable String string) {
        if (string != null) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException ignored) {

            }
        }
        return -1;
    }
}