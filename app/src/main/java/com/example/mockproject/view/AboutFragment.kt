package com.example.mockproject.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mockproject.R
import com.example.mockproject.adapters.RecommendationAdapter
import com.example.mockproject.api.ApiInterface
import com.example.mockproject.api.RetrofitClient
import com.example.mockproject.constant.APIConstant
import com.example.mockproject.listenercallback.OnDataLoaded
import com.example.mockproject.model.CastAndCrew
import com.example.mockproject.model.CastCrewList
import com.example.mockproject.model.Movie
import com.example.mockproject.model.MovieDataRecommend
import com.example.mockproject.model.MovieDataRecommendList
import com.example.mockproject.model.MovieList
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileWriter
import java.io.IOException

//This fragment is for recommending movies
class AboutFragment : Fragment(), View.OnClickListener, OnDataLoaded {
    private var mMovieListFromApi = ArrayList<MovieDataRecommend>()
    private var mMovieToRecommend = ArrayList<MovieDataRecommend>()

    private var mMovieRecommendationList = ArrayList<Movie>()

    private val movieCastAndCrewMap = mutableMapOf<Int, MutableMap<String,Any>>()
    private var mMovieFavorite = ArrayList<Movie>()
    private lateinit var mRecommendationList: RecyclerView
    private lateinit var mMovieRecommendationAdapter: RecommendationAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_about, container, false)
        setHasOptionsMenu(true)
        mRecommendationList = rootView.findViewById(R.id.list_recyclerview_recommend)
//        val mMovieFavoriteData = arguments?.getSerializable("My favorite list") as? ArrayList<*>
        Log.d("Movie From Favorite", mMovieFavorite.toString())
        getSomeMovieForRecommendation()
        mRecommendationList.layoutManager = LinearLayoutManager(context)
        mMovieRecommendationAdapter = RecommendationAdapter(mMovieListFromApi, this)
        mRecommendationList.adapter = mMovieRecommendationAdapter
        return rootView
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
        val retrofitData =
            retrofit.getMovieListRecommendation("popular", APIConstant.API_KEY, "3")

        retrofitData.enqueue(object : Callback<MovieDataRecommendList> {
            override fun onResponse(call: Call<MovieDataRecommendList>, response: Response<MovieDataRecommendList>) {
                val responseBody = response.body()
                val movieGotFromAPI = responseBody?.results as ArrayList<MovieDataRecommend>
                mMovieListFromApi.addAll(movieGotFromAPI)
                Log.d("How many movies? ", mMovieListFromApi.toString())
                // Notify adapter once data is fully loaded
                onMovieLoaded(mMovieListFromApi)
                mMovieRecommendationAdapter.notifyDataSetChanged()
            }

            override fun onFailure(call: Call<MovieDataRecommendList>, t: Throwable) {
                Toast.makeText(context, "Connection Error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getCastAndCrewOfThoseMovies() {
        val retrofit: ApiInterface =
            RetrofitClient().getRetrofitInstance().create(ApiInterface::class.java)
        val gson: Gson = GsonBuilder().setPrettyPrinting().create()
        for (mMovie in mMovieToRecommend) {
            val retrofitData = retrofit.getCastAndCrew(mMovie.id, APIConstant.API_KEY)

            retrofitData.enqueue(object : Callback<CastCrewList?> {
                override fun onResponse(
                    call: Call<CastCrewList?>?, response: Response<CastCrewList?>?
                ) {
                    val responseBody = response!!.body()
                    val castCrewList = mutableListOf<CastAndCrew>()
                    responseBody?.castList?.let { castCrewList.addAll(it) }
                    responseBody?.crewList?.let { castCrewList.addAll(it) }

                    val actors = mutableListOf<String>()
                    for (cast in castCrewList) {
                        actors.add(cast.name!!)
                    }

                    val movieData = mutableMapOf<String, Any>()
                    movieData["actors"] = actors
                    movieData["genres_ids"] = mMovie.genresId
                    movieData["vote_average"] = mMovie.voteAverage

                    movieCastAndCrewMap[mMovie.id] = movieData

                    val jsonString: String = gson.toJson(movieCastAndCrewMap)

                    // Write the JSON string to a file
                    writeJsonToFile(jsonString)

                    Log.d("Cast and crew map", movieCastAndCrewMap.toString())
                }

                override fun onFailure(call: Call<CastCrewList?>?, t: Throwable?) {
                    Toast.makeText(context, "Connection Error", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }


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

    override fun onClick(v: View?) {
        Toast.makeText(context, "You clicked me", Toast.LENGTH_SHORT).show()
    }

    //Calculating based on Jaccard Similarity
    private fun calculateActorSimilarity(actor1: List<String>, actor2: List<String>): Double {

        return 0.0
    }

    private fun calculateGenreSimilarity(genreId1: List<Int>, genreId2: List<Int>): Double {
        return 0.0
    }

    private fun getSimilarMovies(
        inputMovie: Movie,
        movies: List<Movie>,
        numSimilarMovies: Int
    ): List<Movie> {
        TODO("Not Yet Implemented")
    }

    fun displayReceivedMovieFavoriteList(movieList: ArrayList<Movie>) {
        mMovieFavorite.addAll(movieList)
//        Log.e("Movie From Favorite", "Received: $mMovieFavorite")
    }

    override fun onMovieLoaded(movieList: ArrayList<MovieDataRecommend>) {
        mMovieToRecommend.addAll(movieList)
        getCastAndCrewOfThoseMovies()
    }


    override fun onCastAndCrewLoaded(movieId: Int, cast: List<String>, crew: List<String>) {

    }

}
