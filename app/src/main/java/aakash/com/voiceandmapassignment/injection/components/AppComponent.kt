package aakash.com.voiceandmapassignment.injection.components

import aakash.com.voiceandmapassignment.injection.modules.RetrofitModule
import aakash.com.voiceandmapassignment.mapView.MapsActivity
import dagger.Component
import javax.inject.Singleton

/**
 * Created by stllpt031 on 31/1/19.
 */
@Singleton
@Component(modules = [RetrofitModule::class])
interface AppComponent {
    fun inject(activity: MapsActivity)
}