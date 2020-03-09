package co.wangun.pnmfr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import co.wangun.pnmfr.utils.SessionManager
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val request = 100
    private var locationManager: LocationManager? = null
    private var sessionManager: SessionManager? = null


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // init values
        sessionManager = SessionManager(this)

        // init fun
        initFun()
    }

    private fun initFun() {
        // init button
        initBtn()

        // check all permissions
        checkPermission()

        // request location updates
        reqLoc()
    }

    private fun reqLoc() {

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?
        var gpsEnabled = false
        var networkEnabled = false

        try {
            gpsEnabled = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (ex: SecurityException) {
        }

        try {
            networkEnabled = locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (ex: SecurityException) {
        }

        when {
            !gpsEnabled && !networkEnabled -> {
                AlertDialog.Builder(this)
                    .setMessage("GPS belum aktif")
                    .setPositiveButton("Pengaturan") { _, _ ->
                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        finish()
                    }
                    .setNegativeButton("Keluar") { _, _ -> finish() }
                    .create().show()
            }
            networkEnabled -> {
                try {
                    locationManager?.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        0L, 0f, locationListener
                    )
                } catch (ex: SecurityException) {
                }
            }
            else -> {
                try {
                    locationManager?.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        0L, 0f, locationListener
                    )
                } catch (ex: SecurityException) {
                }
            }
        }
    }

    private fun initBtn() {
        register.setOnClickListener {
            val intent = Intent(this, FaceDetectActivity::class.java)
            intent.putExtra("from", "register")
            startActivity(intent)
        }

        recognize.setOnClickListener {
            val intent = Intent(this, FaceDetectActivity::class.java)
            intent.putExtra("from", "recognize")
            startActivity(intent)
        }

        clear.setOnClickListener {
            sessionManager?.clearSession()
        }
    }

    private fun requestPermission(request: Int) {

        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        ActivityCompat.requestPermissions(this, permissions, request)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
            requestCode == request
        ) {
            return
        } else {
            finish()
        }
    }

    private fun checkPermission() {
        val rc: Int = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        )
        val ri: Int = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.INTERNET
        )
        val rw: Int = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        val rl: Int = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (rc == PackageManager.PERMISSION_DENIED ||
            ri == PackageManager.PERMISSION_DENIED ||
            rw == PackageManager.PERMISSION_DENIED ||
            rl == PackageManager.PERMISSION_DENIED
        ) {
            requestPermission(request)
        }
    }

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            sessionManager?.putLoc(location.latitude.toString(), location.longitude.toString())
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }
}
