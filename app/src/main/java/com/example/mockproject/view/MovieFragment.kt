package com.example.mockproject.view

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.mockproject.R
import com.example.mockproject.adapters.MovieAdapter
import com.example.mockproject.api.ApiInterface
import com.example.mockproject.api.RetrofitClient
import com.example.mockproject.constant.APIConstant
import com.example.mockproject.constant.Constant
import com.example.mockproject.database.DatabaseOpenHelper
import com.example.mockproject.listenercallback.BadgeListener
import com.example.mockproject.listenercallback.DetailListener
import com.example.mockproject.listenercallback.MovieListener
import com.example.mockproject.listenercallback.ReminderListener
import com.example.mockproject.listenercallback.ToolbarTitleListener
import com.example.mockproject.model.Movie
import com.example.mockproject.model.MovieList
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MovieFragment(
    private var mDatabaseOpenHelper: DatabaseOpenHelper,
) : Fragment(), View.OnClickListener {
    private var mViewType: Int = MovieAdapter.TYPE_LIST
    private lateinit var mMovieRecyclerView: RecyclerView
    private lateinit var mMovieAdapter: MovieAdapter
    private lateinit var mLinearLayoutManager: LinearLayoutManager
    private lateinit var mGridLayoutManager: GridLayoutManager
    private lateinit var mProgressBarLayout: RelativeLayout
    private lateinit var mSwipeRefreshLayout: SwipeRefreshLayout

    private lateinit var mMovieListDB: ArrayList<Movie>
    private lateinit var mMovieList: ArrayList<Movie>
    private lateinit var mHandler: Handler

    private lateinit var mSharedPreferences: SharedPreferences
    private lateinit var mCategoryPref: String
    private var mRatePref: Int = 0
    private lateinit var mReleaseYearPref: String
    private lateinit var mSortByPref: String
    private var mCurrentPage: Int = 0

    private lateinit var mToolbarTitleListener: ToolbarTitleListener
    private lateinit var mBadgeListener: BadgeListener
    private lateinit var mMovieListener: MovieListener
    private lateinit var mDetailListener: DetailListener
    private lateinit var mReminderListener: ReminderListener

    //Firebase
    private var fAuth = FirebaseAuth.getInstance()
    private var fStore = FirebaseFirestore.getInstance()
    private val user: FirebaseUser? = fAuth.currentUser
    private var isAdmin: String = ""
    private val df = fStore.collection("Users").document(user!!.uid).collection("Black List")

    //Search
    private var searchQuery: String? = null

    fun setToolbarTitleListener(toolbarTitleListener: ToolbarTitleListener) {
        this.mToolbarTitleListener = toolbarTitleListener
    }

    fun setBadgeListener(badgeListener: BadgeListener) {
        this.mBadgeListener = badgeListener
    }

    fun setMovieListener(movieListener: MovieListener) {
        this.mMovieListener = movieListener
    }

    fun setDetailListener(detailListener: DetailListener) {
        this.mDetailListener = detailListener
    }

    fun setRemindListener(reminderListener: ReminderListener) {
        this.mReminderListener = reminderListener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView: View = inflater.inflate(R.layout.fragment_movie, container, false)
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        mSwipeRefreshLayout = rootView.findViewById(R.id.swipe_layout)
        mProgressBarLayout = rootView.findViewById(R.id.progress_bar_layout)
        mMovieRecyclerView = rootView.findViewById(R.id.movie_recycle)
        mHandler = Handler(Looper.getMainLooper())

        if (user != null) {
            val userId = user.uid
            mMovieListDB = mDatabaseOpenHelper.getListMovie(userId)
        }
        Log.d("mMovieDB", mMovieListDB.toString())
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetchAdminStatus()
    }

    override fun onClick(view: View) {
        when (view.id) {
            //If the user logged in, handle the button as usual, if the admin logged in, handle the button the admin way :>
            R.id.item_list_favourite_image_button -> {
                val position = view.tag as Int
                Log.d("MovieTag", view.tag.toString())
                val movieItem: Movie = mMovieList[position]
                Log.d("Current User", user.toString())
                Log.d("Movie Item", movieItem.toString())

                if (user != null) {
                    val userId = user.uid
                    val db = FirebaseFirestore.getInstance()
                    Log.d("isAdmin", isAdmin + "onClick")
                    //To change the favorite button's click event if the admin logged in
                    if (isAdmin == "1") {
                        Toast.makeText(context, "You clicked me", Toast.LENGTH_SHORT).show()
                        handleAdminButtonClick(position)
                    } else {
                        //Handle the favorite button as usual
                        val favoritesRef =
                            db.collection("Users").document(userId).collection("Favorites")
                        if (movieItem.isFavorite) {
                            movieItem.isFavorite = false
                            mDatabaseOpenHelper.deleteMovie(movieItem.id, userId)
                            favoritesRef.document(movieItem.id.toString()).delete()
                                .addOnSuccessListener {
                                    // Delete successful
                                    mMovieAdapter.notifyItemChanged(position)
                                    mMovieListDB.remove(movieItem)
                                    //TO update the number badge of the tab favorite
                                    mBadgeListener.onUpdateBadgeNumber(false)
                                    //Mark the movie as not favorite
                                    mMovieListener.onUpdateFromMovie(movieItem, false)
                                    Toast.makeText(
                                        context,
                                        "Remove successfully",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                }
                                .addOnFailureListener {
                                    // Handle failure
                                    Toast.makeText(
                                        context,
                                        "Remove Failed ${movieItem.id}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        } else {
                            movieItem.isFavorite = true
                            mDatabaseOpenHelper.addMovie(movieItem, userId)
                            mDatabaseOpenHelper.addMovieGenres(movieItem.id, movieItem.genreIds)
                            val favoriteData = hashMapOf(
                                "id" to movieItem.id,
                                "title" to movieItem.title,
                                "poster path" to movieItem.posterPath,
                                "overview" to movieItem.overview,
                                "vote average" to movieItem.voteAverage,
                                "release date" to movieItem.releaseDate,
                                "genre ids" to movieItem.genreIds,
                                "adult" to movieItem.adult,
                                "isFavorite" to movieItem.isFavorite

                            )
                            favoritesRef.document(movieItem.id.toString()).set(favoriteData)
                                .addOnSuccessListener {
                                    // Add successful
                                    mMovieAdapter.notifyItemChanged(position)
                                    mMovieListDB.add(movieItem)
                                    mBadgeListener.onUpdateBadgeNumber(true)
                                    mMovieListener.onUpdateFromMovie(movieItem, true)
                                }
                                .addOnFailureListener {
                                    // Handle failure
                                    Toast.makeText(
                                        context,
                                        "Add Failed ${movieItem.id}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }
                }
            }

            R.id.movie_item -> {
                val position = view.tag as Int
                val movieItem: Movie = mMovieList[position]
                val bundle = Bundle()
                bundle.putSerializable(Constant.MOVIE_KEY, movieItem)
                bundle.putSerializable(Constant.PREVIOUS_FRAGMENT_KEY, "Movie")
                val detailFragment = DetailFragment(mDatabaseOpenHelper)
                detailFragment.setToolbarTitleListener(mToolbarTitleListener)
                detailFragment.setBadgeListener(mBadgeListener)
                detailFragment.setDetailListener(mDetailListener)
                detailFragment.setRemindListener(mReminderListener)
                detailFragment.arguments = bundle
                requireActivity().supportFragmentManager.beginTransaction().apply {
                    setCustomAnimations(R.anim.nav_default_enter_anim,R.anim.nav_default_exit_anim)
                    replace(
                        R.id.movie_fragment_content,
                        detailFragment,
                        Constant.FRAGMENT_DETAIL_TAG
                    )
                    addToBackStack(null)
                    commit()
                    mToolbarTitleListener.onUpdateToolbarTitle(movieItem.title)
                }
            }
        }
    }

    private fun handleAdminButtonClick(position: Int) {
//        val movieItem: Movie = mMovieList[position]
//        val dialogBuilder2 = AlertDialog.Builder(context)
//        df.document(movieItem.id.toString()).get().addOnSuccessListener {
//            //If it exists that means the admin wants to delete it from the black list
//            if (it.exists()) {
//                dialogBuilder2.setCancelable(true)
//                dialogBuilder2.setTitle("Confirmation")
//                dialogBuilder2.setMessage("Are you sure you want to remove this movie from the black list?")
//                dialogBuilder2.setPositiveButton("Ok") { currentDialog, _ ->
//                    df.document(movieItem.id.toString()).delete().addOnSuccessListener {
//                        Toast.makeText(
//                            context,
//                            "Removed successfully, showing this to users :>",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                        currentDialog.dismiss()
//                    }.addOnFailureListener {
//                        Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
//                        currentDialog.dismiss()
//                    }
//                }
//                dialogBuilder2.setNegativeButton("Cancel") { currentDialog, _ ->
//                    currentDialog.dismiss()
//                }
//                val dialog = dialogBuilder2.create()
//                dialog.window?.attributes?.dimAmount =
//                    0.9f // Set this value between 0.0f and 1.0f to control the darkness
//                dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
//                dialog.show()
//            } else {
//                //Not exist? that means the admin is adding it to the blacklist so no user can see it
//                val blacklistItem = hashMapOf(
//                    "id" to movieItem.id,
//                    "title" to movieItem.title,
//                    "poster_path" to movieItem.posterPath,
//                    "overview" to movieItem.overview,
//                    "vote_average" to movieItem.voteAverage,
//                    "release_date" to movieItem.releaseDate,
//                    "genre_ids" to movieItem.genreIds,
//                )
//                dialogBuilder2.setCancelable(true)
//                dialogBuilder2.setTitle("Confirmation")
//                dialogBuilder2.setMessage("Are you sure you want to add this movie to the black list?")
//                dialogBuilder2.setPositiveButton("Ok") { currentDialog, _ ->
//                    df.document(movieItem.id.toString()).set(blacklistItem).addOnSuccessListener {
//                        Toast.makeText(
//                            context,
//                            "Added to the black list successfully, not showing this to users :>",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                        currentDialog.dismiss()
//                    }.addOnFailureListener {
//                        Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
//                        currentDialog.dismiss()
//                    }
//                }
//                dialogBuilder2.setNegativeButton("Cancel") { currentDialog, _ ->
//                    currentDialog.dismiss()
//                }
//                val dialog = dialogBuilder2.create()
//                dialog.window?.attributes?.dimAmount =
//                    0.9f // Set this value between 0.0f and 1.0f to control the darkness
//                dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
//                dialog.show()
//
//            }
//        }

    }

    private fun updateMovieList() {
        mCurrentPage = 1
        mMovieList = ArrayList()
        mLinearLayoutManager = LinearLayoutManager(activity)
        mGridLayoutManager = GridLayoutManager(activity, 2)

        mMovieAdapter = MovieAdapter(mMovieList, mViewType, this, false, isAdmin)
        if (mViewType == MovieAdapter.TYPE_GRID) {
            mMovieRecyclerView.layoutManager = mGridLayoutManager
        } else {
            mMovieRecyclerView.layoutManager = mLinearLayoutManager
        }
        mMovieRecyclerView.setHasFixedSize(true)
        mMovieRecyclerView.adapter = mMovieAdapter
    }

    fun setListMovieByCondition() {
        loadDataBySetting()
        updateMovieList()
        mHandler.postDelayed({
            getMovieListFromApi(isRefresh = false, isLoadMore = false, query = null)
        }, 1000)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun changeView() {
        if (mMovieRecyclerView.layoutManager == mGridLayoutManager) {
            mViewType = MovieAdapter.TYPE_LIST
            mMovieRecyclerView.layoutManager = mLinearLayoutManager
        } else {
            mViewType = MovieAdapter.TYPE_GRID
            mMovieRecyclerView.layoutManager = mGridLayoutManager
        }
        mMovieAdapter.setViewType(mViewType)
        mMovieRecyclerView.adapter = mMovieAdapter
        mMovieAdapter.notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateMovieList(movie: Movie, isFavourite: Boolean) {
        mMovieList.forEach {
            if (it.id == movie.id) {
                it.isFavorite = isFavourite
            }
        }
        mMovieAdapter.notifyDataSetChanged()
    }

    //If the user want to see more movies and this is the end of the page
    private fun onLoadMoreListener() {
        mMovieRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (isLastItemDisplaying(recyclerView)) {
                    mHandler.postDelayed({
                        getMovieListFromApi(
                            isRefresh = false,
                            isLoadMore = true,
                            query = searchQuery
                        )
                    }, 1000)
                }
            }
        })
    }

    //See if this is the last item of the first page
    private fun isLastItemDisplaying(recyclerView: RecyclerView): Boolean {
        if (recyclerView.adapter!!.itemCount != 0) {
            val lastVisibleItemPosition =
                (recyclerView.layoutManager as LinearLayoutManager?)!!.findLastCompletelyVisibleItemPosition()
            if (lastVisibleItemPosition != RecyclerView.NO_POSITION && lastVisibleItemPosition == recyclerView.adapter!!
                    .itemCount - 1
            ) return true
        }
        return false
    }

    //Display movies according to the setting of the user.
    private fun loadDataBySetting() {
        mCategoryPref =
            mSharedPreferences.getString(Constant.PREF_CATEGORY_KEY, "popular").toString()
        mRatePref = mSharedPreferences.getInt(Constant.PREF_RATE_KEY, 0)
        mReleaseYearPref = mSharedPreferences.getString(Constant.PREF_RELEASE_KEY, "").toString()
        mSortByPref = mSharedPreferences.getString(Constant.PREF_SORT_KEY, "").toString()
    }

    private fun getMovieListFromApi(isRefresh: Boolean, isLoadMore: Boolean, query: String?) {
        searchQuery = query
        if (isLoadMore) {
            mCurrentPage += 1
        } else {
            if (!isRefresh) {
                mProgressBarLayout.visibility = View.VISIBLE
            }
        }

        val retrofit: ApiInterface =
            RetrofitClient().getRetrofitInstance().create(ApiInterface::class.java)

        // the API call to include the search query parameter if provided
        val retrofitData = if (!(searchQuery.isNullOrEmpty())) {
            retrofit.searchMovie(
                searchQuery!!,
                APIConstant.API_KEY,
//                includeAdult = false,
//                language = "en-US",
                mCurrentPage,
                mReleaseYearPref
            )
        } else {
            retrofit.getMovieList(mCategoryPref, APIConstant.API_KEY, "$mCurrentPage")
        }

        retrofitData.enqueue(object : Callback<MovieList?> {
            @SuppressLint("NotifyDataSetChanged")
            override fun onResponse(call: Call<MovieList?>, response: Response<MovieList?>) {
                //Remove the loading icon
                mMovieAdapter.removeItemLoading()

                //Getting all of the movie that got from the api, and add them to the MovieRecyclerView
                val responseBody = response.body()
                val movieResultList = responseBody?.results as ArrayList<Movie>
                // Clear the current list if not loading more
                if (!isLoadMore) {
                    mMovieList.clear()
                }
//                if(isAdmin == "1") {
////                    If the admin logged in, display all of the movies as usual
                    mMovieList.addAll(movieResultList)
//                } else {
//                    val adminBlackList =
//                        fStore.collection("Users").document(Constant.FIREBASE_ADMIN_ID)
//                            .collection("Black List")
//                    adminBlackList.get().addOnSuccessListener { querySnapshot ->
//                        if (querySnapshot.isEmpty) {
//                            mMovieList.addAll(movieResultList)
//                        } else {
//                            val blackListIds = querySnapshot.documents.mapNotNull { it.id.toInt() }
//                            Log.d("Black list ids", blackListIds.toString())
//                            movieResultList.removeAll { blackListIds.contains(it.id) }
//                            mMovieList.addAll(movieResultList)
//                        }
//                    }
//                }
                mMovieAdapter.setupMovieFavorite(mMovieListDB)

                //Ignore the setting when searching
                if (query.isNullOrEmpty()) {
                    mMovieAdapter.setupMovieBySetting(
                        mMovieList,
                        mRatePref,
                        mReleaseYearPref,
                        mSortByPref
                    )
                }
                // Add load more item if necessary(mostly the loading icon)
                if (mCurrentPage < responseBody.totalPages) {
                    val loadMoreItem =
                        Movie(0, "0", "0", 0.0, "0", "0", false, listOf(), false, "0", "0")
                    mMovieList.add(loadMoreItem)
                    Log.d("Movie that get by api", mMovieList.toString())
                }

                // Notify adapter of data change
                mMovieAdapter.notifyDataSetChanged()

                if (!isLoadMore && !isRefresh) {
                    mProgressBarLayout.visibility = View.GONE
                }

                if (isRefresh) {
                    mSwipeRefreshLayout.isRefreshing = false
                }
            }

            override fun onFailure(call: Call<MovieList?>, t: Throwable) {
                if (isLoadMore) {
                    mCurrentPage -= 1
                } else {
                    if (isRefresh) {
                        mSwipeRefreshLayout.isRefreshing = false
                    } else {
                        mProgressBarLayout.visibility = View.GONE
                    }
                }
            }
        })
    }


    fun updateMovies(query: String?) {
//        getMovieListFromApi(isRefresh = true, isLoadMore = false, query = null)
        if (query.isNullOrEmpty()) {
            // If the query is empty or null, show all movies
            mMovieAdapter.setupMovieBySetting(
                mMovieList,
                mRatePref,
                mReleaseYearPref,
                mSortByPref,
            )
        } else {
            mMovieList.clear()
            mProgressBarLayout.visibility = View.VISIBLE
            getMovieListFromApi(isRefresh = false, isLoadMore = false, query = query)
//            Toast.makeText(context, "Looking for your movie...", Toast.LENGTH_SHORT).show()
//            Log.d("MovieListAfterRemoving", filteredMovieList.toString())
        }
    }

    //Determine if the admin logged in, to change the icon of the favorite button
    private fun fetchAdminStatus() {
        val userId = user?.uid ?: return
        val userDocRef = fStore.collection("Users").document(userId)
        userDocRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val isAdminStatus = document.getString("isAdmin")!!
                    isAdmin = isAdminStatus
                }
                onFetchAdminStatusComplete()
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error getting document", e)
                onFetchAdminStatusComplete()
            }
    }


    //Display the movies based on the user or the admin (the favorite button)
    private fun onFetchAdminStatusComplete() {
        Log.d("isAdmin", isAdmin)
        loadDataBySetting()
        updateMovieList()
        mHandler.postDelayed({
            getMovieListFromApi(isRefresh = false, isLoadMore = false, query = null)
        }, 1000)

        mSwipeRefreshLayout.setOnRefreshListener {
            mHandler.postDelayed({
                updateMovieList()
                getMovieListFromApi(isRefresh = true, isLoadMore = false, query = null)
            }, 1000)
        }
        onLoadMoreListener()
    }
}