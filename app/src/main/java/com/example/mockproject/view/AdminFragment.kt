package com.example.mockproject.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.mockproject.R
import com.google.firebase.firestore.FirebaseFirestore

class AdminFragment : Fragment() {
    private lateinit var fStore: FirebaseFirestore
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_admin, container, false)
        fStore = FirebaseFirestore.getInstance()
        return rootView
    }

}