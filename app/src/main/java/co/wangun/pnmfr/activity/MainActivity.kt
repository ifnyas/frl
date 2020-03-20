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
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import co.wangun.pnmfr.R
import co.wangun.pnmfr.api.ApiClient
import co.wangun.pnmfr.api.ApiService
import co.wangun.pnmfr.utils.BmpConverter
import co.wangun.pnmfr.utils.SessionManager
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.util.*


class MainActivity : AppCompatActivity() {

    private var locationManager: LocationManager? = null
    private var sessionManager: SessionManager? = null
    private var apiService: ApiService? = null

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

        // init view
        initView()
    }

    private fun initView() {
        // set username view
        val name = sessionManager?.getName()
        if (name == "No Name") {
            loggedin.text = "Masukkan username terlebih dahulu"
            recognize.visibility = View.GONE
        } else {
            loggedin.text = "Logged in as\n$name"
            recognize.visibility = View.VISIBLE
        }

        // set update visibility
        val file = File(sessionManager?.getPath(), "register.jpg")
        if (file.exists()) {
            update.visibility = View.VISIBLE
        } else {
            update.visibility = View.GONE
        }
    }

    private fun checkFace() {

        // init API Service
        apiService = ApiClient.client.create(ApiService::class.java)

        // init values
        val auth = getString(R.string.auth)
        val username = sessionManager?.getName()

        // post request
        apiService?.getUser(auth, username!!)
            ?.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    if (response.isSuccessful) {

                        try {
                            val jsonRESULTS = JSONObject(response.body()!!.string())
                            val jsonMESSAGE = jsonRESULTS.getString("message")

                            if (jsonMESSAGE == "Data fetched successfully") {
                                val jsonRECORDS = jsonRESULTS.getJSONObject("records")
                                val jsonIMG = jsonRECORDS.getString("img_url")
                                BmpConverter.downloadImage(jsonIMG, applicationContext)

                                Toast.makeText(
                                    applicationContext,
                                    "DB muka berhasil di-download",
                                    Toast.LENGTH_LONG
                                ).show()

                                initView()
                            } else {
                                Toast.makeText(
                                    applicationContext,
                                    "Tidak ada DB muka tersimpan pada server",
                                    Toast.LENGTH_LONG
                                ).show()

                                initView()
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    t.printStackTrace()
                }
            })
    }

    private fun nomedia() {

        // create folder .record
        val path = applicationInfo.dataDir + "/.rec"
        val appDir = File(path)
        appDir.mkdirs()
        sessionManager?.putPath(path)

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

                    val name = editText.text
                        .toString()
                        .toLowerCase(Locale.getDefault())
                        .replace(" ", "")

                    if (name.isNotBlank()) {
                        removeFace()
                        sessionManager?.putName(name) // save name
                        checkFace() // check if username have face registered in server
                    } else {
                        Toast.makeText(
                                this,
                                "Nama tidak boleh kosong",
                                Toast.LENGTH_SHORT
                            )
                            .show()
                    }
                }
                .setNegativeButton("Batal") { _, _ -> }
                .create()
                .show()
        }

        update.setOnClickListener {

            val intent = Intent(this, FaceActivity::class.java)
            intent.putExtra("from", "register")
            startActivity(intent)
        }

        recognize.setOnClickListener {

            // update loc
            reqLoc()

            // init intent
            val intent = Intent(this, FaceActivity::class.java)

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
                // check if there is a face in local storage
                val file = File(sessionManager?.getPath(), "register.jpg")

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

            removeFace()

            // clear session
            sessionManager?.clearSession()
            recreate()
            Toast.makeText(
                    this,
                    "Data berhasil di-reset",
                    Toast.LENGTH_SHORT
                )
                .show()
        }
    }

    private fun removeFace() {
        // remove local face
        val file = File(sessionManager?.getPath(), "register.jpg")
        if (file.exists()) {
            file.delete()
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
