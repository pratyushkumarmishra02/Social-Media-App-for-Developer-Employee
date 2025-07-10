package com.app.folionet.Activities

import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.app.folionet.R
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView

class PersonalInformationActivity : AppCompatActivity() {
    private lateinit var backArrow: ImageView
    private lateinit var profilePic: CircleImageView
    private lateinit var tvNameProfile: TextView
    private lateinit var tvEmlProfile: TextView
    private lateinit var tvName: TextView
    private lateinit var tvUname: TextView
    private lateinit var tvEml: TextView
    private lateinit var tvDob: TextView
    private lateinit var tvGender: TextView
    private lateinit var tvPhone: TextView
    private lateinit var deleteAccountBtn: MaterialButton
    private lateinit var editProfileBtn: MaterialButton
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var imageUrl: String = ""
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_GOOGLE_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_personal_information)

        backArrow = findViewById(R.id.backArrow)
        profilePic = findViewById(R.id.profilePic)
        tvNameProfile = findViewById(R.id.tvNameProfile)
        tvEmlProfile = findViewById(R.id.tvEmlProfile)
        tvName = findViewById(R.id.tvName)
        tvUname = findViewById(R.id.tvUname)
        tvEml = findViewById(R.id.tvEml)
        tvDob = findViewById(R.id.tvDob)
        tvGender = findViewById(R.id.tvGender)
        tvPhone = findViewById(R.id.tvPhNumber)
        deleteAccountBtn = findViewById(R.id.deleteAccountBtn)
        editProfileBtn = findViewById(R.id.editProfileBtn)

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseUser = firebaseAuth.currentUser!!
        db = FirebaseFirestore.getInstance()

        //handle back arrow click
        backArrow.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        loadUserDetails()


        //delete account
        deleteAccountBtn.setOnClickListener {
            handleReauthenticationAndDelete()
        }


        //edit profile
        editProfileBtn.setOnClickListener {
           val intent = Intent(this, EditProfileActivity::class.java)
            intent.putExtra("name", tvName.text.toString())
            intent.putExtra("username", tvUname.text.toString())
            intent.putExtra("email", tvEml.text.toString())
            intent.putExtra("dob", tvDob.text.toString())
            intent.putExtra("gender", tvGender.text.toString())
            intent.putExtra("phone", tvPhone.text.toString())
            intent.putExtra("imageUrl", imageUrl)
            startActivity(intent)
        }
    }

    private fun handleReauthenticationAndDelete() {
        val user = FirebaseAuth.getInstance().currentUser
        val providers = user?.providerData?.map { it.providerId } ?: emptyList()

        when {
            providers.contains("google.com") && providers.contains("password") -> {
                AlertDialog.Builder(this)
                    .setTitle("Re-authenticate")
                    .setMessage("Choose how you want to re-authenticate:")
                    .setPositiveButton("Google") { _, _ -> reAuthenticateWithGoogle() }
                    .setNegativeButton("Email/Password") { _, _ ->
                        showPasswordDialogAndDelete(user?.email ?: "")
                    }
                    .show()
            }
            providers.contains("google.com") -> {
                showDialogBox()
            }
            providers.contains("password") -> {
                showPasswordDialogAndDelete(user?.email ?: "")
            }
            else -> {
                Toast.makeText(this, "No supported authentication provider found.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDialogBox() {
        AlertDialog.Builder(this)
            .setTitle("")
            .setTitle("Delete-Confirmation")
            .setMessage("are you sure you want to delete your account?")
            .setPositiveButton("Delete") { _, _ ->
                reAuthenticateWithGoogle()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun showPasswordDialogAndDelete(email: String){
        val editText = EditText(this)
        editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        AlertDialog.Builder(this)
            .setTitle("Re-authenticate")
            .setMessage("Enter your password to confirm account deletion:")
            .setView(editText)
            .setPositiveButton("Delete") { _, _ ->
                val password = editText.text.toString()
                reAuthenticateAndDelete(email,password)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun reAuthenticateAndDelete(email:String,password: String) {
        val user = firebaseAuth.currentUser
        if (user != null && !email.isNullOrEmpty()) {
            val credential = EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential)
                .addOnSuccessListener {
                    // First, delete Firestore document
                    deleteUserDataAndAccount(user.uid)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Re-authentication failed: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.d("PersonalInformationActivity", "Re-authentication failed: ${e.message}")
                }
        }
    }

    private fun reAuthenticateWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.result
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                val user = FirebaseAuth.getInstance().currentUser
                user?.reauthenticate(credential)?.addOnCompleteListener { reauthTask ->
                    if (reauthTask.isSuccessful) {
                        deleteUserDataAndAccount(user.uid)
                    } else {
                        Toast.makeText(this, "Re-authentication failed: ${reauthTask.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Google sign-in failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun deleteUserDataAndAccount(uid: String) {
        db.collection("USERS").document(uid)
            .delete()
            .addOnSuccessListener {
                deleteFirebaseAuthUser()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to delete Firestore data: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun deleteFirebaseAuthUser() {
        val user = firebaseAuth.currentUser
        user?.delete()
            ?.addOnSuccessListener {
                Toast.makeText(this, "Profile deleted permanently.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
            ?.addOnFailureListener { e ->
                Toast.makeText(this, "Failed to delete Auth user: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }





    private fun deleteProfile() {
        if (firebaseUser != null) {

            // Delete the user profile from Firebase Auth
            firebaseUser.delete().addOnCompleteListener(object : OnCompleteListener<Void?> {
                override fun onComplete(task: Task<Void?>) {

                    if (task.isSuccessful()) {
                        Toast.makeText(this@PersonalInformationActivity, "Profile deleted successfully.", Toast.LENGTH_SHORT).show()

                        val intent: Intent = Intent(this@PersonalInformationActivity, LoginActivity::class.java)
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@PersonalInformationActivity, "Failed to delete profile. Please try again later.", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        } else {
            Toast.makeText(this, "No user is currently logged in.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserDetails() {
        //get user data from firestore
        if (firebaseUser != null) {
            val userId: String = firebaseUser.getUid()
            db.collection("USERS").document(userId)
                .get()
                .addOnSuccessListener(OnSuccessListener { documentSnapshot: DocumentSnapshot? ->
                    if (documentSnapshot!!.exists()) {
                        val name = documentSnapshot.getString("name")
                        val email = documentSnapshot.getString("email")
                        val username = documentSnapshot.getString("userName")
                        val dob = documentSnapshot.getString("dob")
                        val gender = documentSnapshot.getString("gender")
                        val phone = documentSnapshot.getString("phone")
                        imageUrl = documentSnapshot.getString("profilePicUrl")?:""

                        if (name != null && !name.isEmpty()) {
                            tvNameProfile.text = name
                        }
                        if (email != null && !email.isEmpty()) {
                            tvEmlProfile.text = email
                        }
                        if (name != null && !name.isEmpty()) {
                            tvName.text = name
                        }
                        if (username != null && !username.isEmpty()) {
                            tvUname.text = username
                        }
                        if (email != null && !email.isEmpty()) {
                            tvEml.text = email
                        }
                        if (dob != null && !dob.isEmpty()) {
                            tvDob.text = dob
                        }
                        if (gender != null && !gender.isEmpty()) {
                            tvGender.text = gender
                        }
                        if (phone != null && !phone.isEmpty()) {
                            tvPhone.text = phone
                        }
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(this)
                                .load(imageUrl)
                                .placeholder(R.drawable.no_profile_pic)
                                .error(R.drawable.no_profile_pic)
                                .into(profilePic)
                        }
                    } else {
                        Toast.makeText(this, "User document does not exist", Toast.LENGTH_SHORT)
                            .show()

                    }

                })
                .addOnFailureListener(OnFailureListener { e: Exception? ->
                    Log.e(ContentValues.TAG, "Failed to fetch user details: " + e!!.message)
                    Toast.makeText(
                        this@PersonalInformationActivity,
                        "Failed to load profile details",
                        Toast.LENGTH_SHORT
                    ).show()
                })

        } else {
            Toast.makeText(this, "Sorry!!You are not an authenticated user", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserDetails()
    }
}