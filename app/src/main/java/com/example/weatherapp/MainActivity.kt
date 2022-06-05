package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
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
import com.example.weatherapp.model.WeatherResponse
import com.example.weatherapp.network.WeatherService
import com.google.android.gms.location.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import retrofit.*

class MainActivity : AppCompatActivity() {

    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    private var mProgressDialog: Dialog? = null

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
            getLocationWeatherDetails(latitude,longitude)
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

    private fun getLocationWeatherDetails(latitude: Double, longitude: Double){
        if(Constant.isNetworkAvailable(this)){
            val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl(Constant.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service : WeatherService =
                retrofit.create<WeatherService>(WeatherService::class.java)

            val listCall: Call<WeatherResponse> = service.getWeather(
                latitude, longitude, Constant.METRIC_UNIT, Constant.APP_ID
            )
            showCustomProgressDialog()
            listCall.enqueue(object : Callback<WeatherResponse>{
                override fun onResponse(response: Response<WeatherResponse>?, retrofit: Retrofit?) {
                    if(response!!.isSuccess){

                        hideProgressDialog()

                        val weatherList: WeatherResponse = response.body()
                        Log.i("Response Result","$weatherList")
                    }else{
                        val rc = response.code()
                        when(rc){
                            400 ->{
                                Log.e("Error 400","Bad Connection")
                            }
                            404 ->{
                                Log.e("Error 404","Not Found")
                            }else ->{
                                Log.e("Error","Generic Error")
                            }
                        }
                    }
                }

                override fun onFailure(t: Throwable?) {

                    hideProgressDialog()

                    Log.e("Error",t!!.message.toString())
                }

            })

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

    private fun showCustomProgressDialog(){
        mProgressDialog = Dialog(this)
        mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)
        mProgressDialog!!.show()
    }

    private fun hideProgressDialog(){
        if(mProgressDialog != null){
            mProgressDialog!!.dismiss()
        }
    }

}