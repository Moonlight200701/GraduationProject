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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.greenrobot.eventbus.EventBus

const val notificationID = 1
const val channelID = "channelID"
val fStore = FirebaseFirestore.getInstance().collection("Users").document(FirebaseAuth.getInstance().currentUser!!.uid).collection("Reminder")
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        var userId = ""
        val user = FirebaseAuth.getInstance().currentUser
        if(user != null){
            userId = user.uid
        }
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
            val location = bundle.getString(Constant.BUNDLE_LOCATION_TO_WATCH_KEY, "").toString()
            "${vote}/10".also { rate = it }

            val databaseOpenHelper = DatabaseOpenHelper(context, null)
            if (databaseOpenHelper.deleteReminderByMovieId(movieId, userId) > 0) {
                val notification = NotificationCompat.Builder(context, channelID)
                    .setSmallIcon(R.drawable.ic_movie_24)
                    .setContentTitle(movieTitle)
                    .setContentText("It's time to watch $movieTitle. Release: $release - Rate: $rate")
                    .setContentText("At the location: $location")
                    .build()
                val manager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(notificationID, notification)

                //Delete from firestore when the notification shows
                fStore.document(movieId.toString()).delete()

                EventBus.getDefault().post(ReminderEvent(movieId))
            } else {
                Log.d("AlarmReceiver", "Remove Fail")
            }
        }
    }
}