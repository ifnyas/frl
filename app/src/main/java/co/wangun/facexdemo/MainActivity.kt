package co.wangun.facexdemo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import co.wangun.facexdemo.utils.SessionManager
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val request = 100

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sessionManager = SessionManager(this)
        checkPermission()

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
            sessionManager.clearSession()
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
}
