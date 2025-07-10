package com.app.folionet.Activities

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.folionet.Adapters.MediaAdapter
import com.app.folionet.Domains.MediaItem
import com.app.folionet.R
import com.bumptech.glide.Glide
import com.example.agrihub.GridSpacingItemDecoration
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity
import java.io.File

class ImageSectionActivity : AppCompatActivity() {

    private lateinit var btnClose: ImageView
    private lateinit var imagePreview: ImageView
    private lateinit var btnNext: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnResize: ImageView
    private lateinit var btnMultiSelect: ImageView
    private lateinit var spinnerAlbums: Spinner
    private lateinit var videoPreview: VideoView
    private lateinit var playPauseBtn :ImageView

    private val mediaList = mutableListOf<MediaItem>()
    private lateinit var adapter: MediaAdapter

    private var isMultiSelectMode = false
    private var selectedAlbum: String? = null
    private val albumList = mutableListOf<String>()
    private val albumMap = mutableMapOf<String, String>()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            loadAlbums()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val UCROP_REQUEST_CODE = 69
    private var currentImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_section)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        btnClose = findViewById(R.id.btnClose)
        imagePreview = findViewById(R.id.imagePreview)
        btnNext = findViewById(R.id.btnNext)
        recyclerView = findViewById(R.id.gridThumbnails)
        btnResize = findViewById(R.id.btnResize)
        btnMultiSelect = findViewById(R.id.btnMultiSelect)
        spinnerAlbums = findViewById(R.id.spinnerAlbums)
        videoPreview = findViewById(R.id.videoPreview)
        playPauseBtn = findViewById(R.id.btnPlayPause1)


        playPauseBtn.setOnClickListener {
            if (videoPreview.isPlaying) {
                videoPreview.pause()
                playPauseBtn.setImageResource(android.R.drawable.ic_media_play)
                playPauseBtn.visibility = View.VISIBLE
            } else {
                videoPreview.start()
                playPauseBtn.setImageResource(android.R.drawable.ic_media_pause)
                playPauseBtn.visibility = View.GONE
            }
        }

        videoPreview.setOnPreparedListener { mp ->
            mp.isLooping = true
            videoPreview.start()
            playPauseBtn.setImageResource(android.R.drawable.ic_media_pause)
            playPauseBtn.visibility = View.GONE
        }

        videoPreview.setOnClickListener {
            if (videoPreview.isPlaying) {
                videoPreview.pause()
                playPauseBtn.setImageResource(android.R.drawable.ic_media_play)
                playPauseBtn.visibility = View.VISIBLE
            }
        }

        btnClose.setOnClickListener { finish() }

        btnNext.setOnClickListener {
            videoPreview.pause()
            val intent = Intent(this, NextUploadActivity::class.java)

            if (isMultiSelectMode) {
                val selectedItems = adapter.getSelectedItems()
                if (selectedItems.isEmpty()) {
                    Toast.makeText(this, "No media selected", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val uriList = ArrayList<Uri>()
                for (item in selectedItems) uriList.add(item.uri)

                intent.putParcelableArrayListExtra("selected_uris", uriList)
                intent.putExtra("is_multiple", true)
            } else {
                if (currentImageUri == null) {
                    Toast.makeText(this, "No media selected", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                intent.putExtra("selected_uri", currentImageUri)
                intent.putExtra("is_multiple", false)
            }

            startActivity(intent)
        }

        btnResize.setOnClickListener {
            val sourceUri = currentImageUri
            if (sourceUri == null) {
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val destinationUri = Uri.fromFile(File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg"))

            val options = UCrop.Options().apply {
                setFreeStyleCropEnabled(true)
                setAllowedGestures(UCropActivity.SCALE, UCropActivity.NONE, UCropActivity.SCALE)
                setMaxScaleMultiplier(10f)
            }

            UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1f, 1f)
                .withOptions(options)
                .start(this)
        }

        btnMultiSelect.setOnClickListener {
            isMultiSelectMode = !isMultiSelectMode
            adapter.enableMultiSelectMode(isMultiSelectMode)

            if (isMultiSelectMode) {
                val selectedItems = adapter.getSelectedItems()

                if (selectedItems.isEmpty() && mediaList.isNotEmpty()) {
                    val toSelect = mediaList.find { it.uri == currentImageUri } ?: mediaList[0]
                    adapter.selectItem(toSelect)
                }

                adapter.getSelectedItems().lastOrNull()?.let { showPreview(it) }
            }
        }

        adapter = MediaAdapter(
            mediaList,
            onItemClick = { mediaItem ->
                if (!isMultiSelectMode) showPreview(mediaItem)
            },
            onSelectionChanged = {
                val selected = adapter.getSelectedItems()
                if (isMultiSelectMode && selected.isNotEmpty()) {
                    showPreview(selected.last())
                } else if (isMultiSelectMode && selected.isEmpty()) {
                    imagePreview.setImageResource(R.drawable.no_profile_pic)
                    imagePreview.visibility = View.VISIBLE
                    videoPreview.visibility = View.GONE
                }
            }
        )

        recyclerView.layoutManager = GridLayoutManager(this, 3)
        recyclerView.adapter = adapter

        spinnerAlbums.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val album = albumList[position]
                selectedAlbum = albumMap[album]
                loadMedia(selectedAlbum)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isNotEmpty()) {
            requestPermissionLauncher.launch(needed.toTypedArray())
        } else {
            loadAlbums()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            val resultUri = data?.let { UCrop.getOutput(it) }
            resultUri?.let {
                currentImageUri = it
                Glide.with(this).load(it).centerCrop().into(imagePreview)
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = data?.let { UCrop.getError(it) }
            Toast.makeText(this, "Crop error: ${cropError?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadAlbums() {
        albumList.clear()
        albumMap.clear()
        val projection = arrayOf(
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_ID
        )
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val cursor: Cursor? = contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            val nameCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val idCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val seen = mutableSetOf<String>()
            while (it.moveToNext()) {
                val name = it.getString(nameCol)
                val id = it.getString(idCol)
                if (!seen.contains(id)) {
                    albumList.add(name)
                    albumMap[name] = id
                    seen.add(id)
                }
            }
        }
        if (albumList.isEmpty()) albumList.add("Recents")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, albumList)
        spinnerAlbums.adapter = adapter
        selectedAlbum = albumMap[albumList.first()]
        loadMedia(selectedAlbum)
    }

    private fun loadMedia(bucketId: String?) {
        mediaList.clear()
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Images.Media.BUCKET_ID
        )
        val selectionBuilder = StringBuilder()
        val selectionArgs = mutableListOf<String>()

        selectionBuilder.append("(")
        selectionBuilder.append("${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ")
        selectionBuilder.append("${MediaStore.Files.FileColumns.MEDIA_TYPE}=?)")
        selectionArgs.add(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString())
        selectionArgs.add(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())

        if (bucketId != null) {
            selectionBuilder.append(" AND ${MediaStore.Images.Media.BUCKET_ID}=?")
            selectionArgs.add(bucketId)
        }

        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        val queryUri = MediaStore.Files.getContentUri("external")
        val cursor = contentResolver.query(
            queryUri,
            projection,
            selectionBuilder.toString(),
            selectionArgs.toTypedArray(),
            sortOrder
        )

        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val typeCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val type = it.getInt(typeCol)
                val contentUri = when (type) {
                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE ->
                        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO ->
                        ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    else -> null
                }
                contentUri?.let { uri -> mediaList.add(MediaItem(uri, type)) }
            }
        }
        this.adapter.notifyDataSetChanged()
        setDefaultPreview()
    }

    private fun setDefaultPreview() {
        if (mediaList.isNotEmpty()) {
            showPreview(mediaList[0])
        } else {
            imagePreview.setImageResource(R.drawable.no_profile_pic)
            imagePreview.visibility = View.VISIBLE
            videoPreview.visibility = View.GONE
        }
    }

    private fun showPreview(mediaItem: MediaItem) {
        currentImageUri = mediaItem.uri
        if (mediaItem.mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
            imagePreview.visibility = View.GONE
            videoPreview.visibility = View.VISIBLE

            playPauseBtn.visibility = View.VISIBLE
            videoPreview.setVideoURI(mediaItem.uri)
            videoPreview.seekTo(1)
            videoPreview.start()
        } else {
            videoPreview.pause()
            videoPreview.visibility = View.GONE
            imagePreview.visibility = View.VISIBLE
            Glide.with(this)
                .load(mediaItem.uri)
                .into(imagePreview)
        }
    }
}
