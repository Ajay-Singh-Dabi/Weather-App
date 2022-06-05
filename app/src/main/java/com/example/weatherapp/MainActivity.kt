package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.location.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.lang.Exception
import android.location.LocationRequest as LocationRequest1

class MainActivity : AppCompatActivity() {

    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if(!isLocationEnabled()){
            Toast.makeText(this,
                "Your location provider is turned off. Please turn it ON"
                , Toast.LENGTH_SHORT).show()

            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }else{
            Dexter.withActivity(this)
                .withPermissions(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                .withListener(object: MultiplePermissionsListener{
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if(report!!.areAllPermissionsGranted()){
                            requestLocationData()
                        }
                        if(report.isAnyPermissionPermanentlyDenied){
                            Toast.makeText(this@MainActivity,
                                "You have denied location permission. Please allow the permission",
                                Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        showRationalDialogForPermissions()
                    }
                }).onSameThread()
                .check()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData(){
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult?) {
            val mLastLocation: Location = locationResult!!.lastLocation
            val latitude = mLastLocation.latitude
            Log.i("Current Latitude","$latitude")

            val longitude = mLastLocation.longitude
            Log.i("Current Longitude","$longitude")
            getLocationWeatherDetails()
        }
    }

    private fun showRationalDialogForPermissions(){
        AlertDialog.Builder(this)
            .setMessage("It looks like you have turned off permissions, Please turn on permission")
            .setPositiveButton(
                "GO TO SETTINGS"){
                                _, _ ->
                                    try{
                                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                        val uri = Uri.fromParts("package",packageName, null)
                                        intent.data = uri
                                        startActivity(intent)
                                    }catch (e: ActivityNotFoundException){
                                        e.printStackTrace()
                                    }

            }.setNegativeButton("Cancel"){ dialog,
                                _ ->
                dialog.dismiss()
            }.show()
    }

    private fun getLocationWeatherDetails(){
        if(Constant.isNetworkAvailable(this)){
            Toast.makeText(this@MainActivity,
                "You have connected to the internet. Now you can make an api call",
                Toast.LENGTH_SHORT).show()
        }else{
            Toast.makeText(this@MainActivity,
                "No internet connection available",
                Toast.LENGTH_SHORT).show()
        }
    }

    private fun isLocationEnabled(): Boolean{

        //this will provide the access to the system location services
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

}