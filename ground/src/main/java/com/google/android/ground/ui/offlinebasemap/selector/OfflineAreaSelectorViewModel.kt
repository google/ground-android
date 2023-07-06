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
package com.google.android.ground.ui.offlinebasemap.selector

import android.content.res.Resources
import androidx.lifecycle.LiveData
import androidx.lifecycle.toLiveData
import com.google.android.ground.R
import com.google.android.ground.model.imagery.OfflineArea
import com.google.android.ground.model.imagery.TileSource
import com.google.android.ground.persistence.uuid.OfflineUuidGenerator
import com.google.android.ground.repository.MapStateRepository
import com.google.android.ground.repository.OfflineAreaRepository
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.rx.Event
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.system.LocationManager
import com.google.android.ground.system.PermissionsManager
import com.google.android.ground.system.SettingsManager
import com.google.android.ground.ui.common.BaseMapViewModel
import com.google.android.ground.ui.map.Bounds
import com.google.android.ground.ui.map.Map
import com.google.android.ground.ui.map.MapController
import io.reactivex.processors.FlowableProcessor
import io.reactivex.processors.PublishProcessor
import javax.inject.Inject
import timber.log.Timber

/** States and behaviors of Map UI used to select areas for download and viewing offline. */
class OfflineAreaSelectorViewModel
@Inject
internal constructor(
  private val offlineAreaRepository: OfflineAreaRepository,
  private val offlineUuidGenerator: OfflineUuidGenerator,
  private val resources: Resources,
  locationManager: LocationManager,
  surveyRepository: SurveyRepository,
  mapStateRepository: MapStateRepository,
  settingsManager: SettingsManager,
  permissionsManager: PermissionsManager,
  mapController: MapController,
) :
  BaseMapViewModel(
    locationManager,
    mapStateRepository,
    settingsManager,
    permissionsManager,
    mapController
  ) {
  enum class DownloadMessage {
    STARTED,
    FAILURE
  }

  private val downloadClicks: @Hot FlowableProcessor<OfflineArea> = PublishProcessor.create()
  val downloadMessages: LiveData<Event<DownloadMessage>>
  val tileSources: List<TileSource>
  private var viewport: Bounds? = null

  init {
    downloadMessages =
      downloadClicks
        .switchMapSingle { baseMap: OfflineArea ->
          offlineAreaRepository
            .addOfflineAreaAndEnqueue(baseMap)
            .toSingleDefault(DownloadMessage.STARTED)
            .onErrorReturn { e: Throwable -> onEnqueueError(e) }
            .map { Event.create(it) }
        }
        .toLiveData()
    tileSources = surveyRepository.activeSurvey!!.tileSources
  }

  private fun onEnqueueError(e: Throwable): DownloadMessage {
    Timber.e("Failed to add area and queue downloads: %s", e.message)
    return DownloadMessage.FAILURE
  }

  fun onDownloadClick() {
    viewport?.let {
      downloadClicks.onNext(
        OfflineArea(
          offlineUuidGenerator.generateUuid(),
          OfflineArea.State.PENDING,
          it,
          resources.getString(R.string.unnamed_area)
        )
      )
    }
  }

  fun onMapReady(map: Map) {
    tileSources.forEach { map.addTileOverlay(it) }
    disposeOnClear(cameraBoundUpdates.subscribe { viewport = it })
  }
}
