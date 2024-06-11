package com.example.musicplayer.ui

import android.app.Application
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore.Audio.Media
import android.provider.MediaStore.VOLUME_EXTERNAL
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.MusicPlayerApplication
import com.example.musicplayer.db.SongEntity
import com.example.musicplayer.models.Song
import com.example.musicplayer.db.SongsDatabase
import com.example.musicplayer.db.UserEntity
import com.example.musicplayer.models.Album
import com.example.musicplayer.ui.util.WorkWithImage
import com.example.musicplayer.ui.util.sdk29AndUp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MusicViewModel @Inject constructor(
    private val app: Application,
) : AndroidViewModel(app) {
    val isReadPermissionGranted = MutableLiveData(false)
    private val _songs: LiveData<List<Song>> = isReadPermissionGranted.switchMap {
        if (it) {
            return@switchMap getSongsAndAlbums()
        }
        return@switchMap null
    }
    val songs: LiveData<List<Song>> get() = _songs

    val songsFavorite = MutableLiveData<List<Song>>()

    private val _songsByAlbumId = MutableLiveData<MutableList<Song>>()
    val songsByAlbumId: LiveData<MutableList<Song>> get() = _songsByAlbumId

    private val _albums = MutableLiveData<MutableList<Album>>()
    val albums: LiveData<MutableList<Album>> get() = _albums

    private val _currentSong = MutableLiveData<Song>()

    val currentSong: LiveData<Song> get() = _currentSong
    var isPlayerOpened = MutableLiveData<Boolean>(false)

    private val _isPlayerExpanded = MutableLiveData<Boolean>()
    val isPlayerExpanded: LiveData<Boolean> get() = _isPlayerExpanded
    private val _isShuffled = MutableLiveData(false)
    val isShuffled: LiveData<Boolean> get() = _isShuffled

    private val _isRepeated = MutableLiveData(false)
    val isRepeated: LiveData<Boolean> get() = _isRepeated

    val playerPaused = MutableLiveData(false)

    val isSongClickable = MutableLiveData(true)

    private var job: Job? = null

    var songToDelete: Song? = null

    var songsInPlayer = arrayListOf<Song>()

    val shuffledSongs: MutableList<Song> = mutableListOf()

    val searchQuery = MutableLiveData("")

    val isLyricsDialogVisible = MutableLiveData(false)

    val isBottomMenuVisible = MutableLiveData(true)

    var currentUserId: Long = 0

    private fun getPositionOfSong(): Int {
        return if (!_isShuffled.value!!) {
            songsInPlayer.indexOf(_currentSong.value)
        } else shuffledSongs.indexOf(
            _currentSong.value
        )
    }

    fun setCurrentSong(song: Song) {
        _currentSong.postValue(song)
    }

    fun setRepeatedSong() {
        _isRepeated.postValue(true)
    }

    fun cancelRepeatedSong() {
        _isRepeated.postValue(false)
    }

    fun setShuffledSongs() {
        _isShuffled.postValue(true)
    }

    fun cancelShuffledSongs() {
        _isShuffled.postValue(false)
    }

    fun setPlayerExpanded() {
        _isPlayerExpanded.postValue(true)
    }

    fun setPlayerCollapsed() {
        _isPlayerExpanded.postValue(false)
    }

    init {
        val sharedPreferences =
            app.applicationContext.getSharedPreferences("preferences", Context.MODE_PRIVATE)
        val savedUserName = sharedPreferences.getString("userName", "")
        val savedPassword = sharedPreferences.getString("password", "")
        if (savedUserName != null || savedPassword != null) {
            val userLog = UserEntity(
                sharedPreferences.getLong("userId", -1),
                savedUserName!!,
                savedPassword!!
            )
            login(userLog, true)
        }
    }

    private fun getSongsAndAlbums(): LiveData<List<Song>> {
        val tempSongs: MutableList<Song> = mutableListOf()
        val tempAlbums: MutableList<Album> = mutableListOf()
        val uri = sdk29AndUp { Media.getContentUri(VOLUME_EXTERNAL) } ?: Media.EXTERNAL_CONTENT_URI
        val selection = StringBuilder("is_music != 0 AND title != ''")
        val sortOrder = Media.DATE_ADDED + " desc"
        val projection: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            arrayOf(
                Media._ID, Media.DATA,
                Media.TITLE, Media.ARTIST, Media.DURATION,
                Media.ALBUM_ID, Media.ALBUM,
                Media.YEAR, Media.CD_TRACK_NUMBER
            )
        } else {
            arrayOf(
                Media._ID, Media.DATA,
                Media.TITLE, Media.ARTIST,
                Media.DURATION, Media.ALBUM_ID,
                Media.ALBUM, Media.YEAR
            )
        }
        val cursor: Cursor? = getApplication<MusicPlayerApplication>().contentResolver.query(
            uri, projection,
            selection.toString(), null, sortOrder
        )
        if (cursor != null) {
            while (cursor.moveToNext()) {
                val song = Song(
                    cursor.getString(0), cursor.getString(1),
                    cursor.getString(2), cursor.getString(3),
                    cursor.getString(4), cursor.getString(5),
                    cursor.getString(6)
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    song.numberInAlbum = cursor.getString(8)
                }
                val album = Album(
                    cursor.getString(5), cursor.getString(1),
                    cursor.getString(6), cursor.getString(3),
                    cursor.getString(7)
                )
                if (song.image == null) {
                    viewModelScope.launch {
                        song.image = WorkWithImage.getSongArt(song.path)
                    }
                }
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
        return MutableLiveData(tempSongs)
    }

    private fun getSongById(songId: String): Song? {
        val uri = sdk29AndUp {
            Media.getContentUri(VOLUME_EXTERNAL)
        } ?: Media.EXTERNAL_CONTENT_URI
        val selection = "${Media._ID} = ? AND is_music != 0 AND title != ''"
        val selectionArgs = arrayOf(songId)
        val cursor: Cursor? = getApplication<MusicPlayerApplication>().contentResolver.query(
            uri,
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
            ),
            selection,
            selectionArgs,
            null
        )
        var song: Song? = null
        cursor?.use { cursor ->
            if (cursor.moveToFirst()) {
                song = Song(
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getString(5),
                    cursor.getString(6)
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    song?.numberInAlbum = cursor.getString(8)
                }
                if (song?.image == null) {
                    viewModelScope.launch {
                        song?.image = WorkWithImage.getSongArt(song?.path ?: "")
                    }
                }
            }
        }
        return song
    }


    fun getSongsByAlbumId(albumId: String) {
        val albumSongs = songs.value!!.filter { song -> song.albumId == albumId }.toMutableList()
        songToDelete?.also {
            albumSongs.remove(it)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            albumSongs.sortBy { song -> song.numberInAlbum?.toInt() ?: 0 }
        }
        _songsByAlbumId.postValue(albumSongs)
    }

    fun searchSongs(): List<Song> {
        val searchedSongs = songs.value!!.filter { song ->
            song.title.contains(
                searchQuery.value!!,
                true
            ) || song.artist.contains(searchQuery.value!!, true)
        }
        return searchedSongs
    }

    fun searchSongsFavorite(): List<Song> {
        val searchedSongs = songsFavorite.value!!.filter { song ->
            song.title.contains(
                searchQuery.value!!,
                true
            ) || song.artist.contains(searchQuery.value!!, true)
        }
        return searchedSongs
    }

    fun getFavoriteSongs() {
        val songDao = SongsDatabase.getInstance(app.applicationContext).songDao()
        viewModelScope.launch {
            val songEntities = async{songDao.getAllSongs()}.await()
            val songs = mutableListOf<Song>()
            songEntities.forEach {
                if (it.isFavorite && it.idUser == currentUserId)
                    async {
                        getSongById(it.idMemory)?.let { it1 ->
                            it1.isFavorite = it.isFavorite
                            songs.add(it1)
                        }
                    }.await()
            }
            songsFavorite.value = songs
        }
    }

    fun getSongLyricsFromDb(song: Song) {
        val songDao = SongsDatabase.getInstance(app.applicationContext).songDao()
        viewModelScope.launch {
            val songEntities = async { songDao.getAllSongs() }.await()
            val songs = mutableListOf<Song>()
            songEntities.forEach {
                getSongById(it.idMemory)?.let { it1 ->
                    songs.add(it1)
                    if (it.idMemory == song.id && it.idUser == currentUserId) {
                        it1.lyrics = it.lyrics
                        _currentSong.postValue(it1)
                    }
                }
            }
        }
    }

    fun addToDatabase(song: Song, isFavorite:Boolean? = null, lyrics: String? = null) {
        val songDao = SongsDatabase.getInstance(app.applicationContext).songDao()
        viewModelScope.launch {
            val songInDb = async { songDao.getSongById(song.id, currentUserId) }.await()
            if (songInDb != null) {
                songDao.insert(
                    SongEntity(
                        id = songInDb.id,
                        idMemory = song.id,
                        idUser = currentUserId,
                        isFavorite = isFavorite ?: songInDb.isFavorite,
                        lyrics = lyrics ?: songInDb.lyrics
                    )
                )
            } else {
                songDao.insert(
                    SongEntity(
                        idMemory = song.id,
                        idUser = currentUserId,
                        isFavorite = isFavorite ?: false,
                        lyrics = lyrics ?: ""
                    )
                )
            }
        }
    }

    fun deleteFromFavorites(song: Song) {
        viewModelScope.launch{
            addToDatabase(song, isFavorite = false)
            delay(500)
            getFavoriteSongs()
        }
    }

    fun deleteFromDatabase(song: Song) {
//        val songDao = SongsDatabase.getInstance(app.applicationContext).songDao()
//        viewModelScope.launch {
//            songDao.delete(
//                Song(
//                    song.id,
//                    song.path,
//                    song.title,
//                    song.artist,
//                    song.duration,
//                    song.albumId,
//                    song.album,
//                    song.numberInAlbum,
//                    song.isFavorite,
//                    song.lyrics
//                )
//            )
//            getFavoriteSongs()
//        }
    }

    fun register(user: UserEntity, lambdaOnSuccess: () -> Unit) {
        val songDao = SongsDatabase.getInstance(app.applicationContext).songDao()
        viewModelScope.launch {
            val userFound: UserEntity? = async { songDao.userSameLogin(user.username) }.await()
            if (userFound == null || userFound.username != user.username) {
                songDao.userSignUp(user)
                lambdaOnSuccess()
            } else
                Toast.makeText(
                    app.applicationContext, "Пользователь с данным логином уже зарегестрирован!",
                    Toast.LENGTH_SHORT
                ).show()
        }
    }

    val isLogin: MutableLiveData<Boolean> = MutableLiveData(false)
    fun login(user: UserEntity, savedUser: Boolean = false) {
        val songDao = SongsDatabase.getInstance(app.applicationContext).songDao()
        var userFound: UserEntity?
        viewModelScope.launch {
            userFound = songDao.userLogin(user.username, user.password)
            if (userFound != null) {
                currentUserId = userFound!!.id
                val sharedPreferences =
                    app.applicationContext.getSharedPreferences("preferences", Context.MODE_PRIVATE)
                sharedPreferences.edit().apply {
                    putLong("uderId", userFound!!.id); putString("userName", userFound!!.username)
                    putString("password", userFound!!.password); apply()
                }
                isLogin.postValue(true)
            } else {
                if (!savedUser)
                    Toast.makeText(
                        app.applicationContext,
                        "Пользователь не найден или введены неверные данные!",
                        Toast.LENGTH_SHORT
                    ).show()
            }
        }
    }

    fun logout() {
        val sharedPreferences =
            app.applicationContext.getSharedPreferences("preferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putLong("uderId", -1); putString("userName", "")
            putString("password", ""); apply()
        }
        isLogin.postValue(false)
    }
}