package com.example.mockproject.model

data class Account(
    val accountId: String,
    val userName: String,
    val email: String,
    val birthdayDate: String,
    val gender: String,
    var status: String
)
