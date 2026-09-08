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
package org.groundplatform.android.ui.offlineareas

import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.groundplatform.android.ui.common.AbstractViewModel
import org.groundplatform.domain.model.imagery.OfflineArea
import org.groundplatform.domain.model.util.toMb
import org.groundplatform.domain.repository.OfflineAreaRepositoryInterface
import org.groundplatform.ui.util.toMbString

/**
 * View model for the offline area manager fragment. Handles the current list of downloaded areas.
 */
class OfflineAreasViewModel
@Inject
internal constructor(private val offlineAreaRepository: OfflineAreaRepositoryInterface) :
  AbstractViewModel() {

  val uiState: StateFlow<OfflineAreasState> =
    offlineAreaRepository
      .offlineAreas()
      .map { list ->
        OfflineAreasState(
          offlineAreas = list.map { toOfflineAreaDetails(it) },
          isLoading = false,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OfflineAreasState(isLoading = true),
      )

  private val navigateToOfflineAreaSelectorChannel = Channel<Unit>(Channel.BUFFERED)
  val navigateToOfflineAreaSelector: Flow<Unit> =
    navigateToOfflineAreaSelectorChannel.receiveAsFlow()

  /** Navigate to the area selector for offline map imagery. */
  fun showOfflineAreaSelector() {
    viewModelScope.launch { navigateToOfflineAreaSelectorChannel.send(Unit) }
  }

  private fun toOfflineAreaDetails(offlineArea: OfflineArea) =
    OfflineAreaDetails(offlineArea.id, offlineArea.name, offlineArea.getSizeOnDevice())

  private fun OfflineArea.getSizeOnDevice() =
    offlineAreaRepository.sizeOnDevice(this).toMb().toMbString()
}
