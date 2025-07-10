package com.app.folionet.Activities

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.app.folionet.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import android.text.TextWatcher
import androidx.cardview.widget.CardView


class RegistrationActivity : AppCompatActivity() {

    private var isPasswordVisible = false
    private lateinit var editTextName: EditText
    private lateinit var editTextUserName: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPhone: EditText
    private lateinit var editTextDOB: EditText
    private lateinit var editTextPwd: EditText
    private lateinit var tvPwd: TextView
    private lateinit var editTextConfirmPwd: EditText
    private lateinit var tvConfirmPwd: TextView
    private lateinit var radioGroupGender: RadioGroup
    private lateinit var signUpBtn: Button
    private lateinit var lgnBtn: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var generateBtn: ImageButton
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var eyeIconPwd: ImageView? = null
    private var eyeIconConfirmPwd: ImageView? = null

    private var profilePicUrl: String? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registration)

        // UI Elements
        editTextName = findViewById(R.id.editTextName)
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPhone = findViewById(R.id.editTextPhone)
        editTextDOB = findViewById(R.id.editTextDOB)
        editTextPwd = findViewById(R.id.editTextPwd)
        editTextConfirmPwd = findViewById(R.id.editTextConfirmPwd)
        radioGroupGender = findViewById(R.id.radioGroupGender)
        progressBar = findViewById(R.id.progressBar)
        signUpBtn = findViewById(R.id.signUpBtn)
        eyeIconPwd = findViewById(R.id.eyeIconPwd)
        eyeIconConfirmPwd = findViewById(R.id.eyeIconConfirmPwd)
        lgnBtn = findViewById(R.id.lgnBtn)
        tvPwd = findViewById(R.id.textViewPassword)
        tvConfirmPwd = findViewById(R.id.textViewConfirmPassword)
        editTextUserName = findViewById(R.id.etUserName)
        generateBtn = findViewById(R.id.btnGenerateUsername)

        // Full screen
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

//        editTextName.addTextChangedListener(object : TextWatcher {
//            override fun afterTextChanged(s: Editable?) {
//                val nameInput = s.toString().trim()
//                if (nameInput.isNotEmpty()) {
//                    val username = generateUsername(nameInput)
//                    editTextUserName.setText(username)
//                } else {
//                    editTextUserName.setText("")
//                }
//            }
//
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//        })

        generateBtn.setOnClickListener {
            val nameInput = editTextName.text.toString().trim()
            if (nameInput.isNotEmpty()) {
                val randomUsername = generateUsername(nameInput)
                editTextUserName.setText(randomUsername)
            } else {
                Toast.makeText(this, "Please enter your name first", Toast.LENGTH_SHORT).show()
            }
        }


        // Google login intent extras
        val googleName = intent.getStringExtra("name")
        val googleEmail = intent.getStringExtra("email")
        profilePicUrl = intent.getStringExtra("photoUrl")

        googleName?.let { editTextName.setText(it) }
        googleEmail?.let {
            editTextEmail.setText(it)
            editTextEmail.isEnabled = false
            editTextPwd.visibility = View.GONE
            editTextConfirmPwd.visibility = View.GONE
            eyeIconPwd?.visibility = View.GONE
            eyeIconConfirmPwd?.visibility = View.GONE
            tvPwd.visibility = View.GONE
            tvConfirmPwd.visibility = View.GONE
        }

        lgnBtn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        signUpBtn.setOnClickListener {
            registerUser()
        }

        setupErrorClearing()
        eyeIconPwd?.setOnClickListener { togglePasswordVisibility() }
        eyeIconConfirmPwd?.setOnClickListener { toggleConfirmPasswordVisibility() }

        editTextDOB.setOnClickListener {
            val calendar = Calendar.getInstance()

            // parse the date from EditText
            val dobText = editTextDOB.text.toString()
            if (dobText.isNotEmpty()) {
                try {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val date = sdf.parse(dobText)
                    if (date != null) {
                        calendar.time = date
                    }
                } catch (e: Exception) {
                    // If parsing fails, fallback to current date
                }
            }

            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val month = calendar.get(Calendar.MONTH)
            val year = calendar.get(Calendar.YEAR)

            val datePicker = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                editTextDOB.setText(
                    String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                )
            }, year, month, day)

            datePicker.show()
        }
    }

    // Function to generate username
    fun generateUsername(name: String): String {
        val cleanedName = name.replace("\\s".toRegex(), "").lowercase() // Remove spaces and lowercase
        val specialChar = "_@#&%*$₹".random() // Choose a random special char
        val randomDigits = (1000..9999).random() // Generate 4-digit random number
        return "$cleanedName$specialChar$randomDigits"
    }

    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            editTextPwd.transformationMethod = PasswordTransformationMethod.getInstance()
            eyeIconPwd?.setImageResource(R.drawable.baseline_visibility_off_24)
        } else {
            editTextPwd.transformationMethod = HideReturnsTransformationMethod.getInstance()
            eyeIconPwd?.setImageResource(R.drawable.baseline_visibility_24)
        }
        editTextPwd.setSelection(editTextPwd.text.length)
        isPasswordVisible = !isPasswordVisible
    }

    private fun toggleConfirmPasswordVisibility() {
        if (isPasswordVisible) {
            editTextConfirmPwd.transformationMethod = PasswordTransformationMethod.getInstance()
            eyeIconConfirmPwd?.setImageResource(R.drawable.baseline_visibility_off_24)
        } else {
            editTextConfirmPwd.transformationMethod = HideReturnsTransformationMethod.getInstance()
            eyeIconConfirmPwd?.setImageResource(R.drawable.baseline_visibility_24)
        }
        editTextConfirmPwd.setSelection(editTextConfirmPwd.text.length)
        isPasswordVisible = !isPasswordVisible
    }

    private fun setupErrorClearing() {
        val fields = listOf(
            editTextName, editTextEmail, editTextPhone,
            editTextDOB, editTextPwd, editTextConfirmPwd
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

    private fun registerUser() {
        val username = editTextUserName.text.toString().trim()
        val name = editTextName.text.toString().trim()
        val email = editTextEmail.text.toString().trim()
        val lowerName = name.lowercase()
        val phone = editTextPhone.text.toString().trim()
        val dob = editTextDOB.text.toString().trim()
        val password = editTextPwd.text.toString()
        val confirmPassword = editTextConfirmPwd.text.toString()
        val genderId = radioGroupGender.checkedRadioButtonId
        val gender =
            if (genderId != -1) findViewById<RadioButton>(genderId).text.toString() else ""

        if (name.isEmpty()) {
            editTextName.error = "Name is required"
            editTextName.requestFocus()
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.error = "Invalid email format"
            editTextEmail.requestFocus()
            return
        }
        if (phone.length != 10 || !phone.all { it.isDigit() }) {
            editTextPhone.error = "Enter valid 10-digit phone number"
            editTextPhone.requestFocus()
            return
        }
        if (dob.isEmpty()) {
            editTextDOB.error = "Date of Birth is required"
            editTextDOB.requestFocus()
            return
        }

        val isGoogleSignIn = intent.hasExtra("email")

        if (!isGoogleSignIn) {
            if (!password.matches(Regex("^[a-zA-Z0-9_@#&*%$₹]+$"))) {
                editTextPwd.error = "Invalid! Enter valid password(combination of special character(_@#&*%$₹),small latter,number) "
                editTextPwd.requestFocus()
                return
            }
            if (password.length < 6) {
                editTextPwd.error = "Password must be at least 6 characters"
                editTextPwd.requestFocus()
                return
            }
            if (password != confirmPassword) {
                editTextConfirmPwd.error = "Passwords do not match"
                editTextConfirmPwd.requestFocus()
                return
            }
        }

        if (gender.isEmpty()) {
            Toast.makeText(this, "Please select gender", Toast.LENGTH_SHORT).show()
            return
        }
        if (username.isEmpty()) {
            Toast.makeText(this, "Enter valid username(combination of special character(_@#&*%$₹),small latter,number)", Toast.LENGTH_SHORT).show()
            return
        }
        if (!username.matches(Regex("^[a-z0-9_@#&*%$₹]+$"))) {
            editTextUserName.error = "Invalid! Enter valid username(combination of special character(_@#&*%$₹),small latter,number)"
            editTextUserName.requestFocus()
            return
        }

        if(username.length < 5){
            editTextUserName.error = "Username must be at least 5 characters"
            editTextUserName.requestFocus()
            return
        }


        // Check if username is available in firestore if no then save to firestore else show error
        db.collection("USERS")
            .whereEqualTo("userName", username)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    editTextUserName.error = "Username already taken"
                } else {
                    progressBar.visibility = View.VISIBLE

                    if (isGoogleSignIn) {
                        val userId = auth.currentUser?.uid ?: return@addOnSuccessListener
                        saveUserToFirestore(userId, name, lowerName, username, email, phone, dob, gender, profilePicUrl)
                    } else {
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener {
                                val userId = auth.currentUser?.uid ?: return@addOnSuccessListener
                                saveUserToFirestore(userId, name, lowerName, username,email, phone, dob, gender, null)
                            }
                            .addOnFailureListener {
                                progressBar.visibility = View.GONE
                                Toast.makeText(this, "Auth Error: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error checking username: ${it.message}", Toast.LENGTH_SHORT).show()
            }

    }

    private fun saveUserToFirestore(
        userId: String,
        name: String,
        lowerName: String,
        username: String,
        email: String,
        phone: String,
        dob: String,
        gender: String,
        profileUrl: String?
    ) {

        val currentDate = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        val createdAtFormatted = dateFormat.format(currentDate)

        // Save to shared preferences
        val sharedPreferences = getSharedPreferences("ExtraDetails", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("createdAtFormatted", createdAtFormatted)
        editor.apply()

        val userMap = hashMapOf(
            "userId" to userId,
            "name" to name,
            "lowerName" to lowerName,
            "userName" to username,
            "email" to email,
            "phone" to phone,
            "dob" to dob,
            "gender" to gender,
            "profilePicUrl" to profileUrl,
            "createdAtTimestamp" to System.currentTimeMillis(),
            "createdAtFormatted" to createdAtFormatted
        )

        db.collection("USERS")
            .document(userId)
            .set(userMap)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "User registered successfully", Toast.LENGTH_SHORT).show()
                val currentUser = FirebaseAuth.getInstance().currentUser
                val providerId = currentUser?.providerData?.get(1)?.providerId

                if (providerId == "google.com") {
                    // Redirect to dashboard if Google login
                    startActivity(Intent(this, MainActivity::class.java))
                } else {
                    // Otherwise, redirect to login
                    startActivity(Intent(this, LoginActivity::class.java))
                }
                finish()
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Firestore Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
