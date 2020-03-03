package co.wangun.facexdemo.api

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface BaseApiService {

    // User Detail
    //@FormUrlEncoded
    @Multipart
    @POST("match_faces")
    fun matchFaces(
        @Header("user_id") userId: String,
        @Part img1: MultipartBody.Part,
        @Part img2: MultipartBody.Part
        //@Body body: String
        //@Header("Content-Type") contentType: String,
        //@Query("bboxes_1") bboxes1: String,
        //@Query("bboxes_2") bboxes2: String
    ): Call<ResponseBody>

    //fun submit(@Body body: String): Call<Void>

    // Login
//    @FormUrlEncoded
//    @POST("api/v1/login")
//    fun loginRequest(
//        @Field("username") username: String,
//        @Field("password") password: String,
//        @Field("auth_check") authCheck: Boolean
//    ): Call<ResponseBody>
}