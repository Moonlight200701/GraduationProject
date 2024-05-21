package com.example.mockproject.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.mockproject.R
import com.google.firebase.auth.EmailAuthProvider
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

        val mOldPasswordText = mOldPasswordET.text.trim().toString()

        mChangePwdButton.setOnClickListener {
            if (mOldPasswordET.text.trim().isEmpty()){
                mOldPasswordET.error = "Please enter your current password"
                Log.d("Old password of the current email", EmailAuthProvider.getCredential(user!!.email!!, "12345678").toString() )
                Toast.makeText(context, "Please enter your current password", Toast.LENGTH_SHORT).show()
            }
            if(mOldPasswordET.text.trim().isNotEmpty() && mNewPasswordET.text.trim().isNotEmpty() && mConfirmNewPasswordEt.text.trim().isNotEmpty()){
                changePassword(mOldPasswordET.text.trim().toString(), mNewPasswordET.text.trim().toString(), mConfirmNewPasswordEt.text.trim().toString())
            }
        }
        return view

    }

    private fun changePassword(oldPass: String, newPass: String, confirmNewPass: String) {
        val authCredential = EmailAuthProvider.getCredential(user!!.email!!, oldPass)

    }

}