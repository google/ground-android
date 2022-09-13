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
package com.google.android.ground.ui.home.mapcontainer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.NavHostFragment
import com.google.android.ground.R
import com.google.android.ground.databinding.MapContainerFragBinding
import com.google.android.ground.model.Survey
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.locationofinterest.LocationOfInterestType
import com.google.android.ground.repository.MapsRepository
import com.google.android.ground.rx.*
import com.google.android.ground.system.PermissionDeniedException
import com.google.android.ground.system.SettingsChangeRequestCanceled
import com.google.android.ground.ui.common.AbstractMapViewerFragment
import com.google.android.ground.ui.home.BottomSheetState
import com.google.android.ground.ui.home.HomeScreenFragmentDirections
import com.google.android.ground.ui.home.HomeScreenViewModel
import com.google.android.ground.ui.map.MapFragment
import com.google.android.ground.ui.map.MapLocationOfInterest
import com.google.android.ground.ui.map.MapType
import com.google.common.collect.ImmutableList
import com.uber.autodispose.ObservableSubscribeProxy
import dagger.hilt.android.AndroidEntryPoint
import java8.util.Optional
import javax.inject.Inject
import kotlin.math.max
import timber.log.Timber

/** Main app view, displaying the map and related controls (center cross-hairs, add button, etc). */
@AndroidEntryPoint
class MapContainerFragment : AbstractMapViewerFragment() {
  @Inject lateinit var mapsRepository: MapsRepository
  lateinit var polygonDrawingViewModel: PolygonDrawingViewModel
  private lateinit var mapContainerViewModel: MapContainerViewModel
  private lateinit var homeScreenViewModel: HomeScreenViewModel
  private lateinit var binding: MapContainerFragBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mapContainerViewModel = getViewModel(MapContainerViewModel::class.java)
    homeScreenViewModel = getViewModel(HomeScreenViewModel::class.java)
    val locationOfInterestRepositionViewModel =
      getViewModel(LocationOfInterestRepositionViewModel::class.java)
    polygonDrawingViewModel = getViewModel(PolygonDrawingViewModel::class.java)
    mapFragment.markerClicks.`as`(RxAutoDispose.disposeOnDestroy(this)).subscribe {
      mapContainerViewModel.onMarkerClick(it)
    }
    mapFragment.markerClicks.`as`(RxAutoDispose.disposeOnDestroy(this)).subscribe {
      homeScreenViewModel.onMarkerClick(it)
    }
    mapFragment.locationOfInterestClicks
      .`as`<ObservableSubscribeProxy<ImmutableList<MapLocationOfInterest>>>(
        RxAutoDispose.disposeOnDestroy(this)
      )
      .subscribe { homeScreenViewModel.onLocationOfInterestClick(it) }
    mapFragment.startDragEvents
      .onBackpressureLatest()
      .`as`(RxAutoDispose.disposeOnDestroy(this))
      .subscribe { mapContainerViewModel.onMapDrag() }
    mapFragment.cameraMovedEvents
      .onBackpressureLatest()
      .`as`(RxAutoDispose.disposeOnDestroy(this))
      .subscribe { mapContainerViewModel.onCameraMove(it) }
    mapFragment.tileProviders.`as`(RxAutoDispose.disposeOnDestroy(this)).subscribe {
      mapContainerViewModel.queueTileProvider(it)
    }

    polygonDrawingViewModel.unsavedMapLocationsOfInterest.observe(this) {
      mapContainerViewModel.setUnsavedMapLocationsOfInterest(it)
    }
    locationOfInterestRepositionViewModel.confirmButtonClicks
      .`as`(RxAutoDispose.autoDisposable(this))
      .subscribe { showConfirmationDialog(it) }
    locationOfInterestRepositionViewModel.cancelButtonClicks
      .`as`(RxAutoDispose.autoDisposable(this))
      .subscribe { mapContainerViewModel.setMode(MapContainerViewModel.Mode.DEFAULT) }
    mapContainerViewModel
      .getSelectMapTypeClicks()
      .`as`(RxAutoDispose.autoDisposable(this))
      .subscribe { showMapTypeSelectorDialog() }
    mapContainerViewModel
      .getZoomThresholdCrossed()
      .`as`(RxAutoDispose.autoDisposable(this))
      .subscribe { onZoomThresholdCrossed() }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    binding = MapContainerFragBinding.inflate(inflater, container, false)
    binding.viewModel = mapContainerViewModel
    binding.homeScreenViewModel = homeScreenViewModel
    binding.lifecycleOwner = this
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    disableAddLocationOfInterestBtn()
  }

  override fun onMapReady(map: MapFragment) {
    Timber.d("MapAdapter ready. Updating subscriptions")

    // Custom views rely on the same instance of MapFragment. That couldn't be injected via Dagger.
    // Hence, initializing them here instead of inflating in layout.
    attachCustomViews(map)
    mapContainerViewModel.setLocationLockEnabled(true)
    polygonDrawingViewModel.setLocationLockEnabled(true)

    // Observe events emitted by the ViewModel.
    mapContainerViewModel.mapLocationsOfInterest.observe(this) { map.setMapLocationsOfInterest(it) }
    mapContainerViewModel.locationLockState.observe(this) { state ->
      onLocationLockStateChange(state, map)
    }
    mapContainerViewModel.cameraUpdateRequests.observe(this) { update ->
      update.ifUnhandled { data -> onCameraUpdate(data, map) }
    }
    mapContainerViewModel.surveyLoadingState.observe(this) { onSurveyChange(it) }
    homeScreenViewModel.bottomSheetState.observe(this) { state: BottomSheetState ->
      onBottomSheetStateChange(state, map)
    }
    mapContainerViewModel.mbtilesFilePaths.observe(this) { map.addLocalTileOverlays(it) }

    // TODO: Do this the RxJava way
    val cameraPosition = mapContainerViewModel.getCameraPosition().value
    cameraPosition?.let { map.moveCamera(it.target, it.zoomLevel) }
    mapsRepository.observableMapType().observe(this) { map.mapType = it }
  }

  private fun attachCustomViews(map: MapFragment) {
    val repositionView = LocationOfInterestRepositionView(context, map)
    mapContainerViewModel.moveLocationOfInterestVisibility.observe(this) {
      repositionView.visibility = it
    }
    binding.mapOverlay.addView(repositionView)

    val polygonDrawingView = PolygonDrawingView(context, map)
    mapContainerViewModel.getAddPolygonVisibility().observe(this) {
      polygonDrawingView.visibility = it
    }
    binding.mapOverlay.addView(polygonDrawingView)
  }

  /** Opens a dialog for selecting a [MapType] for the basemap layer. */
  private fun showMapTypeSelectorDialog() {
    val types = mapFragment.availableMapTypes
    NavHostFragment.findNavController(this)
      .navigate(
        HomeScreenFragmentDirections.actionHomeScreenFragmentToMapTypeDialogFragment(
          types.toTypedArray()
        )
      )
  }

  private fun showConfirmationDialog(point: Point) {
    AlertDialog.Builder(requireContext())
      .setTitle(R.string.move_point_confirmation)
      .setPositiveButton(android.R.string.ok) { _, _ -> moveToNewPosition(point) }
      .setNegativeButton(android.R.string.cancel) { _, _ ->
        mapContainerViewModel.setMode(MapContainerViewModel.Mode.DEFAULT)
      }
      .setCancelable(true)
      .create()
      .show()
  }

  private fun moveToNewPosition(point: Point) {
    val locationOfInterest = mapContainerViewModel.reposLocationOfInterest
    if (locationOfInterest.isEmpty) {
      Timber.e("Move point failed: No locationOfInterest selected")
      return
    }
    if (locationOfInterest.get().type !== LocationOfInterestType.POINT) {
      Timber.e("Only point locations of interest can be moved")
      return
    }
    val loi = locationOfInterest.get()
    val newPointOfInterest =
      loi.copy(
        loi.id,
        loi.surveyId,
        loi.job,
        loi.customId,
        loi.caption,
        loi.created,
        loi.lastModified,
        point
      )
    homeScreenViewModel.updateLocationOfInterest(newPointOfInterest)
  }

  private fun onBottomSheetStateChange(state: BottomSheetState, map: MapFragment) {
    val loi: Optional<LocationOfInterest> = Optional.ofNullable(state.locationOfInterest)
    mapContainerViewModel.setSelectedLocationOfInterest(loi)
    when (state.visibility) {
      BottomSheetState.Visibility.VISIBLE -> {
        map.disableGestures()
        // TODO(#358): Once polygon drawing is implemented, pan & zoom to polygon when
        // selected. This will involve calculating centroid and possibly zoom level based on
        // vertices.
        loi
          .filter { it.type === LocationOfInterestType.POINT }
          .ifPresent { mapContainerViewModel.panAndZoomCamera(it.geometry.vertices[0]) }
      }
      BottomSheetState.Visibility.HIDDEN -> map.enableGestures()
    }
  }

  private fun onSurveyChange(survey: Loadable<Survey>) {
    if (survey.isLoaded) {
      enableAddLocationOfInterestBtn()
    } else {
      disableAddLocationOfInterestBtn()
    }
  }

  private fun enableAddLocationOfInterestBtn() {
    mapContainerViewModel.setLocationOfInterestButtonBackgroundTint(R.color.colorMapAccent)
  }

  private fun disableAddLocationOfInterestBtn() {
    // NOTE: We don't call addLocationOfInterestBtn.setEnabled(false) here since calling it before
    // the fab is
    // shown corrupts its padding when used with useCompatPadding="true".
    mapContainerViewModel.setLocationOfInterestButtonBackgroundTint(R.color.colorGrey500)
  }

  private fun onLocationLockStateChange(result: BooleanOrError, map: MapFragment) {
    result.error().ifPresent { t: Throwable? -> onLocationLockError(t) }
    if (result.isTrue) {
      Timber.d("Location lock enabled")
      map.enableCurrentLocationIndicator()
    } else {
      Timber.d("Location lock disabled")
    }
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

  private fun onCameraUpdate(update: MapContainerViewModel.CameraUpdate, map: MapFragment) {
    Timber.v("Update camera: %s", update)
    if (update.zoomLevel.isPresent) {
      var zoomLevel = update.zoomLevel.get()
      if (!update.isAllowZoomOut) {
        zoomLevel = max(zoomLevel, map.currentZoomLevel)
      }
      map.moveCamera(update.center, zoomLevel)
    } else {
      map.moveCamera(update.center)
    }
  }

  private fun onZoomThresholdCrossed() {
    Timber.v("Refresh markers after zoom threshold crossed")

    mapFragment.refreshMarkerIcons()
  }

  override fun onDestroy() {
    mapContainerViewModel.closeProviders()
    super.onDestroy()
  }
}
