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
import com.example.mockproject.listenercallback.*
import com.example.mockproject.model.Movie
import com.example.mockproject.model.MovieList
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
        mMovieListDB = mDatabaseOpenHelper.getListMovie()
        loadDataBySetting()
        updateMovieList()
        mHandler.postDelayed({
            getMovieListFromApi(false, false)
        }, 1000)

        mSwipeRefreshLayout.setOnRefreshListener {
            mHandler.postDelayed({
                updateMovieList()
                getMovieListFromApi(true, false)
            }, 1000)
        }
        onLoadMoreListener()
        return rootView
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.item_list_favourite_image_button -> {
                val position = view.tag as Int
                val movieItem: Movie = mMovieList[position]
                if (movieItem.isFavorite) {
                    if (mDatabaseOpenHelper.deleteMovie(movieItem.id) > -1) {
                        movieItem.isFavorite = false
                        mMovieAdapter.notifyItemChanged(position)
//                        mMovieListDB.remove(movieItem)
//                        mMovieViewModel.movieListDB.value = mMovieListDB
                        mBadgeListener.onUpdateBadgeNumber(false)
                        mMovieListener.onUpdateFromMovie(movieItem, false)
                    } else {
                        Toast.makeText(context, "Remove Failed ${movieItem.id}", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    if (mDatabaseOpenHelper.addMovie(movieItem) > -1) {
                        movieItem.isFavorite = true
                        mMovieAdapter.notifyItemChanged(position)
//                        mMovieListDB.add(movieItem)
//                        mMovieViewModel.movieListDB.value = mMovieListDB
                        mBadgeListener.onUpdateBadgeNumber(true)
                        mMovieListener.onUpdateFromMovie(movieItem, true)
                    } else {
                        Toast.makeText(
                            context, "Add Failed ${movieItem.id}", Toast.LENGTH_SHORT
                        ).show()
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
                    add(R.id.movie_fragment_content, detailFragment, Constant.FRAGMENT_DETAIL_TAG)
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
            getMovieListFromApi(false, false)
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

    private fun onLoadMoreListener() {
        mMovieRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (isLastItemDisplaying(recyclerView)) {
                    mHandler.postDelayed({
//                        getMovieListFromApi(false, true)
                    }, 1000)
                }
            }
        })
    }

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

    private fun findMoviePositionById(id: Int): Int {
        var position = -1
        val size = mMovieList.size
        for (index in 0 until size) {
            if (mMovieList[index].id == id) {
                position = index
                break
            }
        }
        return position
    }

    private fun loadDataBySetting() {
        mCategoryPref =
            mSharedPreferences.getString(Constant.PREF_CATEGORY_KEY, "popular").toString()
        mRatePref = mSharedPreferences.getInt(Constant.PREF_RATE_KEY, 0)
        mReleaseYearPref = mSharedPreferences.getString(Constant.PREF_RELEASE_KEY, "").toString()
        mSortByPref = mSharedPreferences.getString(Constant.PREF_SORT_KEY, "").toString()
    }

    private fun getMovieListFromApi(isRefresh: Boolean, isLoadMore: Boolean) {
        if (isLoadMore) {
            mCurrentPage += 1
        } else {
            if (!isRefresh) {
                mProgressBarLayout.visibility = View.VISIBLE
            }
        }

        val retrofit: ApiInterface =
            RetrofitClient().getRetrofitInstance().create(ApiInterface::class.java)
        val retrofitData =
            retrofit.getMovieList(mCategoryPref, APIConstant.API_KEY, "$mCurrentPage")
        retrofitData.enqueue(object : Callback<MovieList?> {
            override fun onResponse(call: Call<MovieList?>?, response: Response<MovieList?>?) {
                mMovieAdapter.removeItemLoading()
                val responseBody = response!!.body()
                val movieResultList = responseBody?.results as ArrayList<Movie>
//                mMovieViewModel.movieList.value = movieResultList
                mMovieList.addAll(movieResultList)
                mMovieAdapter.setupMovieFavorite(mMovieListDB)

                mMovieAdapter.setupMovieBySetting(
                    mMovieList,
                    mRatePref,
                    mReleaseYearPref,
                    mSortByPref,

                    )
                if (mCurrentPage < responseBody.totalPages) {
                    val loadMoreItem =
                        Movie(0, "0", "0", 0.0, "0", "0", false, false, "0", "0")
                    mMovieList.add(loadMoreItem)
                    Log.d("Movie that get by api", mMovieList.toString())
                }
                mMovieAdapter.notifyDataSetChanged()
                if (!isLoadMore && !isRefresh) {
                    mProgressBarLayout.visibility = View.GONE
                }
                if (isRefresh) {
                    mSwipeRefreshLayout.isRefreshing = false
                }
            }

            override fun onFailure(call: Call<MovieList?>?, t: Throwable?) {
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
        if (query.isNullOrEmpty() || mMovieList.none { it.title.contains(query, true) }) {
            // If the query is empty or null, show all movies
            mMovieAdapter.setupMovieBySetting(
                mMovieList,
                mRatePref,
                mReleaseYearPref,
                mSortByPref,
            )
            Toast.makeText(context, "Movies not found", Toast.LENGTH_SHORT).show()
        } else {
            mMovieList.filter { it.title.contains(query, ignoreCase = true) }
            val filteredMovieList = ArrayList<Movie>(mMovieList)
            filteredMovieList.removeAll { !it.title.equals(query, true) }
            //add all of the modified list to a new list
            mMovieAdapter.setupMovieBySetting(
                filteredMovieList,
                mRatePref,
                mReleaseYearPref,
                mSortByPref,
            )
            Toast.makeText(context, "Looking for your movie...", Toast.LENGTH_SHORT).show()
            Log.d("MovieListAfterRemoving", filteredMovieList.toString())
            mMovieAdapter.notifyDataSetChanged()
        }
    }
}