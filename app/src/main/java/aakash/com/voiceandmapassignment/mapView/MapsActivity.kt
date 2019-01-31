package aakash.com.voiceandmapassignment.mapView

import aakash.com.voiceandmapassignment.R
import aakash.com.voiceandmapassignment.common.util.BaseActivity
import aakash.com.voiceandmapassignment.common.util.RxSearchObs
import android.annotation.SuppressLint
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.MenuItem
import android.view.Window

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.jakewharton.rxbinding3.view.clicks
import kotlinx.android.synthetic.main.activity_maps.*

class MapsActivity : BaseActivity(), OnMapReadyCallback, LocationListener {
    private lateinit var mMap: GoogleMap
    private lateinit var mLocationManager: LocationManager

    companion object {
        private const val LOCATION_REFRESH_DISTANCE : Float = 10.0f
        private const val LOCATION_REFRESH_TIME : Long = 10L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        setupToolbar()
        setupLocation()
        setupMapFragment()
    }
    override fun onLocationChanged(location: Location?) {
        location?.let {
            setMarker(it.latitude, it.longitude)
        }
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    override fun onProviderEnabled(provider: String?) {}

    override fun onProviderDisabled(provider: String?) {}

    @SuppressLint("MissingPermission")
    private fun setupLocation() {
        mLocationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME,
            LOCATION_REFRESH_DISTANCE, this)
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    private fun setupMapFragment() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        compositeDisposable.add(ivSearch.clicks().subscribe({ it ->
            etSearch.requestFocus()
            RxSearchObs.fromView(etSearch).skipWhile {
                it.length < 3
            }.subscribe {

            }
        }, {
            it.printStackTrace()
        }))
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
    }

    private fun setMarker(lat : Double, lng: Double) {
        val location = LatLng(lat, lng)
        mMap.addMarker(MarkerOptions().position(location).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(location))
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item?.let {
            when (it.itemId) {
                android.R.id.home -> {
                    super.onBackPressed()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
