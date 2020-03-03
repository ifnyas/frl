package co.wangun.facexdemo

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import co.wangun.facexdemo.utils.SessionManager
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val RC_HANDLE_CAMERA_PERM_GRAY = 2
    private var mContext: Context? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mContext = this

        val sessionManager = SessionManager(this)
//        sessionManager.clearSession()
//        Log.d("AAA Name", sessionManager.getName()!!)
//        Log.d("AAA Face", sessionManager.getFace()!!)

        btnGray.setOnClickListener {
            val rc: Int = ActivityCompat.checkSelfPermission(
                mContext as MainActivity,
                Manifest.permission.CAMERA
            )
            val ri: Int = ActivityCompat.checkSelfPermission(
                mContext as MainActivity,
                Manifest.permission.INTERNET
            )
            val rw: Int = ActivityCompat.checkSelfPermission(
                mContext as MainActivity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )

            if (rc == PackageManager.PERMISSION_GRANTED && ri == PackageManager.PERMISSION_GRANTED && rw == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(mContext, FaceDetectGrayActivity::class.java)
                startActivity(intent)
            } else {
                requestCameraPermission(RC_HANDLE_CAMERA_PERM_GRAY)
            }
        }

        clear.setOnClickListener {
            sessionManager.clearSession()
            Log.d("CCC Name", sessionManager.getName()!!)
            Log.d("CCC Face", sessionManager.getFace()!!)
        }

    }

    private fun requestCameraPermission(RC_HANDLE_CAMERA_PERM: Int) {

        Log.w("MainActivity", "Camera permission is not granted. Requesting permission")
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
            requestCode == RC_HANDLE_CAMERA_PERM_GRAY
        ) {
            val intent = Intent(mContext, FaceDetectGrayActivity::class.java)
            startActivity(intent)
            return
        }
        Log.e(
            "MainActivity", "Permission not granted: results len = " + grantResults.size +
                    " Result code = " + if (grantResults.size > 0) grantResults[0] else "(empty)"
        )
    }
}
