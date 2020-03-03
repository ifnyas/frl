package co.wangun.facexdemo.api

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST

interface BaseApiService {

    // User Detail
    @FormUrlEncoded
    @POST("match_faces")
    fun matchFaces(
        @Header("user_id") userId: String,
        //@Header("Content-Type") contentType: String,
        @Field("img_1") img1: String,
        @Field("img_2") img2: String
//        @Query("bboxes_1") bboxes1: String,
//        @Query("bboxes_2") bboxes2: String
    ): Call<ResponseBody>

    // Login
//    @FormUrlEncoded
//    @POST("api/v1/login")
//    fun loginRequest(
//        @Field("username") username: String,
//        @Field("password") password: String,
//        @Field("auth_check") authCheck: Boolean
//    ): Call<ResponseBody>
}