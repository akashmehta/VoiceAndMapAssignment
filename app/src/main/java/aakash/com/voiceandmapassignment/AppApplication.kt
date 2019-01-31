package aakash.com.voiceandmapassignment

import aakash.com.voiceandmapassignment.injection.components.AppComponent
import aakash.com.voiceandmapassignment.injection.modules.RetrofitModule
import android.app.Application

/**
 * Created by stllpt031 on 31/1/19.
 */
class AppApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initDagger()
    }
    companion object {

        private var instance: AppApplication?= null

        fun getAppContext(): AppApplication? {
            return instance
        }
    }

    private lateinit var mComponent: AppComponent

    private fun initDagger() {
        mComponent = DaggerAppComponent.builder()
            .retrofitModule(RetrofitModule())
            .build()
    }

}