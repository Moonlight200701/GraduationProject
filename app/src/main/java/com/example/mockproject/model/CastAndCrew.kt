package com.example.mockproject.model

import com.google.gson.annotations.SerializedName

data class CastAndCrew(
    @SerializedName("name")
    var name: String? = null,
    @SerializedName("profile_path")
    var profilePath: String? = null
)