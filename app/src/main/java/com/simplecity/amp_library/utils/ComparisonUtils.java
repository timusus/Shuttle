package com.simplecity.amp_library.utils;

import androidx.annotation.Nullable;
import java.text.Collator;
import java.util.Locale;

public class ComparisonUtils {

    private static Collator collator = Collator.getInstance(Locale.getDefault());

    private ComparisonUtils() {

    }

    public static int compareLong(long x, long y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

    public static int compareInt(int x, int y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

    /**
     * Null-safe string comparison (nulls first). Uses a Collator, which is slower than normal string comparison.
     * <p>
     * Must be a consistent ordering, or Collections.sort() throws "Comparison method violates its general contract".
     */
    public static int compare(@Nullable String str1, @Nullable String str2) {

        if (str1 == null) {
            return str2 == null ? 0 : -1;
        } else if (str2 == null) {
            return 1;
        } else if (str1.equals(str2)) {
            return 0;
        } else {
            // Collator isn't thread safe, and sorts run on several background threads at once.
            synchronized (collator) {
                return collator.compare(str1, str2);
            }
        }
    }
}