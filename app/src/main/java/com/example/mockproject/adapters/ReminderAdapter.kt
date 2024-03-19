package com.example.mockproject.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mockproject.R
import com.example.mockproject.constant.APIConstant
import com.example.mockproject.model.Movie
import com.squareup.picasso.Picasso

class ReminderAdapter(
    private var mReminderList: ArrayList<Movie>,
    private var mReminderType: Int,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        const val REMINDER_PROFILE = 0
        const val REMINDER_ALL = 1
    }

    private var mReminderListListener: ReminderListListener? = null

    fun setReminderListener(reminderListListener: ReminderListListener) {
        this.mReminderListListener = reminderListListener

    }

    fun updateData(listMovieReminder: ArrayList<Movie>) {
        this.mReminderList = listMovieReminder
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return if (mReminderType == REMINDER_ALL) mReminderList.size else {
            if (mReminderList.size > 3) 3 else mReminderList.size
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return RemindViewHolder(
            mReminderType,
            mReminderListListener,
            LayoutInflater.from(parent.context).inflate(R.layout.reminder_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as RemindViewHolder).bindData(position, mReminderList[position])
    }

    class RemindViewHolder(
        private var reminderType: Int,
        private var mReminderListListener: ReminderListListener?,
        itemView: View
    ) :
        RecyclerView.ViewHolder(itemView) {
        private var imgPoster: ImageView = itemView.findViewById(R.id.item_reminder_poster_img)
        private var title: TextView = itemView.findViewById(R.id.item_reminder_title_text)
        private var releaseDate: TextView = itemView.findViewById(R.id.item_reminder_release_text)
        private var reminderTime: TextView = itemView.findViewById(R.id.item_reminder_time_text)

        fun bindData(position: Int, movie: Movie) {
            if (reminderType == REMINDER_PROFILE) {
                imgPoster.visibility = View.GONE
            } else {
                imgPoster.visibility = View.VISIBLE
                val url = APIConstant.BASE_IMG_URL + movie.posterPath
                Picasso.get().load(url).into(imgPoster)
                if (reminderType == REMINDER_ALL) {
                    itemView.setOnClickListener { mReminderListListener!!.onClickItemReminder(movie) }
                    itemView.setOnLongClickListener {
                        mReminderListListener!!.onLongClickItemReminder(position)
                    }
                }
            }

            title.text = movie.title
            "Release date: ${movie.releaseDate}".also { releaseDate.text = it }
            "Reminder time: ${movie.reminderTimeDisplay}".also { reminderTime.text = it }
        }
    }

    interface ReminderListListener {
        fun onClickItemReminder(movie: Movie)
        fun onLongClickItemReminder(position: Int): Boolean
    }
}