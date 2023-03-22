package com.example.musicplayer.ui.fragments

import android.content.Context
import android.content.res.Configuration
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils.loadAnimation
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.musicplayer.R
import com.example.musicplayer.databinding.FragmentPlayerBinding
import com.example.musicplayer.ui.MainActivity
import com.example.musicplayer.models.Song
import com.example.musicplayer.ui.util.WorkWithImage.Companion.getDrawableWithAnotherColor
import com.example.musicplayer.ui.util.WorkWithImage.Companion.setGradientBackGround
import kotlinx.coroutines.*
import kotlin.math.abs


class PlayerFragment : BaseFragment<FragmentPlayerBinding>() {
    override val viewBinding: FragmentPlayerBinding
        get() = FragmentPlayerBinding.inflate(layoutInflater)
    private var player: MediaPlayer? = null
    private var isBackgroundGradient: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
                if (player != null && fromUser) {
                    player?.seekTo(progress * 1000)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })
        binding.btnPlay.setOnClickListener {
            if (player!!.isPlaying) {
                player?.pause()
                binding.btnPlay.setImageResource(R.drawable.ic_play)
            } else {
                player?.start()
                binding.btnPlay.setImageResource(R.drawable.ic_pause)
            }
        }
        viewModel.currentSong.observe(viewLifecycleOwner) { song ->
            setMediaPlayer(song)
            setInformation(song)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                if (player != null) {
                    val currentPosition = player!!.currentPosition / 1000
                    val currentDuration = getFormattedTime(currentPosition)
                    binding.seekBarSong.progress = currentPosition
                    binding.tvSongDurationCurrent.text = currentDuration
                    viewModel.playerPosition = currentPosition*1000
                }
                delay(1000)
            }
        }
        binding.btnPrevious.setOnClickListener {
            viewModel.setPreviousSong()
        }
        binding.btnNext.setOnClickListener {
            viewModel.setNextSong()
        }

        binding.mainPlayerContainer.setTransitionListener(object : MotionLayout.TransitionListener {
            override fun onTransitionStarted(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int,
            ) {
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
            }

            override fun onTransitionTrigger(
                motionLayout: MotionLayout?,
                triggerId: Int,
                positive: Boolean,
                progress: Float,
            ) {
            }
        })
        if (!viewModel.isPlayerOpened) {
            binding.mainPlayerContainer.transitionToEnd()
            hideBottomNav()
            viewModel.isPlayerOpened = true
        }
        viewModel.isPlayerExpanded.observe(viewLifecycleOwner) {
            if (it) {
                isBackgroundGradient = true
                hideBottomNav()
                binding.mainPlayerContainer.transitionToEnd()
                setGradientBackGround(viewModel.currentSong.value?.path!!,
                    binding.playerContainer,
                    requireContext())
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


    private fun setMediaPlayer(song: Song) {
        val uri = Uri.parse(song.path)
        if (player != null) {
            player?.stop()
            player?.release()
            player = MediaPlayer.create(requireContext().applicationContext, uri)
            player?.start()
        } else {
            player = MediaPlayer.create(requireContext().applicationContext, uri)
            player!!.start()
        }
        player!!.seekTo(viewModel.playerPosition)
        binding.seekBarSong.max = (player!!.duration / 1000)
        player!!.setOnCompletionListener {
            viewModel.setNextSong(true)
        }
    }

    private fun setInformation(song: Song) {
        imageAnimation(requireContext(), binding.ivSong, song.image)
        if (isBackgroundGradient) {
            setGradientBackGround(viewModel.currentSong.value?.path!!,
                binding.playerContainer,
                requireContext())
        }
        binding.apply {
            tvSongTitle.text = song.title
            tvSongArtist.text = song.artist
            tvSongAlbum.text = song.album
            tvSongDurationTotal.text = getFormattedTime((player!!.duration / 1000))
            btnPlay.setImageResource(R.drawable.ic_pause)
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

    override fun onDestroyView() {
        super.onDestroyView()
        player?.stop()
        player = null
    }
}