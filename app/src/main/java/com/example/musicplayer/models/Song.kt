package com.example.musicplayer.models

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import com.example.musicplayer.ui.util.WorkWithImage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

data class Song(
    val id: String = "",
    val path: String = "",
    val title: String = "",
    val artist: String = "",
    val duration: String = "",
    val albumId: String = "",
    val album: String = "",
    var numberInAlbum:String = ""
) : java.io.Serializable {
        var image: ByteArray? = null

    init {
        image = WorkWithImage.getSongArt(path)
    }
}



