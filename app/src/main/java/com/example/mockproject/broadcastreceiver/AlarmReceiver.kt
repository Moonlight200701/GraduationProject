package com.example.mockproject.broadcastreceiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.mockproject.R
import com.example.mockproject.constant.Constant
import com.example.mockproject.database.DatabaseOpenHelper
import com.example.mockproject.eventbus.ReminderEvent
import org.greenrobot.eventbus.EventBus

const val notificationID = 1
const val channelID = "channelID"

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val movieId: Int
        val movieTitle: String
        val release: String
        var rate: String
        val bundle = intent.extras
        if (bundle != null) {
            movieId = bundle.getInt(Constant.BUNDLE_ID_KEY, 0)
            movieTitle = bundle.getString(Constant.BUNDLE_TITLE_KEY, "")
            release = bundle.getString(Constant.BUNDLE_RELEASE_KEY, "")
            val vote = bundle.getDouble(Constant.BUNDLE_RATE_KEY, 0.0).toString()
            "${vote}/10".also { rate = it }

            val databaseOpenHelper = DatabaseOpenHelper(context, null)
            if (databaseOpenHelper.deleteReminderByMovieId(movieId) > 0) {
                val notification = NotificationCompat.Builder(context, channelID)
                    .setSmallIcon(R.drawable.ic_movie_24)
                    .setContentTitle(movieTitle)
                    .setContentText("It's time to watch the film. Release: $release - Rate: $rate")
                    .build()
                val manager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(notificationID, notification)
                EventBus.getDefault().post(ReminderEvent(movieId))
            } else {
                Log.d("AlarmReceiver", "Remove Fail")
            }
        }
    }
}