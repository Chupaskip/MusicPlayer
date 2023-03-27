package com.example.musicplayer.ui.fragments

import android.app.Activity.RESULT_OK
import android.content.ContentUris
import android.content.IntentSender
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicplayer.R
import com.example.musicplayer.databinding.FragmentSongsBinding
import com.example.musicplayer.models.Album
import com.example.musicplayer.models.Song
import com.example.musicplayer.ui.MainActivity
import com.example.musicplayer.ui.adapters.SongAdapter
import java.io.File

private const val ALBUM_ID = "album_id"

class SongsFragment : BaseFragment<FragmentSongsBinding>(), SongAdapter.OnSongClickListener {
    override val viewBinding: FragmentSongsBinding
        get() = FragmentSongsBinding.inflate(layoutInflater)
    private lateinit var songAdapter: SongAdapter
    private lateinit var intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setRecyclerViewSongs()
        intentSenderLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
                if (it.resultCode == RESULT_OK) {
                    viewModel.getSongsAlbums(requireContext())
                }
            }
    }

    private fun setRecyclerViewSongs() {
        songAdapter = SongAdapter(this)
        binding.rvSongs.adapter = songAdapter
        binding.rvSongs.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        viewModel.songs.observe(viewLifecycleOwner) { songs ->
            songAdapter.submitList(songs)
        }
        viewModel.isPlayerOpened.observe(viewLifecycleOwner) {
            if (it) {
                setPaddingRv(binding.rvSongs, 16, 0, 16, 68)
            } else {
                setPaddingRv(binding.rvSongs, 16, 0, 16, 8)
            }
        }
    }

    override fun onSongClick(song: Song) {
        if (viewModel.isSongClickable.value!!) {
            viewModel.songsInPlayer = viewModel.songs.value!!
            val uri = Uri.parse(song.path)
            viewModel.setMediaPlayer(MediaPlayer.create(requireContext().applicationContext, uri))
            viewModel.setCurrentSong(song)
            (activity as MainActivity).supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_player, PlayerFragment())
                .commit()
        }
    }

    override fun onDeleteSong(song: Song) {
        viewModel.songToDelete = song
        if (viewModel.currentSong.value == song) {
            Toast.makeText(requireContext(),
                "You cannot delete song that is playing currently",
                Toast.LENGTH_SHORT).show()
            return
        }
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
                requireContext().contentResolver.delete(contentUti, null, null)
            }
            viewModel.getSongsAlbums(requireContext())
        }
        viewModel.songToDelete = null
    }

    private fun requestDeletePermission(uriList: List<Uri>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pi = MediaStore.createDeleteRequest(requireActivity().contentResolver, uriList)
            try {
                intentSenderLauncher.launch(
                    IntentSenderRequest.Builder(pi.intentSender).build()
                )
            } catch (e: IntentSender.SendIntentException) {
            }
        }
    }
}

