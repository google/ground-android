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

package com.google.android.gnd.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gnd.R
import com.google.android.gnd.rx.BooleanOrError
import com.google.android.gnd.rx.BooleanOrError.Companion.falseValue
import com.google.android.gnd.rx.annotations.Hot
import com.google.android.gnd.system.LocationManager
import com.google.android.gnd.ui.common.AbstractViewModel
import com.google.android.gnd.ui.common.SharedViewModel
import com.google.android.gnd.util.toLiveData
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import timber.log.Timber
import javax.inject.Inject

@SharedViewModel
class LocationLockViewModel @Inject constructor(private val locationManager: LocationManager) :
    AbstractViewModel() {

    private val locationLockChangeRequests: @Hot Subject<Boolean>

    val iconTint: LiveData<Int>
    val locationLockEnabled: @Hot(replays = true) MutableLiveData<Boolean>
    val locationLockState: LiveData<BooleanOrError>
    val locationLockStateFlowable: Flowable<BooleanOrError>
    val locationUpdatesEnabled: LiveData<Boolean>

    init {
        locationLockChangeRequests = PublishSubject.create()
        locationLockEnabled = MutableLiveData()
        locationLockStateFlowable = createLocationLockStateFlowable().share()
        locationLockState = locationLockStateFlowable.toLiveData()
        iconTint = locationLockStateFlowable.map { getIconTint(it) }.toLiveData()
        locationUpdatesEnabled = locationLockStateFlowable.map(BooleanOrError::isTrue).toLiveData()
    }

    private fun createLocationLockStateFlowable(): Flowable<BooleanOrError> {
        return locationLockChangeRequests
            .switchMapSingle { toggleLocationUpdates(it) }
            .startWith(falseValue())
            .toFlowable(BackpressureStrategy.LATEST)
    }

    fun onMapDrag() {
        if (isLocationLockEnabled()) {
            Timber.d("User dragged map. Disabling location lock")
            locationLockChangeRequests.onNext(false)
        }
    }

    fun onLocationLockClick() = requestLockChange(!isLocationLockEnabled())

    fun requestLockChange(value: Boolean) = locationLockChangeRequests.onNext(value)

    fun setLocationLockEnabled(enabled: Boolean) = locationLockEnabled.postValue(enabled)

    private fun isLocationLockEnabled() = locationLockState.value?.isTrue ?: false

    private fun getIconTint(value: BooleanOrError) =
        if (value.isTrue) R.color.colorMapBlue else R.color.colorGrey800

    private fun toggleLocationUpdates(value: Boolean) =
        with(locationManager) {
            if (value) enableLocationUpdates()
            else disableLocationUpdates()
        }
}
