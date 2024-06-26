package com.example.mockproject.api

import com.example.mockproject.model.CastCrewList
import com.example.mockproject.model.MovieList
import com.example.mockproject.model.MovieTrailer
import com.example.mockproject.model.MovieTrailerList
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiInterface {
    @GET("3/movie/{movieCategory}")
    fun getMovieList(
        @Path("movieCategory") movieCategory: String,
        @Query("api_key") apiKey: String,
        @Query("page") pageNumber: String
    ): Call<MovieList>

    @GET("3/movie/{movieId}/credits")
    fun getCastAndCrew(
        @Path("movieId") id: Int,
        @Query("api_key") apiKey: String
    ): Call<CastCrewList>

    //For searching the movie
    @GET("3/search/movie")
    fun searchMovie(
        @Query("query") query: String,
        @Query("api_key") apiKey: String,
//        @Query("include_adult") includeAdult: Boolean,
//        @Query("language") language: String,
        @Query("page") page: Int,
        @Query("year") year: String
    ): Call<MovieList>

    //to specify getting the data list for recommendation
    @GET("3/movie/{movieCategory}")
    fun getMovieListRecommendation(
        @Path("movieCategory") movieCategory: String,
        @Query("api_key") apiKey: String,
        @Query("page") pageNumber: String
    ): Call<MovieList>

    @GET("3/movie/{movieId}/videos")
    fun getMovieTrailer(
        @Path("movieId") id: Int,
        @Query("api_key") apiKey: String
    ): Call<MovieTrailerList>
}