/*
 * Copyright 2020 Google LLC
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
package com.google.android.ground.ui.offlineareas.viewer

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.toLiveData
import com.google.android.ground.coroutines.IoDispatcher
import com.google.android.ground.model.imagery.OfflineArea
import com.google.android.ground.repository.LocationOfInterestRepository
import com.google.android.ground.repository.MapStateRepository
import com.google.android.ground.repository.OfflineAreaRepository
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.rx.Nil
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.system.LocationManager
import com.google.android.ground.system.PermissionsManager
import com.google.android.ground.system.SettingsManager
import com.google.android.ground.ui.common.BaseMapViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import timber.log.Timber

/**
 * View model for the OfflineAreaViewerFragment. Manges offline area deletions and calculates the
 * storage size of an area on the user's device.
 */
class OfflineAreaViewerViewModel
@Inject
constructor(
  offlineAreaRepository: OfflineAreaRepository,
  @ApplicationContext context: Context,
  locationManager: LocationManager,
  mapStateRepository: MapStateRepository,
  settingsManager: SettingsManager,
  permissionsManager: PermissionsManager,
  surveyRepository: SurveyRepository,
  locationOfInterestRepository: LocationOfInterestRepository,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) :
  BaseMapViewModel(
    locationManager,
    mapStateRepository,
    settingsManager,
    offlineAreaRepository,
    permissionsManager,
    surveyRepository,
    locationOfInterestRepository,
    ioDispatcher
  ) {

  private val fragmentArgs: @Hot(replays = true) PublishSubject<OfflineAreaViewerFragmentArgs> =
    PublishSubject.create()
  private val removeAreaClicks: @Hot PublishSubject<Nil> = PublishSubject.create()

  private val context: WeakReference<Context>

  /** Returns the offline area associated with this view model. */
  @JvmField val offlineArea: LiveData<OfflineArea>
  @JvmField var areaName: LiveData<String>

  private var offlineAreaId: String? = null

  init {
    this.context = WeakReference(context)

    // We only need to convert this single to a flowable in order to use it with LiveData.
    // It still only contains a single offline area returned by getOfflineArea.
    val offlineAreaItemAsFlowable: @Hot Flowable<OfflineArea> =
      this.fragmentArgs
        .map(OfflineAreaViewerFragmentArgs::getOfflineAreaId)
        .flatMapSingle(offlineAreaRepository::getOfflineArea)
        .doOnError { Timber.e(it, "Couldn't render area %s", offlineAreaId) }
        .toFlowable(BackpressureStrategy.LATEST)

    areaName = offlineAreaItemAsFlowable.map(OfflineArea::name).toLiveData()
    offlineArea = offlineAreaItemAsFlowable.toLiveData()
  }

  /** Gets a single offline area by the id passed to the OfflineAreaViewerFragment's arguments. */
  fun loadOfflineArea(args: OfflineAreaViewerFragmentArgs) {
    fragmentArgs.onNext(args)
    offlineAreaId = args.offlineAreaId
  }

  /** Deletes the area associated with this viewmodel. */
  fun removeArea() {
    Timber.d("Removing offline area %s", offlineArea.value)
    removeAreaClicks.onNext(Nil.NIL)
  }
}
