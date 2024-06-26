package com.example.mockproject.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

//The reason don't need to tell the other: cuz they all have initial value
data class Movie(
    @SerializedName("id") var id: Int,
    @SerializedName("title") var title: String,
    @SerializedName("overview") var overview: String,
    @SerializedName("vote_average") var voteAverage: Double,
    @SerializedName("release_date") var releaseDate: String,
    @SerializedName("poster_path") var posterPath: String,
    @SerializedName("adult") var adult: Boolean,
    @SerializedName("genre_ids") var genreIds: List<String>, //added this line
    var isFavorite: Boolean = false,
    var reminderTime: String = "",
    var reminderTimeDisplay: String = "",
    var userId: String? = null,
    var location: String = ""
) : Serializable