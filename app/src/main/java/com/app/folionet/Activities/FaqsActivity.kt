package com.app.folionet.Activities

import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.app.folionet.R

class FaqsActivity : AppCompatActivity() {
    private lateinit var backArrow: ConstraintLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_faqs)

        //Initializing the views
        backArrow = findViewById(R.id.backArrowConstraint)

        backArrow.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

    }
}