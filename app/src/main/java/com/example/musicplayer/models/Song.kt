package com.example.musicplayer.models

import com.example.musicplayer.ui.util.WorkWithImage
data class Song(
    val id: String = "",
    val path: String = "",
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val duration: String = "",
) : java.io.Serializable {
    var image:ByteArray? =null
}

fun MutableList<Song>.setImages(){
    this.forEach {song->
        song.image = WorkWithImage.getSongArt(song.path)
    }
}

