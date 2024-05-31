package com.example.mockproject.view

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    private val user: FirebaseUser? = fAuth.currentUser

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

        loadDataBySetting()
        updateMovieList()
        mHandler.postDelayed({
            getMovieListFromApi(false, false, null)
        }, 1000)

        mSwipeRefreshLayout.setOnRefreshListener {
            mHandler.postDelayed({
                updateMovieList()
                getMovieListFromApi(true, false, null)
            }, 1000)
        }
        onLoadMoreListener()
        return rootView
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.item_list_favourite_image_button -> {
                val position = view.tag as Int
                Log.d("MovieTag", view.tag.toString())
                val movieItem: Movie = mMovieList[position]
                Log.d("Current User", user.toString())
                Log.d("Movie Item", movieItem.toString())

                if (user != null) {
                    val userId = user.uid
                    val db = FirebaseFirestore.getInstance()
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
                                mBadgeListener.onUpdateBadgeNumber(false)
                                mMovieListener.onUpdateFromMovie(movieItem, false)
                                Toast.makeText(context, "Remove successfully", Toast.LENGTH_SHORT)
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

            R.id.movie_item -> {
                val position = view.tag as Int
                val movieItem: Movie = mMovieList[position]
                val bundle = Bundle()
                bundle.putSerializable(Constant.MOVIE_KEY, movieItem)
                val detailFragment = DetailFragment(mDatabaseOpenHelper)
                detailFragment.setToolbarTitleListener(mToolbarTitleListener)
                detailFragment.setBadgeListener(mBadgeListener)
                detailFragment.setDetailListener(mDetailListener)
                detailFragment.setRemindListener(mReminderListener)
                detailFragment.arguments = bundle
                requireActivity().supportFragmentManager.beginTransaction().apply {
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

    private fun updateMovieList() {
        mCurrentPage = 1
        mMovieList = ArrayList()
        mLinearLayoutManager = LinearLayoutManager(activity)
        mGridLayoutManager = GridLayoutManager(activity, 2)

        mMovieAdapter = MovieAdapter(mMovieList, mViewType, this, false)
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
                        getMovieListFromApi(isRefresh = false, isLoadMore = true, query = searchQuery)
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
                mMovieList.addAll(movieResultList)
                mMovieAdapter.setupMovieFavorite(mMovieListDB)

                //Ignore the setting when searching
                if(query.isNullOrEmpty()) {
                    mMovieAdapter.setupMovieBySetting(
                        mMovieList,
                        mRatePref,
                        mReleaseYearPref,
                        mSortByPref
                    )
                }
                // Add load more item if necessary
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
}