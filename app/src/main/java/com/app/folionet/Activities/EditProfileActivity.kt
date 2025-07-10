package com.app.folionet.Activities

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.app.folionet.Activities.PersonalInformationActivity
import com.app.folionet.R
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.*

class EditProfileActivity : AppCompatActivity() {

    private lateinit var profilePic: CircleImageView
    private lateinit var editProfilePic: ImageView
    private lateinit var etName: EditText
    private lateinit var etUName: EditText
    private lateinit var etEml: EditText
    private lateinit var etDob: EditText
    private lateinit var etPhone: EditText
    private lateinit var radioGroupGender: RadioGroup
    private lateinit var deleteBtn: MaterialButton
    private lateinit var editBtn: MaterialButton
    private lateinit var backArrowConstraint: ConstraintLayout
    private lateinit var generateBtn: ImageButton

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var firebaseUser: FirebaseUser

    private var url: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        firebaseUser = auth.currentUser!!

        initViews()
        receiveData()
        setupDOBPicker()
        handleSave()
        setupErrorClearing()
        loadImage()
    }

    private fun loadImage() {
        if(firebaseUser != null){
            val uid = firebaseUser.uid
            firestore.collection("USERS")
                .document(uid)
                .get()
                .addOnSuccessListener {
                    if(it != null) {
                        url = it.getString("profilePicUrl")
                        Glide.with(this)
                            .load(url)
                            .placeholder(R.drawable.no_profile_pic)
                            .error(R.drawable.no_profile_pic)
                            .into(profilePic)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this@EditProfileActivity, "Failed to load profile image", Toast.LENGTH_SHORT).show()
                    }
        }else{
            Toast.makeText(this@EditProfileActivity, "Sorry!!You are not an authenticated user", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initViews() {
        profilePic = findViewById(R.id.profilePic)
        editProfilePic = findViewById(R.id.editProfilePic)
        etName = findViewById(R.id.etName)
        etUName = findViewById(R.id.etUName)
        etEml = findViewById(R.id.etEml)
        etDob = findViewById(R.id.etDob)
        etPhone = findViewById(R.id.etPhone)
        radioGroupGender = findViewById(R.id.radioGroupGender)
        deleteBtn = findViewById(R.id.deleteBtn)
        editBtn = findViewById(R.id.editBtn)
        backArrowConstraint = findViewById(R.id.backArrowConstraint)
        generateBtn = findViewById(R.id.btnGenerateUsername)


        //handle back arrow click
        backArrowConstraint.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        generateBtn.setOnClickListener {
            val nameInput = etName.text.toString().trim()
            if (nameInput.isNotEmpty()) {
                val randomUsername = generateUsername(nameInput)
                etUName.setText(randomUsername)
            } else {
                Toast.makeText(this, "Please enter your name first", Toast.LENGTH_SHORT).show()
            }
        }
        editProfilePic.setOnClickListener {
            val intent = Intent(this, UploadImageActivity::class.java)
            intent.putExtra("profilePicUrl", url)
            startActivity(intent)
        }
    }

    private fun receiveData() {
        etName.setText(intent.getStringExtra("name"))
        etUName.setText(intent.getStringExtra("username"))
        etEml.setText(intent.getStringExtra("email"))
        etDob.setText(intent.getStringExtra("dob"))
        etPhone.setText(intent.getStringExtra("phone"))
        when (intent.getStringExtra("gender")) {
            "Male" -> radioGroupGender.check(R.id.radioMale)
            "Female" -> radioGroupGender.check(R.id.radioFemale)
            "Other" -> radioGroupGender.check(R.id.radioOther)
        }
    }

    private fun setupDOBPicker() {
        etDob.setOnClickListener {
            val c = Calendar.getInstance()
            // parse the date from EditText
            val dobText = etDob.text.toString()
            if (dobText.isNotEmpty()) {
                try {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val date = sdf.parse(dobText)
                    if (date != null) {
                        c.time = date
                    }
                } catch (e: Exception) {
                    // If parsing fails, fallback to current date
                }
            }
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            val dpd = DatePickerDialog(this, { _, y, m, d ->
                val formatted = String.format("%02d/%02d/%04d", d, m + 1, y)
                etDob.setText(formatted)
            }, year, month, day)

            dpd.show()
        }
    }


    private fun handleSave() {
        editBtn.setOnClickListener {
            val uid = auth.currentUser?.uid ?: return@setOnClickListener

            val name = etName.text.toString().trim()
            val url = url
            val lowerName = name.lowercase()
            val username = etUName.text.toString().trim()
            val email = etEml.text.toString().trim()
            val dob = etDob.text.toString().trim()
            val phone = etPhone.text.toString().trim()

            val gender = when (radioGroupGender.checkedRadioButtonId) {
                R.id.radioMale -> "Male"
                R.id.radioFemale -> "Female"
                R.id.radioOther -> "Other"
                else -> ""
            }

            //Validation
            if (name.isEmpty()) {
                etName.error = "Name is required"
                etName.requestFocus()
                return@setOnClickListener
            }

            if (username.isEmpty()) {
                Toast.makeText(this, "Enter valid username(combination of special character(_@#&*%$₹),small latter,number)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!username.matches(Regex("^[a-z0-9_@#&*%$₹]+$"))) {
                etUName.error = "Invalid!Enter valid username(combination of special character(_@#&*%$₹),small latter,number) format"
                etUName.requestFocus()
                return@setOnClickListener
            }
            if(username.length < 5){
                etUName.error = "Username must be at least 5 characters"
                etUName.requestFocus()
                return@setOnClickListener
            }

            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEml.error = "Valid email is required"
                etEml.requestFocus()
                return@setOnClickListener
            }

            if (dob.isEmpty()) {
                etDob.error = "Date of Birth is required"
                etDob.requestFocus()
                return@setOnClickListener
            }

            if (phone.isEmpty() || phone.length < 10) {
                etPhone.error = "Valid phone number is required"
                etPhone.requestFocus()
                return@setOnClickListener
            }

            if (gender.isEmpty()) {
                Toast.makeText(this, "Please select a gender", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //get the shared preference
            val sharedPreferences = getSharedPreferences("ExtraDetails", MODE_PRIVATE)
            val createdDate = sharedPreferences.getString("createdAtFormatted",null)

            //Save to Firestore
            val userMap = hashMapOf(
                "UserId" to uid,
                "name" to name,
                "lowerName" to lowerName,
                "userName" to username,
                "email" to email,
                "dob" to dob,
                "phone" to phone,
                "gender" to gender,
                "profilePicUrl" to url,
                "createdAtTimestamp" to System.currentTimeMillis(),
                "createdAtFormatted" to createdDate
            )

            firestore.collection("USERS").document(uid)
                .set(userMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Update Failed: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }

        deleteBtn.setOnClickListener {
            finish()
        }
    }


    // Function to generate username
    fun generateUsername(name: String): String {
        val cleanedName = name.replace("\\s".toRegex(), "").lowercase() // Remove spaces and lowercase
        val specialChar = "_@#&*%$₹".random() // Choose a random special char
        val randomDigits = (1000..9999).random() // Generate 4-digit random number
        return "$cleanedName$specialChar$randomDigits"
    }

    private fun setupErrorClearing() {
        val fields = listOf(
            etName,etUName, etEml, etPhone,
            etDob
        )

        for (field in fields) {
            field.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (!s.isNullOrEmpty()) {
                        field.error = null
                    }
                }

                override fun afterTextChanged(s: android.text.Editable?) {}
            })
        }
    }
    override fun onResume() {
        super.onResume()
        loadImage()
    }
}
