package co.wangun.facexdemo.api;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static Retrofit retrofit = null;

    public static Retrofit getClient() {

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

//        OkHttpClient client = new OkHttpClient.Builder()
//                .connectTimeout(1, TimeUnit.MINUTES) // connect timeout
//                .writeTimeout(1, TimeUnit.MINUTES) // write timeout
//                .readTimeout(1, TimeUnit.MINUTES) // read timeout
//                .addInterceptor(interceptor)
//                .build();

//        OkHttpClient client = UnsafeOkHttpClient.getUnsafeOkHttpClient();

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();

        String baseUrl = "http://www.facexapi.com/";

//        Gson builder = new GsonBuilder().disableHtmlEscaping().create();
        retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
//                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        return retrofit;
    }
}
