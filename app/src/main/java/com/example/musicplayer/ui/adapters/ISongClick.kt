package com.example.musicplayer.ui.adapters

import android.content.ContentUris
import android.content.Context
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
import java.io.File

interface ISongClick {
    val viewModelForClick: MusicViewModel
    val contextForClick: Context
    val activityForClick: MainActivity
    val intentSenderLauncherForClick: ActivityResultLauncher<IntentSenderRequest>
    fun onSongClick(song: Song) {
        if (song.id == (viewModelForClick.songToDelete?.id ?: "")) {
            Toast.makeText(contextForClick, "Song is deleted!", Toast.LENGTH_SHORT).show()
            return
        }
        if (viewModelForClick.isSongClickable.value!!) {
            viewModelForClick.setCurrentSong(song)
            activityForClick.supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_player, PlayerFragment())
                .commit()
        }
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
