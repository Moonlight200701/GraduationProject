package com.example.mockproject.model

import com.google.gson.annotations.SerializedName

data class MovieTrailer(
    @SerializedName("key") var key: String,
    @SerializedName("type") var type: String
)
