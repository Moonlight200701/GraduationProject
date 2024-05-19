package com.example.mockproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var loginBtn: Button
    private lateinit var gotoRegister: Button
    private lateinit var fAuth: FirebaseAuth
    private lateinit var fStore: FirebaseFirestore
    private var backPressedCount = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        fAuth = FirebaseAuth.getInstance()
        fStore = FirebaseFirestore.getInstance();

        email = findViewById(R.id.loginEmail)
        password = findViewById(R.id.loginPassword)
        loginBtn = findViewById(R.id.loginBtn)
        gotoRegister = findViewById(R.id.gotoRegister)

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
                fAuth.signInWithEmailAndPassword(email.text.toString(), mPassword)
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
//                        Toast.makeText(this,"Logged in successfully", Toast.LENGTH_SHORT).show()
                            val user: FirebaseUser? = fAuth.currentUser
                            val db = FirebaseFirestore.getInstance()
                            val docRef = db.collection("Users").document(user!!.uid)
                            docRef.get()
                                .addOnSuccessListener { document ->
                                    if (document != null && document.exists()) {
                                        // Get the data from the document
                                        val status = document.getString("Status")
                                        if (status.equals("Disabled", true)) {
                                            Toast.makeText(
                                                this,
                                                "Account is disabled. Please contact the admin",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            fAuth.signOut()
                                        } else {
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
                                            intent.putExtra("Username", name)
                                            intent.putExtra("Email", email)
                                            intent.putExtra("isAdmin", isAdmin)
                                            intent.putExtra("Birthday", birthday)
                                            intent.putExtra("Gender", gender)
                                            startActivity(intent)
                                            finish()
                                        }
                                    } else {
                                        Log.d("Error no document", "No such document")
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
                                "Email or password doesn't match",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Empty fields are not allowed!", Toast.LENGTH_SHORT).show()
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


}