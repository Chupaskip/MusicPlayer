package com.example.musicplayer.ui.fragments

import android.content.ContentUris
import android.content.IntentSender
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicplayer.R
import com.example.musicplayer.databinding.FragmentSongsBinding
import com.example.musicplayer.models.Song
import com.example.musicplayer.ui.MainActivity
import com.example.musicplayer.ui.adapters.SongAdapter
import java.io.File

var isPlayerOpened = false

class SongsFragment : BaseFragment<FragmentSongsBinding>(), SongAdapter.OnSongClickListener {
    private lateinit var songAdapter: SongAdapter
    override val viewBinding: FragmentSongsBinding
        get() = FragmentSongsBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setRecyclerViewSongs()
    }

    private var isPlayerOpened: Boolean = false
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
                setPaddingRv(binding.rvSongs, 8, 0, 8, 68)
            } else {
                setPaddingRv(binding.rvSongs, 8, 0, 8, 8)
            }
        }
    }

    override fun onSongClick(song: Song) {
        if (viewModel.isSongClickable.value!!) {
            val uri = Uri.parse(song.path)
            viewModel.setMediaPlayer(MediaPlayer.create(requireContext().applicationContext, uri))
            viewModel.setCurrentSong(song)
            if (isPlayerOpened)
                return
            (activity as MainActivity).supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_player, PlayerFragment())
                .commit()
        }
    }

    override fun onDeleteSong(song: Song) {
//      if (song.id == (viewModel.currentSong.value?.id ?: "0")

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
            }
            viewModel.getSongs(requireContext(), song)
//       }
//            Toast.makeText(requireContext(),
//                "You cannot delete song that is playing",
//                Toast.LENGTH_SHORT).show()
//        }
    }

    private fun requestDeletePermission(uriList: List<Uri>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pi = MediaStore.createDeleteRequest(requireActivity().contentResolver, uriList)
            try {
                ActivityCompat.startIntentSenderForResult(
                    requireActivity(),
                    pi.intentSender,
                    444,
                    null,
                    0,
                    0,
                    0,
                    null)
            } catch (e: IntentSender.SendIntentException) {
            }
        }
    }
}

