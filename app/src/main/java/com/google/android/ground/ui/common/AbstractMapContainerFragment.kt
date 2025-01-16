/*
 * Copyright 2021 Google LLC
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
package com.google.android.ground.ui.common

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.android.ground.R
import com.google.android.ground.coroutines.DefaultDispatcher
import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.system.GeocodingManager
import com.google.android.ground.system.PermissionDeniedException
import com.google.android.ground.system.SettingsChangeRequestCanceled
import com.google.android.ground.ui.home.mapcontainer.MapTypeDialogFragmentDirections
import com.google.android.ground.ui.map.Bounds
import com.google.android.ground.ui.map.CameraUpdateRequest
import com.google.android.ground.ui.map.MapFragment
import com.google.android.ground.ui.map.NewCameraPositionViaBounds
import com.google.android.ground.ui.map.NewCameraPositionViaCoordinates
import com.google.android.ground.ui.map.NewCameraPositionViaCoordinatesAndZoomLevel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import timber.log.Timber

/** Injects a [MapFragment] in the container with id "map" and provides shared map functionality. */
abstract class AbstractMapContainerFragment : AbstractFragment() {

  @Inject lateinit var map: MapFragment
  @Inject lateinit var geocodingManager: GeocodingManager
  @Inject @DefaultDispatcher lateinit var defaultDispatcher: CoroutineDispatcher

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    map.attachToParent(this, R.id.map) { onMapAttached(it) }
  }

  private fun onMapAttached(map: MapFragment) {
    val viewModel = getMapViewModel()

    // Removes all markers, overlays, polylines and polygons from the map.
    map.clear()

    launchWhenStarted { map.cameraMovedEvents.collect { viewModel.onMapCameraMoved(it) } }
    launchWhenStarted { map.startDragEvents.collect { viewModel.onMapDragged() } }
    launchWhenStarted { viewModel.locationLock.collect { onLocationLockStateChange(it, map) } }
    launchWhenStarted {
      viewModel.getCameraUpdateRequests().collect { onCameraUpdateRequest(it, map) }
    }

    applyMapConfig(map)
    onMapReady(map)
  }

  private fun applyMapConfig(map: MapFragment) {
    val viewModel = getMapViewModel()
    val config = getMapConfig()

    // Map type
    if (config.overrideMapType != null) {
      map.mapType = config.overrideMapType
    } else {
      launchWhenStarted { viewModel.mapType.collect { map.mapType = it } }
    }

    // Tile overlays.
    if (config.showOfflineImagery) {
      viewModel.offlineTileSources.observe(viewLifecycleOwner) { tileSource ->
        map.clearTileOverlays()
        tileSource?.let { map.addTileOverlay(it) }
      }
    }

    // Map gestures
    if (config.allowGestures) {
      map.enableGestures()
    } else {
      map.disableGestures()
    }

    if (config.allowRotateGestures) {
      map.enableRotation()
    } else {
      map.disableRotation()
    }
  }

  /** Opens a dialog for selecting a [MapType] for the basemap layer. */
  fun showMapTypeSelectorDialog() {
    val types = map.supportedMapTypes.toTypedArray()
    findNavController().navigate(MapTypeDialogFragmentDirections.showMapTypeDialogFragment(types))
  }

  private fun onLocationLockStateChange(result: Result<Boolean>, map: MapFragment) {
    result.fold(
      onSuccess = {
        Timber.v("Location lock success: $it")
        if (it) {
          try {
            map.enableCurrentLocationIndicator()
          } catch (t: Throwable) {
            // User disabled permission while the lock icon was enabled.
            onLocationLockStateError(t)
          }
        }
      },
      onFailure = { exception -> onLocationLockStateError(exception) },
    )
  }

  private fun onLocationLockStateError(t: Throwable?) {
    val messageId =
      when (t) {
        is PermissionDeniedException -> R.string.no_fine_location_permissions
        is SecurityException -> R.string.no_fine_location_permissions
        is SettingsChangeRequestCanceled -> R.string.location_disabled_in_settings
        else -> R.string.location_updates_unknown_error
      }
    Toast.makeText(context, messageId, Toast.LENGTH_LONG).show()
  }

  /** Moves the camera to the given bounds. */
  fun moveToBounds(bounds: Bounds, padding: Int, shouldAnimate: Boolean) {
    onCameraUpdateRequest(NewCameraPositionViaBounds(bounds, padding, shouldAnimate), map)
  }

  /** Moves the camera to a given position. */
  fun moveToPosition(coordinates: Coordinates) {
    onCameraUpdateRequest(NewCameraPositionViaCoordinates(coordinates, shouldAnimate = true), map)
  }

  private fun onCameraUpdateRequest(request: CameraUpdateRequest, map: MapFragment) {
    Timber.v("Update camera: $request")
    when (request) {
      is NewCameraPositionViaCoordinates -> {
        map.moveCamera(request.coordinates, request.shouldAnimate)
      }
      is NewCameraPositionViaCoordinatesAndZoomLevel -> {
        map.moveCamera(
          request.coordinates,
          request.getZoomLevel(map.currentZoomLevel),
          request.shouldAnimate,
        )
      }
      is NewCameraPositionViaBounds -> {
        map.moveCamera(request.bounds, request.padding, request.shouldAnimate)
      }
    }
  }

  /** Called when the map is attached to the fragment. */
  protected open fun onMapReady(map: MapFragment) {}

  /** Provides an implementation of [BaseMapViewModel]. */
  protected abstract fun getMapViewModel(): BaseMapViewModel

  /** Configuration to enable/disable base map features. */
  open fun getMapConfig() = DEFAULT_MAP_CONFIG

  companion object {
    private val DEFAULT_MAP_CONFIG: MapConfig = MapConfig(showOfflineImagery = true)
  }
}
