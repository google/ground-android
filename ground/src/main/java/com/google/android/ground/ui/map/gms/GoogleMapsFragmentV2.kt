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
import com.google.android.ground.R
import com.google.android.ground.model.geometry.*
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.model.job.Style
import com.google.android.ground.rx.Nil
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.MarkerIconFactory
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.ground.ui.map.*
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.util.BitmapUtil
import com.google.android.ground.util.toImmutableList
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
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.util.*
import java8.util.function.Consumer
import java8.util.stream.StreamSupport
import javax.inject.Inject
import kotlin.math.min
import kotlin.math.sqrt
import timber.log.Timber

/**
 * Customization of Google Maps API Fragment that automatically adjusts the Google watermark based
 * on window insets.
 */
@AndroidEntryPoint
class GoogleMapsFragmentV2 : SupportMapFragment(), MapFragment {
  /** Marker click events. */
  private val markerClicks: @Hot Subject<MapPin> = PublishSubject.create()

  /** Ambiguous click events. */
  private val locationOfInterestClicks: @Hot Subject<ImmutableList<MapLocationOfInterest>> =
    PublishSubject.create()

  /** Map drag events. Emits items when the map drag has started. */
  private val startDragEvents: @Hot FlowableProcessor<Nil> = PublishProcessor.create()

  /** Camera move events. Emits items after the camera has stopped moving. */
  private val cameraMovedEvents: @Hot FlowableProcessor<CameraPosition> = PublishProcessor.create()

  // TODO(#693): Simplify impl of tile providers.
  // TODO(#691): This is a limitation of the MapBox tile provider we're using;
  // since one need to call `close` explicitly, we cannot generically expose these as TileProviders;
  // instead we must retain explicit reference to the concrete type.
  private val tileProviders: @Hot PublishSubject<MapBoxOfflineTileProvider> =
    PublishSubject.create()

  /**
   * References to Google Maps SDK Markers present on the map. Used to sync and update polylines
   * with current view and data state.
   */
  private val markers: MutableSet<Marker> = HashSet()
  private val polygons: MutableMap<Geometry, Polyline> = HashMap()

  @Inject lateinit var bitmapUtil: BitmapUtil

  @Inject lateinit var markerIconFactory: MarkerIconFactory
  private var map: GoogleMap? = null

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
    val watermark = view.findViewWithTag<ImageView>("GoogleWatermark") ?: return
    // Watermark may be null if Maps failed to load.
    val params = watermark.layoutParams as RelativeLayout.LayoutParams
    params.setMargins(left, top, right, bottom)
    watermark.layoutParams = params
  }

  override fun getAvailableMapTypes(): ImmutableList<MapType> = MAP_TYPES

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
  ): View {
    val view = super.onCreateView(layoutInflater, viewGroup, bundle)
    ViewCompat.setOnApplyWindowInsetsListener(view!!) { view, insets ->
      onApplyWindowInsets(view, insets)
    }
    return view
  }

  override fun attachToFragment(
    containerFragment: AbstractFragment,
    @IdRes containerId: Int,
    mapReadyAction: Consumer<MapFragment>
  ) {
    containerFragment.replaceFragment(containerId, this)
    getMapAsync { googleMap: GoogleMap ->
      onMapReady(googleMap)
      mapReadyAction.accept(this)
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
      val vertices = value.points
      if (processed.contains((mapLocationOfInterest as MapPolygon).id)) {
        continue
      }

      if (PolyUtil.containsLocation(latLng, vertices, false)) {
        candidates.add(mapLocationOfInterest)
        processed.add(mapLocationOfInterest.id)
      }
    }
    val result = candidates.build()
    if (!result.isEmpty()) {
      locationOfInterestClicks.onNext(result)
    }
  }

  private fun onMarkerClick(marker: Marker): Boolean =
    if (getMap().uiSettings.isZoomGesturesEnabled) {
      markerClicks.onNext(marker.tag as MapPin)
      // Allow map to pan to marker.
      false
    } else {
      // Prevent map from panning to marker.
      true
    }

  override fun getMapPinClicks(): @Hot Observable<MapPin> = markerClicks

  override fun getLocationOfInterestClicks():
    @Hot Observable<ImmutableList<MapLocationOfInterest>> = locationOfInterestClicks

  override fun getStartDragEvents(): @Hot Flowable<Nil> = startDragEvents

  override fun getCameraMovedEvents(): @Hot Flowable<CameraPosition> = cameraMovedEvents

  override fun getTileProviders(): @Hot Observable<MapBoxOfflineTileProvider> = tileProviders

  override fun getDistanceInPixels(point1: Point, point2: Point): Double {
    if (map == null) {
      Timber.e("Null Map reference")
      return 0.toDouble()
    }
    val projection = map!!.projection
    val loc1 = projection.toScreenLocation(toLatLng(point1))
    val loc2 = projection.toScreenLocation(toLatLng(point2))
    val dx = (loc1.x - loc2.x).toDouble()
    val dy = (loc1.y - loc2.y).toDouble()
    return sqrt(dx * dx + dy * dy)
  }

  override fun enableGestures() = getMap().uiSettings.setAllGesturesEnabled(true)

  override fun disableGestures() = getMap().uiSettings.setAllGesturesEnabled(false)

  override fun moveCamera(point: Point) =
    getMap().moveCamera(CameraUpdateFactory.newLatLng(toLatLng(point)))

  override fun moveCamera(point: Point, zoomLevel: Float) =
    getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(toLatLng(point), zoomLevel))

  private fun addMapPin(mapPin: MapPin) {
    val position = toLatLng(mapPin.position)
    // TODO: add the anchor values into the resource dimensions file
    val marker =
      getMap()
        .addMarker(
          MarkerOptions()
            .position(position)
            .icon(getMarkerIcon(mapPin))
            .anchor(0.5f, 0.85f)
            .alpha(1.0f)
        )
    markers.add(marker)
    marker.tag = mapPin
  }

  private fun getMarkerIcon(mapPin: MapPin): BitmapDescriptor =
    markerIconFactory.getMarkerIcon(parseColor(mapPin.style.color), currentZoomLevel)

  private fun addMultiPolygon(multiPolygon: MultiPolygon) =
    multiPolygon.polygons.forEach { addPolygon(it) }

  private fun addPolygon(polygon: Polygon) {
    // TODO(jsunde): Remove before merging
    Timber.v("adding polygon with ${polygon.vertices} vertices")

    val options = PolylineOptions()
    options.clickable(false)
    val vertices = polygon.vertices.map { point: Point -> toLatLng(point) }.toImmutableList()
    options.addAll(vertices)

    val polyline = getMap().addPolyline(options)
    polyline.tag = polygon
    if (!isPolygonCompleted(polygon.vertices)) {
      polyline.startCap = customCap
      polyline.endCap = customCap
    }
    polyline.width = polylineStrokeWidth.toFloat()
    // TODO(jsunde): Figure out where we want to get the style from
    polyline.color = parseColor(Style().color)
    polyline.jointType = JointType.ROUND

    polygons[polygon] = polyline
  }

  private fun isPolygonCompleted(vertices: List<Point>): Boolean =
    vertices.size > 2 && vertices[vertices.size - 1] == vertices[0]

  private val polylineStrokeWidth: Int
    get() = resources.getDimension(R.dimen.polyline_stroke_width).toInt()

  private fun onMapClick(latLng: LatLng) = handleAmbiguity(latLng)

  override fun getCurrentZoomLevel(): Float = getMap().cameraPosition.zoom

  @SuppressLint("MissingPermission")
  override fun enableCurrentLocationIndicator() {
    if (!getMap().isMyLocationEnabled) {
      getMap().isMyLocationEnabled = true
    }
  }

  override fun setMapLocationsOfInterest(features: ImmutableSet<MapLocationOfInterest>) {
    Timber.v("setMapLocationsOfInterest() called with %s locations of interest", features.size)
    val geometriesToUpdate: MutableSet<Geometry> =
      HashSet(features.mapNotNull { it.locationOfInterest?.geometry })

    //    val deletedMarkers: MutableList<Marker> = ArrayList()
    //    for (marker in markers) {
    //      val pin = marker.tag as MapPin
    //      if (features.contains(pin)) {
    //        // If existing pin is present and up-to-date, don't update it.
    //        geometriesToUpdate.remove(pin)
    //      } else {
    //        // If pin isn't present or up-to-date, remove it so it can be added back later.
    //        removeMarker(marker)
    //        deletedMarkers.add(marker)
    //      }
    //    }

    //    // Update markers list.
    //    deletedMarkers.forEach { o: Marker -> markers.remove(o) }

    val polylineIterator: MutableIterator<Map.Entry<Geometry, Polyline>> =
      polygons.entries.iterator()
    while (polylineIterator.hasNext()) {
      val (geometry, polyline) = polylineIterator.next()
      if (geometriesToUpdate.contains(geometry)) {
        // If polygon already exists on map, don't add it.
        geometriesToUpdate.remove(geometry)
      } else {
        // Remove existing polyline not in list of updatedLocationsOfInterest.
        removePolygon(polyline)
        polylineIterator.remove()
      }
    }

    if (geometriesToUpdate.isEmpty()) return

    Timber.v("Updating %d features", geometriesToUpdate.size)
    geometriesToUpdate.forEach {
      when (it) {
        is LineString -> TODO()
        is LinearRing -> TODO()
        is MultiPolygon -> addMultiPolygon(it)
        is Point -> TODO()
        is Polygon -> addPolygon(it)
      }
    }
  }

  override fun refreshMarkerIcons() {
    for (marker in markers) {
      val mapPin = marker.tag as MapPin

      marker.setIcon(getMarkerIcon(mapPin))
    }
  }

  override fun getMapType(): Int = getMap().mapType

  override fun setMapType(mapType: Int) {
    getMap().mapType = mapType
  }

  private fun removeMarker(marker: Marker) {
    Timber.v("Removing marker %s", marker.id)
    marker.remove()
  }

  private fun removePolygon(polyline: Polyline) {
    Timber.v("Removing polyline %s", polyline.id)
    polyline.remove()
  }

  private fun parseColor(colorHexCode: String?): Int {
    return try {
      Color.parseColor(colorHexCode.toString())
    } catch (e: IllegalArgumentException) {
      Timber.w("Invalid color code in job style: %s", colorHexCode)
      resources.getColor(R.color.colorMapAccent)
    }
  }

  private fun onCameraIdle() {
    if (cameraChangeReason == OnCameraMoveStartedListener.REASON_GESTURE) {
      val target = getMap().cameraPosition.target
      val zoom = getMap().cameraPosition.zoom
      cameraMovedEvents.onNext(CameraPosition(fromLatLng(target), zoom))
      cameraChangeReason = OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION
    }
  }

  private fun onCameraMoveStarted(reason: Int) {
    cameraChangeReason = reason
    if (reason == OnCameraMoveStartedListener.REASON_GESTURE) {
      startDragEvents.onNext(Nil.NIL)
    }
  }

  override fun getViewport(): LatLngBounds {
    return getMap().projection.visibleRegion.latLngBounds
  }

  override fun setViewport(bounds: LatLngBounds) {
    getMap().moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0))
  }

  private fun addTileOverlay(filePath: String) {
    val mbtilesFile = File(requireContext().filesDir, filePath)

    if (!mbtilesFile.exists()) {
      Timber.i("mbtiles file %s does not exist", mbtilesFile.absolutePath)
      return
    }

    try {
      val tileProvider = MapBoxOfflineTileProvider(mbtilesFile)
      tileProviders.onNext(tileProvider)
      getMap().addTileOverlay(TileOverlayOptions().tileProvider(tileProvider))
    } catch (e: Exception) {
      Timber.e(e, "Couldn't initialize tile provider for mbtiles file %s", mbtilesFile)
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

  companion object {
    // TODO(#936): Remove placeholder with appropriate images
    private val MAP_TYPES =
      ImmutableList.builder<MapType>()
        .add(MapType(GoogleMap.MAP_TYPE_NORMAL, R.string.road_map, R.drawable.ground_logo))
        .add(MapType(GoogleMap.MAP_TYPE_TERRAIN, R.string.terrain, R.drawable.ground_logo))
        .add(MapType(GoogleMap.MAP_TYPE_HYBRID, R.string.satellite, R.drawable.ground_logo))
        .build()

    private fun fromLatLng(latLng: LatLng): Point =
      Point(Coordinate(latLng.latitude, latLng.longitude))

    private fun toLatLng(point: Point): LatLng = LatLng(point.coordinate.x, point.coordinate.y)
  }
}
