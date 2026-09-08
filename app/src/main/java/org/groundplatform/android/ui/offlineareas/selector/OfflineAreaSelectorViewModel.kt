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

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.groundplatform.android.di.coroutines.IoDispatcher
import org.groundplatform.android.system.LocationManager
import org.groundplatform.android.system.PermissionsManager
import org.groundplatform.android.system.SettingsManager
import org.groundplatform.android.ui.common.BaseMapViewModel
import org.groundplatform.domain.model.imagery.RemoteMogTileSource
import org.groundplatform.domain.model.imagery.TileSource
import org.groundplatform.domain.model.map.Bounds
import org.groundplatform.domain.model.map.CameraPosition
import org.groundplatform.domain.model.util.toMb
import org.groundplatform.domain.repository.LocationOfInterestRepositoryInterface
import org.groundplatform.domain.repository.MapStateRepositoryInterface
import org.groundplatform.domain.repository.OfflineAreaRepositoryInterface
import org.groundplatform.domain.repository.SurveyRepositoryInterface
import org.groundplatform.domain.system.NetworkManagerInterface
import org.groundplatform.ui.util.toMbString
import timber.log.Timber

private const val MIN_DOWNLOAD_ZOOM_LEVEL = 9
private const val MAX_AREA_DOWNLOAD_SIZE_MB = 50

/** States and behaviors of Map UI used to select areas for download and viewing offline. */
class OfflineAreaSelectorViewModel
@Inject
internal constructor(
  private val offlineAreaRepository: OfflineAreaRepositoryInterface,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  locationManager: LocationManager,
  surveyRepository: SurveyRepositoryInterface,
  mapStateRepository: MapStateRepositoryInterface,
  settingsManager: SettingsManager,
  permissionsManager: PermissionsManager,
  locationOfInterestRepository: LocationOfInterestRepositoryInterface,
  private val networkManager: NetworkManagerInterface,
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
  private var updateDownloadSizeJob: Job? = null

  @get:VisibleForTesting
  internal var downloadJob: Job? = null
    private set

  private val _uiState = MutableStateFlow(OfflineAreaSelectorState())
  val uiState: StateFlow<OfflineAreaSelectorState> = _uiState

  private val uiEventChannel = Channel<OfflineAreaSelectorEvent>(Channel.BUFFERED)
  val uiEvent: Flow<OfflineAreaSelectorEvent> = uiEventChannel.receiveAsFlow()

  fun onDownloadClick() {
    if (!networkManager.isNetworkConnected()) {
      viewModelScope.launch { uiEventChannel.send(OfflineAreaSelectorEvent.NetworkUnavailable) }
      return
    }

    val currentViewport = viewport
    if (
      currentViewport == null ||
        _uiState.value.downloadState is OfflineAreaSelectorState.DownloadState.InProgress
    ) {
      // Download was likely clicked before map was ready or already in progress.
      return
    }

    _uiState.update {
      it.copy(downloadState = OfflineAreaSelectorState.DownloadState.InProgress(0f))
    }
    downloadJob =
      viewModelScope.launch(ioDispatcher) {
        try {
          var totalDownloaded = 0
          offlineAreaRepository.downloadTiles(currentViewport).collect {
            (bytesDownloaded, totalBytes) ->
            totalDownloaded = bytesDownloaded
            updateDownloadProgress(bytesDownloaded, totalBytes)
          }
          _uiState.update { it.copy(downloadState = OfflineAreaSelectorState.DownloadState.Idle) }
          if (totalDownloaded > 0) {
            uiEventChannel.send(OfflineAreaSelectorEvent.NavigateOfflineAreaBackToHomeScreen)
          } else {
            uiEventChannel.send(OfflineAreaSelectorEvent.DownloadError)
          }
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Timber.e(e, "Download failed")
          _uiState.update { it.copy(downloadState = OfflineAreaSelectorState.DownloadState.Idle) }
          uiEventChannel.send(OfflineAreaSelectorEvent.DownloadError)
        }
      }
  }

  private fun updateDownloadProgress(bytesDownloaded: Int, totalBytes: Int) {
    val progressValue =
      if (totalBytes > 0) {
        (bytesDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
      } else {
        0f
      }
    _uiState.update {
      it.copy(downloadState = OfflineAreaSelectorState.DownloadState.InProgress(progressValue))
    }
  }

  fun onCancelClick() {
    viewModelScope.launch { uiEventChannel.send(OfflineAreaSelectorEvent.NavigateUp) }
  }

  fun stopDownloading() {
    downloadJob?.cancel()
    downloadJob = null
    _uiState.update { it.copy(downloadState = OfflineAreaSelectorState.DownloadState.Idle) }
  }

  override fun onMapDragged() {
    updateDownloadSizeJob?.cancel()
    updateDownloadSizeJob = null
    _uiState.update { it.copy(bottomTextState = null) }
    super.onMapDragged()
  }

  override fun onMapCameraMoved(newCameraPosition: CameraPosition) {
    super.onMapCameraMoved(newCameraPosition)

    val bounds = newCameraPosition.bounds
    val zoomLevel = newCameraPosition.zoomLevel
    if (bounds == null || zoomLevel == null || zoomLevel < MIN_DOWNLOAD_ZOOM_LEVEL) {
      updateDownloadSizeJob?.cancel()
      updateDownloadSizeJob = null
      viewport = null
      if (bounds != null && zoomLevel != null && zoomLevel < MIN_DOWNLOAD_ZOOM_LEVEL) {
        onLargeAreaSelected()
      }
      return
    }

    viewport = bounds
    updateDownloadSizeJob?.cancel()
    updateDownloadSizeJob = viewModelScope.launch(ioDispatcher) { updateDownloadSize(bounds) }
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
    _uiState.update { it.copy(bottomTextState = OfflineAreaSelectorState.BottomTextState.Loading) }

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
    _uiState.update {
      it.copy(bottomTextState = OfflineAreaSelectorState.BottomTextState.NetworkError)
    }
  }

  private fun onUnavailableAreaSelected() {
    _uiState.update {
      it.copy(bottomTextState = OfflineAreaSelectorState.BottomTextState.NoImageryAvailable)
    }
  }

  private fun onDownloadableAreaSelected(sizeInMb: Float) {
    _uiState.update {
      it.copy(
        bottomTextState = OfflineAreaSelectorState.BottomTextState.AreaSize(sizeInMb.toMbString())
      )
    }
  }

  private fun onLargeAreaSelected() {
    _uiState.update {
      it.copy(bottomTextState = OfflineAreaSelectorState.BottomTextState.AreaTooLarge)
    }
  }
}
