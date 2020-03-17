package co.wangun.pnmfr.api

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    @Multipart
    @POST("api/result")
    fun postResult(
        @Header("authorization") auth: String,
        @Part("username") username: String,
        @Part("lat") lat: String,
        @Part("lng") lng: String,
        @Part("confidence") confidence: String,
        @Part("status") status: Int,
        @Part img: MultipartBody.Part
    ): Call<ResponseBody>
}