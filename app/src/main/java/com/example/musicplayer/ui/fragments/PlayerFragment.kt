package com.example.musicplayer.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils.loadAnimation
import android.widget.ImageView
import android.widget.SeekBar
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.musicplayer.R
import com.example.musicplayer.databinding.FragmentPlayerBinding
import com.example.musicplayer.models.Album
import com.example.musicplayer.models.Song
import com.example.musicplayer.ui.MainActivity
import com.example.musicplayer.ui.util.WorkWithImage.Companion.setGradientBackGround
import kotlinx.coroutines.launch
import kotlin.math.abs


class PlayerFragment : BaseFragment<FragmentPlayerBinding>() {
    override val viewBinding: FragmentPlayerBinding
        get() = FragmentPlayerBinding.inflate(layoutInflater)
    private var isBackgroundGradient: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.isPlayerOpened.value = true
        viewModel.setTimerOfSong()
        viewModel.isShuffled.observe(viewLifecycleOwner) { isShuffled ->
            if (!isShuffled) {
                binding.btnShuffle.setOnClickListener {
                    viewModel.setShuffledSongs()
                }
                binding.btnShuffle.setImageDrawable(AppCompatResources.getDrawable(requireContext(),
                    R.drawable.ic_shuffle))
            } else {
                binding.btnShuffle.setOnClickListener {
                    viewModel.removeShuffledSongs()
                }
                binding.btnShuffle.setImageDrawable(AppCompatResources.getDrawable(requireContext(),
                    R.drawable.ic_shuffle_on))
            }
        }
        viewModel.isRepeated.observe(viewLifecycleOwner) { isRepeated ->
            if (!isRepeated) {
                binding.btnRepeat.setOnClickListener {
                    viewModel.setRepeatedSong()
                }
                binding.btnRepeat.setImageDrawable(AppCompatResources.getDrawable(requireContext(),
                    R.drawable.ic_repeat))
            } else {
                binding.btnRepeat.setOnClickListener {
                    viewModel.cancelRepeatedSong()
                }
                binding.btnRepeat.setImageDrawable(AppCompatResources.getDrawable(requireContext(),
                    R.drawable.ic_repeat_on))
            }
        }
        binding.seekBarSong.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    viewModel.player!!.seekTo(progress * 1000)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })

        viewModel.playerPaused.observe(viewLifecycleOwner) { isPaused ->
            binding.btnPlay.apply {
                if (!isPaused) {
                    setOnClickListener {
                        viewModel.player!!.pause()
                        viewModel.playerPaused.value = true
                    }
                    setImageResource(R.drawable.ic_pause)
                } else {
                    setOnClickListener {
                        viewModel.player!!.start()
                        viewModel.playerPaused.value = false
                    }
                    setImageResource(R.drawable.ic_play)
                }
            }
        }
        viewModel.currentSong.observe(viewLifecycleOwner) { song ->
            setInformation(song)
        }

        viewModel.currentDurationInMSec.observe(viewLifecycleOwner) { currentTime ->
            binding.seekBarSong.progress = currentTime / 1000
            binding.tvSongDurationCurrent.text = getFormattedTime(currentTime / 1000)
            if (viewModel.player!!.duration - currentTime < 200) {
                viewModel.setNextSong()
            }
        }
        viewModel.totalDurationOfSong.observe(viewLifecycleOwner) { totalDuration ->
            binding.seekBarSong.max = totalDuration
            binding.tvSongDurationTotal.text = getFormattedTime(totalDuration)
        }

        binding.btnPrevious.setOnClickListener {
            viewModel.setPreviousSong()
        }
        binding.btnNext.setOnClickListener {
            viewModel.setNextSong(false)
        }

        binding.mainPlayerContainer.setTransitionListener(object : MotionLayout.TransitionListener {
            override fun onTransitionStarted(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int,
            ) {
                viewModel.isSongClickable.postValue(false)
            }

            override fun onTransitionChange(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int,
                progress: Float,
            ) {
                (requireActivity() as MainActivity).also {
                    it.binding.mainActivityContainer.progress = abs(progress)
                }
            }

            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                (requireActivity() as MainActivity).also {
                    if (currentId == motionLayout?.startState) {
                        viewModel.setPlayerCollapsed()
                    } else {
                        viewModel.setPlayerExpanded()
                    }
                }
                viewModel.isSongClickable.postValue(true)
            }

            override fun onTransitionTrigger(
                motionLayout: MotionLayout?,
                triggerId: Int,
                positive: Boolean,
                progress: Float,
            ) {
            }
        })
        viewModel.isPlayerExpanded.observe(viewLifecycleOwner) {
            if (it) {
                isBackgroundGradient = true
                hideBottomNav()
                binding.mainPlayerContainer.transitionToEnd()
                lifecycleScope.launch {
                    setGradientBackGround(viewModel.currentSong.value?.image,
                        binding.playerContainer,
                        requireContext())
                }
            } else {
                isBackgroundGradient = false
                showBottomNav()
                binding.mainPlayerContainer.transitionToStart()
            }
        }
    }

    private fun hideBottomNav() {
        (activity as MainActivity).also {
            it.binding.mainActivityContainer.progress = 1F
        }
    }

    private fun showBottomNav() {
        (activity as MainActivity).also {
            it.binding.mainActivityContainer.progress = 0F
        }
    }

    private fun getFormattedTime(currentPosition: Int): String {
        val seconds = (currentPosition % 60).toString()
        val minutes = (currentPosition / 60).toString()
        val totalOut = "$minutes:$seconds"
        val totalNew = "$minutes:0$seconds"
        return if (seconds.length == 1) {
            totalNew
        } else {
            totalOut
        }
    }

    private fun setInformation(song: Song) {
        imageAnimation(requireContext(), binding.ivSong, song.image)
        if (isBackgroundGradient) {
            lifecycleScope.launch {
                setGradientBackGround(viewModel.currentSong.value?.image,
                    binding.playerContainer,
                    requireContext())
            }
        }
        binding.apply {
            tvSongTitle.text = song.title
            tvSongArtist.text = song.artist
            tvSongAlbum.text = song.album
            tvSongAlbum.setOnClickListener {
                viewModel.songs.value?.filter { s -> s.albumId == song.albumId }.also {
                    if (it != null) {
                        if (it.size > 1) {
                            val album = viewModel.albums.value?.find { a -> a.id == song.albumId }!!
                            val action = when (findNavController().currentDestination?.id) {
                                R.id.songsFragment -> {
                                    SongsFragmentDirections.actionSongsFragmentToAlbumFragment2(
                                        album)
                                }
                                R.id.albumsFragment -> {
                                    AlbumsFragmentDirections.actionAlbumsFragmentToAlbumFragment(
                                        album)
                                }
                                else -> {
                                    null
                                }
                            }
                            action?.also {
                                viewModel.getSongsByAlbumId(viewModel.albums.value?.find { a -> a.id == song.albumId }!!.id)
                                findNavController().navigate(action)
                                binding.mainPlayerContainer.transitionToStart()
                            }
                        }
                    }
                }

            }
        }
    }

    private fun imageAnimation(context: Context, imageView: ImageView, image: ByteArray?) {
        val animOut = loadAnimation(context, android.R.anim.fade_out)
        val animIn = loadAnimation(context, android.R.anim.fade_in)
        animOut.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {
                Glide.with(context)
                    .load(image)
                    .override(500)
                    .error(R.drawable.placeholder_no_art)
                    .into(binding.ivSong)
                imageView.startAnimation(animIn)
            }

            override fun onAnimationRepeat(animation: Animation?) {
            }
        })
        imageView.startAnimation(animOut)
    }
}