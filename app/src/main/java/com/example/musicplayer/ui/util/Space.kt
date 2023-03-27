package com.example.musicplayer.ui.util

import android.content.Context

class Space {
    companion object{
        fun fromDpToPixels(context: Context, dps:Int): Int {
            val scale: Float = context?.resources?.displayMetrics?.density!!
            return (dps * scale + 0.5f).toInt()
        }
    }
}
