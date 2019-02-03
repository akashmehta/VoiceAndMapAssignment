package aakash.com.voiceandmapassignment.mapView

import aakash.com.voiceandmapassignment.R
import aakash.com.voiceandmapassignment.common.services.ForegroundService
import aakash.com.voiceandmapassignment.common.util.BaseActivity
import aakash.com.voiceandmapassignment.common.util.GeoSearchHelper
import aakash.com.voiceandmapassignment.common.util.RxSearchObs
import android.app.AlertDialog
import android.content.Intent
import com.here.android.mpa.mapping.SupportMapFragment
import android.os.Bundle
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.here.android.mpa.cluster.ClusterLayer
import com.here.android.mpa.common.*
import com.here.android.mpa.guidance.NavigationManager
import com.here.android.mpa.mapping.Map
import com.here.android.mpa.mapping.MapMarker
import com.here.android.mpa.mapping.MapRoute
import com.here.android.mpa.routing.*
import com.here.android.mpa.search.PlaceLink
import com.jakewharton.rxbinding3.view.touches
import kotlinx.android.synthetic.main.activity_map.*
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

class MapsActivity : BaseActivity() {

    private var mapView: Map? = null
    private var isPaused = false
    private var posManager: PositioningManager? = null
    private var positionListener: WeakReference<PositioningManager.OnPositionChangedListener>? = null
    private val itemList = ArrayList<String>()
    private val positionList = ArrayList<PlaceLink>()
    private var geoSearchHelper: GeoSearchHelper? = null
    private lateinit var clusterLayer: ClusterLayer
    private var mapMarker: MapMarker? = null
    private var destLocation: GeoCoordinate? = null
    private var sourceLocation: GeoCoordinate? = null
    private var navigationManager: NavigationManager? = null
    private var route1: Route? = null
    private var geoBoundingBox: GeoBoundingBox? = null
    private var forgroundServiceStarted: Boolean = false
    private var isStarted: Boolean = false

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
        mapView = null
        cleanUp()
    }

    private fun cleanUp() {
        positionListener?.let {
            posManager?.removeListener(
                it.get()
            )
        }
        if (navigationManager != null) {
            stopForegroundService()
            navigationManager!!.stop()
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        setupToolbar()
        applicationContext.getExternalFilesDir(null)?.let {
            setupMapFragment()
        }
        setupAnimation()
        rvSearchResults.layoutManager = LinearLayoutManager(this)
        rvSearchResults.adapter = SearchResultAdapter(itemList, compositeDisposable) { it ->
            val placeLink = positionList[it]
            mapView?.let {
                removeMarker()
                destLocation = GeoCoordinate(placeLink.position)
                addMarker(destLocation!!)
                it.addClusterLayer(clusterLayer)

                clearSearchResults()
            }
        }
        setupSpeechRecognizer {place->
            val placeOutput = if (place.contains("navigate to ")) {
                place.removePrefix("navigate to ")
            } else {
                place
            }
            etSearch.setText(placeOutput)
            geoSearchHelper?.requestPlace(placeOutput) { it ->
                positionList.clear()
                itemList.clear()
                it.placeLinks.forEach {
                    itemList.add(it.title)
                    positionList.add(it)
                }
                rvSearchResults.adapter?.notifyDataSetChanged()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item?.let {
            when(item.itemId) {
                android.R.id.home -> super.onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun clearSearchResults() {
        positionList.clear()
        itemList.clear()
        rvSearchResults.adapter?.notifyDataSetChanged()
    }

    private fun removeMarker() {
        mapMarker?.let {
            clusterLayer.removeMarker(mapMarker)
        }
    }

    private fun addMarker(geoCoordinate: GeoCoordinate) {
        mapMarker = MapMarker()
        mapMarker!!.coordinate = geoCoordinate
        clusterLayer.addMarker(mapMarker)
        createRoute()
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
            clusterLayer = ClusterLayer()
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
                        sourceLocation = GeoCoordinate(
                            it.coordinate.latitude,
                            it.coordinate.longitude
                        )
                        geoSearchHelper =
                                GeoSearchHelper(
                                    this@MapsActivity,
                                    sourceLocation!!
                                )
                        mapView?.setCenter(it.coordinate, Map.Animation.NONE)
                        mapView?.zoomLevel = 13.2
                        navigationManager = NavigationManager.getInstance()
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
        compositeDisposable.add(RxSearchObs.fromView(etSearch).skipWhile { it ->
            it.length < 3
        }.throttleFirst(500, TimeUnit.MILLISECONDS).subscribe { place ->
            geoSearchHelper?.requestPlace(place) { it ->
                positionList.clear()
                itemList.clear()
                it.placeLinks.forEach {
                    itemList.add(it.title)
                    positionList.add(it)
                }
                rvSearchResults.adapter?.notifyDataSetChanged()
            }
        }
        )
        compositeDisposable.add(ivNavigate.touches().subscribe(
            {
                when (it.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (!isStarted) {
                            isStarted = true
                            ivNavigate.startAnimation(shrinkAnimation)
                            speechRecognizer.startListening(speechRecognizerIntent)
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isStarted) {
                            isStarted = false
                            ivNavigate.startAnimation(expandAnimation)
                            speechRecognizer.stopListening()
                        }
                    }
                }

            }, {
                it.printStackTrace()
            }
        ))
    }

    private fun createRoute() {
        /* Initialize a CoreRouter */
        val coreRouter = CoreRouter()

        /* Initialize a RoutePlan */
        val routePlan = RoutePlan()

        /*
         * Initialize a RouteOption.HERE SDK allow users to define their own parameters for the
         * route calculation,including transport modes,route types and route restrictions etc.Please
         * refer to API doc for full list of APIs
         */
        val routeOptions = RouteOptions()
        /* Other transport modes are also available e.g Pedestrian */
        routeOptions.transportMode = RouteOptions.TransportMode.CAR
        /* Disable highway in this route. */
        routeOptions.setHighwaysAllowed(false)
        /* Calculate the shortest route available. */
        routeOptions.routeType = RouteOptions.Type.SHORTEST
        /* Calculate 1 route. */
        routeOptions.routeCount = 1
        /* Finally set the route option */
        routePlan.routeOptions = routeOptions

        /* Add both waypoints to the route plan */
        routePlan.addWaypoint(RouteWaypoint(sourceLocation))
        routePlan.addWaypoint(RouteWaypoint(destLocation))
        /* Trigger the route calculation,results will be called back via the listener */
        coreRouter.calculateRoute(routePlan,
            object : Router.Listener<List<RouteResult>, RoutingError> {

                override fun onProgress(i: Int) {
                    /* The calculation progress can be retrieved in this callback. */
                }

                override fun onCalculateRouteFinished(
                    routeResults: List<RouteResult>,
                    routingError: RoutingError
                ) {
                    /* Calculation is done.Let's handle the result */
                    if (routingError == RoutingError.NONE) {
                        if (routeResults[0].route != null) {

                            route1 = routeResults[0].route
                            /* Create a MapRoute so that it can be placed on the map */
                            val mapRoute = MapRoute(routeResults[0].route)

                            /* Show the maneuver number on top of the route */
                            mapRoute.isManeuverNumberVisible = true

                            /* Add the MapRoute to the map */
                            mapView?.addMapObject(mapRoute)

                            /*
                                 * We may also want to make sure the map view is orientated properly
                                 * so the entire route can be easily seen.
                                 */
                            geoBoundingBox = routeResults[0].route.boundingBox
                            mapView?.zoomTo(
                                geoBoundingBox, Map.Animation.NONE,
                                Map.MOVE_PRESERVE_ORIENTATION
                            )

                            startNavigation()
                        } else {
                            Toast.makeText(
                                this@MapsActivity,
                                "Error:route results returned is not valid",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            this@MapsActivity,
                            "Error:route calculation returned error code: $routingError",
                            Toast.LENGTH_LONG
                        ).show()

                    }
                }
            })
    }

    /*
     * Android 8.0 (API level 26) limits how frequently background apps can retrieve the user's
     * current location. Apps can receive location updates only a few times each hour.
     * See href="https://developer.android.com/about/versions/oreo/background-location-limits.html
     * In order to retrieve location updates more frequently start a foreground service.
     * See https://developer.android.com/guide/components/services.html#Foreground
     */
    private fun startForegroundService() {
        if (!forgroundServiceStarted) {
            forgroundServiceStarted = true
            val startIntent = Intent(this@MapsActivity, ForegroundService::class.java)
            startIntent.action = ForegroundService.START_ACTION
            this@MapsActivity.applicationContext.startService(startIntent)
        }
    }

    private fun stopForegroundService() {
        if (forgroundServiceStarted) {
            forgroundServiceStarted = false
            val stopIntent = Intent(this@MapsActivity, ForegroundService::class.java)
            stopIntent.action = ForegroundService.STOP_ACTION
            this@MapsActivity.applicationContext.startService(stopIntent)
        }
    }

    private fun startNavigation() {
//        m_naviControlButton.setText(R.string.stop_navi)
        /* Configure Navigation manager to launch navigation on current map */
        navigationManager?.setMap(mapView)

        /*
         * Start the turn-by-turn navigation.Please note if the transport mode of the passed-in
         * route is pedestrian, the NavigationManager automatically triggers the guidance which is
         * suitable for walking. Simulation and tracking modes can also be launched at this moment
         * by calling either simulate() or startTracking()
         */

        /* Choose navigation modes between real time navigation and simulation */
        val alertDialogBuilder = AlertDialog.Builder(this@MapsActivity)
        alertDialogBuilder.setTitle("Navigation")
        alertDialogBuilder.setMessage("Choose Mode")
        alertDialogBuilder.setNegativeButton("Navigation") { dialoginterface, i ->
            navigationManager?.startNavigation(route1)
            mapView?.tilt = 60f
            startForegroundService()
        }
        alertDialogBuilder.setPositiveButton(
            "Simulation"
        ) { _, _ ->
            navigationManager?.simulate(route1, 60)//Simualtion speed is set to 60 m/s
            mapView?.tilt = 60f
            startForegroundService()
        }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
        /*
         * Set the map update mode to ROADVIEW.This will enable the automatic map movement based on
         * the current location.If user gestures are expected during the navigation, it's
         * recommended to set the map update mode to NONE first. Other supported update mode can be
         * found in HERE Android SDK API doc
         */
        navigationManager?.mapUpdateMode = NavigationManager.MapUpdateMode.ROADVIEW

        /*
         * NavigationManager contains a number of listeners which we can use to monitor the
         * navigation status and getting relevant instructions.In this example, we will add 2
         * listeners for demo purpose,please refer to HERE Android SDK API documentation for details
         */
        addNavigationListeners()
    }

    private fun addNavigationListeners() {

        /*
         * Register a NavigationManagerEventListener to monitor the status change on
         * NavigationManager
         */
        navigationManager?.addNavigationManagerEventListener(
            WeakReference(
                m_navigationManagerEventListener
            )
        )

        /* Register a PositionListener to monitor the position updates */
        navigationManager?.addPositionListener(
            WeakReference(mPositionListener)
        )
    }

    private val mPositionListener = object : NavigationManager.PositionListener() {
        override fun onPositionUpdated(geoPosition: GeoPosition?) {
            /* Current position information can be retrieved in this callback */
        }
    }

    private val m_navigationManagerEventListener = object : NavigationManager.NavigationManagerEventListener() {
        override fun onRunningStateChanged() {
            Toast.makeText(this@MapsActivity, "Running state changed", Toast.LENGTH_SHORT).show()
        }

        override fun onNavigationModeChanged() {
            Toast.makeText(this@MapsActivity, "Navigation mode changed", Toast.LENGTH_SHORT).show()
        }

        override fun onEnded(navigationMode: NavigationManager.NavigationMode?) {
            Toast.makeText(this@MapsActivity, navigationMode!!.toString() + " was ended", Toast.LENGTH_SHORT).show()
            stopForegroundService()
        }

        override fun onMapUpdateModeChanged(mapUpdateMode: NavigationManager.MapUpdateMode?) {}

        override fun onRouteUpdated(route: Route?) {
            Toast.makeText(this@MapsActivity, "Route updated", Toast.LENGTH_SHORT).show()
        }

        override fun onCountryInfo(s: String?, s1: String?) {
            Toast.makeText(
                this@MapsActivity, "Country info updated from $s to $s1",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


}