package aakash.com.voiceandmapassignment

import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication

class AppApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        MultiDex.install(this)
    }
}