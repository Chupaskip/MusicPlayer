package com.example.musicplayer.ui.fragments

import android.app.Activity
import android.app.ActivityManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicplayer.databinding.FragmentFavoriteBinding
import com.example.musicplayer.databinding.FragmentSongsBinding
import com.example.musicplayer.ui.MainActivity
import com.example.musicplayer.ui.MusicViewModel
import com.example.musicplayer.ui.adapters.ISongClick
import com.example.musicplayer.ui.adapters.SongAdapter
import com.example.musicplayer.ui.services.PlayerService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FavoriteFragment : BaseFragment<FragmentFavoriteBinding>(), ISongClick, ServiceConnection {
    override val viewBinding: FragmentFavoriteBinding
        get() = FragmentFavoriteBinding.inflate(layoutInflater)
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
    private var playerService: PlayerService? = null

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val myBinder = service as PlayerService.MyBinder
        playerService = myBinder.getService()
        playerService?.songInPlayer?.also {
            viewModel.setCurrentSong(it)
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        playerService = null
    }

    override fun onResume() {
        super.onResume()
        val intent = Intent(requireContext(), PlayerService::class.java)
        (activity as MainActivity).bindService(intent, this, Context.BIND_AUTO_CREATE)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setRecyclerViewSongs()
        setSearchForSongs()
        intentSenderLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
                if (it.resultCode == Activity.RESULT_OK) {
                    viewModel.isReadPermissionGranted.postValue(true)
                } else {
                    viewModel.songToDelete = null
                }
            }
        viewModel.currentSong.observe(viewLifecycleOwner) {
            if (!viewModel.isPlayerOpened.value!!)
                onSongClick(it, false)
        }
    }

    @Suppress("DEPRECATION")
    private fun isMyServiceRunning(serviceClass: Class<out Service>): Boolean {
        val manager = activityForClick.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Int.MAX_VALUE)
            ?.map { it.service.className }
            ?.contains(serviceClass.name) ?: false
    }


    private fun setRecyclerViewSongs() {
        viewModel.getFavoriteSongs()
        songAdapter = SongAdapter(this)
        binding.rvSongs.adapter = songAdapter
        binding.rvSongs.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        viewModel.songsFavorite.observe(viewLifecycleOwner) { songs ->
            Log.d("MyLog", "songs: $songs")
            songAdapter.submitList(songs)
            viewModelForClick.songsInPlayer = ArrayList(songs)
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
                songAdapter.submitList(viewModel.searchSongsFavorite())
                viewModel.songsInPlayer = ArrayList(viewModel.searchSongsFavorite())
            } else {
                songAdapter.submitList(viewModel.songsFavorite.value)
                viewModel.songsInPlayer =
                    ArrayList(viewModel.songsFavorite.value?.toMutableList() ?: mutableListOf())
            }
        }
    }


}