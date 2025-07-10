package com.app.folionet.Activities

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.folionet.Adapters.PreviewAdapter
import com.app.folionet.Domains.UriDC
import com.app.folionet.R
import com.google.android.material.slider.RangeSlider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.net.toUri
import com.app.folionet.Fragments.DashboardFragment
import com.google.firebase.auth.FirebaseAuth

class NextUploadActivity : AppCompatActivity() {

    private lateinit var recyclerSelectedMedia: RecyclerView
    private val selectedUris = mutableListOf<UriDC>()
    private lateinit var btnPost: TextView
    private lateinit var btnClose: ImageView
    private lateinit var captionInput: EditText
    private lateinit var musicSection: LinearLayout
    private lateinit var musicCard: CardView
    private lateinit var musicBackground: ImageView
    private lateinit var songTitle: TextView
    private lateinit var songArtist: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnPlayPause: ImageView
    private lateinit var audioSeekBar: SeekBar
    private lateinit var audioPlayerLayout: LinearLayout
    private lateinit var rangeSlider: RangeSlider
    private var selectedStart = 0
    private var selectedEnd = 0

    private var selectedAudioUri: Uri? = null
    private var selectedAudioName: String? = null
    private var selectedArtist: String? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var updateSeekBarRunnable: Runnable? = null
    private val handler = Handler()

    private val CHANNEL_BASE_ = "upload_channel"
    private var CHANNEL_ID = "$CHANNEL_BASE_${System.currentTimeMillis()}"
    private val NOTIFICATION_ID = 1001

    private val audioPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            selectedAudioUri = it
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)

            val cursor = contentResolver.query(it, null, null, null, null)
            cursor?.use { c ->
                val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (c.moveToFirst()) {
                    selectedAudioName = c.getString(nameIndex)
                    songTitle.text = selectedAudioName
                }
            }

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(this, it)
                selectedArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            } catch (e: Exception) {
                selectedArtist = null
            } finally {
                retriever.release()
            }

            songArtist.text = selectedArtist ?: "Unknown Artist"
            musicCard.visibility = View.VISIBLE

            playSelectedAudio(it)
        }
    }

    private val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return@run
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_next_upload)

        btnPost = findViewById(R.id.btnPost)
        btnClose = findViewById(R.id.btnClose1)
        captionInput = findViewById(R.id.captionInput)
        musicSection = findViewById(R.id.musicSection)
        musicCard = findViewById(R.id.musicCard)
        musicBackground = findViewById(R.id.musicBackground)
        songTitle = findViewById(R.id.songTitle)
        songArtist = findViewById(R.id.songArtist)
        recyclerSelectedMedia = findViewById(R.id.recyclerSelectedMedia)
        progressBar = findViewById(R.id.progressBar_)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        audioSeekBar = findViewById(R.id.audioSeekBar)
        audioPlayerLayout = findViewById(R.id.audioPlayerLayout)
        rangeSlider = findViewById(R.id.rangeSlider)

        recyclerSelectedMedia.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        handleIntentImages()

        btnClose.setOnClickListener { finish() }

        musicSection.setOnClickListener {
            audioPickerLauncher.launch(arrayOf("audio/*"))
        }

        btnPost.setOnClickListener {
            if (selectedUris.isEmpty()) {
                Toast.makeText(this, "No media selected", Toast.LENGTH_SHORT).show()
            } else {
                uploadPost()
            }
        }


        // Full screen window status bar
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        btnPlayPause.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                isPlaying = false
            } else {
                mediaPlayer?.seekTo(selectedStart)
                mediaPlayer?.start()
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
                isPlaying = true
                startSeekBarUpdater()
            }
        }
        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
        }
    }

    private fun playSelectedAudio(uri: Uri) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(this@NextUploadActivity, uri)
            prepare()
        }

        val maxDuration = mediaPlayer?.duration ?: 30000
        audioSeekBar.max = maxDuration
        audioSeekBar.progress = 0

        btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
        isPlaying = false

        rangeSlider.valueFrom = 0f
        rangeSlider.valueTo = maxDuration.toFloat()
        rangeSlider.values = listOf(0f, minOf(30000f, maxDuration.toFloat()))

        val values = rangeSlider.values
        selectedStart = values[0].toInt()
        selectedEnd = values[1].toInt()

        rangeSlider.addOnChangeListener { slider, _, _ ->
            val updated = slider.values
            selectedStart = updated[0].toInt()
            selectedEnd = updated[1].toInt()
        }

        audioSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) mediaPlayer?.seekTo(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        mediaPlayer?.setOnCompletionListener {
            if (it.currentPosition >= selectedEnd) {
                it.pause()
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                isPlaying = false
                updateSeekBarRunnable?.let { handler.removeCallbacks(it) }
            }
        }
    }

    private fun startSeekBarUpdater() {
        updateSeekBarRunnable = object : Runnable {
            override fun run() {
                mediaPlayer?.let {
                    val current = it.currentPosition
                    audioSeekBar.progress = current
                    if (current >= selectedEnd) {
                        it.pause()
                        btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                        isPlaying = false
                        return
                    }
                    handler.postDelayed(this, 300)
                }
            }
        }
        handler.post(updateSeekBarRunnable!!)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
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

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun showProgressNotification(progress: Int) {
        Log.d("UPLOAD", "Showing progress: $progress%")

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Uploading Post")
            .setContentText("Progress: $progress%")
            .setSmallIcon(R.drawable.ic_notification_)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(100, progress, false)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun dismissNotification() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun uploadPost() {
        if (selectedUris.isEmpty()) {
            Toast.makeText(this, "Select at least one media", Toast.LENGTH_SHORT).show()
            return
        }

        createNotificationChannel()
        val notificationManager = getSystemService(NotificationManager::class.java)
        if (notificationManager.areNotificationsEnabled()) {
            showProgressNotification(0)
        }else{
            AlertDialog.Builder(this)
                .setTitle("Notifications Disabled")
                .setMessage("Please enable notifications in settings to track upload progress.")
                .setPositiveButton("Open Settings") { _, _ ->
                    openAppNotificationSettings()
                }
                .setNegativeButton("Cancel", null)
                .show()

        }

        btnPost.isEnabled = false
        progressBar.visibility = View.VISIBLE

        val caption = captionInput.text.toString()
        val storage = FirebaseStorage.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val uploadedMediaUrls = mutableListOf<String>()

        var totalUploads = selectedUris.size + if (selectedAudioUri != null) 1 else 0
        var completedUploads = 0

        fun updateProgress() {
            completedUploads++
            val progress = (completedUploads * 100) / totalUploads
            showProgressNotification(progress)
        }

        selectedUris.forEach { uriDC ->
            val mediaRef = storage.reference.child("posts/$currentUserUid/${UUID.randomUUID()}")
            mediaRef.putFile(uriDC.uri).continueWithTask {
                if (!it.isSuccessful) throw it.exception ?: Exception("Upload failed")
                mediaRef.downloadUrl
            }.addOnSuccessListener { url ->
                uploadedMediaUrls.add(url.toString())
                updateProgress()

                if (uploadedMediaUrls.size == selectedUris.size) {
                    if (selectedAudioUri != null) {
                        val audioRef = storage.reference.child("audio/$currentUserUid/${UUID.randomUUID()}.mp3")
                        audioRef.putFile(selectedAudioUri!!).continueWithTask {
                            if (!it.isSuccessful) throw it.exception ?: Exception("Audio upload failed")
                            audioRef.downloadUrl
                        }.addOnSuccessListener { audioUrl ->
                            updateProgress()
                            uploadPostToFirestore(firestore, caption, uploadedMediaUrls, audioUrl.toString())
                        }.addOnFailureListener {
                            dismissNotification()
                            progressBar.visibility = View.GONE
                            Toast.makeText(this, "Audio upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        uploadPostToFirestore(firestore, caption, uploadedMediaUrls, null)
                    }
                }
            }.addOnFailureListener {
                dismissNotification()
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Media upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadPostToFirestore(
        firestore: FirebaseFirestore,
        caption: String,
        mediaUrls: List<String>,
        audioUrl: String?
    ) {
        val currentDate = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        val createdAtFormatted = dateFormat.format(currentDate)
        val post = hashMapOf(
            "caption" to caption,
            "mediaUrls" to mediaUrls,
            "audioUrl" to audioUrl,
            "songTitle" to selectedAudioName,
            "songArtist" to selectedArtist,
            "startMs" to selectedStart,
            "endMs" to selectedEnd,
            "userId" to currentUserUid,
            "timestamp" to System.currentTimeMillis(),
            "createdDateFormatted" to createdAtFormatted
        )

        firestore.collection("Posts")
            .add(post)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                updateProgressNotificationComplete()
                Toast.makeText(this, "Post uploaded", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@NextUploadActivity, MainActivity::class.java)
                startActivity(intent)
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                dismissNotification()
                Toast.makeText(this, "Post upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateProgressNotificationComplete() {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_)
            .setContentTitle("Upload Successful")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID, builder.build())
    }

    private fun handleIntentImages() {
        val isMultiple = intent.getBooleanExtra("is_multiple", false)
        if (isMultiple) {
            val uriList = intent.getParcelableArrayListExtra<Uri>("selected_uris")
            uriList?.forEach { uri -> selectedUris.add(UriDC(uri)) }
        } else {
            intent.getParcelableExtra<Uri>("selected_uri")?.let {
                selectedUris.add(UriDC(it))
            }
        }

        if (selectedUris.isNotEmpty()) {
            recyclerSelectedMedia.adapter = PreviewAdapter(selectedUris)
        }
    }

    private fun openAppNotificationSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
        }
        startActivity(intent)
    }


    override fun onDestroy() {
        super.onDestroy()
        updateSeekBarRunnable?.let { handler.removeCallbacks(it) }
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
