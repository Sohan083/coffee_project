package com.example.coffeeproject

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.coffeeproject.model.User
import com.example.coffeeproject.model.User.Companion.user
import com.example.coffeeproject.ui.login.LoginActivity
import com.example.coffeeproject.ui.outletactivity.OutletUpdateViewModel
import com.example.coffeeproject.utils.CustomUtility
import com.example.coffeeproject.utils.GPSLocation
import com.example.coffeeproject.utils.StaticTags.Companion.GPS_REQUEST
import com.example.coffeeproject.utils.StaticTags.Companion.LOCATION_REQUEST
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.internal.NavigationMenuView
import com.google.android.material.navigation.NavigationBarMenuView

import com.google.android.material.navigation.NavigationView
import java.util.*

class MainActivity : AppCompatActivity() {

    private var isGPSEnabled: Boolean = false
    private lateinit var appBarConfiguration: AppBarConfiguration

    private var gpsLocation: GPSLocation? = null
    private lateinit var navController: NavController
    var doubleBackToExitPressedOnce = false
    var outLetViewModel = OutletUpdateViewModel()
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)



        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.title = "Outlet Registration"

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val appBarConfiguration = AppBarConfiguration(topLevelDestinationIds = setOf(
            R.id.outletRegistrationFragment,
            R.id.attendanceFragment,
            R.id.logoutFragment,
            R.id.outletActivityFragment,
        ),drawerLayout)
        findViewById<Toolbar>(R.id.toolbar)
            .setupWithNavController(navController, appBarConfiguration)
        val navigationView =
            findViewById<NavigationView>(R.id.nav_view)
        navigationView?.setupWithNavController(navController)

        menuNav = navigationView.menu


        user = User.instance
        if (user == null) {
            val sweetAlertDialog = SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
            sweetAlertDialog.setTitle("No user found please login")
            sweetAlertDialog.setConfirmButton("Ok") {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }
        val navHeaderMainView = navigationView.getHeaderView(0)
        navHeaderMainView.findViewById<TextView>(R.id.name).text = user?.name
        navHeaderMainView.findViewById<TextView>(R.id.location).text = user?.zone



        //ask for gps
        CustomUtility.haveGpsEnabled(this)




        gpsLocation = GPSLocation(this)
        gpsLocation!!.GPS_Start()
        gpsLocation!!.setLocationChangedListener(object : GPSLocation.LocationChangedListener {
            override fun locationChangeCallback(lat: String?, lon: String?, acc: String?) {
                presentLat = lat
                presentLon = lon
                presentAcc = acc
            }

        })
    }



    companion object {
        var presentLat: String? = null; var presentLon: String? = null; var presentAcc:String? = null
        var retailUserName: String? = null; var retailPassword: String? = null
        var menuNav: Menu? = null
        var isInActivity = false
    }


    override fun onBackPressed() {
        if (Objects.requireNonNull(navController.currentDestination)!!.id  == R.id.attendanceFragment) {
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed()
                return
            }
            this.doubleBackToExitPressedOnce = true
            Toast.makeText(this, "Please click BACK again to go outlet registration", Toast.LENGTH_SHORT).show()
            Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
        } else if (Objects.requireNonNull(navController.currentDestination)?.id == R.id.outletRegistrationFragment) {
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed()
                return
            }
            this.doubleBackToExitPressedOnce = true
            Toast.makeText(this, "Please click BACK again to return back", Toast.LENGTH_SHORT).show()
            Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
        }
        else if(Objects.requireNonNull(navController.currentDestination)!!.id == R.id.pictureFragment) {
            finish()
            startActivity(intent)
        }
        else if(Objects.requireNonNull(navController.currentDestination)!!.id == R.id.activitySelectionFragment){
            viewModelStore.clear()
            isInActivity = false
            super.onBackPressed()
        }
        else {
            super.onBackPressed()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GPS_REQUEST) {
                isGPSEnabled = true
                //invokeLocationAction()
            }
        }
    }
    /*

    private fun invokeLocationAction() {
        when {


            //isPermissionsGranted() -> startLocationUpdate()

            shouldShowRequestPermissionRationale() -> latLong.text = getString(R.string.permission_request)

            else -> ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_REQUEST
            )
        }
    }

     */
/*
    private fun startLocationUpdate() {
        locationViewModel.getLocationData().observe(this, Observer {
            latLong.text =  getString(R.string.latLong, it.longitude, it.latitude)
        })
    }

 */

    private fun isPermissionsGranted() =
        ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

    private fun shouldShowRequestPermissionRationale() =
        ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) && ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_REQUEST -> {
                //invokeLocationAction()
            }
        }
    }
}