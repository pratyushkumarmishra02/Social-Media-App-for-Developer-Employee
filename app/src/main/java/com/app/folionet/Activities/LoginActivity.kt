package com.app.folionet.Activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.app.folionet.R
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore

open class LoginActivity : AppCompatActivity() {
    private var isPasswordVisible = false
    private lateinit var etEml: EditText
    private lateinit var etPwd: EditText
    private lateinit var forgotPwd: TextView
    private lateinit var lgnBtn: Button
    private lateinit var registrationBtn: Button
    private lateinit var eyeIcon: ImageView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var progressBar: ProgressBar
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001
    private lateinit var googleBtn: LinearLayout
    private lateinit var facebookBtn: LinearLayout


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        etEml = findViewById(R.id.editTextEmail)
        etPwd = findViewById(R.id.editTextPwd)
        forgotPwd = findViewById(R.id.forgotPwd)
        lgnBtn = findViewById(R.id.lgnBtn)
        registrationBtn = findViewById(R.id.registrationBtn)
        eyeIcon = findViewById(R.id.eyeIcon)
        progressBar = findViewById(R.id.progressbar)
        googleBtn = findViewById(R.id.googleLgn)
        facebookBtn = findViewById(R.id.facebookLgn)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Google Sign-In Configuration
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Switch account & sign in
        googleBtn.setOnClickListener {
            signOutAndSignInAgain()
        }
        facebookBtn.setOnClickListener {
            startActivity(Intent(this@LoginActivity, FacebookLoginActivity::class.java))
        }

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        eyeIcon.setOnClickListener { togglePasswordVisibility() }

        registrationBtn.setOnClickListener {
            startActivity(Intent(this@LoginActivity, RegistrationActivity::class.java))
        }

        lgnBtn.setOnClickListener { loginUser() }
    }

    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            etPwd.transformationMethod = PasswordTransformationMethod.getInstance()
            eyeIcon.setImageResource(R.drawable.baseline_visibility_off_24)
        } else {
            etPwd.transformationMethod = HideReturnsTransformationMethod.getInstance()
            eyeIcon.setImageResource(R.drawable.baseline_visibility_24)
        }
        etPwd.setSelection(etPwd.length())
        isPasswordVisible = !isPasswordVisible
    }

    private fun loginUser() {
        val eml = etEml.text.toString()
        val pwd = etPwd.text.toString()

        if (eml.isEmpty()) {
            etEml.error = "Email is required!"
            etEml.requestFocus()
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(eml).matches()) {
            etEml.error = "Valid email is required!"
            etEml.requestFocus()
            return
        }
        if (pwd.isEmpty()) {
            etPwd.error = "Password is required!"
            etPwd.requestFocus()
            return
        }

        progressBar.visibility = View.VISIBLE
        auth.signInWithEmailAndPassword(eml, pwd).addOnCompleteListener { task ->
            progressBar.visibility = View.GONE
            if (task.isSuccessful) {
                val firebaseUser = auth.currentUser
                if (firebaseUser != null && firebaseUser.isEmailVerified) {
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                } else {
                    firebaseUser?.sendEmailVerification()
                    auth.signOut()
                    showEmailVerificationDialog()
                }
            } else {
                handleLoginError(task.exception)
            }
        }
    }

    private fun handleLoginError(exception: Exception?) {
        when (exception) {
            is FirebaseAuthInvalidUserException -> {
                etEml.error = "User doesn't exist. Please register."
                etEml.requestFocus()
            }

            is FirebaseAuthInvalidCredentialsException -> {
                etPwd.error = "Invalid credentials. Please try again."
                etPwd.requestFocus()
            }

            else -> {
                Toast.makeText(this, exception?.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showEmailVerificationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Email Not Verified")
        builder.setMessage("Please verify your email before logging in.")
        builder.setPositiveButton("OK") { _, _ ->
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_APP_EMAIL)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
        builder.create().show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d("GoogleSignIn", "Google sign in successful, ID Token: ${account.idToken}")
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.e("GoogleSignIn", "Google Sign-In failed", e)
                Toast.makeText(this, "Google Sign In failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    //after google login it will check whether the user is already exist or not
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val uid = user?.uid

                    if (uid != null) {
                        val db = FirebaseFirestore.getInstance()

                        db.collection("USERS").document(uid)
                            .get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    startActivity(Intent(this, MainActivity::class.java))
                                    finish()
                                } else {
                                    // First-time Google user — go to registration
                                    val intent = Intent(this, RegistrationActivity::class.java)
                                    intent.putExtra("email", user.email)
                                    intent.putExtra("name", user.displayName)
                                    intent.putExtra("photoUrl", user.photoUrl.toString())
                                    startActivity(intent)
                                    finish()
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Failed to check user data", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show()
                }
            }
    }


    //Sign out to allow switching Google accounts
    private fun signOutAndSignInAgain() {
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }
}
