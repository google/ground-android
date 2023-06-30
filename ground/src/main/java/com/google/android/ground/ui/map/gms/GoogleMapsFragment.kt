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
import com.google.android.ground.Config
import com.google.android.ground.R
import com.google.android.ground.model.geometry.*
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.model.imagery.TileSource
import com.google.android.ground.model.imagery.TileSource.Type.MOG_COLLECTION
import com.google.android.ground.model.job.Style
import com.google.android.ground.rx.Nil
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.ground.ui.map.*
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.map.Map
import com.google.android.ground.ui.map.gms.GmsExt.toBounds
import com.google.android.ground.ui.map.gms.mog.MogCollection
import com.google.android.ground.ui.map.gms.mog.MogTileProvider
import com.google.android.ground.ui.map.gms.renderer.PolygonRenderer
import com.google.android.ground.ui.map.gms.renderer.PolylineRenderer
import com.google.android.ground.ui.util.BitmapUtil
import com.google.android.ground.util.invert
import com.google.maps.android.PolyUtil
import com.google.maps.android.clustering.Cluster
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.processors.FlowableProcessor
import io.reactivex.processors.PublishProcessor
import io.reactivex.subjects.PublishSubject
import java.io.File
import java.net.URL
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
@AndroidEntryPoint(SupportMapFragment::class)
class GoogleMapsFragment : Hilt_GoogleMapsFragment(), Map {
  private lateinit var clusterRenderer: FeatureClusterRenderer

  /** Map drag events. Emits items when the map drag has started. */
  private val startDragEventsProcessor: @Hot FlowableProcessor<Nil> = PublishProcessor.create()

  override val startDragEvents: @Hot Flowable<Nil> = this.startDragEventsProcessor

  /** Camera move events. Emits items after the camera has stopped moving. */
  private val cameraMovedEventsProcessor: @Hot FlowableProcessor<CameraPosition> =
    PublishProcessor.create()

  override val cameraMovedEvents: @Hot Flowable<CameraPosition> = cameraMovedEventsProcessor

  // TODO(#693): Simplify impl of tile providers.
  // TODO(#691): This is a limitation of the MapBox tile provider we're using;
  // since one need to call `close` explicitly, we cannot generically expose these as TileProviders;
  // instead we must retain explicit reference to the concrete type.
  private val tileProvidersSubject: @Hot PublishSubject<MapBoxOfflineTileProvider> =
    PublishSubject.create()

  override val tileProviders: @Hot Observable<MapBoxOfflineTileProvider> = tileProvidersSubject

  private lateinit var polylineRenderer: PolylineRenderer
  private lateinit var polygonRenderer: PolygonRenderer

  @Inject lateinit var bitmapUtil: BitmapUtil

  private lateinit var map: GoogleMap

  private lateinit var clusterManager: FeatureClusterManager

  /**
   * References to Google Maps SDK CustomCap present on the map. Used to set the custom drawable to
   * start and end of polygon.
   */
  private var customCap: CustomCap? = null

  override val supportedMapTypes: List<MapType> = IDS_BY_MAP_TYPE.keys.toList()

  private val locationOfInterestInteractionSubject: @Hot PublishSubject<List<Feature>> =
    PublishSubject.create()

  override val locationOfInterestInteractions: @Hot Observable<List<Feature>> =
    locationOfInterestInteractionSubject

  private val polylineStrokeWidth: Float
    get() = resources.getDimension(R.dimen.polyline_stroke_width)

  override var mapType: MapType
    get() = MAP_TYPES_BY_ID[map.mapType]!!
    set(mapType) {
      map.mapType = IDS_BY_MAP_TYPE[mapType]!!
    }

  override var viewport: Bounds
    get() = map.projection.visibleRegion.latLngBounds.toModelObject()
    set(bounds) =
      map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.toGoogleMapsObject(), 0))

  override val currentZoomLevel: Float
    get() = map.cameraPosition.zoom

  private fun onApplyWindowInsets(view: View, insets: WindowInsetsCompat): WindowInsetsCompat {
    val insetBottom = insets.systemWindowInsetBottom
    // TODO: Move extra padding to dimens.xml.
    // HACK: Fix padding when keyboard is shown; we limit the padding here to prevent the
    // watermark from flying up too high due to the combination of translateY and big inset
    // size due to keyboard.
    setCompassPadding(view, 0, insets.systemWindowInsetTop + 200, 0, 0)
    setWatermarkPadding(view, 20, 0, 0, min(insetBottom, 250) + 8)
    return insets
  }

  private fun setCompassPadding(view: View, left: Int, top: Int, right: Int, bottom: Int) {
    // Compass may be null if Maps failed to load.
    val compass = view.findViewWithTag<ImageView>("GoogleMapCompass") ?: return
    val params = compass.layoutParams as RelativeLayout.LayoutParams
    params.setMargins(left, top, right, bottom)
    compass.layoutParams = params
  }

  private fun setWatermarkPadding(view: View, left: Int, top: Int, right: Int, bottom: Int) {
    // Watermark may be null if Maps failed to load.
    val watermark = view.findViewWithTag<ImageView>("GoogleWatermark") ?: return
    val params = watermark.layoutParams as RelativeLayout.LayoutParams
    params.setMargins(left, top, right, bottom)
    watermark.layoutParams = params
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
    onMapReadyCallback: Consumer<Map>
  ) {
    containerFragment.replaceFragment(containerId, this)
    getMapAsync { googleMap: GoogleMap ->
      onMapReady(googleMap)
      onMapReadyCallback.accept(this)
    }
  }

  private fun onMapReady(map: GoogleMap) {
    this.map = map

    // TODO(jsunde): Figure out where we want to get the style from parseColor(Style().color)
    val featureColor = parseColor(Style().color)

    clusterManager = FeatureClusterManager(context, map)
    clusterRenderer =
      FeatureClusterRenderer(
        requireContext(),
        map,
        clusterManager,
        Config.CLUSTERING_ZOOM_THRESHOLD,
        map.cameraPosition.zoom,
        featureColor
      )
    clusterManager.setOnClusterClickListener(this::onClusterItemClick)
    clusterManager.renderer = clusterRenderer

    polylineRenderer = PolylineRenderer(map, getCustomCap(), polylineStrokeWidth, featureColor)
    polygonRenderer =
      PolygonRenderer(map, polylineStrokeWidth, parseColor("#55ffffff"), featureColor)

    map.setOnCameraIdleListener(this::onCameraIdle)
    map.setOnCameraMoveStartedListener(this::onCameraMoveStarted)
    map.setOnMapClickListener(this::onMapClick)

    with(map.uiSettings) {
      isRotateGesturesEnabled = true
      isTiltGesturesEnabled = true
      isMyLocationButtonEnabled = false
      isMapToolbarEnabled = false
      isCompassEnabled = true
      isIndoorLevelPickerEnabled = false
    }
  }

  private fun onClusterItemClick(cluster: Cluster<FeatureClusterItem>): Boolean {
    // Move the camera to point to LOIs within the current cluster
    cluster.items.map { it.feature.geometry }.toBounds()?.let { moveCamera(it) }
    return true
  }

  // Handle taps on ambiguous features.
  private fun handleAmbiguity(latLng: LatLng) {
    val candidates = mutableListOf<Feature>()
    val processed = ArrayList<String>()

    for ((feature, value) in polygonRenderer.getPolygonsWithLoi()) {
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

  override fun getDistanceInPixels(coordinate1: Coordinate, coordinate2: Coordinate): Double {
    val projection = map.projection
    val loc1 = projection.toScreenLocation(coordinate1.toGoogleMapsObject())
    val loc2 = projection.toScreenLocation(coordinate2.toGoogleMapsObject())
    val dx = (loc1.x - loc2.x).toDouble()
    val dy = (loc1.y - loc2.y).toDouble()
    return sqrt(dx * dx + dy * dy)
  }

  override fun enableGestures() = map.uiSettings.setAllGesturesEnabled(true)

  override fun disableGestures() = map.uiSettings.setAllGesturesEnabled(false)

  override fun moveCamera(coordinate: Coordinate) =
    map.animateCamera(CameraUpdateFactory.newLatLng(coordinate.toGoogleMapsObject()))

  override fun moveCamera(coordinate: Coordinate, zoomLevel: Float) =
    map.animateCamera(CameraUpdateFactory.newLatLngZoom(coordinate.toGoogleMapsObject(), zoomLevel))

  override fun moveCamera(bounds: Bounds) {
    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.toGoogleMapsObject(), 100))
  }

  private fun getCustomCap(): CustomCap {
    if (customCap == null) {
      val bitmap = bitmapUtil.fromVector(R.drawable.ic_endpoint)
      customCap = CustomCap(BitmapDescriptorFactory.fromBitmap(bitmap))
    }
    return checkNotNull(customCap)
  }

  private fun onMapClick(latLng: LatLng) = handleAmbiguity(latLng)

  @SuppressLint("MissingPermission")
  override fun enableCurrentLocationIndicator() {
    if (!map.isMyLocationEnabled) {
      map.isMyLocationEnabled = true
    }
  }

  private fun removeStaleFeatures(features: Set<Feature>) {
    removeStalePoints(features)
    polylineRenderer.removeStaleFeatures(features)
    polygonRenderer.removeStaleFeatures(features)
  }

  private fun removeStalePoints(features: Set<Feature>) {
    clusterManager.removeStaleFeatures(features)
  }

  private fun removeAllFeatures() {
    clusterManager.removeAllFeatures()
    polylineRenderer.removeAllFeatures()
    polygonRenderer.removeAllFeatures()
  }

  private fun addOrUpdateLocationOfInterest(feature: Feature) {
    when (feature.geometry) {
      is Point -> clusterManager.addOrUpdateLocationOfInterestFeature(feature)
      is LineString,
      is LinearRing -> polylineRenderer.addFeature(feature, feature.geometry)
      is Polygon -> polygonRenderer.addFeature(feature, feature.geometry)
      is MultiPolygon ->
        feature.geometry.polygons.forEach { polygonRenderer.addFeature(feature, it) }
    }
  }

  override fun renderFeatures(features: Set<Feature>) {
    // Re-cluster and re-render
    if (features.isNotEmpty()) {
      Timber.v("renderFeatures() called with ${features.size} locations of interest")
      removeStaleFeatures(features)
      Timber.v("Updating ${features.size} features")
      features.forEach(this::addOrUpdateLocationOfInterest)
    } else {
      removeAllFeatures()
    }
    clusterManager.cluster()
  }

  override fun refresh() = renderFeatures(clusterManager.getManagedFeatures())

  private fun parseColor(colorHexCode: String?): Int =
    try {
      Color.parseColor(colorHexCode.toString())
    } catch (e: IllegalArgumentException) {
      Timber.w(e, "Invalid color code in job style: $colorHexCode")
      resources.getColor(R.color.colorMapAccent)
    }

  private fun onCameraIdle() {
    clusterRenderer.zoom = map.cameraPosition.zoom
    clusterManager.onCameraIdle()
    cameraMovedEventsProcessor.onNext(
      CameraPosition(
        map.cameraPosition.target.toCoordinate(),
        map.cameraPosition.zoom,
        false,
        map.projection.visibleRegion.latLngBounds.toModelObject()
      )
    )
  }

  private fun onCameraMoveStarted(reason: Int) {
    if (reason == OnCameraMoveStartedListener.REASON_GESTURE) {
      this.startDragEventsProcessor.onNext(Nil.NIL)
    }
  }

  private fun addTileOverlay(filePath: String) {
    val mbtilesFile = File(requireContext().filesDir, filePath)

    if (!mbtilesFile.exists()) {
      Timber.i("mbtiles file ${mbtilesFile.absolutePath} does not exist")
      return
    }

    try {
      val tileProvider = MapBoxOfflineTileProvider(mbtilesFile)
      tileProvidersSubject.onNext(tileProvider)
      map.addTileOverlay(TileOverlayOptions().tileProvider(tileProvider))
    } catch (e: Exception) {
      Timber.e(e, "Couldn't initialize tile provider for mbtiles file $mbtilesFile")
    }
  }

  override fun addLocalTileOverlays(mbtilesFiles: Set<String>) =
    mbtilesFiles.forEach { filePath -> addTileOverlay(filePath) }

  private fun addWebTileOverlay(url: String) {
    val webTileProvider = WebTileProvider(url)
    map.addTileOverlay(TileOverlayOptions().tileProvider(webTileProvider))
  }

  override fun addTileOverlay(tileSource: TileSource) =
    if (tileSource.type == MOG_COLLECTION) addMogCollectionTileOverlay(tileSource.url)
    else error("Unsupported tile source type ${tileSource.type}")

  private fun addMogCollectionTileOverlay(url: URL) {
    // TODO(#1730): Make URLs and zoom level configuration using a standard metadata format (STAC?).
    val mogCollection = MogCollection("${url}/{z}/world.tif", "${url}/{z}/{x}/{y}.tif", 8, 14)
    val tileProvider = MogTileProvider(mogCollection)
    map.addTileOverlay(TileOverlayOptions().tileProvider(tileProvider))
  }

  override fun addWebTileOverlays(urls: List<String>) = urls.forEach { addWebTileOverlay(it) }

  override fun setActiveLocationOfInterest(newLoiId: String?) {
    clusterRenderer.previousActiveLoiId = clusterManager.activeLocationOfInterest
    clusterManager.activeLocationOfInterest = newLoiId

    refresh()
  }

  companion object {
    private val IDS_BY_MAP_TYPE =
      mapOf(
        MapType.ROAD to GoogleMap.MAP_TYPE_NORMAL,
        MapType.TERRAIN to GoogleMap.MAP_TYPE_TERRAIN,
        MapType.SATELLITE to GoogleMap.MAP_TYPE_HYBRID
      )
    private val MAP_TYPES_BY_ID = IDS_BY_MAP_TYPE.invert()
  }
}
