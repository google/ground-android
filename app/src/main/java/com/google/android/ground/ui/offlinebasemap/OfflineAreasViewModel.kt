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
package com.google.android.ground.ui.offlinebasemap

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import com.google.android.ground.model.basemap.OfflineArea
import com.google.android.ground.repository.OfflineAreaRepository
import com.google.android.ground.ui.common.AbstractViewModel
import com.google.android.ground.ui.common.Navigator
import com.google.common.collect.ImmutableList
import javax.inject.Inject
import timber.log.Timber

/**
 * View model for the offline area manager fragment. Handles the current list of downloaded areas.
 */
class OfflineAreasViewModel
@Inject
internal constructor(
  private val navigator: Navigator,
  offlineAreaRepository: OfflineAreaRepository
) : AbstractViewModel() {

  /**
   * Returns the current list of downloaded offline basemaps available for viewing. If an unexpected
   * error accessing the local store is encountered, emits an empty list, circumventing the error.
   */
  val offlineAreas: LiveData<ImmutableList<OfflineArea>>

  /**
   * Returns the visibility of a "no area" message based on the current number of available offline
   * basemaps.
   */
  val noAreasMessageVisibility: LiveData<Int>

  init {
    val offlineAreas =
      offlineAreaRepository.offlineAreasOnceAndStream
        .doOnError {
          Timber.e(it, "Unexpected error accessing offline basemaps in the local store.")
        }
        .onErrorReturnItem(ImmutableList.of())
    this.offlineAreas = LiveDataReactiveStreams.fromPublisher(offlineAreas)
    noAreasMessageVisibility =
      LiveDataReactiveStreams.fromPublisher(
        offlineAreas.map { if (it.isEmpty()) View.VISIBLE else View.GONE }
      )
  }

  /** Navigate to the offline area selector UI from the offline basemaps UI. */
  fun showOfflineAreaSelector() {
    navigator.navigate(OfflineAreasFragmentDirections.showOfflineAreaSelector())
  }
}
