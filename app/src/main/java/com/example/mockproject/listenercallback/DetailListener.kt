package com.example.mockproject.listenercallback

import com.example.mockproject.model.Movie

interface DetailListener {
    fun onUpdateFromDetail(movie: Movie, isFavourite:Boolean)
}