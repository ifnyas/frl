package co.wangun.pnmfr.activity

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import co.wangun.pnmfr.R
import co.wangun.pnmfr.api.ApiClient.client
import co.wangun.pnmfr.api.ApiService
import co.wangun.pnmfr.utils.SessionManager
import kotlinx.android.synthetic.main.activity_result.*
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException

class ResultActivity : AppCompatActivity() {

    private var sessionManager: SessionManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        // init values
        sessionManager = SessionManager(this)
        val img = sessionManager?.getPath() + "/recognize.jpg"
        val status = sessionManager?.getStatus()

        // init view
        nameView.text = sessionManager?.getName()
        imgView.setImageBitmap(BitmapFactory.decodeFile(img))

        if (status == 1) {
            statusView.text = "Login Sukses!"
            locView.text = sessionManager?.getLoc("full")
            scanView.visibility = View.GONE
        } else {
            statusView.text = "Login Gagal!"
        }

        // init button
        backBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        scanView.setOnClickListener {
            val intent = Intent(this, FaceActivity::class.java)
            intent.putExtra("from", "recognize")
            startActivity(intent)
            finish()
        }

        // init post
        sendResult()
    }

    private fun sendResult() {

        // init API Service
        val mApiService = client.create(ApiService::class.java)

        // init values
        val auth = getString(R.string.auth)
        val username = sessionManager?.getName()
        val lat = sessionManager?.getLoc("lat")
        val lng = sessionManager?.getLoc("lng")
        val status = sessionManager?.getStatus()
        val path = sessionManager?.getPath()
        val confidence = sessionManager?.getConfidence()
        val file = File(path, "recognize.jpg")
        val requestFile =
            RequestBody.create(MediaType.parse("multipart/form-data"), file)
        val img =
            MultipartBody.Part.createFormData("img", file.name, requestFile)

        // post request
        mApiService.postResult(auth, username!!, lat!!, lng!!, confidence!!, status!!, img)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    if (response.isSuccessful) {

                        try {
                            val jsonRESULTS = JSONObject(response.body()!!.string())

                            Toast.makeText(
                                applicationContext,
                                jsonRESULTS.getString("status"),
                                Toast.LENGTH_LONG
                            ).show()
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
}
