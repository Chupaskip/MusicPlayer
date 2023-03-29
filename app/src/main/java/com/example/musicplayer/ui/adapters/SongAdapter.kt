package com.example.musicplayer.ui.adapters

import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.musicplayer.R
import com.example.musicplayer.databinding.ItemSongBinding
import com.example.musicplayer.models.Song
import com.example.musicplayer.models.setImage
import com.example.musicplayer.ui.MainActivity
import com.example.musicplayer.ui.MusicViewModel
import com.example.musicplayer.ui.fragments.PlayerFragment
import kotlinx.coroutines.*
import java.io.File

class SongAdapter(private val listener: OnSongClickListener) :
    ListAdapter<Song, SongAdapter.SongViewHolder>(diffUtil) {

    inner class SongViewHolder(private val binding: ItemSongBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(song: Song) {
            binding.tvSong.text = song.title
            binding.tvSongArtist.text = song.artist

            CoroutineScope(Dispatchers.Main).launch {
                if (song.image == null)
                    song.setImage()
                Glide.with(binding.root)
                    .load(song.image)
//                    .placeholder(R.drawable.placeholder_no_art)
                    .error(R.drawable.placeholder_no_art)
                    .into(binding.ivSong)
            }

            binding.root.setOnClickListener {
                listener.onSongClick(song)
            }
            binding.btnMore.setOnClickListener {
                val popUpMenu = PopupMenu(context, binding.btnMore)
                popUpMenu.menuInflater.inflate(R.menu.pop_up_menu, popUpMenu.menu)
                popUpMenu.setOnMenuItemClickListener { option ->
                    when (option.itemId) {
                        R.id.delete -> {
                            deleteSong(song)
                        }
                    }
                    true
                }
                popUpMenu.show()
            }
        }

        private fun deleteSong(song: Song) {
            listener.onDeleteSong(song)
        }
    }

    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<Song>() {
            override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean {
                return oldItem == newItem
            }
        }
    }


    private lateinit var context: Context
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        context = parent.context
        return SongViewHolder(ItemSongBinding.inflate(LayoutInflater.from(parent.context),
            parent,
            false))
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

interface OnSongClickListener {
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
            viewModelForClick.songsInPlayer = viewModelForClick.songs.value!!.toMutableList()
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