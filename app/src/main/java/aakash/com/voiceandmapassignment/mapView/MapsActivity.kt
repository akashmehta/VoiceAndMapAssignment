package aakash.com.voiceandmapassignment.mapView

import aakash.com.voiceandmapassignment.R
import aakash.com.voiceandmapassignment.common.util.BaseActivity
import aakash.com.voiceandmapassignment.common.util.GeoSearchHelper
import aakash.com.voiceandmapassignment.common.util.RxSearchObs
import com.here.android.mpa.mapping.SupportMapFragment
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.here.android.mpa.common.*
import com.here.android.mpa.mapping.Map
import com.jakewharton.rxbinding3.view.clicks
import kotlinx.android.synthetic.main.activity_map.*
import java.io.File
import java.lang.ref.WeakReference

class MapsActivity : BaseActivity() {

    private var mapView: Map? = null
    private var isPaused = false
    private var posManager: PositioningManager? = null
    private var positionListener: WeakReference<PositioningManager.OnPositionChangedListener>? = null
    private val itemList = ArrayList<String>()
    private var geoSearchHelper: GeoSearchHelper? = null

    override fun onPause() {
        posManager?.stop()
        super.onPause()
        isPaused = true
    }

    override fun onResume() {
        super.onResume()
        isPaused = false
        posManager?.start(
            PositioningManager.LocationMethod.GPS_NETWORK
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cleanup
        positionListener?.let {
            posManager?.removeListener(
                it.get()
            )
        }
        mapView = null
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        setupToolbar()
        applicationContext.getExternalFilesDir(null)?.let {
            setupMapFragment()
        }
        rvSearchResults.layoutManager = LinearLayoutManager(this)
        rvSearchResults.adapter = SearchResultAdapter(itemList, compositeDisposable) {

        }
    }

    private fun setupMapFragment() {
        // Search for the Map Fragment
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapfragment) as SupportMapFragment
        val success = com.here.android.mpa.common.MapSettings.setIsolatedDiskCacheRootPath(
            applicationContext!!.getExternalFilesDir(null)!!.absolutePath + File.separator + ".here-maps",
            "HereMapIntent"
        ) /* ATTENTION! Do not forget to update {YOUR_INTENT_NAME} */

        if (!success) {
            Toast.makeText(this, "Unable to set cache directory", Toast.LENGTH_LONG).show()
        } else {
            mapFragment.init { error ->
                if (error == OnEngineInitListener.Error.NONE) {
                    MapEngine.getInstance().init(ApplicationContext(this)) {
                        if (it == OnEngineInitListener.Error.NONE) {
                            posManager = PositioningManager.getInstance()
                            // now the map is ready to be used
                            mapView = mapFragment.map
                            setupPositionListener()
                            mapFragment.positionIndicator.isVisible = true
                            // ...
                        } else {
                            Toast.makeText(this, it.details, Toast.LENGTH_LONG).show()
                        }
                    }

                } else {
                    Toast.makeText(this, error.details, Toast.LENGTH_LONG).show()
                }
            }

        }
        // initialize the Map Fragment and
        // retrieve the map that is associated to the fragment
    }

    private fun setupPositionListener() {
        positionListener = WeakReference(object :
            PositioningManager.OnPositionChangedListener {
            override fun onPositionFixChanged(
                p0: PositioningManager.LocationMethod?,
                p1: PositioningManager.LocationStatus?
            ) {
                println("p0 = [$p0], p1 = [$p1]")
            }

            override fun onPositionUpdated(
                method: PositioningManager.LocationMethod?,
                position: GeoPosition?, isMapMathced: Boolean
            ) {
                if (!isPaused) {
                    position?.let {
                        geoSearchHelper =
                                GeoSearchHelper(
                                    this@MapsActivity,
                                    it.coordinate.latitude,
                                    it.coordinate.longitude
                                )
                        mapView?.setCenter(it.coordinate, Map.Animation.NONE)
                    }
                }
            }
        })
        posManager?.let {
            it.addListener(
                positionListener
            )
            it.start(
                PositioningManager.LocationMethod.GPS_NETWORK
            )
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        compositeDisposable.add(ivSearch.clicks().subscribe({ _ ->
            etSearch.requestFocus()
            RxSearchObs.fromView(etSearch).skipWhile { it ->
                it.length < 3
            }.subscribe { place ->
                geoSearchHelper?.autoSuggestPlaces(place) { it ->
                    it.forEach {
                        itemList.add(Html.fromHtml(it.highlightedTitle).toString())
                    }
                    rvSearchResults.adapter?.notifyDataSetChanged()
                }
            }
        }, {
            it.printStackTrace()
        }))
    }

}