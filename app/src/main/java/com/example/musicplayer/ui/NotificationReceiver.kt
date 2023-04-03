package com.example.musicplayer.ui

import android.content.*
import android.os.IBinder
import com.example.musicplayer.ACTION_CLOSE
import com.example.musicplayer.ACTION_NEXT
import com.example.musicplayer.ACTION_PLAY
import com.example.musicplayer.ACTION_PREVIOUS
import com.example.musicplayer.ui.services.PlayerService

const val ACTION_NAME = "ActionName"

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        val serviceIntent = Intent(context, PlayerService::class.java)
        action?.also {
            when (action) {
                ACTION_PLAY -> {
                    serviceIntent.putExtra(ACTION_NAME, "playPause")
                }
                ACTION_PREVIOUS -> {
                    serviceIntent.putExtra(ACTION_NAME, "previous")
                }
                ACTION_NEXT -> {
                    serviceIntent.putExtra(ACTION_NAME, "next")
                }
                ACTION_CLOSE -> {
                    serviceIntent.putExtra(ACTION_NAME, "close")
                }
            }
            context?.startService(serviceIntent)
        }
    }
}