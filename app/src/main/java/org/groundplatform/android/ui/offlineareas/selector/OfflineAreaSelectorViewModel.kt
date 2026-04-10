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
import org.groundplatform.android.model.map.CameraPosition
import org.groundplatform.android.repository.MapStateRepository
import org.groundplatform.android.repository.OfflineAreaRepository
import org.groundplatform.android.repository.SurveyRepository
import org.groundplatform.android.system.LocationManager
import org.groundplatform.android.system.NetworkManager
import org.groundplatform.android.system.PermissionsManager
import org.groundplatform.android.system.SettingsManager
import org.groundplatform.android.ui.common.BaseMapViewModel
import org.groundplatform.android.ui.offlineareas.selector.model.OfflineAreaSelectorEvent
import org.groundplatform.android.ui.offlineareas.selector.model.OfflineAreaSelectorState
import org.groundplatform.android.util.toMb
import org.groundplatform.android.util.toMbString
import org.groundplatform.domain.model.map.Bounds
import org.groundplatform.domain.repository.LocationOfInterestRepositoryInterface
import timber.log.Timber

private const val MIN_DOWNLOAD_ZOOM_LEVEL = 9
private const val MAX_AREA_DOWNLOAD_SIZE_MB = 50

/** States and behaviors of Map UI used to select areas for download and viewing offline. */
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
  locationOfInterestRepository: LocationOfInterestRepositoryInterface,
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

  private val _uiState = MutableStateFlow(OfflineAreaSelectorState())
  val uiState: StateFlow<OfflineAreaSelectorState> = _uiState

  private val _uiEvent = MutableSharedFlow<OfflineAreaSelectorEvent>(replay = 0)
  val uiEvent = _uiEvent.asSharedFlow()

  var downloadJob: Job? = null

  fun onDownloadClick() {
    if (!networkManager.isNetworkConnected()) {
      viewModelScope.launch { _uiEvent.emit(OfflineAreaSelectorEvent.NetworkUnavailable) }
      return
    }

    if (viewport == null) {
      // Download was likely clicked before map was ready.
      return
    }

    _uiState.value =
      _uiState.value.copy(downloadState = OfflineAreaSelectorState.DownloadState.InProgress(0f))
    downloadJob =
      viewModelScope.launch(ioDispatcher) {
        offlineAreaRepository
          .downloadTiles(viewport!!)
          .catch {
            _uiState.value =
              _uiState.value.copy(downloadState = OfflineAreaSelectorState.DownloadState.Idle)
            _uiEvent.emit(OfflineAreaSelectorEvent.DownloadError)
            Timber.d("Download Stopped by $it ")
          }
          .collect { (bytesDownloaded, totalBytes) ->
            updateDownloadProgress(bytesDownloaded, totalBytes)
          }
        _uiState.value =
          _uiState.value.copy(downloadState = OfflineAreaSelectorState.DownloadState.Idle)
        _uiEvent.emit(OfflineAreaSelectorEvent.NavigateOfflineAreaBackToHomeScreen)
      }
  }

  private fun updateDownloadProgress(bytesDownloaded: Int, totalBytes: Int) {
    val progressValue =
      if (totalBytes > 0) {
        (bytesDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
      } else {
        0f
      }
    _uiState.value =
      _uiState.value.copy(
        downloadState = OfflineAreaSelectorState.DownloadState.InProgress(progressValue)
      )
  }

  fun onCancelClick() {
    viewModelScope.launch { _uiEvent.emit(OfflineAreaSelectorEvent.NavigateUp) }
  }

  fun stopDownloading() {
    downloadJob?.cancel()
    downloadJob = null
    _uiState.value =
      _uiState.value.copy(downloadState = OfflineAreaSelectorState.DownloadState.Idle)
  }

  override fun onMapDragged() {
    _uiState.value = _uiState.value.copy(bottomTextState = null)
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
    _uiState.value =
      _uiState.value.copy(bottomTextState = OfflineAreaSelectorState.BottomTextState.Loading)

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
    _uiState.value =
      _uiState.value.copy(bottomTextState = OfflineAreaSelectorState.BottomTextState.NetworkError)
  }

  private fun onUnavailableAreaSelected() {
    _uiState.value =
      _uiState.value.copy(
        bottomTextState = OfflineAreaSelectorState.BottomTextState.NoImageryAvailable
      )
  }

  private fun onDownloadableAreaSelected(sizeInMb: Float) {
    _uiState.value =
      _uiState.value.copy(
        bottomTextState = OfflineAreaSelectorState.BottomTextState.AreaSize(sizeInMb.toMbString())
      )
  }

  private fun onLargeAreaSelected() {
    _uiState.value =
      _uiState.value.copy(bottomTextState = OfflineAreaSelectorState.BottomTextState.AreaTooLarge)
  }
}
