package com.example.musicplayer.ui.fragments

import android.app.Activity.RESULT_OK
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicplayer.databinding.FragmentSongsBinding
import com.example.musicplayer.models.Song
import com.example.musicplayer.ui.MainActivity
import com.example.musicplayer.ui.MusicViewModel
import com.example.musicplayer.ui.adapters.ISongClick
import com.example.musicplayer.ui.adapters.SongAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SongsFragment : BaseFragment<FragmentSongsBinding>(), ISongClick {
    override val viewBinding: FragmentSongsBinding
        get() = FragmentSongsBinding.inflate(layoutInflater)
    override val contextForClick: Context
        get() = requireContext()
    override val activityForClick: MainActivity
        get() = activity as MainActivity
    override val viewModelForClick: MusicViewModel
        get() = viewModel
    override val intentSenderLauncherForClick: ActivityResultLauncher<IntentSenderRequest>
        get() = intentSenderLauncher
    private lateinit var songAdapter: SongAdapter
    private lateinit var intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setRecyclerViewSongs()
        setSearchForSongs()
        intentSenderLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
                if (it.resultCode == RESULT_OK) {
                    viewModel.isReadPermissionGranted.postValue(true)
                } else {
                    viewModel.songToDelete = null
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
            viewModelForClick.songsInPlayer = songs.toMutableList()
        }
        viewModel.isPlayerOpened.observe(viewLifecycleOwner) {
            if (it) {
                setPaddingRv(binding.rvSongs, 16, 0, 16, 48)
            } else {
                setPaddingRv(binding.rvSongs, 16, 0, 16, 0)
            }
        }
    }

    private fun setSearchForSongs() {
        var job: Job? = null
        binding.etSearch.addTextChangedListener { searchText ->
            job?.cancel()
            job = lifecycleScope.launch {
                delay(1000)
                viewModel.searchQuery.value = searchText.toString()
                if (searchText.toString() == "") {
                    requireActivity().currentFocus?.let { view ->
                        val manager = requireActivity().getSystemService(
                            Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        manager.hideSoftInputFromWindow(
                            view.windowToken, 0)
                    }
                    binding.etSearch.clearFocus()
                }
            }
        }
        viewModel.searchQuery.observe(viewLifecycleOwner) {
            if (it != "") {
                songAdapter.submitList(viewModel.searchSongs())
                viewModel.songsInPlayer = viewModel.searchSongs().toMutableList()
            } else {
                songAdapter.submitList(viewModel.songs.value)
                viewModel.songsInPlayer = viewModel.songs.value?.toMutableList() ?: mutableListOf()
            }
        }
    }
}

