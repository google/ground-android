/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.ground.ui.map.gms

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.annotation.IdRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cocoahero.android.gmaps.addons.mapbox.MapBoxOfflineTileProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.maps.model.Polygon as MapsPolygon
import com.google.android.ground.R
import com.google.android.ground.model.geometry.LineString
import com.google.android.ground.model.geometry.LinearRing
import com.google.android.ground.model.geometry.MultiPolygon
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.model.job.Style
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.rx.Nil
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.MarkerIconFactory
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.ground.ui.map.*
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.util.BitmapUtil
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.google.maps.android.PolyUtil
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.processors.FlowableProcessor
import io.reactivex.processors.PublishProcessor
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.io.File
import java8.util.function.Consumer
import javax.inject.Inject
import kotlin.math.min
import kotlin.math.sqrt
import timber.log.Timber

/**
 * Customization of Google Maps API Fragment that automatically adjusts the Google watermark based
 * on window insets.
 */
@AndroidEntryPoint
class GoogleMapsFragment : SupportMapFragment(), MapFragment {
  /** Marker click events. */
  private val markerClicks: @Hot Subject<MapLocationOfInterest> = PublishSubject.create()

  /** Ambiguous click events. */
  private val locationOfInterestClicks: @Hot Subject<ImmutableList<MapLocationOfInterest>> =
    PublishSubject.create()

  /** Map drag events. Emits items when the map drag has started. */
  private val startDragEventsProcessor: @Hot FlowableProcessor<Nil> = PublishProcessor.create()

  /** Camera move events. Emits items after the camera has stopped moving. */
  private val cameraMovedEventsProcessor: @Hot FlowableProcessor<CameraPosition> =
    PublishProcessor.create()

  // TODO(#693): Simplify impl of tile providers.
  // TODO(#691): This is a limitation of the MapBox tile provider we're using;
  // since one need to call `close` explicitly, we cannot generically expose these as TileProviders;
  // instead we must retain explicit reference to the concrete type.
  private val tileProvidersSubject: @Hot PublishSubject<MapBoxOfflineTileProvider> =
    PublishSubject.create()

  /**
   * References to Google Maps SDK Markers present on the map. Used to sync and update polylines
   * with current view and data state.
   */
  private val markers: MutableMap<Marker, MapLocationOfInterest> = HashMap()
  private val polygons: MutableMap<MapLocationOfInterest, MutableList<MapsPolygon>> = HashMap()

  @Inject lateinit var bitmapUtil: BitmapUtil

  @Inject lateinit var markerIconFactory: MarkerIconFactory
  private var map: GoogleMap? = null

  /**
   * User selected [LocationOfInterest] by either clicking the bottom card or horizontal scrolling.
   */
  private var activeLocationOfInterest: String? = null

  /**
   * References to Google Maps SDK CustomCap present on the map. Used to set the custom drawable to
   * start and end of polygon.
   */
  private lateinit var customCap: CustomCap
  private var cameraChangeReason = OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION

  private fun onApplyWindowInsets(view: View, insets: WindowInsetsCompat): WindowInsetsCompat {
    val insetBottom = insets.systemWindowInsetBottom
    // TODO: Move extra padding to dimens.xml.
    // HACK: Fix padding when keyboard is shown; we limit the padding here to prevent the
    // watermark from flying up too high due to the combination of translateY and big inset
    // size due to keyboard.
    setWatermarkPadding(view, 20, 0, 0, min(insetBottom, 250) + 8)
    return insets
  }

  private fun setWatermarkPadding(view: View, left: Int, top: Int, right: Int, bottom: Int) {
    // Watermark may be null if Maps failed to load.
    val watermark = view.findViewWithTag<ImageView>("GoogleWatermark") ?: return
    val params = watermark.layoutParams as RelativeLayout.LayoutParams
    params.setMargins(left, top, right, bottom)
    watermark.layoutParams = params
  }

  override val availableMapTypes: ImmutableList<MapType> = MAP_TYPES

  private fun getMap(): GoogleMap {
    checkNotNull(map) { "Map is not ready" }
    return map!!
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    customCap = CustomCap(bitmapUtil.bitmapDescriptorFromVector(R.drawable.ic_endpoint))
  }

  override fun onCreateView(
    layoutInflater: LayoutInflater,
    viewGroup: ViewGroup?,
    bundle: Bundle?
  ): View =
    super.onCreateView(layoutInflater, viewGroup, bundle)!!.apply {
      ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        onApplyWindowInsets(view, insets)
      }
    }

  override fun attachToFragment(
    containerFragment: AbstractFragment,
    @IdRes containerId: Int,
    mapAdapter: Consumer<MapFragment>
  ) {
    containerFragment.replaceFragment(containerId, this)
    getMapAsync { googleMap: GoogleMap ->
      onMapReady(googleMap)
      mapAdapter.accept(this)
    }
  }

  private fun onMapReady(map: GoogleMap) {
    this.map = map

    map.setOnMarkerClickListener { marker: Marker -> onMarkerClick(marker) }

    val uiSettings = map.uiSettings
    uiSettings.isRotateGesturesEnabled = false
    uiSettings.isTiltGesturesEnabled = false
    uiSettings.isMyLocationButtonEnabled = false
    uiSettings.isMapToolbarEnabled = false
    uiSettings.isCompassEnabled = false
    uiSettings.isIndoorLevelPickerEnabled = false
    map.setOnCameraIdleListener { onCameraIdle() }
    map.setOnCameraMoveStartedListener { reason: Int -> onCameraMoveStarted(reason) }
    map.setOnMapClickListener { latLng: LatLng -> onMapClick(latLng) }
  }

  // Handle taps on ambiguous features.
  private fun handleAmbiguity(latLng: LatLng) {
    val candidates = ImmutableList.builder<MapLocationOfInterest>()
    val processed = ArrayList<String>()

    for ((mapLocationOfInterest, value) in polygons) {
      val loiId = mapLocationOfInterest.locationOfInterest.id
      if (processed.contains(loiId)) {
        continue
      }

      if (value.any { PolyUtil.containsLocation(latLng, it.points, false) }) {
        candidates.add(mapLocationOfInterest)
      }

      processed.add(loiId)
    }
    val result = candidates.build()
    if (!result.isEmpty()) {
      locationOfInterestClicks.onNext(result)
    }
  }

  private fun onMarkerClick(marker: Marker): Boolean =
    if (getMap().uiSettings.isZoomGesturesEnabled) {
      markers[marker]?.let { markerClicks.onNext(it) }
      // Allow map to pan to marker.
      false
    } else {
      // Prevent map from panning to marker.
      true
    }

  override val locationOfInterestInteractions: @Hot Observable<MapLocationOfInterest> = markerClicks

  override val ambiguousLocationOfInterestInteractions:
    @Hot
    Observable<ImmutableList<MapLocationOfInterest>> =
    locationOfInterestClicks

  override val startDragEvents: @Hot Flowable<Nil> = this.startDragEventsProcessor

  override val cameraMovedEvents: @Hot Flowable<CameraPosition> = cameraMovedEventsProcessor

  override val tileProviders: @Hot Observable<MapBoxOfflineTileProvider> = tileProvidersSubject

  override fun getDistanceInPixels(point1: Point, point2: Point): Double {
    if (map == null) {
      Timber.e("Null Map reference")
      return 0.toDouble()
    }
    val projection = map!!.projection
    val loc1 = projection.toScreenLocation(point1.toLatLng())
    val loc2 = projection.toScreenLocation(point2.toLatLng())
    val dx = (loc1.x - loc2.x).toDouble()
    val dy = (loc1.y - loc2.y).toDouble()
    return sqrt(dx * dx + dy * dy)
  }

  override fun enableGestures() = getMap().uiSettings.setAllGesturesEnabled(true)

  override fun disableGestures() = getMap().uiSettings.setAllGesturesEnabled(false)

  override fun moveCamera(point: Point) =
    getMap().moveCamera(CameraUpdateFactory.newLatLng(point.toLatLng()))

  override fun moveCamera(point: Point, zoomLevel: Float) =
    getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(point.toLatLng(), zoomLevel))

  private fun addMapPin(mapLocationOfInterest: MapLocationOfInterest, point: Point) {
    val position = point.toLatLng()
    // TODO: add the anchor values into the resource dimensions file
    val marker =
      getMap()
        .addMarker(
          MarkerOptions().position(position).icon(getMarkerIcon()).anchor(0.5f, 0.85f).alpha(1.0f)
        )
    markers[marker] = mapLocationOfInterest
    marker.tag = Pair(mapLocationOfInterest.locationOfInterest.id, LocationOfInterest::javaClass)
  }

  private fun getMarkerIcon(isSelected: Boolean = false): BitmapDescriptor =
    markerIconFactory.getMarkerIcon(parseColor(Style().color), currentZoomLevel, isSelected)

  private fun addMultiPolygon(
    locationOfInterest: MapLocationOfInterest,
    multiPolygon: MultiPolygon
  ) = multiPolygon.polygons.forEach { addPolygon(locationOfInterest, it) }

  private fun addPolygon(locationOfInterest: MapLocationOfInterest, polygon: Polygon) {
    val options = PolygonOptions()
    options.clickable(false)
    val shellVertices = polygon.shell.vertices.map { it.toLatLng() }
    options.addAll(shellVertices)
    val holes = polygon.holes.map { hole -> hole.vertices.map { point -> point.toLatLng() } }
    holes.forEach { options.addHole(it) }

    val mapsPolygon = getMap().addPolygon(options)
    mapsPolygon.tag = Pair(locationOfInterest.locationOfInterest.id, LocationOfInterest::javaClass)
    mapsPolygon.strokeWidth = polylineStrokeWidth.toFloat()
    // TODO(jsunde): Figure out where we want to get the style from
    //  parseColor(Style().color)
    mapsPolygon.fillColor = parseColor("#55ffffff")
    mapsPolygon.strokeColor = parseColor(Style().color)
    mapsPolygon.strokeJointType = JointType.ROUND

    polygons.getOrPut(locationOfInterest) { mutableListOf() }.add(mapsPolygon)
  }

  private val polylineStrokeWidth: Int
    get() = resources.getDimension(R.dimen.polyline_stroke_width).toInt()

  private fun onMapClick(latLng: LatLng) = handleAmbiguity(latLng)

  override val currentZoomLevel: Float
    get() = getMap().cameraPosition.zoom

  @SuppressLint("MissingPermission")
  override fun enableCurrentLocationIndicator() {
    if (!getMap().isMyLocationEnabled) {
      getMap().isMyLocationEnabled = true
    }
  }

  override fun renderLocationsOfInterest(features: ImmutableSet<MapLocationOfInterest>) {
    Timber.v("setMapLocationsOfInterest() called with ${features.size} locations of interest")
    val featuresToUpdate: MutableSet<MapLocationOfInterest> = HashSet(features)

    val deletedMarkers: MutableList<Marker> = ArrayList()
    for ((marker, pinLocationOfInterest) in markers) {
      if (features.contains(pinLocationOfInterest)) {
        // If existing pin is present and up-to-date, don't update it.
        featuresToUpdate.remove(pinLocationOfInterest)
      } else {
        // If pin isn't present or up-to-date, remove it so it can be added back later.
        removeMarker(marker)
        deletedMarkers.add(marker)
      }
    }

    // Update markers list.
    deletedMarkers.forEach { markers.remove(it) }

    val mapsPolygonIterator = polygons.entries.iterator()
    while (mapsPolygonIterator.hasNext()) {
      val (mapLocationOfInterest, polygons) = mapsPolygonIterator.next()
      if (features.contains(mapLocationOfInterest)) {
        // If polygons already exists on map, don't add them.
        featuresToUpdate.remove(mapLocationOfInterest)
      } else {
        // Remove existing polygons not in list of updatedLocationsOfInterest.
        polygons.forEach { removePolygon(it) }
        mapsPolygonIterator.remove()
      }
    }

    if (features.isEmpty()) return

    Timber.v("Updating ${features.size} features")
    features.forEach {
      val geometry = it.locationOfInterest.geometry

      when (geometry) {
        is LineString -> TODO()
        is LinearRing -> TODO()
        is MultiPolygon -> addMultiPolygon(it, geometry)
        is Point -> addMapPin(it, geometry)
        is Polygon -> addPolygon(it, geometry)
      }
    }
  }

  override fun refreshRenderedLocationsOfInterest() {
    for ((marker, mapLocationOfInterest) in markers) {
      val isSelected = mapLocationOfInterest.locationOfInterest.id == activeLocationOfInterest
      marker.setIcon(getMarkerIcon(isSelected))
    }
  }

  override var mapType: Int
    get() = getMap().mapType
    set(mapType) {
      getMap().mapType = mapType
    }

  private fun removeMarker(marker: Marker) {
    Timber.v("Removing marker ${marker.id}")
    marker.remove()
  }

  private fun removePolygon(polygon: MapsPolygon) {
    Timber.v("Removing polygon ${polygon.id}")
    polygon.remove()
  }

  private fun parseColor(colorHexCode: String?): Int =
    try {
      Color.parseColor(colorHexCode.toString())
    } catch (e: IllegalArgumentException) {
      Timber.w("Invalid color code in job style: $colorHexCode")
      resources.getColor(R.color.colorMapAccent)
    }

  private fun onCameraIdle() {
    if (cameraChangeReason == OnCameraMoveStartedListener.REASON_GESTURE) {
      cameraMovedEventsProcessor.onNext(
        CameraPosition(
          getMap().cameraPosition.target.toPoint(),
          getMap().cameraPosition.zoom,
          false,
          getMap().projection.visibleRegion.latLngBounds.toModelObject()
        )
      )
      cameraChangeReason = OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION
    }
  }

  private fun onCameraMoveStarted(reason: Int) {
    cameraChangeReason = reason
    if (reason == OnCameraMoveStartedListener.REASON_GESTURE) {
      this.startDragEventsProcessor.onNext(Nil.NIL)
    }
  }

  override var viewport: Bounds
    get() = getMap().projection.visibleRegion.latLngBounds.toModelObject()
    set(bounds) =
      getMap().moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.toGoogleMapsObject(), 0))

  private fun addTileOverlay(filePath: String) {
    val mbtilesFile = File(requireContext().filesDir, filePath)

    if (!mbtilesFile.exists()) {
      Timber.i("mbtiles file ${mbtilesFile.absolutePath} does not exist")
      return
    }

    try {
      val tileProvider = MapBoxOfflineTileProvider(mbtilesFile)
      tileProvidersSubject.onNext(tileProvider)
      getMap().addTileOverlay(TileOverlayOptions().tileProvider(tileProvider))
    } catch (e: Exception) {
      Timber.e(e, "Couldn't initialize tile provider for mbtiles file $mbtilesFile")
    }
  }

  override fun addLocalTileOverlays(mbtilesFiles: ImmutableSet<String>) =
    mbtilesFiles.forEach { filePath -> addTileOverlay(filePath) }

  private fun addRemoteTileOverlay(url: String) {
    val webTileProvider = WebTileProvider(url)
    getMap().addTileOverlay(TileOverlayOptions().tileProvider(webTileProvider))
  }

  override fun addRemoteTileOverlays(urls: ImmutableList<String>) =
    urls.forEach { addRemoteTileOverlay(it) }

  override fun setActiveLocationOfInterest(locationOfInterest: LocationOfInterest?) {
    val newId = locationOfInterest?.id
    if (activeLocationOfInterest == newId) return

    activeLocationOfInterest = newId
    // TODO: Optimize the performance by refreshing old and new rendered geometries
    refreshRenderedLocationsOfInterest()
  }

  companion object {
    // TODO(#936): Remove placeholder with appropriate images
    private val MAP_TYPES =
      ImmutableList.builder<MapType>()
        .add(MapType(GoogleMap.MAP_TYPE_NORMAL, R.string.road_map, R.drawable.ground_logo))
        .add(MapType(GoogleMap.MAP_TYPE_TERRAIN, R.string.terrain, R.drawable.ground_logo))
        .add(MapType(GoogleMap.MAP_TYPE_HYBRID, R.string.satellite, R.drawable.ground_logo))
        .build()
  }
}
