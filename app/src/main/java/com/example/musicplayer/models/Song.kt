package com.example.musicplayer.models

import com.example.musicplayer.ui.util.WorkWithImage

data class Song(
    val id: String = "",
    val path: String = "",
    val title: String = "",
    val artist: String = "",
    val duration: String = "",
    val albumId: String = "",
    val album: String = "",
    var numberInAlbum:String? = ""
) : java.io.Serializable {
        var image: ByteArray? = null

}
suspend fun Song.setImage(){
    this.image = WorkWithImage.getSongArt(this.path)
}





