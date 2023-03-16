package com.example.musicplayer.ui.fragments

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils.loadAnimation
import android.widget.ImageView
import android.widget.SeekBar
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.musicplayer.R
import com.example.musicplayer.databinding.FragmentPlayerBinding
import com.example.musicplayer.ui.MainActivity
import com.example.musicplayer.ui.models.Song
import com.example.musicplayer.ui.util.Image.Companion.getDrawableWithAnotherColor
import com.example.musicplayer.ui.util.Image.Companion.setGradientBackGround
import kotlinx.coroutines.*


class PlayerFragment : BaseFragment<FragmentPlayerBinding>() {
    override val viewBinding: FragmentPlayerBinding
        get() = FragmentPlayerBinding.inflate(layoutInflater)
    private val args: PlayerFragmentArgs by navArgs()
    private lateinit var song: Song
    private var player: MediaPlayer? = null
    private var isShuffled = false
    private var isRepeated = false
    private var shuffledSongs: ArrayList<Song> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        song = args.song
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


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
        setMediaPlayer()
        setInformation()

        viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                if (player != null) {
                    val currentPosition = player!!.currentPosition / 1000
                    val currentDuration = getFormattedTime(currentPosition)
                    binding.seekBarSong.progress = currentPosition
                    binding.tvSongDurationCurrent.text = currentDuration
                }
                delay(100)
            }
        }
        binding.btnPrevious.setOnClickListener {
            setPreviousSong()
        }
        binding.btnNext.setOnClickListener {
            setNextSong()
        }
        binding.btnShuffle.setOnClickListener {
            if (!isShuffled) {
                isShuffled = true
                shuffledSongs = ArrayList(MainActivity.songs)
                shuffledSongs.shuffle()
                binding.btnShuffle.setImageDrawable(getDrawableWithAnotherColor(requireContext(),
                    R.drawable.ic_shuffle,
                    R.color.purple_200))
            } else {
                isShuffled = false
                binding.btnShuffle.setImageDrawable(AppCompatResources.getDrawable(requireContext(),
                    R.drawable.ic_shuffle))
            }
        }
        binding.btnRepeat.setOnClickListener {
            if (!isRepeated) {
                isRepeated = true
                binding.btnRepeat.setImageDrawable(getDrawableWithAnotherColor(requireContext(),
                    R.drawable.ic_repeat,
                    R.color.purple_200))
            } else {
                isRepeated = false
                binding.btnRepeat.setImageDrawable(AppCompatResources.getDrawable(requireContext(),
                    R.drawable.ic_repeat))
            }
        }
        binding.btnClose.setOnClickListener{
            findNavController().popBackStack()
        }
    }


    private fun setPreviousSong() {
        val positionOfSongInList =
            if (!isShuffled) MainActivity.songs.indexOf(song) else shuffledSongs.indexOf(song)
        if (positionOfSongInList > 0) {
            song =
                if (!isShuffled) MainActivity.songs[positionOfSongInList - 1] else shuffledSongs[positionOfSongInList - 1]
            setMediaPlayer()
            setInformation()
        }
    }


    private fun setNextSong() {
        val positionOfSongInList =
            if (!isShuffled) MainActivity.songs.indexOf(song) else shuffledSongs.indexOf(song)
        song = if (positionOfSongInList < MainActivity.songs.size - 1 && !isShuffled) {
            MainActivity.songs[positionOfSongInList + 1]
        } else {
            if (!isShuffled)
                MainActivity.songs[0]
            else if (positionOfSongInList < shuffledSongs.size - 1) {
                shuffledSongs[positionOfSongInList + 1]
            } else
                shuffledSongs[0]
        }
        setMediaPlayer()
        setInformation()
    }

    private fun getFormattedTime(currentPosition: Int): String {
        var totalOut = ""
        var totalNew = ""
        val seconds = (currentPosition % 60).toString()
        val minutes = (currentPosition / 60).toString()
        totalOut = "$minutes:$seconds"
        totalNew = "$minutes:0$seconds"
        return if (seconds.length == 1) {
            totalNew
        } else {
            totalOut
        }
    }


    private fun setMediaPlayer() {
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
        binding.seekBarSong.max = (player!!.duration / 1000)
        player!!.setOnCompletionListener {
            if (isRepeated) {
                player = null
                setMediaPlayer()
            } else {
                setNextSong()
            }
        }
    }

    private fun setInformation() {
        setGradientBackGround(song.path, binding.playerLayout, requireContext())
        imageAnimation(requireContext(), binding.ivSong, song.image)
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
        isPlayerOpened = false
        player?.stop()
        player = null
        isRepeated = false
        isShuffled = false
    }


}