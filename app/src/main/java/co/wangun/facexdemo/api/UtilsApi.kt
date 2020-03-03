package co.wangun.facexdemo.api

object UtilsApi {
    val getApiService: BaseApiService
        get() = RetrofitClient.getClient("https://www.facexapi.com/")!!
            .create(BaseApiService::class.java)
}