package com.example.musicplayer.ui.adapters

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Service
import android.content.ContentUris
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.example.musicplayer.R
import com.example.musicplayer.models.Song
import com.example.musicplayer.ui.MainActivity
import com.example.musicplayer.ui.MusicViewModel
import com.example.musicplayer.ui.fragments.PlayerFragment
import com.example.musicplayer.ui.fragments.SONG
import com.example.musicplayer.ui.fragments.SONGS_IN_PLAYER
import com.example.musicplayer.ui.services.PlayerService
import java.io.File

interface ISongClick {
    val viewModelForClick: MusicViewModel
    val contextForClick: Context
    val activityForClick: MainActivity
    val intentSenderLauncherForClick: ActivityResultLauncher<IntentSenderRequest>
    @SuppressLint("SuspiciousIndentation")
    fun onSongClick(song: Song, fromUser: Boolean = true) {
        if (song.id == (viewModelForClick.songToDelete?.id ?: "")) {
            Toast.makeText(contextForClick, "Song is deleted!", Toast.LENGTH_SHORT).show()
            return
        }
        if (viewModelForClick.isSongClickable.value!!) {
            viewModelForClick.setCurrentSong(song)
            val intent = Intent(activityForClick, PlayerService::class.java)
                intent.putParcelableArrayListExtra(SONGS_IN_PLAYER,
                    viewModelForClick.songsInPlayer)
            intent.putExtra(SONG, song)

            if (!isMyServiceRunning(PlayerService::class.java) || fromUser) {
                activityForClick.startService(intent)
            }
            if (activityForClick.supportFragmentManager.fragments.find { fragment -> fragment == PlayerFragment() } == null) {
                activityForClick.supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container_player, PlayerFragment())
                    .commit()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun isMyServiceRunning(serviceClass: Class<out Service>): Boolean {
        val manager = activityForClick.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Int.MAX_VALUE)
            ?.map { it.service.className }
            ?.contains(serviceClass.name) ?: false
    }

    fun onDeleteSong(song: Song) {
        if (viewModelForClick.currentSong.value == song) {
            Toast.makeText(contextForClick,
                "You cannot delete song that is playing currently",
                Toast.LENGTH_SHORT).show()
            return
        }
        viewModelForClick.songToDelete = song
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val itemUri =
                ContentUris.withAppendedId(MediaStore.Audio.Media.getContentUri("external"),
                    song.id.toLong())
            requestDeletePermission(listOf(itemUri))
        } else {
            val contentUti =
                ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    song.id.toLong())
            val file = File(song.path)
            val isFileDeleted = file.delete()
            if (isFileDeleted) {
                activityForClick.contentResolver.delete(contentUti, null, null)
            }
            viewModelForClick.isReadPermissionGranted.postValue(true)
        }
    }

    fun addSongToFavorite(song: Song){
        viewModelForClick.addToDatabase(song, isFavorite = true)
    }

    fun deleteFromFavorite(song: Song){
        viewModelForClick.deleteFromFavorites(song)
    }

    private fun requestDeletePermission(uriList: List<Uri>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pi = MediaStore.createDeleteRequest(activityForClick.contentResolver, uriList)
            try {
                intentSenderLauncherForClick.launch(
                    IntentSenderRequest.Builder(pi.intentSender).build()
                )
            } catch (e: IntentSender.SendIntentException) {
            }
        }
    }

}
