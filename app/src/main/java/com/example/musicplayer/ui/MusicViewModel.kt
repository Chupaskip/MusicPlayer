package com.example.musicplayer.ui

import android.content.Context
import android.database.Cursor
import android.media.MediaPlayer
import android.os.Build
import android.provider.MediaStore.Audio.Media
import android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.models.Album
import com.example.musicplayer.models.Song
import com.example.musicplayer.ui.util.Constants.Companion.EMPTY_SEARCH
import com.example.musicplayer.ui.util.sdk29AndUp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*


class MusicViewModel : ViewModel() {
    private val _songs = MutableLiveData<MutableList<Song>>()
    val songs: LiveData<MutableList<Song>> get() = _songs

    private val _songsByAlbumId = MutableLiveData<MutableList<Song>>()
    val songsByAlbumId: LiveData<MutableList<Song>> get() = _songsByAlbumId

    private val _albums = MutableLiveData<MutableList<Album>>()
    val albums: LiveData<MutableList<Album>> get() = _albums

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

    var songToDelete: Song? = null

    var songsInPlayer = mutableListOf<Song>()

    private val shuffledSongs: MutableList<Song> = mutableListOf()

    val searchQuery = MutableLiveData(EMPTY_SEARCH)

    fun setMediaPlayer(newPlayer: MediaPlayer) {
        player = if (currentSong.value != null) {
            player.release()
            newPlayer
        } else {
            newPlayer
        }
        playerPaused.postValue(false)
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
                if (!isShuffled.value!!) songsInPlayer[position - 1] else shuffledSongs[position - 1]
            } else {
                if (!isShuffled.value!!) {
                    songsInPlayer[songsInPlayer.size - 1]
                } else {
                    shuffledSongs[shuffledSongs.size - 1]
                }
            }
    }

    fun setNextSong(onComplete: Boolean = true) {
        if (isRepeated.value!! && onComplete) {
            _currentSong.postValue(_currentSong.value)
            return
        }
        val position = getPositionOfSong()
        _currentSong.value = if (!isShuffled.value!!) {
            if (position < songsInPlayer.size - 1)
                songsInPlayer[position + 1]
            else
                songsInPlayer[0]
        } else {
            if (position < shuffledSongs.size - 1) {
                shuffledSongs[position + 1]
            } else {
                shuffledSongs[0]
            }
        }
    }

    private fun getPositionOfSong(): Int {
        return if (!_isShuffled.value!!) {
            songsInPlayer.indexOf(_currentSong.value)
        } else shuffledSongs.indexOf(
            _currentSong.value)
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
            (songsInPlayer.shuffled().toList())
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

    fun getSongsAlbums(context: Context) {
        val tempSongs: MutableList<Song> = mutableListOf()
        val tempAlbums: MutableList<Album> = mutableListOf()
        val uri = sdk29AndUp {
            Media.getContentUri(
                VOLUME_EXTERNAL_PRIMARY
            )
        } ?: Media.EXTERNAL_CONTENT_URI
        val selection = StringBuilder("is_music != 0 AND title != ''")
        val sortOrder = Media.DATE_ADDED + " desc"
        val projection: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            arrayOf(
                Media._ID,
                Media.DATA,
                Media.TITLE,
                Media.ARTIST,
                Media.DURATION,
                Media.ALBUM_ID,
                Media.ALBUM,
                Media.YEAR,
                Media.CD_TRACK_NUMBER
            )
        } else {
            arrayOf(
                Media._ID,
                Media.DATA,
                Media.TITLE,
                Media.ARTIST,
                Media.DURATION,
                Media.ALBUM_ID,
                Media.ALBUM,
                Media.YEAR
            )
        }
        val cursor: Cursor? = context.contentResolver.query(uri, projection,
            selection.toString(), null, sortOrder)
        if (cursor != null) {
            while (cursor.moveToNext()) {
                val song = Song(
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getString(5),
                    cursor.getString(6)
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    song.numberInAlbum = cursor.getString(8)
                }
                val album = Album(
                    cursor.getString(5),
                    cursor.getString(1),
                    cursor.getString(6),
                    cursor.getString(3),
                    cursor.getString(7)
                )
                tempSongs.add(song)
                if (tempAlbums.none { a -> a.id == album.id } && tempSongs.filter { s -> s.albumId == album.id }.size > 1) {
                    tempAlbums.add(album)
                }
            }
            cursor.close()
        }
        songToDelete?.also {
            tempSongs.remove(it)
        }
        _albums.postValue(tempAlbums)
        _songs.postValue(tempSongs)
    }

    fun getSongsByAlbumId(context: Context, albumId: String) {
        val albumSongs = songs.value!!.filter { song -> song.albumId == albumId }.toMutableList()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            albumSongs.sortBy { song -> song.numberInAlbum.toInt() }
        }
        _songsByAlbumId.postValue(albumSongs)
    }

    fun searchSongs(): List<Song> {
        val searchedSongs = songs.value!!.filter { song ->
            song.title.contains(searchQuery.value!!) || song.artist.contains(searchQuery.value!!)
        }
        return searchedSongs
    }
}

