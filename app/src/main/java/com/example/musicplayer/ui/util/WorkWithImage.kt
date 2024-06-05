package com.example.musicplayer.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.media.MediaMetadataRetriever
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import com.example.musicplayer.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt


class WorkWithImage {
    companion object {
        suspend fun getSongArt(uri: String): ByteArray? {
            val art = withContext(Dispatchers.Default) {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(uri)
                val art: ByteArray? = retriever.embeddedPicture
                retriever.release()
                art
            }
            return art
        }

        fun setGradientBackGround(art: ByteArray?, view: View, context: Context) {
            val bitmap: Bitmap = if (art != null) {
                BitmapFactory.decodeByteArray(art, 0, art.size)
            } else {
                val drawable = ContextCompat.getDrawable(context, R.drawable.placeholder_no_art)
                drawable?.toBitmap(100, 100)!!
            }
            Palette.from(bitmap).generate { palette ->
                val swatch = palette?.dominantSwatch
                swatch?.also {
                    val gradientDrawable =
                        GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                            intArrayOf(ContextCompat.getColor(context, R.color.purple_500),
                                swatch.rgb))
                    view.background = gradientDrawable
                }
            }
        }

        private fun getBrighterOrDarkerColor(color: Int, factor: Double): Int {
            val a: Int = Color.alpha(color)
            val r = (Color.red(color) * factor).roundToInt()
            val g = (Color.green(color) * factor).roundToInt()
            val b = (Color.blue(color) * factor).roundToInt()
            return Color.argb(a,
                min(r, 255),
                min(g, 255),
                min(b, 255))
        }

        fun getDrawableWithAnotherColor(
            context: Context,
            @DrawableRes
            drawable: Int,
            @ColorRes
            color: Int,
        ): Drawable {
            val unwrappedDrawable = AppCompatResources.getDrawable(context, drawable)
            val wrappedDrawable = DrawableCompat.wrap(unwrappedDrawable!!)
            DrawableCompat.setTint(wrappedDrawable, context.getColor(color))
            return wrappedDrawable
        }
    }
}