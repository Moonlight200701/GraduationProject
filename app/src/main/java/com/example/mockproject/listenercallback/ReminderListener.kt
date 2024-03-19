package com.example.mockproject.listenercallback

import com.example.mockproject.model.Movie

interface ReminderListener {
    fun onLoadReminder()
    fun onReminderGoToMovieDetail(movie: Movie)
}