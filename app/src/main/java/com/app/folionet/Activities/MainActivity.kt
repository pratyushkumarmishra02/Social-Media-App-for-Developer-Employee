package com.app.folionet.Activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.app.folionet.Fragments.*
import com.app.folionet.R
import com.ismaeldivita.chipnavigation.ChipNavigationBar

class MainActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private var exitDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Full screen window status bar
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        val bottomNav = findViewById<ChipNavigationBar>(R.id.bottom_nav)
        drawerLayout = findViewById(R.id.drawer_layout1)

        if (savedInstanceState == null) {
            bottomNav.setItemSelected(R.id.home, true)
            navigateTo(DashboardFragment())
        }

        bottomNav.setOnItemSelectedListener { id ->
            when (id) {
                R.id.fav -> navigateTo(FavFragment())
                R.id.reel -> navigateTo(ReelFragment())
                R.id.profile -> navigateTo(ProfileFragment())
                R.id.create -> {
                    val prev = bottomNav.id
                    showCreateDialog (prev,bottomNav)
                }
                R.id.home -> navigateTo(DashboardFragment())
                else -> navigateTo(DashboardFragment())
            }
            true
        }
    }

    private fun navigateTo(f: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, f)
            .commit()
    }

    private fun showCreateDialog(prev:Int,bottomNav: ChipNavigationBar) {
        val dialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.creationlayout)
            window?.apply {
                setLayout(MATCH_PARENT, WRAP_CONTENT)
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setGravity(Gravity.BOTTOM)
            }
            setOnDismissListener { bottomNav.setItemSelected(prev,true) }
        }

        dialog.findViewById<LinearLayout>(R.id.createPost).setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this@MainActivity, ImageSectionActivity::class.java)
            startActivity(intent)
        }
        dialog.findViewById<LinearLayout>(R.id.createReel).setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this@MainActivity, ReelPickActivity::class.java)
            startActivity(intent)
        }
        dialog.findViewById<LinearLayout>(R.id.createProject).setOnClickListener {
            dialog.dismiss()
            Toast.makeText(this, "Create project", Toast.LENGTH_SHORT).show()
        }
        dialog.findViewById<ImageView>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
            bottomNav.setItemSelected(prev)
        }

        dialog.show()
    }

    fun toggleDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }


    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        if (::drawerLayout.isInitialized && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            showExitDialog()
        }
    }

    private fun showExitDialog() {
        exitDialog = AlertDialog.Builder(this)
            .setTitle("EXIT")
            .setMessage("Do you want to really exit?")
            .setPositiveButton("Exit") { _, _ -> finishAffinity() }
            .setNegativeButton("Cancel", null)
            .setCancelable(false)
            .create()
        exitDialog?.show()
    }

    override fun onPause() {
        super.onPause()
        exitDialog?.dismiss()
        exitDialog = null
    }
}
