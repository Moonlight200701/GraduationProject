package com.example.mockproject.listenercallback

import com.example.mockproject.model.Movie
import com.example.mockproject.model.MovieDataRecommend

interface OnDataLoaded {
    fun onMovieLoaded(movieList: ArrayList<MovieDataRecommend>)
}