package com.example.musicplayer.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val idMemory: String = "",
    val idUser: Long = 0,
    var isFavorite: Boolean = false,
    var lyrics: String = ""
)