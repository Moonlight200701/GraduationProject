package com.example.mockproject.listenercallback

import com.example.mockproject.model.Movie

interface MovieListener {
    fun onUpdateFromMovie(movie: Movie, isFavourite:Boolean)
}