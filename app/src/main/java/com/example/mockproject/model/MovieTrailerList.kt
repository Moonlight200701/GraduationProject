package com.example.mockproject.model

import com.google.gson.annotations.SerializedName

data class MovieTrailerList(
    @SerializedName("id") var id: String,
    @SerializedName("results") var results: List<MovieTrailer>
)
