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
import androidx.annotation.StringRes
import androidx.navigation.fragment.NavHostFragment
import com.google.android.ground.R
import com.google.android.ground.repository.MapsRepository
import com.google.android.ground.rx.RxAutoDispose
import com.google.android.ground.system.PermissionDeniedException
import com.google.android.ground.system.SettingsChangeRequestCanceled
import com.google.android.ground.ui.home.mapcontainer.BaseMapViewModel
import com.google.android.ground.ui.home.mapcontainer.MapTypeDialogFragmentDirections
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.map.MapFragment
import com.google.android.ground.ui.map.MapType
import javax.inject.Inject
import kotlin.math.max
import timber.log.Timber

/** Injects a [MapFragment] in the container with id "map". */
abstract class AbstractMapViewerFragment : AbstractFragment() {

  @Inject lateinit var mapFragment: MapFragment
  @Inject lateinit var mapsRepository: MapsRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mapFragment.cameraMovedEvents
      .onBackpressureLatest()
      .`as`(RxAutoDispose.disposeOnDestroy(this))
      .subscribe { onCameraMoved(it) }
    getMapViewModel().getSelectMapTypeClicks().`as`(RxAutoDispose.autoDisposable(this)).subscribe {
      showMapTypeSelectorDialog()
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    mapFragment.attachToFragment(this, R.id.map) { onMapAttached(it) }
  }

  private fun onMapAttached(mapFragment: MapFragment) {
    mapsRepository.observableMapType().observe(viewLifecycleOwner) { mapFragment.mapType = it }
    getMapViewModel().locationLockState.observe(viewLifecycleOwner) {
      onLocationLockStateChange(it, mapFragment)
    }
    getMapViewModel().cameraUpdateRequests.observe(viewLifecycleOwner) { update ->
      update.ifUnhandled { data -> onCameraUpdateRequest(data, mapFragment) }
    }
    getMapViewModel().setLocationLockEnabled(true)
    onMapReady(mapFragment)
  }

  /** Opens a dialog for selecting a [MapType] for the basemap layer. */
  private fun showMapTypeSelectorDialog() {
    val types = mapFragment.availableMapTypes
    NavHostFragment.findNavController(this)
      .navigate(MapTypeDialogFragmentDirections.showMapTypeDialogFragment(types.toTypedArray()))
  }

  private fun onLocationLockStateChange(result: Result<Boolean>, map: MapFragment) {
    result
      .onSuccess {
        Timber.d("Location lock: $it")
        if (it) {
          map.enableCurrentLocationIndicator()
        }
      }
      .onFailure { exception: Throwable -> onLocationLockError(exception) }
  }

  private fun onLocationLockError(t: Throwable?) {
    when (t) {
      is PermissionDeniedException ->
        showUserActionFailureMessage(R.string.no_fine_location_permissions)
      is SettingsChangeRequestCanceled ->
        showUserActionFailureMessage(R.string.location_disabled_in_settings)
      else -> showUserActionFailureMessage(R.string.location_updates_unknown_error)
    }
  }

  private fun showUserActionFailureMessage(@StringRes resId: Int) {
    Toast.makeText(context, resId, Toast.LENGTH_LONG).show()
  }

  private fun onCameraUpdateRequest(newPosition: CameraPosition, map: MapFragment) {
    Timber.v("Update camera: %s", newPosition)
    if (newPosition.zoomLevel != null) {
      var zoomLevel = newPosition.zoomLevel
      if (!newPosition.isAllowZoomOut) {
        zoomLevel = max(zoomLevel, map.currentZoomLevel)
      }
      map.moveCamera(newPosition.target, zoomLevel)
    } else {
      map.moveCamera(newPosition.target)
    }

    // Manually notify that the camera has moved as `mapFragment.cameraMovedEvents` only returns
    // an event when the map is moved by the user (REASON_GESTURE).
    onCameraMoved(newPosition)
  }

  protected open fun onCameraMoved(position: CameraPosition) {
    getMapViewModel().onCameraMove(position)
  }

  protected abstract fun onMapReady(mapFragment: MapFragment)

  protected abstract fun getMapViewModel(): BaseMapViewModel
}
