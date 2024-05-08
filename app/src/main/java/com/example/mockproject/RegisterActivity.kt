package com.example.mockproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var fullName: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var confirmPassword: EditText
    private lateinit var registerBtn: Button
    private lateinit var goToLogin: Button
    private var valid = true

    private lateinit var auth: FirebaseAuth
    private lateinit var fStore: FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        fullName = findViewById(R.id.registerName)
        email = findViewById(R.id.registerEmail)
        password = findViewById(R.id.registerPassword)
        confirmPassword = findViewById(R.id.confirmPassword)
        registerBtn = findViewById(R.id.registerBtn)
        goToLogin = findViewById(R.id.gotoLogin)

        checkField(fullName)
        checkField(email)
        checkField(password)
        checkField(confirmPassword)

        auth = FirebaseAuth.getInstance()
        fStore = FirebaseFirestore.getInstance()

        goToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        registerBtn.setOnClickListener {
            try {
                val mEmail = email.text.toString()
                val mPassword = password.text.toString()
                val mConfirmPassword = confirmPassword.text.toString()

                if (mEmail.isNotEmpty() && mPassword.isNotEmpty() && mConfirmPassword.isNotEmpty()) {
                    if (mPassword == mConfirmPassword) {
                        auth.createUserWithEmailAndPassword(mEmail, mPassword)
                            .addOnCompleteListener {
                                if (it.isSuccessful) {
                                    val user: FirebaseUser? = auth.currentUser
                                    val df: DocumentReference =
                                        fStore.collection("Users").document(user!!.uid)
                                    val userInfo: HashMap<String, Any> = HashMap()
                                    userInfo["FullName"] = fullName.text.toString()
                                    userInfo["Email"] = email.text.toString()
                                    userInfo["Password"] = password.text.toString()
                                    userInfo["isAdmin"] = "0"
                                    df.set(userInfo)
                                    startActivity(Intent(this, LoginActivity::class.java))
                                    finish()
                                } else {
                                    Toast.makeText(
                                        this,
                                        "Password doesn't match",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    } else {
                        Toast.makeText(this, "Password doesn't match", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Empty fields are not allowed !", Toast.LENGTH_SHORT)
                        .show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    private fun checkField(text: EditText): Boolean {
        if (text.text.isEmpty()) {
            text.error = "This field cannot be empty"
            valid = false
        } else {
            valid = true
        }
        return valid
    }
}