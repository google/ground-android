/*
 * Copyright 2022 Google LLC
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

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MutableLiveData
import com.google.android.ground.R
import com.google.android.ground.rx.Event
import com.google.android.ground.rx.Nil
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.map.LocationController
import com.google.android.ground.ui.map.MapController
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import timber.log.Timber

open class BaseMapViewModel
@Inject
constructor(private val locationController: LocationController, mapController: MapController) :
  AbstractViewModel() {

  protected val cameraZoomSubject: @Hot Subject<Float> = PublishSubject.create()
  private val locationLockEnabled: @Hot(replays = true) MutableLiveData<Boolean> = MutableLiveData()
  private val selectMapTypeClicks: @Hot Subject<Nil> = PublishSubject.create()

  val cameraUpdateRequests: LiveData<Event<CameraPosition>>
  val locationLockIconTint: LiveData<Int>
  val locationLockIcon: LiveData<Int>
  val locationLockState: LiveData<Result<Boolean>>

  init {
    // THIS SHOULD NOT BE CALLED ON CONFIG CHANGE
    val locationLockStateFlowable = locationController.getLocationLockUpdates()
    locationLockIconTint =
      LiveDataReactiveStreams.fromPublisher(
        locationLockStateFlowable
          .map { lockState ->
            if (lockState.getOrDefault(false)) LOCATION_LOCK_ICON_TINT_ENABLED
            else LOCATION_LOCK_ICON_TINT_DISABLED
          }
          .startWith(LOCATION_LOCK_ICON_TINT_DISABLED)
      )
    locationLockIcon =
      LiveDataReactiveStreams.fromPublisher(
        locationLockStateFlowable
          .map { lockState ->
            if (lockState.getOrDefault(false)) LOCATION_LOCK_ICON_ENABLED
            else LOCATION_LOCK_ICON_DISABLED
          }
          .startWith(LOCATION_LOCK_ICON_DISABLED)
      )
    locationLockState =
      LiveDataReactiveStreams.fromPublisher(
        locationLockStateFlowable.startWith(Result.success(false))
      )
    cameraUpdateRequests =
      LiveDataReactiveStreams.fromPublisher(
        mapController.getCameraUpdates().map { Event.create(it) }
      )
  }

  fun getLocationLockEnabled(): LiveData<Boolean> = locationLockEnabled

  fun setLocationLockEnabled(enabled: Boolean) {
    locationLockEnabled.postValue(enabled)
  }

  /** Called when map type button is clicked by the user. */
  fun onMapTypeButtonClicked() {
    selectMapTypeClicks.onNext(Nil.NIL)
  }

  fun getSelectMapTypeClicks(): Observable<Nil> {
    return selectMapTypeClicks
  }

  /** Called when location lock button is clicked by the user. */
  fun onLocationLockClick() {
    if (isLocationLockEnabled()) {
      locationController.unlock()
    } else {
      locationController.lock()
    }
  }

  /** Called when the map starts to move by the user. */
  fun onMapDragged() {
    if (isLocationLockEnabled()) {
      Timber.d("User dragged map. Disabling location lock")
      locationController.unlock()
    }
  }

  /** Returns true if the location lock is enabled. */
  private fun isLocationLockEnabled(): Boolean = locationLockState.value!!.getOrDefault(false)

  /** Called when the map camera is moved. */
  open fun onMapCameraMoved(newCameraPosition: CameraPosition) {
    newCameraPosition.zoomLevel?.let { cameraZoomSubject.onNext(it) }
  }

  companion object {
    private const val LOCATION_LOCK_ICON_TINT_ENABLED = R.color.colorMapBlue
    private const val LOCATION_LOCK_ICON_TINT_DISABLED = R.color.colorGrey800

    // TODO(Shobhit): Consider adding another icon for representing "GPS disabled" state.
    private const val LOCATION_LOCK_ICON_ENABLED = R.drawable.ic_gps_lock
    private const val LOCATION_LOCK_ICON_DISABLED = R.drawable.ic_gps_lock_not_fixed
  }
}
