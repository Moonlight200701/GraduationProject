package com.example.mockproject.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class MovieDataRecommend(
    @SerializedName("id") var id: Int,
    @SerializedName("title") var title: String,
    @SerializedName("overview") var overview: String,
    @SerializedName("vote_average") var voteAverage: Double,
    @SerializedName("release_date") var releaseDate: String,
    @SerializedName("poster_path") var posterPath: String,
    @SerializedName("adult") var adult: Boolean,
    @SerializedName("genre_ids") var genresId: List<String>
) : Serializable

