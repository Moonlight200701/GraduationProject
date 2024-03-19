package com.example.mockproject.listenercallback

import com.example.mockproject.model.Movie

interface FavouriteListener {
    fun onUpdateFromFavorite(movie: Movie)
}