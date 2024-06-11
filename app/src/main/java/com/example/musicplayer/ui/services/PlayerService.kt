package com.example.musicplayer.ui.services

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.Service
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaMetadata
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.musicplayer.ACTION_CLOSE
import com.example.musicplayer.ACTION_NEXT
import com.example.musicplayer.ACTION_PLAY
import com.example.musicplayer.ACTION_PREVIOUS
import com.example.musicplayer.CHANNEL_ID_2
import com.example.musicplayer.R
import com.example.musicplayer.models.Song
import com.example.musicplayer.ui.ACTION_NAME
import com.example.musicplayer.ui.MainActivity
import com.example.musicplayer.ui.NotificationReceiver
import com.example.musicplayer.ui.Playable
import com.example.musicplayer.ui.fragments.SONG
import com.example.musicplayer.ui.fragments.SONGS_IN_PLAYER
import java.util.Collections

class PlayerService() : Service(), MediaPlayer.OnCompletionListener {
    private val binder: IBinder = MyBinder()
    var mediaPlayer: MediaPlayer? = null
    var songInPlayer: Song? = null
    var songsInPlayer = arrayListOf<Song>()
    var shuffledSongs = arrayListOf<Song>()
    var playable: Playable? = null
    var isRepeated: Boolean = false
    var isShuffled: Boolean = false
    var isPaused: Boolean = false
    var mediaSessionCompat: MediaSessionCompat? = null


    override fun onCreate() {
        super.onCreate()
        mediaSessionCompat = MediaSessionCompat(baseContext, "Audio")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableArrayListExtra(SONGS_IN_PLAYER, Song::class.java)?.also {
                songsInPlayer = it
            }
            intent?.getParcelableExtra(SONG, Song::class.java)?.also {
                songInPlayer = it
                createMediaPlayer()
            }
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableArrayListExtra<Song>(SONGS_IN_PLAYER)?.also {
                songsInPlayer = it
            }
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra<Song>(SONG)?.also {
                songInPlayer = it
                createMediaPlayer()
            }
        }
        return START_NOT_STICKY
    }


    inner class MyBinder : Binder() {
        fun getService(): PlayerService {
            return this@PlayerService
        }
    }

    fun start() {
        showNotification(R.drawable.ic_pause)
        isPaused = false
        mediaPlayer?.start()
    }

    fun pause() {
        showNotification(R.drawable.ic_play)
        isPaused = true
        mediaPlayer?.pause()
    }

    fun getDuration() = mediaPlayer?.duration

    fun seekTo(position: Int) {
        mediaPlayer!!.seekTo(position)
    }

    private fun createMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer?.release()
        }
        val uri = Uri.parse(songInPlayer!!.path)
        mediaPlayer = MediaPlayer.create(baseContext, uri)
        mediaPlayer?.setOnPreparedListener { start() }
        mediaPlayer!!.setOnCompletionListener(this)
        isPaused = false
        playable?.pausePlayClick()
    }

    fun getCurrentPosition() = mediaPlayer?.currentPosition

    fun setNextSong(onComplete: Boolean = false) {
        if (isRepeated && onComplete) {
            createMediaPlayer()
            return
        }
        val position = getPositionOfSong()
        songInPlayer = if (!isShuffled) {
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
        createMediaPlayer()
        playable?.nextClick()
    }

    fun setPreviousSong() {
        val position = getPositionOfSong()
        songInPlayer = if (position > 0) {
            if (!isShuffled) songsInPlayer[position - 1] else shuffledSongs[position - 1]
        } else {
            if (!isShuffled) {
                songsInPlayer[songsInPlayer.size - 1]
            } else {
                shuffledSongs[shuffledSongs.size - 1]
            }
        }
        createMediaPlayer()
        playable?.previousClick()
    }

    private fun getPositionOfSong(): Int {
        return if (!isShuffled) {
            songsInPlayer.indexOf(songInPlayer)
        } else shuffledSongs.indexOf(
            songInPlayer)
    }

    override fun onCompletion(mp: MediaPlayer?) {
        setNextSong(true)
        playable!!.nextClick()
    }

    fun setShuffledSongs() {
        shuffledSongs.clear()
        val shuffledList =
            (songsInPlayer.shuffled().toList())
        Collections.swap(shuffledList, 0, shuffledList.indexOf(songInPlayer))
        shuffledSongs.addAll(shuffledList)
        isShuffled = true
    }

    fun cancelShuffledSongs() {
        isShuffled = false
    }

    fun showNotification(playPause: Int) {
        val intent = Intent(baseContext, MainActivity::class.java)
        intent.putExtra("TEST", songInPlayer)
        intent.flags = FLAG_ACTIVITY_SINGLE_TOP
        val contentIntent =
            PendingIntent.getActivity(baseContext, 0, intent, FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)
        val prevIntent = Intent(baseContext, NotificationReceiver::class.java)
            .setAction(ACTION_PREVIOUS)
        val prevContentIntent =
            PendingIntent.getBroadcast(baseContext,
                0,
                prevIntent,
                PendingIntent.FLAG_IMMUTABLE)
        val nextIntent = Intent(baseContext, NotificationReceiver::class.java)
            .setAction(ACTION_NEXT)
        val nextContentIntent =
            PendingIntent.getBroadcast(baseContext,
                0,
                nextIntent,
                PendingIntent.FLAG_IMMUTABLE)
        val pauseIntent = Intent(baseContext, NotificationReceiver::class.java)
            .setAction(ACTION_PLAY)
        val pauseContentIntent =
            PendingIntent.getBroadcast(baseContext,
                0,
                pauseIntent,
                FLAG_IMMUTABLE)
        val closeIntent = Intent(baseContext, NotificationReceiver::class.java)
            .setAction(ACTION_CLOSE)
        val closeContentIntent =
            PendingIntent.getBroadcast(baseContext,
                0,
                closeIntent,
                FLAG_IMMUTABLE)
        var image: Bitmap?
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(songInPlayer?.path)
        val byteArray = retriever.embeddedPicture
        byteArray.also { byteArray ->
            image = if (byteArray != null)
                BitmapFactory.decodeByteArray(byteArray,
                    0,
                    byteArray.size)
            else
                AppCompatResources.getDrawable(baseContext, R.drawable.placeholder_no_art)
                    ?.toBitmap(500, 500)
        }
        val bld = MediaMetadataCompat.Builder()
        bld.putBitmap(MediaMetadata.METADATA_KEY_ART, image)
        mediaSessionCompat!!.setMetadata(bld.build())
        val notification = NotificationCompat.Builder(baseContext, CHANNEL_ID_2)
            .setSmallIcon(playPause)
            .setContentIntent(contentIntent)
            .setContentTitle(songInPlayer?.title)
            .setContentText(songInPlayer?.artist)
            .addAction(R.drawable.ic_previous, "Previous", prevContentIntent)
            .addAction(playPause, "Pause", pauseContentIntent)
            .setColor(Color.argb(255, 255, 255, 255))
            .addAction(R.drawable.ic_next, "Next", nextContentIntent)
            .addAction(R.drawable.ic_close, "Close", closeContentIntent)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSessionCompat?.sessionToken))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        startForeground(1, notification)
    }
}