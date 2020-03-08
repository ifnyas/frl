package co.wangun.facexdemo.api

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface BaseApiService {

    @Multipart
    @POST("api/match")
    fun matchFaces(
        @Header("authorization") auth: String,
        @Part img1: MultipartBody.Part,
        @Part img2: MultipartBody.Part
    ): Call<ResponseBody>
}