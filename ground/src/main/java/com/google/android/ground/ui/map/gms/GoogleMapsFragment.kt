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
import com.google.android.ground.model.geometry.*
import com.google.android.ground.model.job.Style
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.rx.Nil
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.MarkerIconFactory
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.ground.ui.map.*
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.util.BitmapUtil
import com.google.maps.android.PolyUtil
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.processors.FlowableProcessor
import io.reactivex.processors.PublishProcessor
import io.reactivex.subjects.PublishSubject
import java.io.File
import java8.util.function.Consumer
import javax.inject.Inject
import kotlin.math.min
import kotlin.math.sqrt
import kotlinx.collections.immutable.toPersistentList
import timber.log.Timber

/**
 * Customization of Google Maps API Fragment that automatically adjusts the Google watermark based
 * on window insets.
 */
@AndroidEntryPoint
class GoogleMapsFragment : SupportMapFragment(), MapFragment {

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

  private val polygons: MutableMap<Feature, MutableList<MapsPolygon>> = HashMap()

  @Inject lateinit var bitmapUtil: BitmapUtil

  @Inject lateinit var markerIconFactory: MarkerIconFactory
  private var map: GoogleMap? = null

  private lateinit var clusterManager: FeatureClusterManager

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

  override val availableMapTypes: List<MapType> = MAP_TYPES

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
    this.clusterManager = FeatureClusterManager(context, map)
    clusterManager.setOnClusterItemClickListener(this::onClusterItemClick)
    clusterManager.renderer = FeatureClusterRenderer(context, map, clusterManager)

    map.setOnCameraIdleListener(this::onCameraIdle)
    map.setOnCameraMoveStartedListener(this::onCameraMoveStarted)
    map.setOnMapClickListener(this::onMapClick)

    with(map.uiSettings) {
      isRotateGesturesEnabled = false
      isTiltGesturesEnabled = false
      isMyLocationButtonEnabled = false
      isMapToolbarEnabled = false
      isCompassEnabled = false
      isIndoorLevelPickerEnabled = false
    }
  }

  // Handle taps on ambiguous features.
  private fun handleAmbiguity(latLng: LatLng) {
    val candidates = mutableListOf<Feature>()
    val processed = ArrayList<String>()

    for ((feature, value) in
      polygons.filter { it.key.tag.type == FeatureType.LOCATION_OF_INTEREST.ordinal }) {
      val loiId = feature.tag.id

      if (processed.contains(loiId)) {
        continue
      }

      if (value.any { PolyUtil.containsLocation(latLng, it.points, false) }) {
        candidates.add(feature)
      }

      processed.add(loiId)
    }

    val result = candidates.toPersistentList()

    if (!result.isEmpty()) {
      locationOfInterestInteractionSubject.onNext(result)
    }
  }

  /** Handles both cluster and marker clicks. */
  private fun onClusterItemClick(item: FeatureClusterItem): Boolean {
    return if (getMap().uiSettings.isZoomGesturesEnabled) {
      locationOfInterestInteractionSubject.onNext(listOf(item.feature))
      // Allow map to pan to marker.
      false
    } else {
      // Prevent map from panning to marker.
      true
    }
  }

  private val locationOfInterestInteractionSubject: @Hot PublishSubject<List<Feature>> =
    PublishSubject.create()
  override val locationOfInterestInteractions: @Hot Observable<List<Feature>> =
    locationOfInterestInteractionSubject

  override val startDragEvents: @Hot Flowable<Nil> = this.startDragEventsProcessor

  override val cameraMovedEvents: @Hot Flowable<CameraPosition> = cameraMovedEventsProcessor

  override val tileProviders: @Hot Observable<MapBoxOfflineTileProvider> = tileProvidersSubject

  override fun getDistanceInPixels(coordinate1: Coordinate, coordinate2: Coordinate): Double {
    if (map == null) {
      Timber.e("Null Map reference")
      return 0.toDouble()
    }
    val projection = map!!.projection
    val loc1 = projection.toScreenLocation(coordinate1.toGoogleMapsObject())
    val loc2 = projection.toScreenLocation(coordinate2.toGoogleMapsObject())
    val dx = (loc1.x - loc2.x).toDouble()
    val dy = (loc1.y - loc2.y).toDouble()
    return sqrt(dx * dx + dy * dy)
  }

  override fun enableGestures() = getMap().uiSettings.setAllGesturesEnabled(true)

  override fun disableGestures() = getMap().uiSettings.setAllGesturesEnabled(false)

  override fun moveCamera(coordinate: Coordinate) =
    getMap().animateCamera(CameraUpdateFactory.newLatLng(coordinate.toGoogleMapsObject()))

  override fun moveCamera(coordinate: Coordinate, zoomLevel: Float) =
    getMap()
      .animateCamera(CameraUpdateFactory.newLatLngZoom(coordinate.toGoogleMapsObject(), zoomLevel))

  private fun addMultiPolygon(locationOfInterest: Feature, multiPolygon: MultiPolygon) =
    multiPolygon.polygons.forEach { addPolygon(locationOfInterest, it) }

  private fun addPolygon(feature: Feature, polygon: Polygon) {
    val options = PolygonOptions()
    options.clickable(false)

    val shellVertices = polygon.shell.vertices.map { it.toLatLng() }
    options.addAll(shellVertices)

    val holes = polygon.holes.map { hole -> hole.vertices.map { point -> point.toLatLng() } }
    holes.forEach { options.addHole(it) }

    val mapsPolygon = getMap().addPolygon(options)
    mapsPolygon.tag = Pair(feature.tag.id, LocationOfInterest::javaClass)
    mapsPolygon.strokeWidth = polylineStrokeWidth.toFloat()
    // TODO(jsunde): Figure out where we want to get the style from
    //  parseColor(Style().color)
    mapsPolygon.fillColor = parseColor("#55ffffff")
    mapsPolygon.strokeColor = parseColor(Style().color)
    mapsPolygon.strokeJointType = JointType.ROUND

    polygons.getOrPut(feature) { mutableListOf() }.add(mapsPolygon)
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

  private fun removeStaleFeatures(features: Set<Feature>) {
    clusterManager.removeStaleFeatures(
      features.filter { it.tag.type == FeatureType.LOCATION_OF_INTEREST.ordinal }.toSet()
    )

    val deletedIds = polygons.keys.map { it.tag.id } - features.map { it.tag.id }.toSet()
    val deletedPolygons = polygons.filter { deletedIds.contains(it.key.tag.id) }
    deletedPolygons.values.forEach { it.forEach(MapsPolygon::remove) }
    polygons.minusAssign(deletedPolygons.keys)
  }

  private fun addOrUpdateLocationOfInterest(feature: Feature) {
    when (feature.geometry) {
      is Point -> clusterManager.addOrUpdateLocationOfInterestFeature(feature)
      is Polygon -> addPolygon(feature, feature.geometry)
      is MultiPolygon -> addMultiPolygon(feature, feature.geometry)
      else -> TODO()
    }
  }

  override fun renderFeatures(features: Set<Feature>) {
    // Re-cluster and re-render
    if (features.isNotEmpty()) {
      Timber.v("renderLocationsOfInterest() called with ${features.size} locations of interest")
      removeStaleFeatures(features)
      Timber.v("Updating ${features.size} features")
      features.forEach(this::addOrUpdateLocationOfInterest)
      clusterManager.cluster()
    }
  }

  override fun refresh() = renderFeatures(clusterManager.getManagedFeatures())

  override var mapType: Int
    get() = getMap().mapType
    set(mapType) {
      getMap().mapType = mapType
    }

  private fun parseColor(colorHexCode: String?): Int =
    try {
      Color.parseColor(colorHexCode.toString())
    } catch (e: IllegalArgumentException) {
      Timber.w("Invalid color code in job style: $colorHexCode")
      resources.getColor(R.color.colorMapAccent)
    }

  private fun onCameraIdle() {
    clusterManager.onCameraIdle()

    if (cameraChangeReason == OnCameraMoveStartedListener.REASON_GESTURE) {
      cameraMovedEventsProcessor.onNext(
        CameraPosition(
          getMap().cameraPosition.target.toCoordinate(),
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

  override fun addLocalTileOverlays(mbtilesFiles: Set<String>) =
    mbtilesFiles.forEach { filePath -> addTileOverlay(filePath) }

  private fun addRemoteTileOverlay(url: String) {
    val webTileProvider = WebTileProvider(url)
    getMap().addTileOverlay(TileOverlayOptions().tileProvider(webTileProvider))
  }

  override fun addRemoteTileOverlays(urls: List<String>) = urls.forEach { addRemoteTileOverlay(it) }

  override fun setActiveLocationOfInterest(locationOfInterest: LocationOfInterest?) {
    val newId = locationOfInterest?.id
    if (activeLocationOfInterest == newId) return
    clusterManager.activeLocationOfInterest = newId

    refresh()
  }

  companion object {
    // TODO(#936): Remove placeholder with appropriate images
    private val MAP_TYPES =
      listOf(
        MapType(GoogleMap.MAP_TYPE_NORMAL, R.string.road_map, R.drawable.ground_logo),
        MapType(GoogleMap.MAP_TYPE_TERRAIN, R.string.terrain, R.drawable.ground_logo),
        MapType(GoogleMap.MAP_TYPE_HYBRID, R.string.satellite, R.drawable.ground_logo)
      )
  }
}
