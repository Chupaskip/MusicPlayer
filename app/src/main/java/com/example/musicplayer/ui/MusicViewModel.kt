package com.example.musicplayer.ui

import android.content.Context
import android.database.Cursor
import android.media.MediaPlayer
import android.os.Build
import android.provider.MediaStore.Audio.Media
import android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.models.Song
import com.example.musicplayer.models.setImages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*


class MusicViewModel : ViewModel() {
    private val _songs = MutableLiveData<MutableList<Song>>()
    val songs: LiveData<MutableList<Song>> get() = _songs

    private val shuffledSongs: MutableList<Song> = mutableListOf()

    private val _currentSong = MutableLiveData<Song>()
    val currentSong: LiveData<Song> get() = _currentSong

    var isPlayerOpened = MutableLiveData<Boolean>()
    private val _isPlayerExpanded = MutableLiveData<Boolean>()
    val isPlayerExpanded: LiveData<Boolean> get() = _isPlayerExpanded

    private val _isShuffled = MutableLiveData(false)
    val isShuffled: LiveData<Boolean> get() = _isShuffled

    private val _isRepeated = MutableLiveData(false)
    val isRepeated: LiveData<Boolean> get() = _isRepeated

    val currentDurationInMSec = MutableLiveData<Int>()

    val totalDurationOfSong = MutableLiveData<Int>()

    lateinit var player: MediaPlayer

    val playerPaused = MutableLiveData(false)

    val isSongClickable = MutableLiveData(true)

    private var job: Job? = null

    fun setMediaPlayer(newPlayer: MediaPlayer) {
        player = if (currentSong.value != null) {
            player.release()
            newPlayer
        } else {
            newPlayer
        }
        setTimerOfSong()
        player.setOnPreparedListener { player.start() }
        totalDurationOfSong.value = (player.duration / 1000)
    }

    fun setTimerOfSong() {
        if (job == null)
            job = viewModelScope.launch {
                while (true) {
                    currentDurationInMSec.postValue(player.currentPosition)
                    delay(500)
                }
            }
    }

    fun setPreviousSong() {
        val position = getPositionOfSong()
        _currentSong.value =
            if (position > 0) {
                if (!isShuffled.value!!) _songs.value!![position - 1] else shuffledSongs[position - 1]
            } else {
                if (!isShuffled.value!!) {
                    songs.value!![songs.value!!.size - 1]
                } else {
                    shuffledSongs[shuffledSongs.size - 1]
                }
            }
    }

    fun setNextSong() {
        if (isRepeated.value!!) {
            _currentSong.postValue(_currentSong.value)
            return
        }
        val position = getPositionOfSong()
        _currentSong.value = if (position < _songs.value?.size!! - 1 && !isShuffled.value!!) {
            _songs.value!![position + 1]
        } else {
            if (!isShuffled.value!!) {
                _songs.value!![0]
            } else if (position < shuffledSongs.size - 1) {
                shuffledSongs[position + 1]
            } else {
                shuffledSongs[0]
            }
        }
    }

    private fun getPositionOfSong(): Int {
        return if (!_isShuffled.value!!) _songs.value?.indexOf(_currentSong.value)!! else shuffledSongs.indexOf(
            _currentSong.value)!!
    }


    fun setCurrentSong(song: Song) {
        currentDurationInMSec.value = 0
        _currentSong.postValue(song)
    }

    fun setRepeatedSong() {
        _isRepeated.postValue(true)
    }

    fun cancelRepeatedSong() {
        _isRepeated.postValue(false)
    }

    fun setShuffledSongs() {
        val shuffledList =
            (_songs.value!!.shuffled()).toList()
        Collections.swap(shuffledList, 0, shuffledList.indexOf(currentSong.value))
        shuffledSongs.addAll(shuffledList)
        _isShuffled.postValue(true)
    }

    fun removeShuffledSongs() {
        _isShuffled.postValue(false)
    }

    fun setPlayerExpanded() {
        _isPlayerExpanded.postValue(true)
    }

    fun setPlayerCollapsed() {
        _isPlayerExpanded.postValue(false)
    }

    fun getSongs(context: Context, deletedSong: Song? = null) {
        val tempSongs: MutableList<Song> = arrayListOf()
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Media.getContentUri(
                VOLUME_EXTERNAL_PRIMARY
            )
        } else {
            Media.EXTERNAL_CONTENT_URI
        }
        val selection = StringBuilder("is_music != 0 AND title != ''")
        val projection: Array<String> = arrayOf(
            Media._ID,
            Media.DATA,
            Media.TITLE,
            Media.ARTIST,
            Media.ALBUM,
            Media.DURATION
        )
        val cursor: Cursor? = context.contentResolver.query(uri, projection,
            selection.toString(), null, null)
        if (cursor != null) {
            while (cursor.moveToNext()) {
                val song = Song(
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getString(5)
                )

                tempSongs.add(song)
            }
            cursor.close()
            tempSongs.setImages()
        }
        deletedSong?.also {
            tempSongs.remove(it)
        }
        _songs.postValue(tempSongs)
    }
}

