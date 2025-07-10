package com.app.folionet.Activities

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.app.folionet.R
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class UploadImageActivity : AppCompatActivity() {

    private lateinit var profileImageView: ImageView
    private lateinit var btnChooseImage: MaterialButton
    private lateinit var btnUploadImage: MaterialButton
    private lateinit var backArrow: ImageView
    private var selectedImageUri: Uri? = null

    private val PICK_IMAGE_REQUEST = 101
    private val CAPTURE_IMAGE_REQUEST = 102
    private val CAMERA_PERMISSION_REQUEST = 2001

    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private var cameraImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_image)

        profileImageView = findViewById(R.id.profileImageView)
        btnChooseImage = findViewById(R.id.btnChooseImage)
        btnUploadImage = findViewById(R.id.btnUploadImage)
        backArrow = findViewById(R.id.backArrow)

        // Handle back arrow click
        backArrow.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Load existing image from intent
        intent.getStringExtra("profilePicUrl")?.let { imageUrl ->
            Log.d("UploadImageActivity", "Image URL: $imageUrl")
            if (imageUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.no_profile_pic)
                    .into(profileImageView)
            }
        }

        btnChooseImage.setOnClickListener { showImagePickerDialog() }
        btnUploadImage.setOnClickListener { uploadImageToFirebase() }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Camera", "Gallery")
        AlertDialog.Builder(this)
            .setTitle("Choose Image Source")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermission()
                    1 -> openGallery()
                }
            }.show()
    }

    private fun checkCameraPermission() {
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), CAMERA_PERMISSION_REQUEST)
        } else {
            openCamera()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun openCamera() {
        val imageFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "IMG_${System.currentTimeMillis()}.jpg")
        cameraImageUri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            imageFile
        )

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        startActivityForResult(cameraIntent, CAPTURE_IMAGE_REQUEST)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_REQUEST, CAPTURE_IMAGE_REQUEST -> {
                    selectedImageUri = data?.data ?: cameraImageUri
                    selectedImageUri?.let { uri ->
                        val path = uri.path ?: return
                        val original = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                        val rotation = getExifRotation(path)
                        val fixed = rotateBitmap(original, rotation)

                        // Use Glide to show fixed image
                        Glide.with(this).load(fixed).into(profileImageView)

                        // Convert bitmap to Uri/File to upload:
                        val file = File(cacheDir, "rotated_${System.currentTimeMillis()}.jpg")
                        FileOutputStream(file).use { out ->
                            fixed.compress(Bitmap.CompressFormat.JPEG, 90, out)
                        }
                        selectedImageUri = Uri.fromFile(file)
                    }
                }
            }
        }
    }


    fun getExifRotation(path: String): Int {
        return try {
            val ei = ExifInterface(path)
            val orientation = ei.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: IOException) {
            0
        }
    }

    fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }



    private fun uploadImageToFirebase() {
        val uid = auth.currentUser?.uid ?: return
        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
            return
        }

        val fileName = "profile_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpg"
        val imageRef = storage.reference.child("profile_images/$uid/$fileName")

        imageRef.putFile(selectedImageUri!!)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    saveImageUrlToFirestore(uid, downloadUri.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Upload failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun saveImageUrlToFirestore(uid: String, imageUrl: String) {
        firestore.collection("USERS").document(uid)
            .update("profilePicUrl", imageUrl)
            .addOnSuccessListener {
                Toast.makeText(this, "Image uploaded successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save URL: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }
}
