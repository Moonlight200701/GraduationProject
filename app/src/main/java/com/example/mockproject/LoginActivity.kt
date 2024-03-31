package com.example.mockproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import com.google.firebase.auth.FirebaseAuth
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

        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true){
            override fun handleOnBackPressed() = if (backPressedCount < 1) {
                backPressedCount++
                Toast.makeText(this@LoginActivity, "Press back again to exit", Toast.LENGTH_SHORT).show()
            } else {
                finish()
            }

        })

        loginBtn.setOnClickListener {
            val mEmail = email.text.toString().trim()
            val mPassword = password.text.toString().trim()
            if (mEmail.isNotEmpty() && mPassword.isNotEmpty()) {
                fAuth.signInWithEmailAndPassword(email.text.toString(), mPassword).addOnCompleteListener {
                    if (it.isSuccessful) {
                        Toast.makeText(this,"Logged in successfully", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Email or password doesn't match", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Empty fields are not allowed !", Toast.LENGTH_SHORT).show()
            }
        }

    }

//    override fun onStart() {
//        super.onStart()
//        val intent = Intent(this, MainActivity::class.java)
//        if (FirebaseAuth.getInstance().currentUser != null) {
//            startActivity(intent)
//        }
//    }
    //for if the user is already logged in
}