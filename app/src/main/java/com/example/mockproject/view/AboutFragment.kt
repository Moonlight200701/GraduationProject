package com.example.mockproject.view

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mockproject.R
import com.example.mockproject.adapters.RecommendationAdapter
import com.example.mockproject.api.ApiInterface
import com.example.mockproject.api.RetrofitClient
import com.example.mockproject.constant.APIConstant
import com.example.mockproject.constant.Constant
import com.example.mockproject.database.DatabaseOpenHelper
import com.example.mockproject.listenercallback.BadgeListener
import com.example.mockproject.listenercallback.DetailListener
import com.example.mockproject.listenercallback.MovieListener
import com.example.mockproject.listenercallback.OnDataLoaded
import com.example.mockproject.listenercallback.ReminderListener
import com.example.mockproject.listenercallback.ToolbarTitleListener
import com.example.mockproject.model.CastAndCrew
import com.example.mockproject.model.CastCrewList
import com.example.mockproject.model.Movie
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.concurrent.CountDownLatch
import kotlin.math.abs

//This fragment is for recommending movies
class AboutFragment(private var mDatabaseOpenHelper: DatabaseOpenHelper) : Fragment(),
    View.OnClickListener, OnDataLoaded {
    //The movie from the API
    private var mMovieListFromApi = ArrayList<Movie>()

    //The movie from the Favorite list
    private var mMovieFavorite = ArrayList<Movie>()

    //List of movies after jaccard
    private var orderedMovieList = ArrayList<Movie>()

    //The data inside the onResponse when using retrofit
    private val mMovieDataForRecommend = mutableMapOf<Int, MutableMap<String, Any>>()
    private val mMovieDataInFavoriteList = mutableMapOf<Int, MutableMap<String, Any>>()

    private lateinit var mRecommendationList: RecyclerView
    private lateinit var mMovieRecommendationAdapter: RecommendationAdapter
    private lateinit var mLoadingIcon: ProgressBar

    private lateinit var mToolbarTitleListener: ToolbarTitleListener
    private lateinit var mBadgeListener: BadgeListener
    private lateinit var mMovieListener: MovieListener
    private lateinit var mDetailListener: DetailListener
    private lateinit var mReminderListener: ReminderListener

    private var currentUserId = FirebaseAuth.getInstance().currentUser!!.uid
    private var fStore = FirebaseFirestore.getInstance().collection("Users").document(currentUserId)
    private var role: String = ""

    private lateinit var mHandler: Handler

    //Purpose of the listeners: Update
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

    fun setMovieListener(movieListener: MovieListener) {
        this.mMovieListener = movieListener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_about, container, false)
        setHasOptionsMenu(true)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mRecommendationList = view.findViewById(R.id.list_recyclerview_recommend)
//        val mMovieFavoriteData = arguments?.getSerializable("My favorite list") as? ArrayList<*>
//        Log.d("Movie From Favorite", mMovieFavorite.toString())

        fStore.get().addOnSuccessListener {
            role = it.getString("isAdmin")!!
        }.addOnFailureListener {
            Log.d("isAdmin exception", "No such role in this document")
        }

        //Initialize the recommendationList
        mRecommendationList.layoutManager = LinearLayoutManager(context)

        mMovieRecommendationAdapter = RecommendationAdapter(
            mutableListOf(),
            this
        )

        mLoadingIcon = view.findViewById(R.id.recommendation_loading)
        mHandler = Handler(Looper.getMainLooper())

        //Get the dataSet
        getSomeMovieForRecommendation()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val item = menu.findItem(R.id.change_view)
        val item2 = menu.findItem(R.id.action_search)
        item.isVisible = false
        item2.isVisible = false
    }

    private fun getSomeMovieForRecommendation() {
        val retrofit: ApiInterface =
            RetrofitClient().getRetrofitInstance().create(ApiInterface::class.java)

        // Create a new coroutine to run the network requests
        CoroutineScope(Dispatchers.IO).launch {
            for (page in 1..6) {
                try {
                    val response = retrofit.getMovieListRecommendation(
                        "popular",
                        APIConstant.API_KEY,
                        page.toString()
                    ).execute()
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        val movieGotFromAPI = responseBody?.results as ArrayList<Movie>
                        withContext(Dispatchers.Main) {
                            mMovieListFromApi.addAll(movieGotFromAPI)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Connection Error", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            Log.d("How many movies?", mMovieListFromApi.size.toString())

            // Now that all movies have been fetched, get the cast and crew
            getCastAndCrewOfThoseMovies()
        }
    }

    private fun getCastAndCrewOfThoseMovies() {
        val retrofit: ApiInterface =
            RetrofitClient().getRetrofitInstance().create(ApiInterface::class.java)
        val gson: Gson =
            GsonBuilder().setPrettyPrinting().create() //for writing to json file purposes

        // Create a CountDownLatch with the size of mMovieListFromApi - the purpose? To prevent the application to process with unfinished data
        val latch = CountDownLatch(mMovieListFromApi.size)
        // Use a coroutine to make the network requests
        CoroutineScope(Dispatchers.IO).launch {
            for (mMovie in mMovieListFromApi) {
                val retrofitData = retrofit.getCastAndCrew(mMovie.id, APIConstant.API_KEY)

                retrofitData.enqueue(object : Callback<CastCrewList> {
                    override fun onResponse(
                        call: Call<CastCrewList?>, response: Response<CastCrewList?>
                    ) {
                        if (response.isSuccessful) {
                            val responseBody = response.body()
                            val castCrewList = mutableListOf<CastAndCrew>()
                            responseBody?.castList?.let { castCrewList.addAll(it) }
                            responseBody?.crewList?.let { castCrewList.addAll(it) }

                            val actors = mutableListOf<String>()
                            for (cast in castCrewList) {
                                actors.add(cast.name!!)
                            }

                            val movieData = mutableMapOf<String, Any>()
                            movieData[Constant.ACTOR_KEY] = actors
                            movieData[Constant.GENRE_ID_KEY] = mMovie.genreIds
                            movieData[Constant.VOTE_AVERAGE_KEY] = mMovie.voteAverage

                            mMovieDataForRecommend[mMovie.id] = movieData
                        }

                        // Decrement the latch count
                        latch.countDown()
                    }

                    override fun onFailure(call: Call<CastCrewList>, t: Throwable) {
                        Log.d("Retrofit error", "Network error")

                        // Decrement the latch count even if there's an error
                        latch.countDown()
                    }
                })
            }

            // Wait for all requests to complete
            latch.await()

            // Switch to the Main thread to update the UI
            withContext(Dispatchers.Main) {
                getCastAndCrewForTheMovieFavorites()

                //For observing purposes, doesn't have any purpose with the recommendation
                val jsonString: String = gson.toJson(mMovieDataForRecommend)
                writeJsonToFile(jsonString)

                Log.d("Cast and crew map", mMovieDataForRecommend.toString())
            }
        }
    }


    private fun getCastAndCrewForTheMovieFavorites() {
        val retrofit: ApiInterface =
            RetrofitClient().getRetrofitInstance().create(ApiInterface::class.java)

        // Create a CountDownLatch with the size of mMovieFavorite
        val latch = CountDownLatch(mMovieFavorite.size)

        // Use a coroutine to make the network requests
        CoroutineScope(Dispatchers.IO).launch {
            for (mMovie in mMovieFavorite) {
                val retrofitData = retrofit.getCastAndCrew(mMovie.id, APIConstant.API_KEY)

                retrofitData.enqueue(object : Callback<CastCrewList> {
                    override fun onResponse(
                        call: Call<CastCrewList?>, response: Response<CastCrewList?>
                    ) {
                        if (response.isSuccessful) {
                            val responseBody = response.body()
                            val castCrewList = mutableListOf<CastAndCrew>()
                            responseBody?.castList?.let { castCrewList.addAll(it) }
                            responseBody?.crewList?.let { castCrewList.addAll(it) }

                            val actors = mutableListOf<String>()
                            for (cast in castCrewList) {
                                actors.add(cast.name!!)
                            }
                            val movieData = mutableMapOf<String, Any>()
                            movieData[Constant.ACTOR_KEY] = actors
                            movieData[Constant.GENRE_ID_KEY] = mMovie.genreIds
                            movieData[Constant.VOTE_AVERAGE_KEY] = mMovie.voteAverage
                            mMovieDataInFavoriteList[mMovie.id] = movieData
                        }
                        // Decrement the latch count
                        latch.countDown()
                    }

                    override fun onFailure(call: Call<CastCrewList>, t: Throwable) {
                        Log.d("Retrofit error", "Network error")
                        // Decrement the latch count even if there's an error
                        latch.countDown()
                    }
                })
            }

            // Wait for all requests to complete
            latch.await()

            //Back to main thread after the background thread has finished fetching the data
            withContext(Dispatchers.Main) {
                onMovieLoaded(mMovieDataForRecommend, mMovieDataInFavoriteList)
            }
        }
    }


    //For observing, don't have any value with the recommendation
    private fun writeJsonToFile(jsonString: String) {
        val file = File(context?.filesDir, "cast_and_crew.json")

        try {
            FileWriter(file).use { writer ->
                writer.write(jsonString)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.movie_item -> {
                val position = view.tag as Int
                val movieItem = orderedMovieList[position]
                val bundle = Bundle()
                bundle.putSerializable(Constant.MOVIE_KEY, movieItem)
                bundle.putSerializable(Constant.PREVIOUS_FRAGMENT_KEY, "Suggest")
                val detailFragment = DetailFragment(mDatabaseOpenHelper)
                detailFragment.setToolbarTitleListener(mToolbarTitleListener)
                detailFragment.setBadgeListener(mBadgeListener)
                detailFragment.setDetailListener(mDetailListener)
                detailFragment.setRemindListener(mReminderListener)
                detailFragment.arguments = bundle
                requireActivity().supportFragmentManager.beginTransaction().apply {
                    setCustomAnimations(R.anim.nav_default_enter_anim,R.anim.nav_default_exit_anim)
                    replace(
                        R.id.recommendation_fragment,
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

    //Calculating based on Jaccard Similarity
    private fun loadMovieRecommendationList(movieList: MutableList<Movie>) {
        mMovieRecommendationAdapter = RecommendationAdapter(movieList, this)
        mRecommendationList.adapter = mMovieRecommendationAdapter
    }

    private fun getSimilarMovies(movieToRecommendList: MutableMap<Int, MutableMap<String, Any>>, movieFavoriteList: MutableMap<Int, MutableMap<String, Any>>, numSimilarMovies: Int): List<Int> {
        val similarityScore = mutableMapOf<Int, Double>()
        for ((movieId, movieData) in movieToRecommendList) {
            val genreIds = movieData[Constant.GENRE_ID_KEY] as List<*>
            val actors = movieData[Constant.ACTOR_KEY] as List<*>
            val rating = movieData[Constant.VOTE_AVERAGE_KEY] as Double

            //If there is the same movie in fav movie list as in the sample set, skip it
            if (movieId in movieFavoriteList) {
                continue
            }

            var totalSimilarity = 0.0
            var numSimilarities = 0

            for ((_, favMovieData) in movieFavoriteList) {
                val favGenreIds = favMovieData[Constant.GENRE_ID_KEY] as List<*>
                val favActors = favMovieData[Constant.ACTOR_KEY] as List<*>
                val favRating = favMovieData[Constant.VOTE_AVERAGE_KEY] as Double

                //Calculating Jaccard Similarity:
                val genreIdIntersect = genreIds.intersect(favGenreIds.toSet()).size.toDouble()
                val genreIdUnion = genreIds.union(favGenreIds).size.toDouble()
                val actorIntersect = actors.intersect(favActors.toSet()).size.toDouble()
                val actorUnion = actors.union(favActors).size.toDouble()
                val ratingSimilarity = 1 - abs(rating - favRating) / 10.0
                val jaccardSimilarity = ((genreIdIntersect / genreIdUnion) + (actorIntersect / actorUnion)) + ratingSimilarity / 3.0

                totalSimilarity += jaccardSimilarity
                numSimilarities++
            }

            val averageSimilarity = if (numSimilarities > 0) totalSimilarity / numSimilarities else 0.0
            similarityScore[movieId] = averageSimilarity
            Log.d("Similarity Scores", similarityScore.toString())
        }

        val sortedSimilarityScore = similarityScore.toList().sortedByDescending { (_, score) -> score }.toMap()
        val topSimilarMoviesIds = sortedSimilarityScore.keys.take(numSimilarMovies)

        getMoviesDetailOnId(topSimilarMoviesIds)
        return topSimilarMoviesIds
    }


    //For observing purpose
    fun displayReceivedMovieFavoriteList(movieList: ArrayList<Movie>) {
        mMovieFavorite.addAll(movieList)
        Log.d("Movie from favorite", mMovieFavorite.toString())
    }

    //Beginning of the jaccard similarity
    override fun onMovieLoaded(
        movieList: MutableMap<Int, MutableMap<String, Any>>,
        movieFavoriteList: MutableMap<Int, MutableMap<String, Any>>
    ) {
        if (role == "1") {
            // If the user is an admin, directly load all movies from mMovieListFromApi
            orderedMovieList.addAll(mMovieListFromApi)
            loadMovieRecommendationList(orderedMovieList)
            mLoadingIcon.visibility = View.GONE
        } else {
            // If the user is not an admin, proceed with the similarity calculation
            if (movieList.isNotEmpty() && movieFavoriteList.isNotEmpty()) {
                mLoadingIcon.visibility = View.VISIBLE
                mRecommendationList.visibility = View.INVISIBLE
                // Perform similarity calculation
                mHandler.postDelayed({
                    getSimilarMovies(movieList, movieFavoriteList, 5)
                },3000)


            } else {
                // Log a message or handle the case where data is not fully loaded
                Log.d("AboutFragment", "Movie data is not fully loaded yet")
            }
        }
    }


    //Display the list of movies after jaccard, after jaccard, only ids present
    private fun getMoviesDetailOnId(movieIdList: List<Int>) {
        for (movieId in movieIdList) {
            val movie = mMovieListFromApi.find { it.id == movieId }
            if (movie != null) {
                orderedMovieList.add(movie)
                loadMovieRecommendationList(orderedMovieList)
            }
            Log.d("movie From api after sorting", orderedMovieList.toString())
        }
        mLoadingIcon.visibility = View.GONE
        mRecommendationList.visibility = View.VISIBLE
    }


}




