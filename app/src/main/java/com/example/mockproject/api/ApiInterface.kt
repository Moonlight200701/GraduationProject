package com.example.mockproject.api

import com.example.mockproject.model.CastCrewList
import com.example.mockproject.model.MovieList
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
}