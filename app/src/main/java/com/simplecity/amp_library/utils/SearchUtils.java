package com.simplecity.amp_library.utils;

public class SearchUtils {

    private static final String TAG = "SearchUtils";

    public static class JaroWinklerObject<T> {
        public T object;
        String[] fields;
        public double score;

        /**
         * Calculate the highest Jaro-Winkler score, for the passed in filter string & fields.
         * The object param is just a holder.
         *
         * @param object       a holder for the object which owns the fields being compared.
         * @param filterString the string to match fields against
         * @param fields       the fields to match the filter string against. Order matters here: subsequent fields
         *                     have a small amount shaved off in order to weight results in favour of earlier fields.
         */
        public JaroWinklerObject(T object, String filterString, String... fields) {
            this.object = object;
            this.fields = fields;

            //Iterate over our fields, and take the highest matching score.
            //We subtract a little from the score each iteration, so that the first field takes precedence.
            //In other words, the scores are weighted in favour of field order.
            for (int i = 0, fieldsLength = fields.length; i < fieldsLength; i++) {
                String field = fields[i];
                score = Math.max(score, StringUtils.getJaroWinklerSimilarity(field, filterString) - (i * 0.001));
            }
        }
    }
}