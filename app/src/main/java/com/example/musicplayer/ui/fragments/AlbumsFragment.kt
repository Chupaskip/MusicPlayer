package com.example.musicplayer.ui.fragments

import android.os.Bundle
import android.view.View
import android.widget.GridLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicplayer.R
import com.example.musicplayer.databinding.FragmentAlbumsBinding
import com.example.musicplayer.models.Album
import com.example.musicplayer.ui.MainActivity
import com.example.musicplayer.ui.adapters.AlbumAdapter
import com.example.musicplayer.ui.adapters.SongAdapter
import kotlinx.coroutines.launch

class AlbumsFragment : BaseFragment<FragmentAlbumsBinding>(), AlbumAdapter.OnAlbumClickListener {
    override val viewBinding: FragmentAlbumsBinding
        get() = FragmentAlbumsBinding.inflate(layoutInflater)
    private lateinit var albumAdapter: AlbumAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setRecyclerViewAlbums()
    }

    private fun setRecyclerViewAlbums() {
        albumAdapter = AlbumAdapter(this)
        binding.rvAlbums.adapter = albumAdapter
        binding.rvAlbums.layoutManager =
            GridLayoutManager(requireContext(), 2, GridLayoutManager.VERTICAL, false)
        viewModel.albums.observe(viewLifecycleOwner) { albums ->
            albumAdapter.submitList(albums)
        }
        viewModel.isPlayerOpened.observe(viewLifecycleOwner) {
            if (it) {
                setPaddingRv(binding.rvAlbums, 8, 16, 8, 68)
            } else {
                setPaddingRv(binding.rvAlbums, 8, 16, 8, 16)
            }
        }
    }

    override fun onAlbumClick(album: Album) {
            viewModel.getSongsByAlbumId(album.id)
        val action = AlbumsFragmentDirections.actionAlbumsFragmentToAlbumFragment(album)
        findNavController().navigate(action)
    }
}