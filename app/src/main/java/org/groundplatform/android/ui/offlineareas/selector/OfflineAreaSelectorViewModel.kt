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
package org.groundplatform.android.ui.offlineareas.selector

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.groundplatform.android.di.coroutines.IoDispatcher
import org.groundplatform.android.model.imagery.RemoteMogTileSource
import org.groundplatform.android.model.imagery.TileSource
import org.groundplatform.android.model.map.Bounds
import org.groundplatform.android.model.map.CameraPosition
import org.groundplatform.android.repository.LocationOfInterestRepository
import org.groundplatform.android.repository.MapStateRepository
import org.groundplatform.android.repository.OfflineAreaRepository
import org.groundplatform.android.repository.SurveyRepository
import org.groundplatform.android.system.LocationManager
import org.groundplatform.android.system.NetworkManager
import org.groundplatform.android.system.PermissionsManager
import org.groundplatform.android.system.SettingsManager
import org.groundplatform.android.ui.common.BaseMapViewModel
import org.groundplatform.android.ui.common.SharedViewModel
import org.groundplatform.android.ui.offlineareas.selector.model.BottomTextState
import org.groundplatform.android.ui.offlineareas.selector.model.UiState
import org.groundplatform.android.util.toMb
import org.groundplatform.android.util.toMbString
import timber.log.Timber

private const val MIN_DOWNLOAD_ZOOM_LEVEL = 9
private const val MAX_AREA_DOWNLOAD_SIZE_MB = 50

/** States and behaviors of Map UI used to select areas for download and viewing offline. */
@SharedViewModel
class OfflineAreaSelectorViewModel
@Inject
internal constructor(
  private val offlineAreaRepository: OfflineAreaRepository,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  locationManager: LocationManager,
  surveyRepository: SurveyRepository,
  mapStateRepository: MapStateRepository,
  settingsManager: SettingsManager,
  permissionsManager: PermissionsManager,
  locationOfInterestRepository: LocationOfInterestRepository,
  private val networkManager: NetworkManager,
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

  val remoteTileSource: TileSource = RemoteMogTileSource

  private var viewport: Bounds? = null
  val isDownloadProgressVisible = MutableLiveData(false)
  val downloadProgress = MutableLiveData(0f)

  private val _bottomTextState = MutableStateFlow<BottomTextState?>(null)
  val bottomTextState: StateFlow<BottomTextState?> = _bottomTextState

  val downloadButtonEnabled = MutableLiveData(false)
  val isFailure = MutableLiveData(false)

  private val _navigate = MutableSharedFlow<UiState>(replay = 0)
  val navigate = _navigate.asSharedFlow()

  private val _networkUnavailableEvent = MutableSharedFlow<Unit>()
  val networkUnavailableEvent = _networkUnavailableEvent.asSharedFlow()

  var downloadJob: Job? = null

  fun onDownloadClick() {
    if (!networkManager.isNetworkConnected()) {
      viewModelScope.launch { _networkUnavailableEvent.emit(Unit) }
      return
    }

    if (viewport == null) {
      // Download was likely clicked before map was ready.
      return
    }

    isDownloadProgressVisible.value = true
    downloadProgress.value = 0f
    downloadJob =
      viewModelScope.launch(ioDispatcher) {
        offlineAreaRepository
          .downloadTiles(viewport!!)
          .catch {
            isFailure.postValue(true)
            isDownloadProgressVisible.postValue(false)
            Timber.d("Download Stopped by $it ")
          }
          .collect { (bytesDownloaded, totalBytes) ->
            updateDownloadProgress(bytesDownloaded, totalBytes)
          }
        isDownloadProgressVisible.postValue(false)
        _navigate.emit(UiState.OfflineAreaBackToHomeScreen)
      }
  }

  private fun updateDownloadProgress(bytesDownloaded: Int, totalBytes: Int) {
    val progressValue =
      if (totalBytes > 0) {
        (bytesDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
      } else {
        0f
      }
    downloadProgress.postValue(progressValue)
  }

  fun onCancelClick() {
    viewModelScope.launch { _navigate.emit(UiState.Up) }
  }

  fun stopDownloading() {
    downloadJob?.cancel()
    downloadJob = null
    isDownloadProgressVisible.postValue(false)
  }

  override fun onMapDragged() {
    downloadButtonEnabled.postValue(false)
    _bottomTextState.value = null
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
    Timber.d("Checking imagery availability for bounds: $bounds")
    val hasHiResImagery =
      offlineAreaRepository.hasHiResImagery(bounds).getOrElse {
        onUpdateDownloadSizeError()
        return
      }
    if (!hasHiResImagery) {
      Timber.d("No hi-res imagery available for selected area")
      onUnavailableAreaSelected()
      return
    }
    _bottomTextState.value = BottomTextState.Loading

    offlineAreaRepository
      .estimateSizeOnDisk(bounds)
      .onSuccess {
        val sizeInMb = it.toMb()
        Timber.d("Estimated download size: ${sizeInMb}MB")
        if (sizeInMb > MAX_AREA_DOWNLOAD_SIZE_MB) {
          Timber.d("Area too large: ${sizeInMb}MB > ${MAX_AREA_DOWNLOAD_SIZE_MB}MB")
          onLargeAreaSelected()
        } else {
          Timber.d("Area downloadable: ${sizeInMb}MB, enabling download button")
          onDownloadableAreaSelected(sizeInMb)
        }
      }
      .onFailure { onUpdateDownloadSizeError() }
  }

  private fun onUpdateDownloadSizeError() {
    _bottomTextState.value = BottomTextState.NetworkError
    downloadButtonEnabled.postValue(false)
  }

  private fun onUnavailableAreaSelected() {
    _bottomTextState.value = BottomTextState.NoImageryAvailable
    downloadButtonEnabled.postValue(false)
  }

  private fun onDownloadableAreaSelected(sizeInMb: Float) {
    _bottomTextState.value = BottomTextState.AreaSize(sizeInMb.toMbString())
    downloadButtonEnabled.postValue(true)
  }

  private fun onLargeAreaSelected() {
    _bottomTextState.value = BottomTextState.AreaTooLarge
    downloadButtonEnabled.postValue(false)
  }
}
