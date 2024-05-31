package com.example.mockproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mockproject.database.DatabaseOpenHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LoginActivity : AppCompatActivity() {
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var loginBtn: Button
    private lateinit var gotoRegister: Button
    private lateinit var fAuth: FirebaseAuth
    private lateinit var fStore: FirebaseFirestore
    private lateinit var mDatabaseOpenHelper: DatabaseOpenHelper
    private lateinit var forgotPasswordTV: TextView
    private var backPressedCount = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        fAuth = FirebaseAuth.getInstance()
        fStore = FirebaseFirestore.getInstance()

        email = findViewById(R.id.loginEmail)
        password = findViewById(R.id.loginPassword)
        loginBtn = findViewById(R.id.loginBtn)
        gotoRegister = findViewById(R.id.gotoRegister)
        forgotPasswordTV = findViewById(R.id.forgot_password_tv)

        mDatabaseOpenHelper = DatabaseOpenHelper(this, null)

        gotoRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        //If the user click the back button 2 times, exit the application
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = if (backPressedCount <= 0) {
                backPressedCount++
                Toast.makeText(this@LoginActivity, "Press back again to exit", Toast.LENGTH_SHORT)
                    .show()
            } else {
                finish()
            }

        })

        loginBtn.setOnClickListener {
            val mEmail = email.text.toString().trim()
            val mPassword = password.text.toString().trim()
            if (mEmail.isNotEmpty() && mPassword.isNotEmpty()) {
                fAuth.signInWithEmailAndPassword(mEmail, mPassword)
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            loginBtn.isEnabled = false
                            val user: FirebaseUser? = fAuth.currentUser
                            val db = FirebaseFirestore.getInstance()
                            val docRef = db.collection("Users").document(user!!.uid)
                            docRef.get()
                                .addOnSuccessListener { document ->
                                    if (document != null && document.exists()) {
                                        // if the account got disabled, show a toast and log out
                                        val status = document.getString("Status")
                                        if (status.equals("Disabled", true)) {
                                            Toast.makeText(
                                                this,
                                                "Account is disabled. Please contact the admin",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            fAuth.signOut()
                                        } else {
                                            //cái này hơi thừa, vì minh co the goi trong main activity
                                            val email = document.getString("Email")
                                            val name = document.getString("FullName")
                                            val isAdmin = document.getString("isAdmin")
                                            val birthday = document.getString("Birthday")
                                            val gender = document.getString("Gender")

                                            Log.d("From firebase", name.toString())
                                            Toast.makeText(
                                                this,
                                                "Welcome $name!",
                                                Toast.LENGTH_SHORT
                                            )
                                                .show()

                                            val intent = Intent(this, MainActivity::class.java)

                                            updatePassword(mPassword)

                                            //No need to sync when the admin logs in
                                            if (document.getString("isAdmin") == "1") {
                                                intent.putExtra("Username", name)
                                                intent.putExtra("Email", email)
                                                intent.putExtra("isAdmin", isAdmin)
                                                intent.putExtra("Birthday", birthday)
                                                intent.putExtra("Gender", gender)
                                                startActivity(intent)
                                                finish()
                                            } else {
                                                //Sync the data from the fireStore
                                                lifecycleScope.launch {
                                                    val result =
                                                        mDatabaseOpenHelper.synchronizeWithFireStore()
                                                    Log.d(
                                                        "Is Sync complete? 1 for yes, 0 for no",
                                                        result.toString()
                                                    )
                                                    intent.putExtra("Username", name)
                                                    intent.putExtra("Email", email)
                                                    intent.putExtra("isAdmin", isAdmin)
                                                    intent.putExtra("Birthday", birthday)
                                                    intent.putExtra("Gender", gender)
                                                    startActivity(intent)
                                                    finish()
                                                }
                                                saveTheSignInTime()
                                            }
                                        }
                                    } else {
                                        Log.d("Error no document", "No such document")
                                        Toast.makeText(
                                            this,
                                            "Your account got terminated :<",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        loginBtn.isEnabled = true
                                        fAuth.signOut()
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    Log.d("Exception", "get failed with ", exception)
                                    Toast.makeText(
                                        this,
                                        "Connection error. Please try again later.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                        } else {
                            Toast.makeText(
                                this,
                                "Email or password doesn't match ",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Empty fields are not allowed!", Toast.LENGTH_SHORT).show()
            }
        }

        forgotPasswordTV.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    //If the user changed the password or get to change the password by the forgot password, update the password in the firestore
    private fun updatePassword(newPassword: String) {
        val newPasswordData: HashMap<String, Any> = hashMapOf(
            "Password" to newPassword
        )
        fStore.collection("Users").document(fAuth.currentUser!!.uid).update(newPasswordData)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.d("Update new Password", "Updated New Password successfully")
                }
            }
    }

    private fun showForgotPasswordDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        val dialogView =
            LayoutInflater.from(this).inflate(R.layout.forgot_password_dialog, null)
        val emailForgotPasswordEt =
            dialogView.findViewById<EditText>(R.id.forgot_password_dialog_et)
        dialogBuilder.setView(dialogView)
        dialogBuilder.setCancelable(true)
        dialogBuilder.setPositiveButton("Ok") { _, _ ->
            if (emailForgotPasswordEt.text.isNotEmpty()) {
                val email = emailForgotPasswordEt.text.toString()
                fAuth.sendPasswordResetEmail(email).addOnCompleteListener {
                    if (it.isSuccessful) {
                        Toast.makeText(
                            this,
                            "Password reset email sent, please check the email address",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
        val dialog = dialogBuilder.create()
        dialog.window?.attributes?.dimAmount =
            0.9f // Set this value between 0.0f and 1.0f to control the darkness
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        dialog.show()

    }


    //Save the current login time for the admin
    private fun saveTheSignInTime() {
        val user = fAuth.currentUser
        if (user != null) {
            val df = fStore.collection("Users").document(user.uid)
            val userInfo: HashMap<String, Any> = hashMapOf(
                "Last Login time" to SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()
                ).format(Date())
            )
            df.get().addOnSuccessListener {
                if (it.exists()) {
                    df.update(userInfo).addOnSuccessListener {
                        Log.d("Update login time", "Updated successfully")
                    }
                }
            }
        }
    }

//    if the user is already logged in
//    override fun onStart() {
//        super.onStart()
//        val intent = Intent(this, MainActivity::class.java)
//        if (FirebaseAuth.getInstance().currentUser != null) {
//            startActivity(intent)
//            Toast.makeText(this@LoginActivity, FirebaseAuth.getInstance().currentUser.toString(), Toast.LENGTH_SHORT).show()
//            finish()
//        }
//    }

    //Sync the data from the firestore if the user sign in into a new device
}