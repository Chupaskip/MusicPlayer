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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.musicplayer.R
import com.example.musicplayer.databinding.FragmentAlbumBinding
import com.example.musicplayer.models.Song
import com.example.musicplayer.ui.MainActivity
import com.example.musicplayer.ui.adapters.SongAdapter
import kotlinx.coroutines.launch
import java.io.File


class AlbumFragment : BaseFragment<FragmentAlbumBinding>(), SongAdapter.OnSongClickListener {
    override val viewBinding: FragmentAlbumBinding
        get() = FragmentAlbumBinding.inflate(layoutInflater)
    private val args: AlbumFragmentArgs by navArgs()
    private lateinit var songAdapter: SongAdapter
    private lateinit var intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setInformationAlbum()
        setRecyclerViewSongs()
    }

    private fun setInformationAlbum() {
        val album = args.album
        binding.apply {
            Glide.with(requireContext())
                .load(album.image)
                .placeholder(R.drawable.placeholder_no_art)
                .error(R.drawable.placeholder_no_art)
                .into(ivAlbum)
            tvAlbumTitle.text = album.title
            tvAlbumYear.text = album.year
            tvAlbumArtist.text = album.artist
        }
    }

    private fun setRecyclerViewSongs() {
        songAdapter = SongAdapter(this)
        binding.rvSongs.adapter = songAdapter
        binding.rvSongs.itemAnimator = null
        binding.rvSongs.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        viewModel.songsByAlbumId.observe(viewLifecycleOwner) { songs ->
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
            viewModel.songsInPlayer = viewModel.songsByAlbumId.value!!
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