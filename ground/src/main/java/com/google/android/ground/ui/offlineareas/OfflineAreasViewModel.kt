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
package com.google.android.ground.ui.offlineareas

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.google.android.ground.model.imagery.OfflineArea
import com.google.android.ground.repository.OfflineAreaRepository
import com.google.android.ground.ui.common.AbstractViewModel
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.util.toMb
import com.google.android.ground.util.toMbString
import javax.inject.Inject
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * View model for the offline area manager fragment. Handles the current list of downloaded areas.
 */
class OfflineAreasViewModel
@Inject
internal constructor(
  private val navigator: Navigator,
  private val offlineAreaRepository: OfflineAreaRepository,
) : AbstractViewModel() {

  /**
   * Returns the current list of downloaded offline map imagery areas available for viewing. If an
   * unexpected error accessing the local store is encountered, emits an empty list, circumventing
   * the error.
   */
  val offlineAreas: LiveData<List<OfflineAreaListItemViewModel>>

  val showList: LiveData<Boolean>
  val showNoAreasMessage: LiveData<Boolean>
  val showProgressSpinner: LiveData<Boolean>

  init {
    val offlineAreas = offlineAreaRepository.offlineAreas().map { toViewModel(it) }
    this.offlineAreas = offlineAreas.asLiveData()
    showProgressSpinner = offlineAreas.map { false }.onStart { emit(true) }.asLiveData()
    showNoAreasMessage = offlineAreas.map { it.isEmpty() }.onStart { emit(false) }.asLiveData()
    showList = offlineAreas.map { it.isNotEmpty() }.onStart { emit(false) }.asLiveData()
  }

  private fun toViewModel(offlineAreas: List<OfflineArea>): List<OfflineAreaListItemViewModel> =
    offlineAreas.map { toViewModel(it) }

  private fun toViewModel(offlineArea: OfflineArea) =
    OfflineAreaListItemViewModel(
      navigator,
      offlineArea,
      offlineAreaRepository.sizeOnDevice(offlineArea).toMb().toMbString(),
    )

  /** Navigate to the area selector for offline map imagery. */
  fun showOfflineAreaSelector() {
    navigator.navigate(OfflineAreasFragmentDirections.showOfflineAreaSelector())
  }
}
