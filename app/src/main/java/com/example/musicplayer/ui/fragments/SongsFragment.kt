package com.example.musicplayer.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicplayer.R
import com.example.musicplayer.databinding.FragmentSongsBinding
import com.example.musicplayer.ui.MainActivity
import com.example.musicplayer.ui.MainActivity.Companion.songs
import com.example.musicplayer.ui.adapters.SongAdapter
import com.example.musicplayer.ui.models.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

var isPlayerOpened = false

class SongsFragment : BaseFragment<FragmentSongsBinding>(), SongAdapter.OnSongClickListener {
    private lateinit var songAdapter: SongAdapter
    override val viewBinding: FragmentSongsBinding
        get() = FragmentSongsBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setRecyclerViewSongs()
    }

    private fun setRecyclerViewSongs() {
        songAdapter = SongAdapter(this)
        binding.rvSongs.adapter = songAdapter
        binding.rvSongs.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)



        if (songs.isNotEmpty()) {
            songAdapter.submitList(songs)
        }
    }

    fun showPlayer(){
        (activity as MainActivity).supportFragmentManager.fragments.find { f -> f is PlayerFragment }
            .also {
                if (it == null)
                    setPaddingRv(binding.rvSongs, 8, 0, 8, 8)
                else
                    setPaddingRv(binding.rvSongs, 8, 0, 8, 50)
            }
    }
    override fun onSongClick(song: Song) {
        if (isPlayerOpened)
            return
        isPlayerOpened = true
        (activity as MainActivity).supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_player, PlayerFragment.newInstance(song))
            .commit()
    }
}

