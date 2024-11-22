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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.gms.maps.model.TileProvider
import com.google.android.ground.Config
import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.model.imagery.LocalTileSource
import com.google.android.ground.model.imagery.RemoteMogTileSource
import com.google.android.ground.model.imagery.TileSource
import com.google.android.ground.persistence.remote.RemoteStorageManager
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.ground.ui.map.Bounds
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.map.Feature
import com.google.android.ground.ui.map.MapFragment
import com.google.android.ground.ui.map.MapType
import com.google.android.ground.ui.map.gms.features.FeatureManager
import com.google.android.ground.ui.map.gms.mog.MogCollection
import com.google.android.ground.ui.map.gms.mog.MogTileProvider
import com.google.android.ground.util.invert
import com.google.android.ground.util.systemInsets
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.min
import kotlin.math.sqrt
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

const val TILE_OVERLAY_Z = 0f
const val POLYGON_Z = 1f
const val POLYLINE_Z = 1f
const val CLUSTER_Z = 2f
const val MARKER_Z = 3f

/**
 * Customization of Google Maps API Fragment that automatically adjusts the Google watermark based
 * on window insets.
 */
@AndroidEntryPoint
class GoogleMapsFragment : SupportMapFragment(), MapFragment {
  /** Map drag events. Emits items when the map drag has started. */
  override val startDragEvents = MutableSharedFlow<Unit>()

  /** Camera move events. Emits items after the camera has stopped moving. */
  override val cameraMovedEvents = MutableSharedFlow<CameraPosition>()

  @Inject lateinit var featureManager: FeatureManager
  @Inject lateinit var remoteStorageManager: RemoteStorageManager

  private lateinit var map: GoogleMap

  override val supportedMapTypes: List<MapType> = IDS_BY_MAP_TYPE.keys.toList()

  private val tileOverlays = mutableListOf<TileOverlay>()

  override val featureClicks = MutableSharedFlow<Set<Feature>>()

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
    // TODO: Move extra padding to dimens.xml.
    // HACK: Fix padding when keyboard is shown; we limit the padding here to prevent the
    // watermark from flying up too high due to the combination of translateY and big inset
    // size due to keyboard.
    setCompassPadding(view, 0, insets.systemInsets().top + 200, 0, 0)
    setWatermarkPadding(view, 20, 0, 0, min(insets.systemInsets().bottom, 250) + 8)
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
    bundle: Bundle?,
  ): View {
    Timber.v("Lifecyle event: onCreateView()")
    return super.onCreateView(layoutInflater, viewGroup, bundle).apply {
      ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        onApplyWindowInsets(view, insets)
      }
    }
  }

  override fun attachToParent(
    containerFragment: AbstractFragment,
    @IdRes containerId: Int,
    onMapReadyCallback: (MapFragment) -> Unit,
  ) {
    containerFragment.replaceFragment(containerId, this)
    getMapAsync { googleMap: GoogleMap ->
      onMapReady(googleMap)
      onMapReadyCallback(this)
    }
  }

  private fun onMapReady(map: GoogleMap) {
    Timber.v("Map event: onMapReady()")

    this.map = map

    featureManager.onMapReady(map)

    map.setOnCameraIdleListener(this::onCameraIdle)
    map.setOnCameraMoveStartedListener(this::onCameraMoveStarted)
    map.setOnMapClickListener { onMapClick(it) }

    with(map.uiSettings) {
      isRotateGesturesEnabled = true
      isTiltGesturesEnabled = true
      isMyLocationButtonEnabled = false
      isMapToolbarEnabled = false
      isCompassEnabled = true
      isIndoorLevelPickerEnabled = false
    }

    viewLifecycleOwner.lifecycleScope.launch {
      featureManager.markerClicks.collect { featureClicks.emit(setOf(it)) }
    }
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

  override fun enableRotation() {
    map.uiSettings.isRotateGesturesEnabled = true
  }

  override fun disableRotation() {
    map.uiSettings.isRotateGesturesEnabled = false
  }

  override fun moveCamera(coordinates: Coordinates, shouldAnimate: Boolean) =
    moveCamera(CameraUpdateFactory.newLatLng(coordinates.toGoogleMapsObject()), shouldAnimate)

  override fun moveCamera(coordinates: Coordinates, zoomLevel: Float, shouldAnimate: Boolean) =
    moveCamera(
      CameraUpdateFactory.newLatLngZoom(coordinates.toGoogleMapsObject(), zoomLevel),
      shouldAnimate,
    )

  override fun moveCamera(bounds: Bounds, padding: Int, shouldAnimate: Boolean) =
    moveCamera(
      CameraUpdateFactory.newLatLngBounds(bounds.toGoogleMapsObject(), padding),
      shouldAnimate,
    )

  private fun moveCamera(cameraUpdate: CameraUpdate, shouldAnimate: Boolean) =
    if (shouldAnimate) map.animateCamera(cameraUpdate) else map.moveCamera(cameraUpdate)

  private fun onMapClick(latLng: LatLng) {
    val clickedPolygons = featureManager.getIntersectingPolygons(latLng)
    if (clickedPolygons.isNotEmpty()) {
      viewLifecycleOwner.lifecycleScope.launch { featureClicks.emit(clickedPolygons) }
    }
  }

  @SuppressLint("MissingPermission")
  override fun enableCurrentLocationIndicator() {
    if (!map.isMyLocationEnabled) {
      map.isMyLocationEnabled = true
    }
  }

  override fun setFeatures(newFeatures: Set<Feature>) {
    Timber.v("setFeatures() called with ${newFeatures.size} features")
    featureManager.setFeatures(newFeatures)
  }

  private fun onCameraIdle() {
    val cameraPosition = map.cameraPosition
    val projection = map.projection

    featureManager.zoom = map.cameraPosition.zoom
    featureManager.onCameraIdle()

    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        cameraMovedEvents.emit(
          CameraPosition(
            cameraPosition.target.toCoordinates(),
            cameraPosition.zoom,
            projection.visibleRegion.latLngBounds.toModelObject(),
          )
        )
      }
    }
  }

  private fun onCameraMoveStarted(reason: Int) {
    if (reason == OnCameraMoveStartedListener.REASON_GESTURE) {
      viewLifecycleOwner.lifecycleScope.launch { startDragEvents.emit(Unit) }
    }
  }

  override fun addTileOverlay(source: TileSource) =
    when (source) {
      is LocalTileSource -> addLocalTileOverlay(source.localFilePath, source.clipBounds)
      is RemoteMogTileSource -> addRemoteMogTileOverlay(source.remotePath)
    }

  private fun addLocalTileOverlay(url: String, bounds: List<Bounds>) {
    addTileOverlay(
      ClippingTileProvider(TemplateUrlTileProvider(url), bounds.map { it.toGoogleMapsObject() })
    )
  }

  private fun addRemoteMogTileOverlay(url: String) {
    // TODO(#1730): Make sub-paths configurable and stop hardcoding here.
    val mogCollection = MogCollection(Config.getMogSources(url))
    addTileOverlay(MogTileProvider(mogCollection, remoteStorageManager))
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
    map.clear()
    featureManager.setFeatures(emptySet())
  }

  companion object {
    private val IDS_BY_MAP_TYPE =
      mapOf(
        MapType.ROAD to GoogleMap.MAP_TYPE_NORMAL,
        MapType.TERRAIN to GoogleMap.MAP_TYPE_TERRAIN,
        MapType.SATELLITE to GoogleMap.MAP_TYPE_HYBRID,
      )
    private val MAP_TYPES_BY_ID = IDS_BY_MAP_TYPE.invert()
  }
}
