package aakash.com.voiceandmapassignment.injection.modules

import aakash.com.voiceandmapassignment.AppApplication
import com.readystatesoftware.chuck.ChuckInterceptor
import dagger.Module
import dagger.Provides
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import stllpt.com.basesetupproject.AppApplication
import stllpt.com.basesetupproject.BuildConfig
import stllpt.com.basesetupproject.common.helper.ConnectivityAwareClient
import stllpt.com.basesetupproject.shareddata.endpoints.ApiEndPoints
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Created by stllpt031 on 14/8/18.
 */
@Module
class
RetrofitModule {
    @Provides
    @Singleton
    @Named("fileHttp")
    fun provideFileHttpLogging(): OkHttpClient {
        val logging = HttpLoggingInterceptor()
        logging.level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        return OkHttpClient.Builder()
                .addInterceptor(ChuckInterceptor(AppApplication.getAppContext()))
                .addNetworkInterceptor(logging)
                .readTimeout(30, TimeUnit.SECONDS)
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()
    }

    @Provides
    @Singleton
    @Named("dataHttp")
    fun provideDataHttpLogging(): OkHttpClient {
        val logging = HttpLoggingInterceptor()
        logging.level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        return ConnectivityAwareClient(OkHttpClient.Builder()
                .addInterceptor(ChuckInterceptor(AppApplication.getAppContext()))
                .addNetworkInterceptor(logging)
                .build())
    }

    @Provides
    @Singleton
    @Named("fileRetrofit")
    fun provideFileRetrofit(@Named("fileHttp") okHttpClient: OkHttpClient) = Retrofit.Builder()
            .baseUrl(BuildConfig.API)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
            .client(okHttpClient)
            .build()

    @Provides
    @Singleton
    @Named("dataRetrofit")
    fun provideDataRetrofit(@Named("dataHttp") okHttpClient: OkHttpClient) = Retrofit.Builder()
            .baseUrl(BuildConfig.API)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
            .client(okHttpClient)
            .build()

    @Provides
    @Singleton
    fun provideApiService(@Named("dataRetrofit") retrofit: Retrofit) = retrofit.create(ApiEndPoints::class.java)

    @Provides
    @Singleton
    @Named("fileEndPoint")
    fun provideFileApiService(@Named("fileRetrofit") retrofit: Retrofit) = retrofit.create(ApiEndPoints::class.java)

}