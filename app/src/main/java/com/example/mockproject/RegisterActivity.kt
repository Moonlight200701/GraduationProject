package com.example.mockproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.HashMap
import java.util.Locale

class RegisterActivity : AppCompatActivity() {

    private lateinit var fullName: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var confirmPassword: EditText
    private lateinit var registerBtn: Button
    private lateinit var goToLogin: Button
    private var valid = true

    private lateinit var fAuth: FirebaseAuth
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

        fAuth = FirebaseAuth.getInstance()
        fStore = FirebaseFirestore.getInstance()

        goToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        registerBtn.setOnClickListener {
            val mEmail = email.text.toString()
            val mPassword = password.text.toString()
            val mConfirmPassword = confirmPassword.text.toString()

            if (validateFields(mEmail, mPassword, mConfirmPassword)) {
                checkEmailAndRegister(mEmail, mPassword)
            }
        }
    }

    private fun validateFields(email: String, password: String, confirmPassword: String): Boolean {
        if (email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
            if (email.matches("^[A-Za-z0-9+_.-]+@(.+)$".toRegex())) {
                if (password.length in 8..14) {
                    if (password == confirmPassword) {
                        val nameOfEmail = email.substringBefore("@")
                        if (nameOfEmail.length in 6..30) {
                            return true
                        } else {
                            Toast.makeText(
                                this,
                                "Name of the email must be between 6 to 30 characters",
                                Toast.LENGTH_SHORT
                            ).show()
                            clearFields()
                        }
                    } else {
                        Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Password must be between 8 and 14 characters",
                        Toast.LENGTH_SHORT
                    ).show()
                    clearPasswordFields()
                }
            } else {
                Toast.makeText(this, "That is not a valid email, try again", Toast.LENGTH_SHORT).show()
                clearFields()
            }
        } else {
            Toast.makeText(this, "Empty fields are not allowed!", Toast.LENGTH_SHORT).show()
        }
        return false
    }

    private fun checkEmailAndRegister(email: String, password: String) {
        fAuth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { task ->
                val signInMethods = task.result?.signInMethods
                if (!signInMethods.isNullOrEmpty()) {
                    Toast.makeText(this, "Email already exists", Toast.LENGTH_SHORT).show()
                } else {
                    registerUser(email, password)
                }
            }
    }

    private fun registerUser(email: String, password: String) {
        fAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val user: FirebaseUser? = fAuth.currentUser
                    val df: DocumentReference = fStore.collection("Users").document(user!!.uid)
                    val userInfo: HashMap<String, Any> = HashMap()
                    userInfo["FullName"] = fullName.text.toString()
                    userInfo["Email"] = email
                    userInfo["Password"] = password
                    userInfo["isAdmin"] = "0"
                    userInfo["Status"] = "Enabled"
                    userInfo["Birthday"] = "Unknown"
                    userInfo["Gender"] = "Unknown"
                    userInfo["CreatedTime"] = SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()
                    ).format(Date())
                    userInfo["Marked"] = false
                    df.set(userInfo)
                    startActivity(Intent(this, LoginActivity::class.java))
                    Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, it.exception?.message + ", try again", Toast.LENGTH_SHORT).show()
                    clearFields()
                }
            }
    }

    private fun clearFields() {
        email.text.clear()
        password.text.clear()
        confirmPassword.text.clear()
    }

    private fun clearPasswordFields() {
        password.text.clear()
        confirmPassword.text.clear()
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