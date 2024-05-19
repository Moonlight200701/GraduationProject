package com.example.mockproject.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.example.mockproject.R
import com.google.firebase.auth.FirebaseAuth

class ChangePasswordFragment : Fragment() {
    //    private lateinit var mToolbarTitleListener: ToolbarTitleListener

    //UI
    private lateinit var mOldPasswordET: EditText
    private lateinit var mNewPasswordET: EditText
    private lateinit var mConfirmNewPasswordEt: EditText
    private lateinit var mChangePwdButton: Button
    private var user = FirebaseAuth.getInstance().currentUser


    //    fun setToolbarTitleListener(toolbarTitleListener: ToolbarTitleListener) {
//        this.mToolbarTitleListener = toolbarTitleListener
//    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_change_password, container, false)
        mOldPasswordET = view.findViewById(R.id.frg_password_oldPwd)
        mNewPasswordET = view.findViewById(R.id.frg_password_newPwd)
        mConfirmNewPasswordEt = view.findViewById(R.id.frg_password_confirmNewPwd)
        mChangePwdButton = view.findViewById(R.id.frg_password_change_btn)


        return view

    }

}