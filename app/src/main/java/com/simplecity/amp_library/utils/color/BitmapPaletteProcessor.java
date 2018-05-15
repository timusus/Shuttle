/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.simplecity.amp_library.utils.color;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v4.util.Pair;
import android.support.v7.graphics.Palette;
import java.util.List;

/**
 * A class the processes media notifications and extracts the right text and background colors.
 */
public class BitmapPaletteProcessor {

    /**
     * The fraction below which we select the vibrant instead of the light/dark vibrant color
     */
    private static final float POPULATION_FRACTION_FOR_MORE_VIBRANT = 1.0f;

    /**
     * Minimum saturation that a muted color must have if there exists if deciding between two
     * colors
     */
    private static final float MIN_SATURATION_WHEN_DECIDING = 0.19f;

    /**
     * Minimum fraction that any color must have to be picked up as a text color
     */
    private static final double MINIMUM_IMAGE_FRACTION = 0.002;

    /**
     * The population fraction to select the dominant color as the text color over a the colored
     * ones.
     */
    private static final float POPULATION_FRACTION_FOR_DOMINANT = 0.01f;

    /**
     * The population fraction to select a white or black color as the background over a color.
     */
    private static final float POPULATION_FRACTION_FOR_WHITE_OR_BLACK = 2.5f;
    private static final float BLACK_MAX_LIGHTNESS = 0.08f;
    private static final float WHITE_MIN_LIGHTNESS = 0.90f;
    private static final int RESIZE_BITMAP_AREA = 150 * 150;
    private float[] mFilteredBackgroundHsl = null;
    private Palette.Filter mBlackWhiteFilter = (rgb, hsl) -> !isWhiteOrBlack(hsl);


    /**
     * Processes a builder of a media notification and calculates the appropriate colors that should
     * be used.
     *
     * @param bitmap
     *
     * returns a {@link Pair} of integers. The first is the background colour, second is foreground colour.
     */
    public Pair<Integer, Integer> processBitmap(Bitmap bitmap) {
        int backgroundColor = 0;
        Palette.Builder paletteBuilder = Palette.from(bitmap).clearFilters(); // we want all colors, red / white / black ones too!;
        Palette palette = paletteBuilder.generate();
        backgroundColor = findBackgroundColorAndFilter(palette);
        if (mFilteredBackgroundHsl != null) {
            paletteBuilder.addFilter((rgb, hsl) -> {
                // at least 10 degrees hue difference
                float diff = Math.abs(hsl[0] - mFilteredBackgroundHsl[0]);
                return diff > 10 && diff < 350;
            });
        }
        paletteBuilder.addFilter(mBlackWhiteFilter);
        palette = paletteBuilder.generate();

        int foregroundColor = selectForegroundColor(backgroundColor, palette);

        return new Pair<>(backgroundColor, foregroundColor);
    }

    private int selectForegroundColor(int backgroundColor, Palette palette) {
        if (ColorHelper.isColorLight(backgroundColor)) {
            return selectForegroundColorForSwatches(palette.getDarkVibrantSwatch(),
                    palette.getVibrantSwatch(),
                    palette.getDarkMutedSwatch(),
                    palette.getMutedSwatch(),
                    palette.getDominantSwatch(),
                    Color.BLACK);
        } else {
            return selectForegroundColorForSwatches(palette.getLightVibrantSwatch(),
                    palette.getVibrantSwatch(),
                    palette.getLightMutedSwatch(),
                    palette.getMutedSwatch(),
                    palette.getDominantSwatch(),
                    Color.WHITE);
        }
    }

    private int selectForegroundColorForSwatches(Palette.Swatch moreVibrant,
            Palette.Swatch vibrant, Palette.Swatch moreMutedSwatch, Palette.Swatch mutedSwatch,
            Palette.Swatch dominantSwatch, int fallbackColor) {
        Palette.Swatch coloredCandidate = selectVibrantCandidate(moreVibrant, vibrant);
        if (coloredCandidate == null) {
            coloredCandidate = selectMutedCandidate(mutedSwatch, moreMutedSwatch);
        }
        if (coloredCandidate != null) {
            if (dominantSwatch == coloredCandidate) {
                return coloredCandidate.getRgb();
            } else if ((float) coloredCandidate.getPopulation() / dominantSwatch.getPopulation()
                    < POPULATION_FRACTION_FOR_DOMINANT
                    && dominantSwatch.getHsl()[1] > MIN_SATURATION_WHEN_DECIDING) {
                return dominantSwatch.getRgb();
            } else {
                return coloredCandidate.getRgb();
            }
        } else if (hasEnoughPopulation(dominantSwatch)) {
            return dominantSwatch.getRgb();
        } else {
            return fallbackColor;
        }
    }

    private Palette.Swatch selectMutedCandidate(Palette.Swatch first,
            Palette.Swatch second) {
        boolean firstValid = hasEnoughPopulation(first);
        boolean secondValid = hasEnoughPopulation(second);
        if (firstValid && secondValid) {
            float firstSaturation = first.getHsl()[1];
            float secondSaturation = second.getHsl()[1];
            float populationFraction = first.getPopulation() / (float) second.getPopulation();
            if (firstSaturation * populationFraction > secondSaturation) {
                return first;
            } else {
                return second;
            }
        } else if (firstValid) {
            return first;
        } else if (secondValid) {
            return second;
        }
        return null;
    }

    private Palette.Swatch selectVibrantCandidate(Palette.Swatch first, Palette.Swatch second) {
        boolean firstValid = hasEnoughPopulation(first);
        boolean secondValid = hasEnoughPopulation(second);
        if (firstValid && secondValid) {
            int firstPopulation = first.getPopulation();
            int secondPopulation = second.getPopulation();
            if (firstPopulation / (float) secondPopulation
                    < POPULATION_FRACTION_FOR_MORE_VIBRANT) {
                return second;
            } else {
                return first;
            }
        } else if (firstValid) {
            return first;
        } else if (secondValid) {
            return second;
        }
        return null;
    }

    private boolean hasEnoughPopulation(Palette.Swatch swatch) {
        // We want a fraction that is at least 1% of the image
        return swatch != null
                && (swatch.getPopulation() / (float) RESIZE_BITMAP_AREA > MINIMUM_IMAGE_FRACTION);
    }

    private int findBackgroundColorAndFilter(Palette palette) {
        // by default we use the dominant palette
        Palette.Swatch dominantSwatch = palette.getDominantSwatch();
        if (dominantSwatch == null) {
            // We're not filtering on white or black
            mFilteredBackgroundHsl = null;
            return Color.WHITE;
        }

        if (!isWhiteOrBlack(dominantSwatch.getHsl())) {
            mFilteredBackgroundHsl = dominantSwatch.getHsl();
            return dominantSwatch.getRgb();
        }
        // Oh well, we selected black or white. Lets look at the second color!
        List<Palette.Swatch> swatches = palette.getSwatches();
        float highestNonWhitePopulation = -1;
        Palette.Swatch second = null;
        for (Palette.Swatch swatch: swatches) {
            if (swatch != dominantSwatch
                    && swatch.getPopulation() > highestNonWhitePopulation
                    && !isWhiteOrBlack(swatch.getHsl())) {
                second = swatch;
                highestNonWhitePopulation = swatch.getPopulation();
            }
        }
        if (second == null) {
            // We're not filtering on white or black
            mFilteredBackgroundHsl = null;
            return dominantSwatch.getRgb();
        }
        if (dominantSwatch.getPopulation() / highestNonWhitePopulation
                > POPULATION_FRACTION_FOR_WHITE_OR_BLACK) {
            // The dominant swatch is very dominant, lets take it!
            // We're not filtering on white or black
            mFilteredBackgroundHsl = null;
            return dominantSwatch.getRgb();
        } else {
            mFilteredBackgroundHsl = second.getHsl();
            return second.getRgb();
        }
    }

    private boolean isWhiteOrBlack(float[] hsl) {
        return isBlack(hsl) || isWhite(hsl);
    }


    /**
     * @return true if the color represents a color which is close to black.
     */
    private boolean isBlack(float[] hslColor) {
        return hslColor[2] <= BLACK_MAX_LIGHTNESS;
    }

    /**
     * @return true if the color represents a color which is close to white.
     */
    private boolean isWhite(float[] hslColor) {
        return hslColor[2] >= WHITE_MIN_LIGHTNESS;
    }

}