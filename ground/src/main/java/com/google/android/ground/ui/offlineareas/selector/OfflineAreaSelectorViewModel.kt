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
package com.google.android.ground.ui.offlineareas.selector

import android.content.res.Resources
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.ground.R
import com.google.android.ground.coroutines.IoDispatcher
import com.google.android.ground.repository.LocationOfInterestRepository
import com.google.android.ground.repository.MapStateRepository
import com.google.android.ground.repository.OfflineAreaRepository
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.system.LocationManager
import com.google.android.ground.system.PermissionsManager
import com.google.android.ground.system.SettingsManager
import com.google.android.ground.ui.common.BaseMapViewModel
import com.google.android.ground.ui.common.SharedViewModel
import com.google.android.ground.ui.map.Bounds
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.util.toMb
import com.google.android.ground.util.toMbString
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

private const val MIN_DOWNLOAD_ZOOM_LEVEL = 9
private const val MAX_AREA_DOWNLOAD_SIZE_MB = 50

/** States and behaviors of Map UI used to select areas for download and viewing offline. */
@SharedViewModel
class OfflineAreaSelectorViewModel
@Inject
internal constructor(
  private val offlineAreaRepository: OfflineAreaRepository,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  private val resources: Resources,
  locationManager: LocationManager,
  surveyRepository: SurveyRepository,
  mapStateRepository: MapStateRepository,
  settingsManager: SettingsManager,
  permissionsManager: PermissionsManager,
  locationOfInterestRepository: LocationOfInterestRepository,
) :
  BaseMapViewModel(
    locationManager,
    mapStateRepository,
    settingsManager,
    offlineAreaRepository,
    permissionsManager,
    surveyRepository,
    locationOfInterestRepository,
  ) {

  val remoteTileSource = offlineAreaRepository.getRemoteTileSource()
  private var viewport: Bounds? = null
  private val offlineAreaSizeLoadingSymbol =
    resources.getString(R.string.offline_area_size_loading_symbol)
  val isDownloadProgressVisible = MutableLiveData(false)
  val downloadProgress = MutableLiveData(0f)
  val bottomText = MutableLiveData<String?>(null)
  val downloadButtonEnabled = MutableLiveData(false)

  private val _navigate = MutableSharedFlow<UiState>(replay = 0)
  val navigate = _navigate.asSharedFlow()

  fun onDownloadClick() {
    if (viewport == null) {
      // Download was likely clicked before map was ready.
      return
    }

    isDownloadProgressVisible.value = true
    downloadProgress.value = 0f
    viewModelScope.launch(ioDispatcher) {
      offlineAreaRepository.downloadTiles(viewport!!).collect { (bytesDownloaded, totalBytes) ->
        val progressValue =
          if (totalBytes > 0) {
            (bytesDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
          } else {
            0f
          }
        downloadProgress.postValue(progressValue)
      }
      isDownloadProgressVisible.postValue(false)
      _navigate.emit(UiState.OfflineAreaBackToHomeScreen)
    }
  }

  fun onCancelClick() {
    viewModelScope.launch { _navigate.emit(UiState.Up) }
  }

  override fun onMapDragged() {
    downloadButtonEnabled.postValue(false)
    bottomText.postValue(null)
    super.onMapDragged()
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

  private suspend fun updateDownloadSize(bounds: Bounds) {
    if (!offlineAreaRepository.hasHiResImagery(bounds)) {
      onUnavailableAreaSelected()
      return
    }
    bottomText.postValue(
      resources.getString(R.string.selected_offline_area_size, offlineAreaSizeLoadingSymbol)
    )
    val sizeInMb = offlineAreaRepository.estimateSizeOnDisk(bounds).toMb()
    if (sizeInMb > MAX_AREA_DOWNLOAD_SIZE_MB) {
      onLargeAreaSelected()
    } else {
      onDownloadableAreaSelected(sizeInMb)
    }
  }

  private fun onUnavailableAreaSelected() {
    bottomText.postValue(resources.getString(R.string.no_imagery_available_for_area))
    downloadButtonEnabled.postValue(false)
  }

  private fun onDownloadableAreaSelected(sizeInMb: Float) {
    bottomText.postValue(
      resources.getString(R.string.selected_offline_area_size, sizeInMb.toMbString())
    )
    downloadButtonEnabled.postValue(true)
  }

  private fun onLargeAreaSelected() {
    bottomText.postValue(resources.getString(R.string.selected_offline_area_too_large))
    downloadButtonEnabled.postValue(false)
  }
}
