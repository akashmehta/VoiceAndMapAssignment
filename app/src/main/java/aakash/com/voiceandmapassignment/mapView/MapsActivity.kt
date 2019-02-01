package aakash.com.voiceandmapassignment.mapView

import aakash.com.voiceandmapassignment.R
import aakash.com.voiceandmapassignment.common.util.BaseActivity
import com.here.android.mpa.mapping.SupportMapFragment
import android.os.Bundle
import com.here.android.mpa.common.*
import com.here.android.mpa.mapping.Map
import java.lang.ref.WeakReference

class MapsActivity : BaseActivity() {

    private var mapView: Map? = null
    private var isPaused = false
    private var posManager : PositioningManager? = null
    private var positionListener: WeakReference<PositioningManager.OnPositionChangedListener>? = null
    override fun onPause() {
        if (posManager != null) {
            posManager!!.stop()
        }
        super.onPause()
        isPaused = true
    }

    override fun onResume() {
        super.onResume()
        isPaused = false
        if (posManager != null) {
            posManager!!.start(
                PositioningManager.LocationMethod.GPS_NETWORK
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (posManager != null) {
            // Cleanup
            positionListener?.let {
                posManager?.removeListener(
                    it.get()
                )
            }
        }
        mapView = null
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        MapEngine.getInstance().init(ApplicationContext(this)) {
            if (it == OnEngineInitListener.Error.NONE) {
                posManager= PositioningManager.getInstance()
                setupMapFragment()
            }
        }
    }

    private fun setupPositionListener() {
        positionListener = WeakReference(object :
            PositioningManager.OnPositionChangedListener {
            override fun onPositionFixChanged(
                p0: PositioningManager.LocationMethod?,
                p1: PositioningManager.LocationStatus?
            ) {

            }

            override fun onPositionUpdated(
                method: PositioningManager.LocationMethod?,
                position: GeoPosition?, isMapMathced: Boolean
            ) {
                if (!isPaused) {
                    mapView?.setCenter(position?.coordinate, Map.Animation.NONE)
                }
            }
        })

        posManager?.addListener(
            positionListener
        )
    }

    private fun setupMapFragment() {
        // Search for the Map Fragment
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapfragment) as SupportMapFragment
        // initialize the Map Fragment and
        // retrieve the map that is associated to the fragment

        mapFragment.init { error ->
            if (error == OnEngineInitListener.Error.NONE) {
                // now the map is ready to be used
                mapView = mapFragment.map
                setupPositionListener()
                mapFragment.positionIndicator.isVisible = true
                // ...
            } else {
                println("ERROR: Cannot initialize SupportMapFragment")
            }
        }
    }

}