package com.example.mockproject.listenercallback

import android.graphics.Bitmap

interface ProfileListener {
    fun onSaveProfile(
        name: String,
        email: String,
        birthday: String,
        isMale: Boolean,
        avatarBitmap: Bitmap?
    )
}