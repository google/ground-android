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

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.ground.coroutines.IoDispatcher
import com.google.android.ground.model.imagery.TileSource
import com.google.android.ground.repository.LocationOfInterestRepository
import com.google.android.ground.repository.MapStateRepository
import com.google.android.ground.repository.OfflineAreaRepository
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.system.LocationManager
import com.google.android.ground.system.PermissionsManager
import com.google.android.ground.system.SettingsManager
import com.google.android.ground.ui.common.BaseMapViewModel
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.ui.common.SharedViewModel
import com.google.android.ground.ui.map.Bounds
import com.google.android.ground.ui.map.Map
import com.google.android.ground.ui.map.MapType
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch

/** States and behaviors of Map UI used to select areas for download and viewing offline. */
@SharedViewModel
class OfflineAreaSelectorViewModel
@Inject
internal constructor(
  private val offlineAreaRepository: OfflineAreaRepository,
  private val navigator: Navigator,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  locationManager: LocationManager,
  surveyRepository: SurveyRepository,
  mapStateRepository: MapStateRepository,
  settingsManager: SettingsManager,
  permissionsManager: PermissionsManager,
  locationOfInterestRepository: LocationOfInterestRepository
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
  enum class DownloadMessage {
    STARTED,
    FAILURE
  }

  val tileSources: List<TileSource>
  private var viewport: Bounds? = null
  val isDownloadProgressVisible = MutableLiveData(false)
  val downloadProgressMax = MutableLiveData(0)
  val downloadProgress = MutableLiveData(0)

  init {
    tileSources = surveyRepository.activeSurvey!!.tileSources
  }

  fun onDownloadClick() {
    if (viewport == null) {
      // Download was likely clicked before map was ready.
      return
    }

    isDownloadProgressVisible.value = true
    downloadProgress.value = 0
    viewModelScope.launch(ioDispatcher) {
      offlineAreaRepository.downloadTiles(viewport!!).collect { (byteDownloaded, totalBytes) ->
        // Set total bytes / max value on first iteration.
        if (downloadProgressMax.value != totalBytes) downloadProgressMax.postValue(totalBytes)
        // Add number of bytes downloaded to progress.
        downloadProgress.postValue(byteDownloaded)
      }
      isDownloadProgressVisible.postValue(false)
      navigator.navigateUp()
    }
  }

  fun onMapReady(map: Map) {
    tileSources.forEach { map.addTileOverlay(it) }
    disposeOnClear(cameraBoundUpdates.subscribe { viewport = it })
  }
}
