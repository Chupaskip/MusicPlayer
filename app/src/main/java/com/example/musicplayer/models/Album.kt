package com.example.musicplayer.models

import com.example.musicplayer.ui.util.WorkWithImage

data class Album(
    val id: String,
    val path: String,
    val title: String,
    val artist: String,
    val year: String?,
):java.io.Serializable {
    var image: ByteArray? = null

    init {
        image = WorkWithImage.getSongArt(path)
    }
}