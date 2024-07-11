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
import com.google.firebase.firestore.FirebaseFirestore

class ChangePasswordFragment : Fragment() {
    //UI
    private lateinit var mOldPasswordET: EditText
    private lateinit var mNewPasswordET: EditText
    private lateinit var mConfirmNewPasswordET: EditText
    private lateinit var mChangePwdButton: Button
    private lateinit var mCancelChangingPassword: Button
    private var user = FirebaseAuth.getInstance().currentUser

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_change_password, container, false)
        mOldPasswordET = view.findViewById(R.id.frg_password_oldPwd)
        mNewPasswordET = view.findViewById(R.id.frg_password_newPwd)
        mConfirmNewPasswordET = view.findViewById(R.id.frg_password_confirmNewPwd)
        mChangePwdButton = view.findViewById(R.id.frg_password_change_btn)
        mCancelChangingPassword = view.findViewById(R.id.frg_password_cancel_btn)


        mChangePwdButton.setOnClickListener {
            val mOldPasswordText = mOldPasswordET.text.trim()
            val mNewPasswordText = mNewPasswordET.text.trim()
            val mConfirmPasswordText = mConfirmNewPasswordET.text.trim()

            if (mOldPasswordText.isEmpty()) {
                mOldPasswordET.error = "Please enter your current password"
                Log.d("Edit text", mOldPasswordText.toString())
                //Test
                Log.d(
                    "Old password of the current email",
                    EmailAuthProvider.getCredential(user!!.email!!, "12345678").toString()
                )
            } else if (mNewPasswordText.isNotEmpty() && mConfirmPasswordText.isNotEmpty()) {
                if (mNewPasswordText.length < 8 || mNewPasswordText.length > 14) {
                    Toast.makeText(
                        context,
                        "New password length must be between 8 and 14 characters",
                        Toast.LENGTH_SHORT
                    ).show()
                    mNewPasswordET.text.clear()
                    mConfirmNewPasswordET.text.clear()
                } else if (!mNewPasswordText.contentEquals(mConfirmPasswordText)) {
                    Log.d("new pass and confirm pass", "$mNewPasswordText $mConfirmPasswordText")
                    Toast.makeText(
                        context,
                        "Password and confirm password are not the same!",
                        Toast.LENGTH_SHORT
                    ).show()
                    mNewPasswordET.text.clear()
                    mConfirmNewPasswordET.text.clear()
                } else if (mNewPasswordText.contentEquals(mOldPasswordText)) {
                    Toast.makeText(
                        context,
                        "New password and the current password cannot be the same!",
                        Toast.LENGTH_SHORT
                    ).show()
                    mNewPasswordET.text.clear()
                    mConfirmNewPasswordET.text.clear()
                } else {
                    changePassword(
                        mOldPasswordET.text.trim().toString(),
                        mNewPasswordET.text.trim().toString()
                    )
                }
            } else {
                mNewPasswordET.error = "This field cannot be empty"
                mConfirmNewPasswordET.error = "This field cannot be empty"
            }
        }

        mCancelChangingPassword.setOnClickListener{
            requireActivity().supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.nav_default_enter_anim, R.anim.nav_default_exit_anim)
                .remove(this)
                .commit()
        }
        return view

    }

    private fun changePassword(oldPass: String, newPass: String) {
        //Get the current User Credential
        val authCredential = EmailAuthProvider.getCredential(user!!.email!!, oldPass)
        user!!.reauthenticate(authCredential).addOnSuccessListener {
            user!!.updatePassword(newPass).addOnSuccessListener {
                //Store into the firestore for easier observing and fixing, but shouldn't store it here
//                val newPasswordData: Map<String, Any> = hashMapOf("Password" to newPass)
//                fStore.update(newPasswordData).addOnSuccessListener {
                    Toast.makeText(context, "Update password successfully", Toast.LENGTH_SHORT)
                        .show()
                    requireActivity().supportFragmentManager.popBackStack()
//                }.addOnFailureListener {
//                    Toast.makeText(context, "Fail to update new password", Toast.LENGTH_SHORT)
//                        .show()
//                }
            }.addOnFailureListener {
                Toast.makeText(context, "Fail to update new password", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(
                context,
                "Your old password doesn't match, try again",
                Toast.LENGTH_SHORT
            ).show()
            mOldPasswordET.text.clear()
            mNewPasswordET.text.clear()
            mConfirmNewPasswordET.text.clear()
        }
    }

}