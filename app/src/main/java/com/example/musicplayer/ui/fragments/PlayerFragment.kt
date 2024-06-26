package com.example.musicplayer.ui.fragments

import android.app.Service.STOP_FOREGROUND_REMOVE
import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.musicplayer.R
import com.example.musicplayer.databinding.FragmentPlayerBinding
import com.example.musicplayer.models.Song
import com.example.musicplayer.ui.MainActivity
import com.example.musicplayer.ui.Playable
import com.example.musicplayer.ui.services.PlayerService
import com.example.musicplayer.ui.util.WorkWithImage
import com.example.musicplayer.ui.util.WorkWithImage.Companion.setGradientBackGround
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

const val SONGS_IN_PLAYER = "songsInPlayer"
const val SONG = "song"

class PlayerFragment : BaseFragment<FragmentPlayerBinding>(), ServiceConnection, Playable {
    override fun closePlayer() {
        goToClosedState()
    }

    override val viewBinding: FragmentPlayerBinding
        get() = FragmentPlayerBinding.inflate(layoutInflater)
    private var isBackgroundGradient: Boolean = false
    var playerService: PlayerService? = null

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val myBinder = service as PlayerService.MyBinder
        playerService = myBinder.getService()
        playerService?.playable = this
        playerService?.songInPlayer = viewModel.currentSong.value
        if (playerService?.isShuffled!!) {
            playerService?.setShuffledSongs()
        }
        lifecycleScope.launch {
            while (true) {
                binding.seekBarSong.progress = playerService!!.getCurrentPosition()!! / 1000
                binding.tvSongDurationCurrent.text =
                    getFormattedTime(playerService!!.getCurrentPosition()!! / 1000)
                binding.seekBarSong.max = playerService!!.getDuration()!! / 1000
                binding.tvSongDurationTotal.text =
                    getFormattedTime(playerService!!.getDuration()!! / 1000)
                delay(100)
                viewModel.playerPaused.value = playerService?.isPaused
            }
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        playerService = null
    }

    override fun onResume() {
        super.onResume()
        val intent = Intent(requireContext(), PlayerService::class.java)
        (activity as MainActivity).bindService(intent, this, BIND_AUTO_CREATE)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.isPlayerOpened.value = true
        viewModel.isShuffled.observe(viewLifecycleOwner) { isShuffled ->
            playerService?.isShuffled = isShuffled
            if (!isShuffled) {
                binding.btnShuffle.setOnClickListener {
                    playerService?.setShuffledSongs()
                    viewModel.setShuffledSongs()
                }
                binding.btnShuffle.setImageDrawable(
                    AppCompatResources.getDrawable(
                        requireContext(),
                        R.drawable.ic_shuffle
                    )
                )
            } else {
                binding.btnShuffle.setOnClickListener {
                    playerService?.cancelShuffledSongs()
                    viewModel.cancelShuffledSongs()
                }
                binding.btnShuffle.setImageDrawable(
                    AppCompatResources.getDrawable(
                        requireContext(),
                        R.drawable.ic_shuffle_on
                    )
                )
            }
        }
        viewModel.isRepeated.observe(viewLifecycleOwner) { isRepeated ->
            playerService?.isRepeated = isRepeated
            if (!isRepeated) {
                binding.btnRepeat.setOnClickListener {
                    viewModel.setRepeatedSong()
                }
                binding.btnRepeat.setImageDrawable(
                    AppCompatResources.getDrawable(
                        requireContext(),
                        R.drawable.ic_repeat
                    )
                )
            } else {
                binding.btnRepeat.setOnClickListener {
                    viewModel.cancelRepeatedSong()
                }
                binding.btnRepeat.setImageDrawable(
                    AppCompatResources.getDrawable(
                        requireContext(),
                        R.drawable.ic_repeat_on
                    )
                )
            }
        }
        binding.seekBarSong.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    playerService?.seekTo(progress * 1000)
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
                        playerService?.pause()
                        viewModel.playerPaused.value = true
                    }
                    setImageResource(R.drawable.ic_pause)
                } else {
                    setOnClickListener {
                        playerService?.start()
                        viewModel.playerPaused.value = false
                    }
                    setImageResource(R.drawable.ic_play)
                }
            }
        }
        viewModel.currentSong.observe(viewLifecycleOwner) { song ->
            if (song != null)
                setInformation(song)
        }

        binding.btnNext.setOnClickListener {
            playerService?.setNextSong()
            viewModel.setCurrentSong(playerService!!.songInPlayer!!)
        }
        binding.btnPrevious.setOnClickListener {
            playerService?.setPreviousSong()
            viewModel.setCurrentSong(playerService!!.songInPlayer!!)
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
                if (endId == R.id.closed) {
                    return
                }
                (requireActivity() as MainActivity).also {
                    it.binding.mainActivityContainer.progress = abs(progress)
                }
            }

            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                if (currentId == motionLayout?.startState) {
                    viewModel.setPlayerCollapsed()
                } else if (currentId == motionLayout?.endState && motionLayout.currentState != R.id.closed) {
                    viewModel.setPlayerExpanded()
                } else if (motionLayout?.currentState == R.id.closed) {
                    goToClosedState()
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
                    val image = WorkWithImage.getSongArt(viewModel.currentSong.value!!.path)
                    setGradientBackGround(
                        image,
                        binding.playerContainer,
                        requireContext()
                    )
                }
            } else {
                isBackgroundGradient = false
                showBottomNav()
                binding.mainPlayerContainer.transitionToStart()
            }
        }

        binding.btnClose.setOnClickListener {
            binding.mainPlayerContainer.transitionToStart()
        }

        binding.btnLyrics.setOnClickListener {
            viewModel.getSongLyricsFromDb(playerService!!.songInPlayer!!)
            viewModel.isLyricsDialogVisible.value = true
        }

        viewModel.isLyricsDialogVisible.observe(viewLifecycleOwner) {
            binding.composeViewLyrics?.setContent {
                if (it) {
                    Dialog(
                        onDismissRequest = { viewModel.isLyricsDialogVisible.value = false },
                        properties = DialogProperties(decorFitsSystemWindows = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(16.dp)
                                .imePadding()
                                .clip(RoundedCornerShape(10.dp))
                        ) {
                            var changeMode by rememberSaveable { mutableStateOf(false) }
                            var lyricsText by rememberSaveable {
                                viewModel.currentSong.value?.lyrics?.let {
                                    mutableStateOf(it)
                                } ?: mutableStateOf("")
                            }

                            Row {
                                Column {
                                    if (!changeMode) {
                                        viewModel.currentSong.observeAsState().value?.lyrics?.let {
                                            lyricsText = it
                                            Text(
                                                text = it.ifBlank { "Текст ещё не добавлен" },
                                                color = Color.White, fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .verticalScroll(rememberScrollState())
                                                    .fillMaxWidth()
                                                    .wrapContentSize(Alignment.TopCenter)
                                                    .weight(1f),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                    if (changeMode) {
                                        // Задаем цвета полю
                                        val outlineTextFieldColors =
                                            TextFieldDefaults.outlinedTextFieldColors(
                                                focusedBorderColor = colorResource(id = R.color.purple_700),
                                                unfocusedBorderColor = colorResource(id = R.color.purple_700),
                                                textColor = Color.White,
                                                backgroundColor = Color.Transparent
                                            )
                                        TextField(
                                            value = lyricsText,
                                            modifier = Modifier
                                                .weight(1f),
                                            onValueChange = {
                                                lyricsText = it
                                                viewModel.currentSong.value?.lyrics = it
                                            },
                                            colors = outlineTextFieldColors
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            if (changeMode) {
                                                viewModel.currentSong.value?.let { song ->
                                                    viewModel.addToDatabase(
                                                        song, lyrics = song.lyrics
                                                    )
                                                }
                                            }
                                            changeMode = !changeMode
                                        }, modifier = Modifier
                                            .padding(top = 16.dp)
                                            .fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = colorResource(
                                                id = R.color.purple_700
                                            ), contentColor = Color.White
                                        )
                                    ) {
                                        Text(text = if (!changeMode) "Изменить текст" else "Сохранить текст")
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.isLyricsDialogVisible.value = false
                                        }, modifier = Modifier
                                            .padding(top = 8.dp)
                                            .fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = colorResource(
                                                id = R.color.purple_700
                                            ), contentColor = Color.White
                                        )
                                    ) {
                                        Text(text = "Вернуться к плееру")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun goToClosedState() {
        (requireActivity() as MainActivity).also {
            val playerFragment =
                it.supportFragmentManager.findFragmentById(R.id.fragment_container_player) as? PlayerFragment
            it.supportFragmentManager.beginTransaction()
                .remove(playerFragment!!)
                .commit()
        }
        playerService?.pause()
        playerService?.stopForeground(STOP_FOREGROUND_REMOVE)
        playerService?.stopSelf()
        viewModel.isPlayerOpened.postValue(false)
        showBottomNav()
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
        setImageAndBackground(requireContext(), song)
        binding.apply {
            tvSongTitle.text = song.title
            tvSongArtist.text = song.artist
            tvSongAlbum.text = song.album
            setOnAlbumClick(song)
        }
    }

    private fun setOnAlbumClick(song: Song) {
        binding.tvSongAlbum.setOnClickListener {
            viewModel.songs.value?.filter { s -> s.albumId == song.albumId }.also {
                if (it != null) {
                    if (it.size > 1) {
                        val album = viewModel.albums.value?.find { a -> a.id == song.albumId }!!
                        val action = when (findNavController().currentDestination?.id) {
                            R.id.songsFragment -> {
                                SongsFragmentDirections.actionSongsFragmentToAlbumFragment(
                                    album
                                )
                            }

                            R.id.albumsFragment -> {
                                AlbumsFragmentDirections.actionAlbumsFragmentToAlbumFragment(
                                    album
                                )
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

    var prevImage: ByteArray? = null
    private fun setImageAndBackground(context: Context, song: Song) {
        lifecycleScope.launch {
            val image = WorkWithImage.getSongArt(song.path)
            if (prevImage == null) {
                prevImage = image
                Glide.with(context)
                    .load(image)
                    .override(500)
                    .error(R.drawable.placeholder_no_art)
                    .into(binding.ivSong)
            }
            if (!prevImage.contentEquals(image))
                Glide.with(context)
                    .load(image)
                    .override(500)
                    .error(R.drawable.placeholder_no_art)
                    .into(binding.ivSong)
            if (isBackgroundGradient) {
                setGradientBackGround(
                    image,
                    binding.playerContainer,
                    requireContext()
                )
            }
            prevImage = image
        }
        playerService?.showNotification(R.drawable.ic_pause)
    }


    override fun pausePlayClick() {
        viewModel.playerPaused.value = playerService!!.isPaused
    }

    override fun previousClick() {
        viewModel.setCurrentSong(playerService!!.songInPlayer!!)
        viewModel.getSongLyricsFromDb(playerService!!.songInPlayer!!)
    }

    override fun nextClick() {
        viewModel.playerPaused.postValue(false)
        viewModel.setCurrentSong(playerService!!.songInPlayer!!)
        viewModel.getSongLyricsFromDb(playerService!!.songInPlayer!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        playerService?.playable = null
    }
}