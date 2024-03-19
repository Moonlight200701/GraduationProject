package com.example.mockproject.view

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.example.mockproject.R
import com.example.mockproject.adapters.CastAndCrewAdapter
import com.example.mockproject.api.ApiInterface
import com.example.mockproject.api.RetrofitClient
import com.example.mockproject.broadcastreceiver.*
import com.example.mockproject.constant.APIConstant
import com.example.mockproject.constant.Constant
import com.example.mockproject.database.DatabaseOpenHelper
import com.example.mockproject.eventbus.ReminderEvent
import com.example.mockproject.listenercallback.BadgeListener
import com.example.mockproject.listenercallback.DetailListener
import com.example.mockproject.listenercallback.ReminderListener
import com.example.mockproject.listenercallback.ToolbarTitleListener
import com.example.mockproject.model.*
import com.example.mockproject.util.NotificationUtil
import com.squareup.picasso.Picasso
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import java.util.*
import kotlin.collections.ArrayList


class DetailFragment(private var mDatabaseOpenHelper: DatabaseOpenHelper) : Fragment(),
    View.OnClickListener {
    private lateinit var mMovie: Movie
    private lateinit var mMovieReminder: Movie
    private var mReminderExisted: Boolean = false
    private lateinit var mCastAndCrewList: ArrayList<CastAndCrew>

    private lateinit var mFavouriteBtn: ImageButton
    private lateinit var mReleaseDateText: TextView
    private lateinit var mRateText: TextView
    private lateinit var mPosterImg: ImageView
    private lateinit var mOverviewText: TextView
    private lateinit var mReminderBtn: Button
    private lateinit var mReminderTimeText: TextView
    private lateinit var mCastRecyclerView: RecyclerView
    private lateinit var mCastAndCrewAdapter: CastAndCrewAdapter

    private lateinit var mToolbarTitleListener: ToolbarTitleListener
    private lateinit var mBadgeListener: BadgeListener
    private lateinit var mDetailListener: DetailListener
    private lateinit var mReminderListener: ReminderListener

    fun setToolbarTitleListener(toolbarTitleListener: ToolbarTitleListener) {
        this.mToolbarTitleListener = toolbarTitleListener
    }

    fun setBadgeListener(badgeListener: BadgeListener) {
        this.mBadgeListener = badgeListener
    }

    fun setDetailListener(detailListener: DetailListener) {
        this.mDetailListener = detailListener
    }

    fun setRemindListener(reminderListener: ReminderListener) {
        this.mReminderListener = reminderListener
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_detail, container, false)
        val bundle = this.arguments
        if (bundle != null) {
            mMovie = bundle.getSerializable(Constant.MOVIE_KEY) as Movie
            val movieReminderList = mDatabaseOpenHelper.getReminderByMovieId(mMovie.id)
            if (movieReminderList.isEmpty()) {
                mReminderExisted = false
            } else {
                mReminderExisted = true
                mMovieReminder = movieReminderList[0]
            }
        }

        mFavouriteBtn = view.findViewById(R.id.favourite_img_btn)
        mReleaseDateText = view.findViewById(R.id.release_date_text)
        mRateText = view.findViewById(R.id.rate_text)
        mPosterImg = view.findViewById(R.id.movie_poster_img)
        mReminderBtn = view.findViewById(R.id.reminder_btn)
        mReminderTimeText = view.findViewById(R.id.reminder_time_text)
        mOverviewText = view.findViewById(R.id.overview_text)
        mCastRecyclerView = view.findViewById(R.id.cast_and_crew_recyclerview)

        if (mMovie.isFavorite) {
            mFavouriteBtn.setImageResource(R.drawable.ic_star_black_24)
        } else {
            mFavouriteBtn.setImageResource(R.drawable.ic_star_outline_24)
        }
        mFavouriteBtn.setOnClickListener(this)

        mReleaseDateText.text = mMovie.releaseDate
        "${mMovie.voteAverage}/10".also { mRateText.text = it }
        val url = APIConstant.BASE_IMG_URL + mMovie.posterPath
        Picasso.get().load(url).into(mPosterImg)
        mReminderBtn.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createReminder()
            }
        }
        if (mReminderExisted) {
            mReminderTimeText.visibility = View.VISIBLE
            mReminderTimeText.text = mMovieReminder.reminderTimeDisplay
        } else {
            mReminderTimeText.visibility = View.GONE
        }

        mOverviewText.text = mMovie.overview

        val layoutManager = LinearLayoutManager(activity)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        mCastAndCrewList = arrayListOf()
        mCastAndCrewAdapter = CastAndCrewAdapter(mCastAndCrewList)
        mCastRecyclerView.layoutManager = layoutManager
        mCastRecyclerView.setHasFixedSize(true)
        mCastRecyclerView.adapter = mCastAndCrewAdapter
        getCastAndCrewFromApi()
        setHasOptionsMenu(true)
        return view
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val item = menu.findItem(R.id.change_view)
        item.isVisible = false
    }

    override fun onDetach() {
        mToolbarTitleListener.onUpdateToolbarTitle("Movie")
        super.onDetach()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.favourite_img_btn -> {
                if (mMovie.isFavorite) {
                    if (mDatabaseOpenHelper.deleteMovie(mMovie.id) > -1) {
                        mMovie.isFavorite = false
                        mFavouriteBtn.setImageResource(R.drawable.ic_star_outline_24)
                        mDetailListener.onUpdateFromDetail(mMovie, false)
                        mBadgeListener.onUpdateBadgeNumber(false)
                    } else {
                        Toast.makeText(context, "Remove Failed ${mMovie.id}", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    if (mDatabaseOpenHelper.addMovie(mMovie) > -1) {
                        mMovie.isFavorite = true
                        mFavouriteBtn.setImageResource(R.drawable.ic_star_black_24)
                        mDetailListener.onUpdateFromDetail(mMovie, true)
                        mBadgeListener.onUpdateBadgeNumber(true)
                    } else {
                        Toast.makeText(context, "Add Failed ${mMovie.id}", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }
    }

    fun updateMovie(movieId: Int) {
        if (movieId == mMovie.id) {
            mMovie.isFavorite = false
            mFavouriteBtn.setImageResource(R.drawable.ic_star_outline_24)
        }
    }

    private fun getCastAndCrewFromApi() {
        val retrofit: ApiInterface =
            RetrofitClient().getRetrofitInstance().create(ApiInterface::class.java)
        val retrofitData = retrofit.getCastAndCrew(mMovie.id, APIConstant.API_KEY)

        retrofitData.enqueue(object : Callback<CastCrewList?> {
            override fun onResponse(
                call: Call<CastCrewList?>?,
                response: Response<CastCrewList?>?
            ) {
                val responseBody = response!!.body()
                mCastAndCrewList.addAll(responseBody!!.castList)
                mCastAndCrewList.addAll(responseBody.crewList)
                mCastAndCrewAdapter.notifyDataSetChanged()
            }

            override fun onFailure(call: Call<CastCrewList?>?, t: Throwable?) {
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createReminder() {
        val currentDateTime = Calendar.getInstance()
        val startYear = currentDateTime.get(Calendar.YEAR)
        val startMonth = currentDateTime.get(Calendar.MONTH)
        val startDay = currentDateTime.get(Calendar.DAY_OF_MONTH)
        val startHour = currentDateTime.get(Calendar.HOUR_OF_DAY)
        val startMinute = currentDateTime.get(Calendar.MINUTE)

        DatePickerDialog(requireContext(), { _, year, month, day ->
            TimePickerDialog(requireContext(), { _, hour, minute ->
                val pickedDateTime = Calendar.getInstance()
                pickedDateTime.set(year, month, day, hour, minute)
                currentDateTime.set(year, month, day, hour, minute)
                val reminderTimeInMillis = currentDateTime.timeInMillis
                mMovie.reminderTime = reminderTimeInMillis.toString()

                val monthDisplay = month + 1
                val reminderTimeDisplay = "$year/$monthDisplay/$day-$hour:$minute"
                mReminderTimeText.visibility = View.VISIBLE
                mReminderTimeText.text = reminderTimeDisplay
                mMovie.reminderTimeDisplay = reminderTimeDisplay

                if (mReminderExisted) {
                    if (mDatabaseOpenHelper.updateReminder(mMovie) > 0) {
                        NotificationUtil().cancelNotification(mMovie.id, requireContext())
                        NotificationUtil().createNotification(
                            mMovie,
                            reminderTimeInMillis,
                            requireContext()
                        )
                        mReminderListener.onLoadReminder()
                    } else {
                        Toast.makeText(context, "Update reminder fail", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    if (mDatabaseOpenHelper.addReminder(mMovie) > 0) {
                        mReminderExisted = true
                        mReminderListener.onLoadReminder()
                        NotificationUtil().createNotification(
                            mMovie,
                            reminderTimeInMillis,
                            requireContext()
                        )
                    } else {
                        Toast.makeText(context, "Add reminder fail", Toast.LENGTH_SHORT).show()
                    }
                }
            }, startHour, startMinute, true).show()
        }, startYear, startMonth, startDay).show()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    /**
     * Delete Reminder when notification pushed
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDeleteReminderEvent(reminderEvent: ReminderEvent) {
        if (reminderEvent.mMovieId == mMovie.id) {
            mReminderExisted = false
            mReminderTimeText.visibility = View.GONE
        }
    }
}