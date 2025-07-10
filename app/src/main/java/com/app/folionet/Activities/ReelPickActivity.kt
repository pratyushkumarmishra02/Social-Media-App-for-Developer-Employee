package com.app.folionet.Activities

import android.app.Activity
import android.content.Intent
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.app.folionet.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File

class ReelPickActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var btnAddAudio: ImageView
    private lateinit var btnNext: TextView
    private lateinit var hintForSelection: TextView
    private val AUDIO_REQUEST = 101

    private var selectedVideoUri: Uri? = null
    private var selectedAudioUri: Uri? = null

    private var audioPlayer: MediaPlayer? = null

    private var isMuted = false

    private val pickVideo = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { setupNewVideo(it) } ?: toast("No video selected")
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_reel_pick)


        playerView = findViewById(R.id.playerView)
        btnNext = findViewById(R.id.btnNext1)
        btnAddAudio = findViewById(R.id.btnAddAudio)
        hintForSelection = findViewById(R.id.hintForSelection)

        setupButtons()
    }

    private fun setupButtons() {
        findViewById<ImageView>(R.id.btnClose1).setOnClickListener { finish() }
        findViewById<FloatingActionButton>(R.id.btnFloat).setOnClickListener { pickVideo.launch("video/*") }

        btnAddAudio.setOnClickListener { toggleMute() }
        //findViewById<LinearLayout>(R.id.musicSelect).setOnClickListener { pickAudio() }

        btnNext.setOnClickListener {
            selectedVideoUri?.let { videoUri ->
                player?.pause()
                Intent(this, ThumbnailActivity::class.java).apply {
                    putExtra("video_uri", videoUri.toString())
                    selectedAudioUri?.let { putExtra("audio_uri", it.toString()) }
                }.also { startActivity(it) }
            } ?: toast("Please select a video")
        }
    }

    private fun setupNewVideo(videoUri: Uri) {
        selectedVideoUri = videoUri
        cleanUpPlayer()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(), true
            )
            .build()
            .also {
                playerView.visibility= View.VISIBLE
                playerView.player = it
                it.setMediaItem(MediaItem.fromUri(videoUri))
                it.prepare()
                it.playWhenReady = true
            }
        isMuted = false
        hintForSelection.visibility = View.GONE
        btnAddAudio.visibility=View.VISIBLE
        btnAddAudio.visibility = View.VISIBLE
        toast("Video loaded")
    }

    private fun toggleMute() {
        player?.let {
            isMuted = !isMuted
            if (isMuted) {
                audioPlayer?.pause()
                it.volume = 0f
                it.pause()
                btnAddAudio.setImageResource(R.drawable.ic_sound_off)
                toast("Video muted. Select new audio.")
            } else {
                it.volume = 1f
                it.play()
                btnAddAudio.setImageResource(R.drawable.ic_sound_on)
                toast("Original audio restored.")
            }
        }
    }

//    private fun pickAudio() {
//        if (!isMuted) toast("Mute video first") else {
//            startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).apply { type = "audio/*" }, AUDIO_REQUEST)
//        }
//    }

    override fun onActivityResult(rc: Int, result: Int, d: Intent?) {
        super.onActivityResult(rc, result, d)
        if (rc == AUDIO_REQUEST && result == Activity.RESULT_OK) {
            d?.data?.let {
                selectedAudioUri = it
                compareDurationsAndPlay()
            }
        }
    }

    private fun getMediaDuration(uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(this, uri)
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            time?.toLong() ?: 0L
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        } finally {
            retriever.release()
        }
    }

    private fun compareDurationsAndPlay() {
        val videoDuration = selectedVideoUri?.let { getMediaDuration(it) } ?: 0L
        val audioDuration = selectedAudioUri?.let { getMediaDuration(it) } ?: 0L

        if (audioDuration > videoDuration) {
            toast("Selected audio is longer than the video.")
        } else {
            playAudioUri(selectedAudioUri!!)
        }
    }

    private fun playAudioUri(uri: Uri) {
        audioPlayer?.release()
        audioPlayer = MediaPlayer.create(this, uri).apply {
            isLooping = false
            setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
        }
    }

    private fun cleanUpPlayer() {
        player?.release()
        player = null
    }

    override fun onStop() {
        super.onStop()
        audioPlayer?.run { pause(); release() }
        audioPlayer = null
        cleanUpPlayer()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
