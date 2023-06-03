package com.example.v3

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

class AboutAppActivity : AppCompatActivity() {
    lateinit var drawerLayout: DrawerLayout
    lateinit var actionBarDrawerToggle: ActionBarDrawerToggle

    fun showToast(context: Context = applicationContext, message: String, duration: Int = Toast.LENGTH_LONG) {
        Toast.makeText(context, message, duration).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_app)

        //inicijalizacija svih elemenata UI-a
        // drawer layout instance to toggle the menu icon to open
        // drawer and back button to close drawer
        drawerLayout = findViewById(R.id.my_drawer_layout)
        actionBarDrawerToggle = ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close)

        // pass the Open and Close toggle for the drawer layout listener
        // to toggle the button
        drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()

        // to make the Navigation drawer icon always appear on the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //navigation drawer akcije na klik
        val navigationView = findViewById<NavigationView>(R.id.nav_view) //Assuming you have a NavigationView with id 'nav_view'
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.singleConversion -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
                R.id.aboutApp -> {
                    val intent = Intent(this, AboutAppActivity::class.java)
                    startActivity(intent)
                }
                R.id.exitApp -> {
                    finishAffinity()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START) // Assuming 'drawerLayout' is the ID of your DrawerLayout
            true
        }

    }

    // override the onOptionsItemSelected(), function to implement, the item click listener callback, to open and close the navigation, drawer when the icon is clicked
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            true
        } else super.onOptionsItemSelected(item)
    }

}