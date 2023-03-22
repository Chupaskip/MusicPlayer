package com.example.musicplayer.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicplayer.R
import com.example.musicplayer.databinding.FragmentSongsBinding
import com.example.musicplayer.models.Song
import com.example.musicplayer.ui.MainActivity
import com.example.musicplayer.ui.adapters.SongAdapter

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
        viewModel.isPlayerExpanded.observe(viewLifecycleOwner) {
            isPlayerOpened = it
            if (!it) {
                setPaddingRv(binding.rvSongs, 8, 0, 8, 58)
            } else {
                setPaddingRv(binding.rvSongs, 8, 0, 8, 8)
            }
        }
    }

    override fun onSongClick(song: Song) {
        viewModel.setCurrentSong(song)
        if (isPlayerOpened)
            return
        (activity as MainActivity).supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_player, PlayerFragment())
            .commit()
    }
}

