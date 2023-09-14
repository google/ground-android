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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.annotation.IdRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
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
import com.google.android.ground.model.imagery.TileSource.Type.TILED_WEB_MAP
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
import io.reactivex.processors.FlowableProcessor
import io.reactivex.processors.PublishProcessor
import java8.util.function.Consumer
import javax.inject.Inject
import kotlin.math.min
import kotlin.math.sqrt
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

const val TILE_OVERLAY_Z = 0f
const val POLYGON_Z = 1f
const val CLUSTER_Z = 2f
const val MARKER_Z = 3f
/**
 * Customization of Google Maps API Fragment that automatically adjusts the Google watermark based
 * on window insets.
 */
@AndroidEntryPoint(SupportMapFragment::class)
class GoogleMapsFragment : Hilt_GoogleMapsFragment(), Map {
  // TODO: Inject dispatchers.
  private val defaultDispatcher = Dispatchers.Default

  private lateinit var clusterRenderer: FeatureClusterRenderer

  /** Map drag events. Emits items when the map drag has started. */
  private val startDragEventsProcessor: @Hot FlowableProcessor<Nil> = PublishProcessor.create()

  override val startDragEvents: @Hot Flowable<Nil> = this.startDragEventsProcessor

  /** Camera move events. Emits items after the camera has stopped moving. */
  private val cameraMovedEventsProcessor: @Hot FlowableProcessor<CameraPosition> =
    PublishProcessor.create()

  override val cameraMovedEvents: @Hot Flowable<CameraPosition> = cameraMovedEventsProcessor

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

  private val tileOverlays = mutableListOf<TileOverlay>()

  override val featureClicks = MutableSharedFlow<Set<Feature>>()

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

    val featureColor = resources.getColor(R.color.clusterColor)

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
      PolygonRenderer(
        map,
        polylineStrokeWidth,
        resources.getColor(R.color.polyLineColor),
        featureColor
      )

    map.setOnCameraIdleListener(this::onCameraIdle)
    map.setOnCameraMoveStartedListener(this::onCameraMoveStarted)

    map.setOnMapClickListener { lifecycleScope.launch(defaultDispatcher) { onMapClick(it) } }

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

  override fun getDistanceInPixels(coordinates1: Coordinates, coordinates2: Coordinates): Double {
    val projection = map.projection
    val loc1 = projection.toScreenLocation(coordinates1.toGoogleMapsObject())
    val loc2 = projection.toScreenLocation(coordinates2.toGoogleMapsObject())
    val dx = (loc1.x - loc2.x).toDouble()
    val dy = (loc1.y - loc2.y).toDouble()
    return sqrt(dx * dx + dy * dy)
  }

  override fun enableGestures() = map.uiSettings.setAllGesturesEnabled(true)

  override fun disableGestures() = map.uiSettings.setAllGesturesEnabled(false)

  override fun moveCamera(coordinates: Coordinates) =
    map.animateCamera(CameraUpdateFactory.newLatLng(coordinates.toGoogleMapsObject()))

  override fun moveCamera(coordinates: Coordinates, zoomLevel: Float) =
    map.animateCamera(
      CameraUpdateFactory.newLatLngZoom(coordinates.toGoogleMapsObject(), zoomLevel)
    )

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

  private suspend fun onMapClick(latLng: LatLng) {
    val clickedPolygons = getPolygonFeaturesContaining(latLng)
    if (clickedPolygons.isNotEmpty()) {
      featureClicks.emit(clickedPolygons)
    }
  }

  private fun getPolygonFeaturesContaining(latLng: LatLng) =
    polygonRenderer
      .getPolygonsByFeature()
      .filterValues { polygons ->
        polygons.any { PolyUtil.containsLocation(latLng, it.points, false) }
      }
      .keys

  @SuppressLint("MissingPermission")
  override fun enableCurrentLocationIndicator() {
    if (!map.isMyLocationEnabled) {
      map.isMyLocationEnabled = true
    }
  }

  private fun removeStaleFeatures(features: Set<Feature>) {
    Timber.d("Removing stale features from map")
    clusterManager.removeStaleFeatures(features)
    polylineRenderer.removeStaleFeatures(features)
    polygonRenderer.removeStaleFeatures(features)
  }

  private fun removeAllFeatures() {
    Timber.d("Removing all features from map")
    clusterManager.removeAllFeatures()
    polylineRenderer.removeAllFeatures()
    polygonRenderer.removeAllFeatures()
  }

  private fun addOrUpdateLocationOfInterest(feature: Feature) {
    when (feature.geometry) {
      is Point -> clusterManager.addOrUpdateLocationOfInterestFeature(feature)
      is LineString,
      is LinearRing -> polylineRenderer.addFeature(feature)
      is Polygon,
      is MultiPolygon -> polygonRenderer.addFeature(feature)
    }
  }

  override fun renderFeatures(features: Set<Feature>) {
    // Re-cluster and re-render
    Timber.v("renderFeatures() called with ${features.size} locations of interest")
    if (features.isNotEmpty()) {
      removeStaleFeatures(features)
      Timber.d("Updating ${features.size} features")
      features.forEach(this::addOrUpdateLocationOfInterest)
    } else {
      removeAllFeatures()
    }
    clusterManager.cluster()
  }

  override fun refresh() {
    Timber.v("Refresh features")
    renderFeatures(clusterManager.getManagedFeatures())
  }

  private fun onCameraIdle() {
    clusterRenderer.zoom = map.cameraPosition.zoom
    clusterManager.onCameraIdle()
    cameraMovedEventsProcessor.onNext(
      CameraPosition(
        map.cameraPosition.target.toCoordinates(),
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

  private fun addOfflineTileOverlay(url: String, bounds: List<Bounds>) {
    addTileOverlay(
      ClippingTileProvider(TemplateUrlTileProvider(url), bounds.map { it.toGoogleMapsObject() })
    )
  }

  override fun addTileOverlay(tileSource: TileSource) =
    when (tileSource.type) {
      MOG_COLLECTION -> addMogCollectionTileOverlay(tileSource.url)
      TILED_WEB_MAP -> addOfflineTileOverlay(tileSource.url, tileSource.clipBounds)
      else -> error("Unsupported tile source type ${tileSource.type}")
    }

  private fun addMogCollectionTileOverlay(url: String) {
    // TODO(#1730): Make sub-paths configurable and stop hardcoding here.
    val mogCollection = MogCollection(Config.getMogSources(url))
    addTileOverlay(MogTileProvider(mogCollection))
  }

  private fun addTileOverlay(tileProvider: TileProvider) {
    val tileOverlay =
      map.addTileOverlay(TileOverlayOptions().tileProvider(tileProvider).zIndex(TILE_OVERLAY_Z))
    if (tileOverlay == null) {
      Timber.e("Unable to add tile overlay $tileProvider")
      return
    }

    tileOverlays.add(tileOverlay)
  }

  override fun clearTileOverlays() {
    if (tileOverlays.isEmpty()) return

    tileOverlays.toImmutableList().forEach { it.remove() }
    tileOverlays.clear()
  }

  override fun clear() {
    Timber.d("Removes all markers, overlays, polylines and polygons from the map.")
    map.clear()
  }

  override fun setActiveLocationOfInterest(newLoiId: String?) {
    if (newLoiId == clusterManager.activeLocationOfInterest) return

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
