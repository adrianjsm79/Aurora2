package com.tecsup.aurora.utils

import android.app.Activity
import android.content.Intent
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.tecsup.aurora.R
import com.tecsup.aurora.ui.activities.AboutActivity
import com.tecsup.aurora.ui.activities.HomeActivity
import com.tecsup.aurora.ui.activities.NotificationsActivity

class NavigationDrawerController(
    private val activity: Activity,
    private val drawerLayout: DrawerLayout,
    private val navigationView: NavigationView
) {

    fun setup(onLogout: () -> Unit) {

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    if (activity !is HomeActivity) {
                        val intent = Intent(activity, HomeActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        activity.startActivity(intent)
                        activity.finish()
                    }
                }
                R.id.nav_notifications -> {
                    val intent = Intent(activity, NotificationsActivity::class.java)
                    activity.startActivity(intent)
                    activity.finish()
                }
                R.id.nav_about -> {
                    val intent = Intent(activity, AboutActivity::class.java)
                    activity.startActivity(intent)
                    activity.finish()
                }
                R.id.nav_support -> {
                    val intent = Intent(activity, AboutActivity::class.java)
                    activity.startActivity(intent)
                    activity.finish()
                }
                R.id.nav_share -> {
                    shareApp()
                }
                R.id.nav_logout -> {
                    onLogout()
                }
            }

            drawerLayout.closeDrawer(GravityCompat.END)
            true
        }
    }

    fun updateHeaderUserInfo(name: String, email: String) {
        val headerView = navigationView.getHeaderView(0)
        if (headerView != null) {
            val nameText = headerView.findViewById<TextView>(R.id.text_username_nav)
            val emailText = headerView.findViewById<TextView>(R.id.text_email_nav)

            nameText.text = name
            emailText.text = email
        }
    }

    fun openDrawer() {
        drawerLayout.openDrawer(GravityCompat.END)
    }


    private fun shareApp() {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "¡Descarga Aurora para mantenerte seguro! https://github.com/adrianjsm79/Aurora2/releases")
            type = "text/plain"
        }
        activity.startActivity(Intent.createChooser(sendIntent, "Compartir Aurora con..."))
    }

}