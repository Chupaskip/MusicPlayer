package com.example.musicplayer.ui

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.musicplayer.models.Song
import com.example.musicplayer.models.setImages
import java.util.Collections


class MusicViewModel : ViewModel() {
    private val _songs = MutableLiveData<ArrayList<Song>>()
    val songs: LiveData<ArrayList<Song>> get() = _songs

    private val _shuffledSongs = MutableLiveData<ArrayList<Song>>()
    val shuffledSongs: LiveData<ArrayList<Song>> get() = _shuffledSongs

    private val _currentSong = MutableLiveData<Song>()
    val currentSong: LiveData<Song> get() = _currentSong

    var isPlayerOpened: Boolean = false
    private val _isPlayerExpanded = MutableLiveData<Boolean>()
    val isPlayerExpanded: LiveData<Boolean> get() = _isPlayerExpanded

    private val _isShuffled = MutableLiveData(false)
    val isShuffled: LiveData<Boolean> get() = _isShuffled

    private val _isRepeated = MutableLiveData(false)
    val isRepeated: LiveData<Boolean> get() = _isRepeated

    var playerPosition:Int = 0

    fun setPreviousSong() {
        val position = getPositionOfSong()
        if (position > 0) {
            _currentSong.value =
                if (!isShuffled.value!!) _songs.value!![position - 1] else shuffledSongs.value!![position - 1]
        }
    }

    fun setNextSong(onComplete:Boolean=false) {
        playerPosition = 0
        if (isRepeated.value!!&&onComplete) {
            _currentSong.postValue(_currentSong.value)
            return
        }
        val position = getPositionOfSong()
        _currentSong.value = if (position < _songs.value?.size!! - 1 && !isShuffled.value!!) {
            _songs.value!![position + 1]
        } else {
            if (!isShuffled.value!!) {
                _songs.value!![0]
            } else if (position < shuffledSongs.value!!.size - 1) {
                shuffledSongs.value!![position + 1]
            } else {
                shuffledSongs.value!![0]
            }
        }
    }

    private fun getPositionOfSong(): Int {
        return if (!_isShuffled.value!!) _songs.value?.indexOf(_currentSong.value)!! else shuffledSongs.value?.indexOf(
            _currentSong.value)!!
    }



    fun setCurrentSong(song: Song) {
        playerPosition = 0
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
            ArrayList(_songs.value!!.shuffled())
        Collections.swap(shuffledList, 0, shuffledList.indexOf(currentSong.value))
        _shuffledSongs.postValue(shuffledList)
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

    fun getSongs(context: Context) {
        val tempSongs: ArrayList<Song> = arrayListOf()
//        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(
                VOLUME_EXTERNAL_PRIMARY
            )
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val selection = StringBuilder("is_music != 0 AND title != ''")
        val projection: Array<String> = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
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
        Log.i("SongsTest", tempSongs.toString())
        _songs.postValue(tempSongs)
    }
}