package com.example.mockproject.util

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.example.mockproject.broadcastreceiver.AlarmReceiver
import com.example.mockproject.constant.Constant
import com.example.mockproject.model.Movie


class NotificationUtil {
    fun createNotification(movie: Movie, reminderTime: Long, context: Context) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val bundle = Bundle()
        bundle.putInt(Constant.BUNDLE_ID_KEY, movie.id)
        bundle.putString(Constant.BUNDLE_TITLE_KEY, movie.title)
        bundle.putString(Constant.BUNDLE_RELEASE_KEY, movie.releaseDate)
        bundle.putDouble(Constant.BUNDLE_RATE_KEY, movie.voteAverage)
        intent.putExtras(bundle)

        val pendingIntent = PendingIntent.getBroadcast(
            context, movie.id, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            )
        }
    }

    fun cancelNotification(movieId: Int, context: Context) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            movieId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarmManager = context
            .getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }
}