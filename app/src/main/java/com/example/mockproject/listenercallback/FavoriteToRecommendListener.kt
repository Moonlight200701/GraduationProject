package com.example.mockproject.listenercallback

import com.example.mockproject.model.Movie

interface FavoriteToRecommendListener {
    fun fromFavoriteToRecommendation(movieList: ArrayList<Movie>) //This is for passing the data between fragments in ViewPager
}