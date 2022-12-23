package com.google.android.ground.ui.home.mapcontainer

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MutableLiveData
import com.google.android.ground.R
import com.google.android.ground.rx.Event
import com.google.android.ground.rx.Nil
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.common.AbstractViewModel
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.map.LocationController
import com.google.android.ground.ui.map.MapController
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import timber.log.Timber

open class BaseMapViewModel
constructor(
  private val locationController: LocationController,
  private val mapController: MapController
) : AbstractViewModel() {

  private val locationLockEnabled: @Hot(replays = true) MutableLiveData<Boolean> = MutableLiveData()
  private val selectMapTypeClicks: @Hot Subject<Nil> = PublishSubject.create()

  val cameraUpdateRequests: LiveData<Event<CameraPosition>>
  val iconTint: LiveData<Int>
  val locationLockState: LiveData<Result<Boolean>>

  init {
    // THIS SHOULD NOT BE CALLED ON CONFIG CHANGE
    val locationLockStateFlowable = locationController.getLocationLockUpdates()
    iconTint =
      LiveDataReactiveStreams.fromPublisher(
        locationLockStateFlowable
          .map { locked: Result<Boolean> ->
            if (locked.getOrDefault(false)) R.color.colorMapBlue else R.color.colorGrey800
          }
          .startWith(R.color.colorGrey800)
      )
    locationLockState =
      LiveDataReactiveStreams.fromPublisher(
        locationLockStateFlowable.startWith(Result.success(false))
      )
    cameraUpdateRequests = LiveDataReactiveStreams.fromPublisher(createCameraUpdateFlowable())
  }

  fun getLocationLockEnabled(): LiveData<Boolean> = locationLockEnabled

  fun setLocationLockEnabled(enabled: Boolean) {
    locationLockEnabled.postValue(enabled)
  }

  fun onMapTypeButtonClicked() {
    selectMapTypeClicks.onNext(Nil.NIL)
  }

  fun getSelectMapTypeClicks(): Observable<Nil> {
    return selectMapTypeClicks
  }

  fun onLocationLockClick() {
    if (isLocationLockEnabled()) {
      locationController.unlock()
    } else {
      locationController.lock()
    }
  }

  fun onMapDrag() {
    if (isLocationLockEnabled()) {
      Timber.d("User dragged map. Disabling location lock")
      locationController.unlock()
    }
  }

  private fun isLocationLockEnabled(): Boolean = locationLockState.value!!.getOrDefault(false)

  private fun createCameraUpdateFlowable(): Flowable<Event<CameraPosition>> =
    mapController.getCameraUpdates().map { Event.create(it) }

  open fun onCameraMove(position: CameraPosition) {
    // Override if needed
  }
}
