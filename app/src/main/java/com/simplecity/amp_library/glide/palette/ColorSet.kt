package com.simplecity.amp_library.glide.palette

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.support.annotation.WorkerThread
import com.simplecity.amp_library.utils.color.BitmapPaletteProcessor
import com.simplecity.amp_library.utils.color.ColorHelper

class ColorSet(
    var primaryColor: Int,
    var accentColor: Int,
    var primaryTextColorTinted: Int,
    var secondaryTextColorTinted: Int,
    var primaryTextColor: Int,
    var secondaryTextColor: Int
) {

    companion object {

        private val bitmapPaletteProcessor = BitmapPaletteProcessor()
        private val colorHelper = ColorHelper()

        @WorkerThread
        fun fromBitmap(context: Context, bitmap: Bitmap): ColorSet {
            val colors = bitmapPaletteProcessor.processBitmap(bitmap)
            val tintedTextColors = colorHelper.ensureColors(context, true, colors.first!!, colors.second!!)

            val primaryTextColor = ColorHelper.resolvePrimaryColor(context, colors.first!!)
            val secondaryTextColor = ColorHelper.resolveSecondaryColor(context, colors.first!!)

            return ColorSet(colors.first!!, colors.second!!, tintedTextColors.first!!, tintedTextColors.second!!, primaryTextColor, secondaryTextColor)
        }

        fun fromPrimaryAccentColors(context: Context, primaryColor: Int, accentColor: Int): ColorSet {
            val tintedTextColor = colorHelper.ensureColors(context, true, primaryColor, accentColor)

            val primaryTextColor = ColorHelper.resolvePrimaryColor(context, primaryColor)
            val secondaryTextColor = ColorHelper.resolveSecondaryColor(context, primaryColor)

            return ColorSet(primaryColor, accentColor, tintedTextColor.first!!, tintedTextColor.second!!, primaryTextColor, secondaryTextColor)
        }

        fun empty(): ColorSet {
            return ColorSet(Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT)
        }

        /**
         * @return an approximate byte size for this object. Currently based on 4 integers @ 4 bytes each.
         */
        fun estimatedSize(): Int {
            return 4 * 4
        }
    }
}