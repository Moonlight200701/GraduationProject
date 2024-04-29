package com.example.mockproject.listenercallback

interface OnDataLoaded {
    fun onMovieLoaded(movieList: MutableMap<Int, MutableMap<String, Any>>, movieFavoriteList:  MutableMap<Int, MutableMap<String, Any>>)
}