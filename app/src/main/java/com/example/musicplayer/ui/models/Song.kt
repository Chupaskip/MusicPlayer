package com.example.musicplayer.ui.models

import com.example.musicplayer.ui.util.Image
import java.time.ZoneId

data class Song(
    val id: String,
    val path: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: String,
) : java.io.Serializable {
    var image:ByteArray? = Image.getSongArt(path)
}

fun ArrayList<Song>.setImages(){
    this.forEach {song->
        song.image = Image.getSongArt(song.path)
    }
}

