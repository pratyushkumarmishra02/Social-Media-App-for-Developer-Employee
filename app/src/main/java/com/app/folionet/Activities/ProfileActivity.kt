package com.app.folionet.Activities

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.app.folionet.Activities.PersonalInformationActivity
import com.app.folionet.R
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {
    private lateinit var profilePic: ImageView
    private lateinit var welcomeName: TextView
    private lateinit var personalInformation:LinearLayout
    private lateinit var yourActivity:LinearLayout
    private lateinit var notification:LinearLayout
    private lateinit var timeManagement:LinearLayout
    private lateinit var accountPrivacy:LinearLayout
    private lateinit var blockedAc:LinearLayout
    private lateinit var help:LinearLayout
    private lateinit var faqs:LinearLayout
    private lateinit var backArrow:ConstraintLayout
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        // Initialize views
        profilePic = findViewById(R.id.profilePic)
        welcomeName = findViewById(R.id.welcomeName)
        personalInformation = findViewById(R.id.personalInformation)
        yourActivity = findViewById(R.id.yourActivity)
        notification = findViewById(R.id.notification)
        timeManagement = findViewById(R.id.timeManagement)
        accountPrivacy = findViewById(R.id.accountPrivacy)
        blockedAc = findViewById(R.id.blockedAcMAin)
        help = findViewById(R.id.helpSupport)
        faqs = findViewById(R.id.faqs)
        backArrow = findViewById(R.id.backArrowConstraint)

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseUser = firebaseAuth.currentUser!!
        db = FirebaseFirestore.getInstance()

        loadUserDetails()

        // Full screen window status bar
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        faqs.setOnClickListener {
            val intent = Intent(this, FaqsActivity::class.java)
            startActivity(intent)
        }

        backArrow.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        personalInformation.setOnClickListener {
            val intent = Intent(this, PersonalInformationActivity::class.java)
            startActivity(intent)
        }


    }

    private fun loadUserDetails() {
        if (firebaseUser != null) {
            val userId: String = firebaseUser.getUid()
            db.collection("USERS").document(userId)
                .get()
                .addOnSuccessListener(OnSuccessListener { documentSnapshot: DocumentSnapshot? ->
                    if (documentSnapshot!!.exists()) {
                        val name = documentSnapshot.getString("name")
                        val imageUrl = documentSnapshot.getString("profilePicUrl")
                        if (name != null && !name.isEmpty()) {
                            welcomeName.text = name
                        }
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(this)
                                .load(imageUrl)
                                .placeholder(R.drawable.no_profile_pic)
                                .error(R.drawable.no_profile_pic)
                                .into(profilePic)
                        }

                    }
                })
                .addOnFailureListener(OnFailureListener{ e: Exception? ->
                    Log.e(ContentValues.TAG, "Failed to fetch user details: " + e!!.message)
                    Toast.makeText(this@ProfileActivity, "Failed to load profile details", Toast.LENGTH_SHORT).show()
                })
        }else{
            Toast.makeText(this, "Sorry!!You are not an authenticated user", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onResume() {
        super.onResume()
        loadUserDetails()
    }
}