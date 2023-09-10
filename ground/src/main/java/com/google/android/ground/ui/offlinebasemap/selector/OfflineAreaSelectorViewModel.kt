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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.ground.R
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
import com.google.android.ground.ui.common.MapConfig
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.ui.common.SharedViewModel
import com.google.android.ground.ui.map.Bounds
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.map.Map
import com.google.android.ground.ui.map.MapType
import javax.inject.Inject
import kotlin.math.ceil
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch

private const val MIN_DOWNLOAD_ZOOM_LEVEL = 9
private const val MAX_AREA_DOWNLOAD_SIZE_MB = 50

/** States and behaviors of Map UI used to select areas for download and viewing offline. */
@SharedViewModel
class OfflineAreaSelectorViewModel
@Inject
internal constructor(
  private val offlineAreaRepository: OfflineAreaRepository,
  private val navigator: Navigator,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  private val resources: Resources,
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

  val tileSources: List<TileSource>
  private var viewport: Bounds? = null
  val isDownloadProgressVisible = MutableLiveData(false)
  val downloadProgressMax = MutableLiveData(0)
  val downloadProgress = MutableLiveData(0)
  val sizeOnDisk = MutableLiveData<String>(null)
  val visibleBottomTextViewId = MutableLiveData<Int>(null)
  val downloadButtonEnabled = MutableLiveData(false)

  override val mapConfig: MapConfig
    get() = super.mapConfig.copy(showTileOverlays = false, overrideMapType = MapType.ROAD)

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
      offlineAreaRepository.downloadTiles(viewport!!).collect { (bytesDownloaded, totalBytes) ->
        // Set total bytes / max value on first iteration.
        if (downloadProgressMax.value != totalBytes) downloadProgressMax.postValue(totalBytes)
        // Add number of bytes downloaded to progress.
        downloadProgress.postValue(bytesDownloaded)
      }
      isDownloadProgressVisible.postValue(false)
      navigator.navigateUp()
    }
  }

  fun onCancelClick() {
    navigator.navigateUp()
  }

  fun onMapReady(map: Map) {
    tileSources.forEach { map.addTileOverlay(it) }
  }

  override fun onMapCameraMoved(newCameraPosition: CameraPosition) {
    super.onMapCameraMoved(newCameraPosition)
    val bounds = newCameraPosition.bounds
    val zoomLevel = newCameraPosition.zoomLevel
    if (bounds == null || zoomLevel == null) return
    if (zoomLevel < MIN_DOWNLOAD_ZOOM_LEVEL) {
      onLargeAreaSelected()
      return
    }

    viewport = bounds
    viewModelScope.launch(ioDispatcher) { updateDownloadSize(bounds) }
  }

  private fun onStartEstimatingDownloadSize() {
    sizeOnDisk.postValue(resources.getString(R.string.offline_area_size_loading_symbol))
    visibleBottomTextViewId.postValue(R.id.size_on_disk_text_view)
  }

  private suspend fun updateDownloadSize(bounds: Bounds) {
    onStartEstimatingDownloadSize()
    val sizeInMb = offlineAreaRepository.estimateSizeOnDisk(bounds) / (1024f * 1024f)
    if (sizeInMb > MAX_AREA_DOWNLOAD_SIZE_MB) {
      onLargeAreaSelected()
    } else {
      onDownloadableAreaSelected(sizeInMb)
    }
  }

  private fun onDownloadableAreaSelected(sizeInMb: Float) {
    val sizeString = if (sizeInMb < 1f) "<1" else ceil(sizeInMb).toInt().toString()
    sizeOnDisk.postValue(sizeString)
    downloadButtonEnabled.postValue(true)
  }

  private fun onLargeAreaSelected() {
    visibleBottomTextViewId.postValue(R.id.area_too_large_text_view)
    downloadButtonEnabled.postValue(false)
  }
}
