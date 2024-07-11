package com.example.mockproject.view

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mockproject.R
import com.example.mockproject.adapters.CastAndCrewAdapter
import com.example.mockproject.adapters.TrailerAdapter
import com.example.mockproject.api.ApiInterface
import com.example.mockproject.api.RetrofitClient
import com.example.mockproject.constant.APIConstant
import com.example.mockproject.constant.Constant
import com.example.mockproject.database.DatabaseOpenHelper
import com.example.mockproject.eventbus.ReminderEvent
import com.example.mockproject.listenercallback.BadgeListener
import com.example.mockproject.listenercallback.DetailListener
import com.example.mockproject.listenercallback.ReminderListener
import com.example.mockproject.listenercallback.ToolbarTitleListener
import com.example.mockproject.model.CastAndCrew
import com.example.mockproject.model.CastCrewList
import com.example.mockproject.model.Movie
import com.example.mockproject.model.MovieTrailer
import com.example.mockproject.model.MovieTrailerList
import com.example.mockproject.util.NotificationUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar
import java.util.concurrent.CountDownLatch


class DetailFragment(private var mDatabaseOpenHelper: DatabaseOpenHelper) : Fragment(),
    View.OnClickListener {
    private lateinit var mMovie: Movie
    private lateinit var mMovieReminder: Movie
    private var mReminderExisted: Boolean = false
    private lateinit var mCastAndCrewList: ArrayList<CastAndCrew>
    private lateinit var mTrailerList: ArrayList<MovieTrailer>

    private lateinit var mFavouriteBtn: ImageButton
    private lateinit var mReleaseDateText: TextView
    private lateinit var mRateText: TextView
    private lateinit var mPosterImg: ImageView
    private lateinit var mOverviewText: TextView
    private lateinit var mReminderBtn: Button
    private lateinit var mReminderTimeText: TextView
    private lateinit var mCastRecyclerView: RecyclerView
    private lateinit var mCastAndCrewAdapter: CastAndCrewAdapter
    private lateinit var mTrailerRecyclerView: RecyclerView
    private lateinit var mTrailerAdapter: TrailerAdapter

    private lateinit var mToolbarTitleListener: ToolbarTitleListener
    private lateinit var mBadgeListener: BadgeListener
    private lateinit var mDetailListener: DetailListener
    private lateinit var mReminderListener: ReminderListener

    //Firebase
    private var fAuth = FirebaseAuth.getInstance()
    private val user: FirebaseUser? = fAuth.currentUser
    private val fStore = FirebaseFirestore.getInstance()
    private val df = fStore.collection("Users").document(user!!.uid).collection("Reminder")

    private var previousFragmentName: String? = null

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_detail, container, false)
        val bundle = this.arguments
        var userId = ""
        if (user != null) {
            userId = user.uid
        }
        if (bundle != null) {
            mMovie = bundle.getSerializable(Constant.MOVIE_KEY) as Movie
            //For setting the Toolbar if navigate back to the previous one
            previousFragmentName = bundle.getSerializable(Constant.PREVIOUS_FRAGMENT_KEY) as String
            val movieReminderList = mDatabaseOpenHelper.getReminderByMovieId(mMovie.id, userId)
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
        mTrailerRecyclerView = view.findViewById(R.id.trailer_recyclerView)


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

        val layoutManager = LinearLayoutManager(context)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        mCastAndCrewList = arrayListOf()
        mCastAndCrewAdapter = CastAndCrewAdapter(mCastAndCrewList)
        mCastRecyclerView.layoutManager = layoutManager
        mCastRecyclerView.setHasFixedSize(true)
        mCastRecyclerView.adapter = mCastAndCrewAdapter

        //Trailer
        val layoutManager2 = LinearLayoutManager(context)
        layoutManager2.orientation = LinearLayoutManager.HORIZONTAL
        mTrailerList = arrayListOf()
        mTrailerAdapter = TrailerAdapter(mTrailerList)
        mTrailerRecyclerView.layoutManager = layoutManager2
        mTrailerRecyclerView.setHasFixedSize(true)
        mTrailerRecyclerView.adapter = mTrailerAdapter
        setHasOptionsMenu(true)
        return view
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        previousFragmentName = arguments?.getString(Constant.PREVIOUS_FRAGMENT_KEY)
        Log.d("Previous Fragment", previousFragmentName.toString())

        CoroutineScope(Dispatchers.IO).launch {
            getTrailerVideoFromApi()

            withContext(Dispatchers.Main) {
                mTrailerAdapter.notifyDataSetChanged()
                getCastAndCrewFromApi()
            }
        }
    }


    override fun onPrepareOptionsMenu(menu: Menu) {
        val item = menu.findItem(R.id.change_view)
        val item2 = menu.findItem(R.id.action_search)
        item.isVisible = false
        item2.isVisible = false
    }

    override fun onDetach() {
        mToolbarTitleListener.onUpdateToolbarTitle(previousFragmentName ?: "Movie")
        super.onDetach()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.favourite_img_btn -> {
                val user: FirebaseUser? = fAuth.currentUser
                if (user != null) {
                    val userId = user.uid
                    val db = FirebaseFirestore.getInstance()
                    val favoritesRef =
                        db.collection("Users").document(userId).collection("Favorites")
                    if (mMovie.isFavorite) {
                        mMovie.isFavorite = false
                        mDatabaseOpenHelper.deleteMovie(mMovie.id, userId)
                        favoritesRef.document(mMovie.id.toString()).delete()
                            .addOnSuccessListener {
                                mFavouriteBtn.setImageResource(R.drawable.ic_star_outline_24)
                                mDetailListener.onUpdateFromDetail(mMovie, false)
                                mBadgeListener.onUpdateBadgeNumber(false)
                                Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT)
                                    .show()
                            }.addOnFailureListener {
                                // Handle failure
                                Toast.makeText(
                                    context, "Remove Failed ${mMovie.id}", Toast.LENGTH_SHORT
                                ).show()
                            }
                    } else {
                        //This is for writing the data to the firestore, also create a new data in the local database
                        mMovie.isFavorite = true
                        mDatabaseOpenHelper.addMovie(mMovie, userId)
                        mDatabaseOpenHelper.addMovieGenres(mMovie.id, mMovie.genreIds)
                        val favoriteData = hashMapOf(
                            "id" to mMovie.id,
                            "title" to mMovie.title,
                            "poster path" to mMovie.posterPath,
                            "overview" to mMovie.overview,
                            "vote average" to mMovie.voteAverage,
                            "release date" to mMovie.releaseDate,
                            "genre ids" to mMovie.genreIds,
                            "adult" to mMovie.adult,
                            "isFavorite" to mMovie.isFavorite
                            // Add other movie details here as needed
                        )
                        favoritesRef.document(mMovie.id.toString()).set(favoriteData)
                            .addOnSuccessListener {
                                mFavouriteBtn.setImageResource(R.drawable.ic_star_black_24)
                                mDetailListener.onUpdateFromDetail(mMovie, true)
                                mBadgeListener.onUpdateBadgeNumber(true)
                            }.addOnFailureListener {
                                Toast.makeText(
                                    context,
                                    "Add to favorites failed",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }
            }
        }
    }

    fun updateMovie(movieId: Int) { //Remove movie from favorite in the detail
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
            @SuppressLint("NotifyDataSetChanged")
            override fun onResponse(call: Call<CastCrewList?>, response: Response<CastCrewList?>) {
                val responseBody = response.body()
                mCastAndCrewList.addAll(responseBody!!.castList)
                mCastAndCrewList.addAll(responseBody.crewList)
                Log.d("Cast and crew list", responseBody.toString())
                mCastAndCrewAdapter.notifyDataSetChanged()
            }

            override fun onFailure(call: Call<CastCrewList?>, t: Throwable) {
            }
        })
    }

    //For the trailer of the movie
    private fun getTrailerVideoFromApi() {
        val countDownLatch = CountDownLatch(1)
        val retrofit: ApiInterface =
            RetrofitClient().getRetrofitInstance().create(ApiInterface::class.java)
        val retrofitData = retrofit.getMovieTrailer(mMovie.id, APIConstant.API_KEY)

        retrofitData.enqueue(object : Callback<MovieTrailerList> {
            override fun onResponse(
                call: Call<MovieTrailerList>,
                response: Response<MovieTrailerList>
            ) {

                val responseBody = response.body()
                Log.d("retrofit data", responseBody.toString())
                val keys = responseBody?.results
                if (keys != null) {
                    mTrailerList.addAll(keys)
                }
                countDownLatch.countDown() // Decrement the count of the latch, releasing the waiting thread when count reaches zero
            }

            override fun onFailure(call: Call<MovieTrailerList>, t: Throwable) {
                // Handle the error scenario, possibly with a retry mechanism
                countDownLatch.countDown() // Ensure the count is decremented even on failure
            }
        })

        try {
            countDownLatch.await() // Wait for the count to reach zero
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createReminder() {
        var userId = ""
        if (user != null) {
            userId = user.uid
        }
        //Set the current date time to the datepicker and timepicker
        val currentDateTime = Calendar.getInstance()
        val startYear = currentDateTime.get(Calendar.YEAR)
        val startMonth = currentDateTime.get(Calendar.MONTH)
        val startDay = currentDateTime.get(Calendar.DAY_OF_MONTH)
        val startHour = currentDateTime.get(Calendar.HOUR_OF_DAY)
        val startMinute = currentDateTime.get(Calendar.MINUTE)

        //Showing the triple dialogs
        DatePickerDialog(requireContext(), { _, year, month, day ->
            TimePickerDialog(requireContext(), { _, hour, minute ->
                val pickedDateTime = Calendar.getInstance()
                pickedDateTime.set(year, month, day, hour, minute)
                currentDateTime.set(year, month, day, hour, minute)
                val reminderTimeInMillis = currentDateTime.timeInMillis
                mMovie.reminderTime = reminderTimeInMillis.toString()
                val currentTimeMillis = Calendar.getInstance().timeInMillis
                if (reminderTimeInMillis < currentTimeMillis) {
                    Toast.makeText(
                        context,
                        "Please select a future date and time",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@TimePickerDialog
                }

                val monthDisplay = month + 1
                val reminderTimeDisplay = "$year/$monthDisplay/$day-$hour:$minute"

                //Create the Location dialog, let the user decide where do they want to watch, this can be a cinema, a site...
                val dialogBuilder = AlertDialog.Builder(context)
                val dialogView =
                    LayoutInflater.from(context).inflate(R.layout.reminder_location_dialog, null)
                val reminderEt = dialogView.findViewById<EditText>(R.id.reminder_dialog_location_et)
                dialogBuilder.setView(dialogView)
                dialogBuilder.setCancelable(true)
                dialogBuilder.setPositiveButton("OK") { _, _ ->
                    mReminderTimeText.visibility = View.VISIBLE
                    mReminderTimeText.text = reminderTimeDisplay
                    mMovie.reminderTimeDisplay = reminderTimeDisplay

                    if (reminderEt.text.trim().isNotEmpty()) {
                        if (mReminderExisted) {
                            if (mDatabaseOpenHelper.updateReminder(
                                    mMovie,
                                    userId,
                                    reminderEt.text.toString()
                                ) > 0
                            ) {
                                //Adding to the fireStore
                                val reminderItem = hashMapOf(
                                    "id" to mMovie.id,
                                    "title" to mMovie.title,
                                    "poster path" to mMovie.posterPath,
                                    "overview" to mMovie.overview,
                                    "vote average" to mMovie.voteAverage,
                                    "release date" to mMovie.releaseDate,
                                    "genre ids" to mMovie.genreIds,
                                    "reminder time" to mMovie.reminderTime,
                                    "reminder time display" to mMovie.reminderTimeDisplay,
                                    "isFavorite" to mMovie.isFavorite,
                                    "location" to reminderEt.text.toString()
                                )
                                df.document(mMovie.id.toString()).update(reminderItem)
                                    .addOnSuccessListener {

                                    }
                                NotificationUtil().cancelNotification(mMovie.id, requireContext())
                                NotificationUtil().createNotification(
                                    mMovie,
                                    reminderTimeInMillis,
                                    requireContext(),
                                    reminderEt.text.toString()
                                )
                                mReminderListener.onLoadReminder()

                            } else {
                                Toast.makeText(context, "Update reminder fail", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        } else {
                            if (mDatabaseOpenHelper.addReminder(
                                    mMovie,
                                    userId,
                                    reminderEt.text.toString()
                                ) > 0
                            ) {
                                mReminderExisted = true
                                mReminderListener.onLoadReminder()
                                val reminderItem = hashMapOf(
                                    "id" to mMovie.id,
                                    "title" to mMovie.title,
                                    "poster path" to mMovie.posterPath,
                                    "overview" to mMovie.overview,
                                    "vote average" to mMovie.voteAverage,
                                    "release date" to mMovie.releaseDate,
                                    "genre ids" to mMovie.genreIds,
                                    "adult" to mMovie.adult,
                                    "reminder time" to mMovie.reminderTime,
                                    "reminder time display" to mMovie.reminderTimeDisplay,
                                    "isFavorite" to mMovie.isFavorite,
                                    "location" to reminderEt.text.toString()
                                )
                                df.document(mMovie.id.toString()).set(reminderItem)
                                    .addOnSuccessListener {
                                        Log.d("Reminder add", "Add a reminder successfully")
                                    }
                                NotificationUtil().createNotification(
                                    mMovie,
                                    reminderTimeInMillis,
                                    requireContext(),
                                    reminderEt.text.toString()
                                )
                            } else {
                                Toast.makeText(context, "Add reminder fail", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    }
                }
                dialogBuilder.setNegativeButton("Cancel", null)
                val dialog = dialogBuilder.create()
                dialog.window?.attributes?.dimAmount =
                    0.9f // Set this value between 0.0f and 1.0f to control the darkness
                dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                dialogBuilder.show()
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