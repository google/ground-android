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

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.groundplatform.android.model.imagery.OfflineArea
import org.groundplatform.android.repository.OfflineAreaRepository
import org.groundplatform.android.ui.common.AbstractViewModel
import org.groundplatform.android.util.toMb
import org.groundplatform.android.util.toMbString

/**
 * View model for the offline area manager fragment. Handles the current list of downloaded areas.
 */
class OfflineAreasViewModel
@Inject
internal constructor(private val offlineAreaRepository: OfflineAreaRepository) :
  AbstractViewModel() {

  /**
   * Returns the current list of downloaded offline map imagery areas available for viewing. If an
   * unexpected error accessing the local store is encountered, emits an empty list, circumventing
   * the error.
   */
  val offlineAreas: LiveData<List<OfflineAreaDetails>>

  val showList: LiveData<Boolean>
  val showNoAreasMessage: LiveData<Boolean>
  val showProgressSpinner: LiveData<Boolean>

  private val _navigateToOfflineAreaSelector =
    MutableSharedFlow<Unit>(extraBufferCapacity = 1, replay = 0)
  val navigateToOfflineAreaSelector = _navigateToOfflineAreaSelector.asSharedFlow()

  init {
    val offlineAreas =
      offlineAreaRepository.offlineAreas().map { list -> list.map { toOfflineAreaDetails(it) } }
    this.offlineAreas = offlineAreas.asLiveData()
    showProgressSpinner = offlineAreas.map { false }.onStart { emit(true) }.asLiveData()
    showNoAreasMessage = offlineAreas.map { it.isEmpty() }.onStart { emit(false) }.asLiveData()
    showList = offlineAreas.map { it.isNotEmpty() }.onStart { emit(false) }.asLiveData()
  }

  private fun toOfflineAreaDetails(offlineArea: OfflineArea) =
    OfflineAreaDetails(offlineArea.id, offlineArea.name, offlineArea.getSizeOnDevice())

  private fun OfflineArea.getSizeOnDevice() =
    offlineAreaRepository.sizeOnDevice(this).toMb().toMbString()

  /** Navigate to the area selector for offline map imagery. */
  fun showOfflineAreaSelector() {
    viewModelScope.launch { _navigateToOfflineAreaSelector.emit(Unit) }
  }
}
