package com.example.musicplayer.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.navigation.NavArgument
import androidx.navigation.NavArgumentBuilder
import androidx.navigation.fragment.findNavController
import androidx.navigation.navArgument
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicplayer.databinding.FragmentSongsBinding
import com.example.musicplayer.ui.MainActivity.Companion.songs
import com.example.musicplayer.ui.adapters.SongAdapter
import com.example.musicplayer.ui.models.Song

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

    override fun onSongClick(song: Song) {
        if (isPlayerOpened)
            return
        isPlayerOpened = true
//        val player = PlayerBottomSheetFragment.newInstance(song)
//        player.showsDialog=true
//        player.show(childFragmentManager, "Player")
        val action = SongsFragmentDirections.actionSongsFragmentToPlayerFragment(song)
        findNavController().navigate(action)
    }
}
