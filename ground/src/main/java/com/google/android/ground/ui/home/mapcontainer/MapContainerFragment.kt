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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ground.R
import com.google.android.ground.databinding.MapContainerFragBinding
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.locationofinterest.LocationOfInterestType
import com.google.android.ground.repository.MapsRepository
import com.google.android.ground.rx.RxAutoDispose
import com.google.android.ground.system.PermissionDeniedException
import com.google.android.ground.system.SettingsChangeRequestCanceled
import com.google.android.ground.ui.common.AbstractMapViewerFragment
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.ui.home.BottomSheetState
import com.google.android.ground.ui.home.HomeScreenFragmentDirections
import com.google.android.ground.ui.home.HomeScreenViewModel
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.map.MapFragment
import com.google.android.ground.ui.map.MapLocationOfInterest
import com.google.android.ground.ui.map.MapType
import com.google.android.ground.ui.map.gms.toGoogleMapsObject
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

  @Inject lateinit var loiCardSource: LoiCardSource
  @Inject lateinit var mapsRepository: MapsRepository
  @Inject lateinit var navigator: Navigator

  lateinit var polygonDrawingViewModel: PolygonDrawingViewModel
  private lateinit var mapContainerViewModel: MapContainerViewModel
  private lateinit var homeScreenViewModel: HomeScreenViewModel
  private lateinit var binding: MapContainerFragBinding

  private val adapter: LoiCardAdapter = LoiCardAdapter()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mapContainerViewModel = getViewModel(MapContainerViewModel::class.java)
    homeScreenViewModel = getViewModel(HomeScreenViewModel::class.java)
    val locationOfInterestRepositionViewModel =
      getViewModel(LocationOfInterestRepositionViewModel::class.java)
    polygonDrawingViewModel = getViewModel(PolygonDrawingViewModel::class.java)
    mapFragment.locationOfInterestInteractions
      .`as`(RxAutoDispose.disposeOnDestroy(this))
      .subscribe { mapContainerViewModel.onMarkerClick(it) }
    mapFragment.locationOfInterestInteractions
      .`as`(RxAutoDispose.disposeOnDestroy(this))
      .subscribe { homeScreenViewModel.onMarkerClick(it) }
    mapFragment.ambiguousLocationOfInterestInteractions
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
      .subscribe { onCameraMoved(it) }
    mapFragment.tileProviders.`as`(RxAutoDispose.disposeOnDestroy(this)).subscribe {
      mapContainerViewModel.queueTileProvider(it)
    }

    polygonDrawingViewModel.unsavedMapLocationsOfInterest.observe(this) {
      mapContainerViewModel.setUnsavedMapLocationsOfInterest(it)
    }
    locationOfInterestRepositionViewModel
      .getConfirmButtonClicks()
      .`as`(RxAutoDispose.autoDisposable(this))
      .subscribe { showConfirmationDialog(it) }
    locationOfInterestRepositionViewModel
      .getCancelButtonClicks()
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
    loiCardSource.locationsOfInterest.observe(this) { adapter.updateData(it) }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = MapContainerFragBinding.inflate(inflater, container, false)
    binding.viewModel = mapContainerViewModel
    binding.homeScreenViewModel = homeScreenViewModel
    binding.lifecycleOwner = this
    setupRecyclerView(binding.mapControls.recyclerView)
    return binding.root
  }

  private fun setupRecyclerView(recyclerView: RecyclerView) {
    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
    recyclerView.addOnScrollListener(
      object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
          super.onScrollStateChanged(recyclerView, newState)
          val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
          val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
          val firstCompletelyVisiblePosition =
            layoutManager.findFirstCompletelyVisibleItemPosition()
          var midPosition = (firstVisiblePosition + lastVisiblePosition) / 2

          // Focus the last card
          if (firstCompletelyVisiblePosition > midPosition) {
            midPosition = firstCompletelyVisiblePosition
          }

          adapter.focusItemAtIndex(midPosition)
        }
      }
    )
    adapter.setLoiCardFocusedListener { mapFragment.setActiveLocationOfInterest(it) }
    adapter.setCollectDataListener { navigateToDataCollectionFragment(it) }
    recyclerView.adapter = adapter
  }

  private fun navigateToDataCollectionFragment(loi: LocationOfInterest) {
    navigator.navigate(
      HomeScreenFragmentDirections.actionHomeScreenFragmentToDataCollectionFragment(
        /* surveyId = */ loi.surveyId,
        /* locationOfInterestId = */ loi.id,
        /* submissionId = */ "dummy submission id"
      )
    )
  }

  override fun onMapReady(mapFragment: MapFragment) {
    Timber.d("MapAdapter ready. Updating subscriptions")

    // Custom views rely on the same instance of MapFragment. That couldn't be injected via Dagger.
    // Hence, initializing them here instead of inflating in layout.
    attachCustomViews(mapFragment)
    mapContainerViewModel.setLocationLockEnabled(true)
    polygonDrawingViewModel.setLocationLockEnabled(true)

    // Observe events emitted by the ViewModel.
    mapContainerViewModel.mapLocationsOfInterest.observe(this) {
      mapFragment.renderLocationsOfInterest(it)
    }
    mapContainerViewModel.locationLockState.observe(this) { result ->
      onLocationLockStateChange(result, mapFragment)
    }
    mapContainerViewModel.cameraUpdateRequests.observe(this) { update ->
      update.ifUnhandled { data -> onCameraUpdateRequest(data, mapFragment) }
    }
    homeScreenViewModel.bottomSheetState.observe(this) { state: BottomSheetState ->
      onBottomSheetStateChange(state, mapFragment)
    }
    mapContainerViewModel.mbtilesFilePaths.observe(this) { mapFragment.addLocalTileOverlays(it) }

    mapsRepository.observableMapType().observe(this) { mapFragment.mapType = it }
  }

  private fun attachCustomViews(map: MapFragment) {
    val repositionView = LocationOfInterestRepositionView(requireContext(), map)
    mapContainerViewModel.moveLocationOfInterestVisibility.observe(this) {
      repositionView.visibility = it
    }
    binding.mapOverlay.addView(repositionView)

    val polygonDrawingView = PolygonDrawingView(requireContext(), map)
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
    val newPointOfInterest = loi.copy(geometry = point)
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

  private fun onLocationLockStateChange(result: Result<Boolean>, map: MapFragment) {
    result.fold(
      { isSuccessful: Boolean ->
        {
          Timber.d("Location lock: $isSuccessful")
          if (isSuccessful) {
            map.enableCurrentLocationIndicator()
          }
        }
      },
      { exception: Throwable -> onLocationLockError(exception) }
    )
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

  private fun onZoomThresholdCrossed() {
    Timber.v("Refresh markers after zoom threshold crossed")

    mapFragment.refreshRenderedLocationsOfInterest()
  }

  override fun onDestroy() {
    mapContainerViewModel.closeProviders()
    super.onDestroy()
  }

  private fun onCameraMoved(position: CameraPosition) {
    mapContainerViewModel.onCameraMove(position)
    loiCardSource.onCameraBoundsUpdated(position.bounds?.toGoogleMapsObject())
  }
}
