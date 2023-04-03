package com.example.musicplayer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Build.VERSION_CODES.O
import dagger.hilt.android.HiltAndroidApp

const val CHANNEL_ID_1 = "channel1"
const val CHANNEL_ID_2 = "channel2"
const val ACTION_PREVIOUS = "actionPrevious"
const val ACTION_NEXT = "actionNext"
const val ACTION_PLAY = "actionPlay"
const val ACTION_CLOSE = "actionClose"

@HiltAndroidApp
class MusicPlayerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= O) {
            val channel1 =
                NotificationChannel(CHANNEL_ID_1, "Chanel(1)", NotificationManager.IMPORTANCE_HIGH)
            channel1.description = "Channel 1 Desc..."

            val channel2 =
                NotificationChannel(CHANNEL_ID_2, "Chanel(2)", NotificationManager.IMPORTANCE_HIGH)
            channel2.description = "Channel 2 Desc..."
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel1)
            notificationManager.createNotificationChannel(channel2)
        }
    }
}