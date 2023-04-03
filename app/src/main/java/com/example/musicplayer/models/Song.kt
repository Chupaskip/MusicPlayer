package com.example.musicplayer.models

import android.os.Parcelable
import com.example.musicplayer.ui.util.WorkWithImage
import kotlinx.android.parcel.Parcelize
import kotlinx.parcelize.IgnoredOnParcel

@Parcelize
data class Song(
    val id: String = "",
    val path: String = "",
    val title: String = "",
    val artist: String = "",
    val duration: String = "",
    val albumId: String = "",
    val album: String = "",
    var numberInAlbum:String? = ""
):Parcelable  {
    @IgnoredOnParcel
    var image: ByteArray? = null
}
suspend fun Song.setImage(){
    this.image = WorkWithImage.getSongArt(this.path)
}





