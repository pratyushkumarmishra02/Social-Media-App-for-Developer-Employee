package com.app.folionet.Activities

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.folionet.Adapters.FrameAdapter
import com.app.folionet.Fragments.DashboardFragment
import com.app.folionet.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import java.io.ByteArrayOutputStream

class ThumbnailActivity : AppCompatActivity() {

    private lateinit var imgCoverPreview: ImageView
    private lateinit var recyclerFrames: androidx.recyclerview.widget.RecyclerView
    private lateinit var btnShare: Button
    private lateinit var btnClose: ImageView
    private lateinit var etCaption: EditText
    private lateinit var progressBar: ProgressBar

    private lateinit var videoUri: Uri
    private var selectedCoverBitmap: Bitmap? = null
    private var selectedCoverUrl: String? = null

    private val storage = FirebaseStorage.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var currentUserUid: String


    private val CHANNEL_BASE_ = "upload_channel"
    private var channelId = "$CHANNEL_BASE_${System.currentTimeMillis()}"
    private val NOTIFICATION_ID = 1001

    private val notifLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) toast("Enable notifications to track upload progress")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_thumbnail)

        imgCoverPreview = findViewById(R.id.coverImage)
        recyclerFrames = findViewById(R.id.recyclerFrames)
        btnShare = findViewById(R.id.btnShare)
        btnClose = findViewById(R.id.btnClose)
        etCaption = findViewById(R.id.etCaption)
        progressBar = findViewById(R.id.thumbnailProgressbar)

        askNotificationPermission()
        createNotificationChannel()

        intent.getStringExtra("video_uri")?.let {
            videoUri = Uri.parse(it)
        } ?: return finishWithToast("No video provided")

        setupFramePicker()
        btnClose.setOnClickListener { finish() }
        btnShare.setOnClickListener {
            if (::videoUri.isInitialized.not()) {
                toast("Please select a video first")
            } else {
                startUploadSequence()
            }
        }


        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            finishWithToast("Not logged in")
            return
        }
        currentUserUid = uid
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Upload Progress",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Progress for reel upload"
                val soundUri = Uri.parse("android.resource://$packageName/${R.raw.notify}")
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                setSound(soundUri, attrs)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun setupFramePicker() {
        val retriever = MediaMetadataRetriever().apply { setDataSource(this@ThumbnailActivity, videoUri) }
        val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
        val interval = dur * 1000 / 10
        val frames = List(10) { i ->
            retriever.getFrameAtTime(i * interval, MediaMetadataRetriever.OPTION_CLOSEST)
        }.filterNotNull()
        retriever.release()

        recyclerFrames.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerFrames.adapter = FrameAdapter(frames) { bmp ->
            imgCoverPreview.setImageBitmap(bmp)
            selectedCoverBitmap = bmp
        }
    }

    private fun startUploadSequence() {
        if (videoUri == null) {
            toast("Please select a video first")
            return
        }

        btnShare.isEnabled = false
        progressBar.visibility = View.VISIBLE
        showProgressNotification(0)
        selectedCoverBitmap?.let { bmp ->
            uploadCover(bmp) { url -> selectedCoverUrl = url; uploadVideoWithProgress() }
        } ?: uploadVideoWithProgress()
    }

    private fun uploadCover(bitmap: Bitmap, cb: (String?) -> Unit) {
        val data = ByteArrayOutputStream().apply { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, this) }.toByteArray()
        val ref = storage.reference.child("reel_covers/$currentUserUid/${System.currentTimeMillis()}.jpg")
        val task = ref.putBytes(data)
        task.addOnProgressListener { snapshot ->
            val totale = snapshot.totalByteCount
            val transferred = snapshot.bytesTransferred
            val progressPercent = ((transferred * 0.5) / totale * 100).toInt()
            updateProgressNotification(progressPercent)
        }.addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { cb(it.toString()) }.addOnFailureListener { cb(null) }
        }.addOnFailureListener { cb(null) }
    }

    private fun uploadVideoWithProgress() {
        val ref = storage.reference.child("reels/$currentUserUid/${System.currentTimeMillis()}.mp4")
        val task = ref.putFile(videoUri)
        task.addOnProgressListener { snapshot ->
            val totale = snapshot.totalByteCount
            val transferred = snapshot.bytesTransferred
            val progressPercent = (50 + (transferred * 0.5 / totale * 100)).toInt()
            updateProgressNotification(progressPercent)
        }.addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { saveToFirestore(it.toString()) }
        }.addOnFailureListener {
            dismissNotification(); toast("Video upload failed"); btnShare.isEnabled = true
        }
    }

    private fun saveToFirestore(videoUrl: String) {
        val notifMgr = getSystemService(NotificationManager::class.java)
        if (!notifMgr.areNotificationsEnabled()) showEnableNotificationsDialog()
        val data = mapOf(
            "videoUrl" to videoUrl,
            "coverUrl" to (selectedCoverUrl ?: ""),
            "caption" to etCaption.text.toString(),
            "userId" to currentUserUid,
            "timestamp" to FieldValue.serverTimestamp()
        )
        firestore.collection("REELS")
            .add(data)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                updateProgressNotificationComplete()
                val intent = Intent(this@ThumbnailActivity, MainActivity::class.java)
                startActivity(intent)

            }
            .addOnFailureListener {
                dismissNotification(); toast("Firestore save failed: ${it.message}"); btnShare.isEnabled = true
            }
    }


    private fun updateProgressNotificationComplete() {
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification_)
            .setContentTitle("Upload Successful")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID, builder.build())
    }

    private fun showProgressNotification(progress: Int) {
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification_)
            .setContentTitle("Uploading Reel")
            .setContentText("Progress: $progress%")
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(100, progress, false)

        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, builder.build())
    }

    private fun updateProgressNotification(progress: Int) = showProgressNotification(progress)

    private fun dismissNotification() {
        getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
    }

    private fun showEnableNotificationsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Enable Notifications")
            .setMessage("Enable notifications in Settings to track upload progress.")
            .setPositiveButton("Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                })
            }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun finishWithToast(msg: String) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); finish() }
    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
