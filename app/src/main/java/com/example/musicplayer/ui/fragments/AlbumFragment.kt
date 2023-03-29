package com.example.musicplayer.ui.fragments

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.musicplayer.R
import com.example.musicplayer.databinding.FragmentAlbumBinding
import com.example.musicplayer.models.Song
import com.example.musicplayer.models.setImage
import com.example.musicplayer.ui.MainActivity
import com.example.musicplayer.ui.MusicViewModel
import com.example.musicplayer.ui.adapters.ISongClick
import com.example.musicplayer.ui.adapters.SongAdapter
import kotlinx.coroutines.launch


class AlbumFragment : BaseFragment<FragmentAlbumBinding>(), ISongClick {
    override val viewBinding: FragmentAlbumBinding
        get() = FragmentAlbumBinding.inflate(layoutInflater)
    private val args: AlbumFragmentArgs by navArgs()
    private lateinit var songAdapter: SongAdapter
    private lateinit var intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>
    override val contextForClick: Context
        get() = requireContext()
    override val activityForClick: MainActivity
        get() = activity as MainActivity
    override val viewModelForClick: MusicViewModel
        get() = viewModel
    override val intentSenderLauncherForClick: ActivityResultLauncher<IntentSenderRequest>
        get() = intentSenderLauncher

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setInformationAlbum()
        setRecyclerViewSongs()
        intentSenderLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
                if (it.resultCode == Activity.RESULT_OK) {
                    viewModel.isReadPermissionGranted.postValue(true)
                    viewModel.getSongsByAlbumId(viewModel.songToDelete?.albumId ?: "")
                } else {
                    viewModel.songToDelete = null
                }
            }
        viewModel.songsInPlayer = viewModel.songsByAlbumId.value?.toMutableList() ?: mutableListOf()
    }

    private fun setInformationAlbum() {
        val album = args.album
        binding.apply {
            lifecycleScope.launch {
                if (album.image == null) {
                    album.setImage()
                }
                Glide.with(requireContext())
                    .load(album.image)
                    .error(R.drawable.placeholder_no_art)
                    .into(ivAlbum)
            }
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
                setPaddingRv(binding.rvSongs, 16, 0, 16, 48)
            } else {
                setPaddingRv(binding.rvSongs, 16, 0, 16, 0)
            }
        }
    }
}