package co.wangun.pnmfr.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import co.wangun.pnmfr.R
import co.wangun.pnmfr.utils.SessionManager
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException


class MainActivity : AppCompatActivity() {

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

        // create record folder and .nomedia
        nomedia()

        // debug - set username to PNMUser1
        sessionManager?.putName(getString(R.string.user))
    }

    private fun nomedia() {

        // create folder .record
        val path = applicationInfo.dataDir + "/.rec"
        val appDir = File(path)
        appDir.mkdirs()
        sessionManager?.putPath(path)
        Log.d("RRR", sessionManager?.getPath().toString())

        // create .nomedia file
        val file = File(path, ".nomedia")
        try {
            file.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // debug
        Log.d("MainActivity", path)
        /*Environment.getExternalStorageDirectory().absolutePath + getString(R.string.app_name)*/
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

        username.setOnClickListener {

            val editText = EditText(this)
            AlertDialog.Builder(this)
                .setMessage("Masukkan Username")
                .setView(editText)
                .setPositiveButton("Simpan") { _, _ ->
                    val name = editText.text.toString()
                    if (name.isNotBlank()) {
                        sessionManager?.putName(name)
                    }
                    Toast.makeText(
                            this,
                            "Username berhasil disimpan dengan nama '$name'", Toast.LENGTH_SHORT
                        )
                        .show()
                }
                .setNegativeButton("Batal") { _, _ -> }
                .create()
                .show()
        }

        register.setOnClickListener {

            val intent = Intent(this, FaceActivity::class.java)
            intent.putExtra("from", "register")
            startActivity(intent)
        }

        recognize.setOnClickListener {

            // update loc
            reqLoc()

            // init intent
            val intent = Intent(this, FaceActivity::class.java)

            // check if there is a face in local storage
            val file = File(sessionManager?.getPath(), "register.jpg")

            // put code here to check if there is any registered face for user in server

            // start intent if gps ready
            if (sessionManager?.getLoc("lat") == "No Lat") {
                Toast.makeText(
                        this,
                        "GPS masih dalam proses mencari koordinat...",
                        Toast.LENGTH_LONG
                    )
                    .show()
                Log.d("MA", sessionManager?.getLoc("full").toString())
            } else {
                // set recognize or register for the next activity
                if (file.exists()) {
                    intent.putExtra("from", "recognize")
                    startActivity(intent)
                } else {
                    AlertDialog.Builder(this)
                        .setMessage("Wajah kamu belum pernah terdaftar sebelumnya")
                        .setPositiveButton("Daftar") { _, _ ->
                            intent.putExtra("from", "register")
                            startActivity(intent)
                        }
                        .setNegativeButton("Lain Kali") { _, _ -> }
                        .create().show()
                }
            }
        }

        clear.setOnClickListener {

            // remove local face
            val file = File(sessionManager?.getPath(), "register.jpg")
            if (file.exists()) {
                file.delete()
            }

            // clear session
            sessionManager?.clearSession()
            recreate()
            Toast.makeText(
                    this,
                    "Foto dan username berhasil dihapus",
                    Toast.LENGTH_SHORT
                )
                .show()
        }
    }

    private fun requestPermission() {

        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        ActivityCompat.requestPermissions(this, permissions, 100)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
            requestCode == 100
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
            requestPermission()
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
