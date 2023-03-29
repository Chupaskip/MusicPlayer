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
}
suspend fun Album.setImage(){
    this.image = WorkWithImage.getSongArt(this.path)
}
