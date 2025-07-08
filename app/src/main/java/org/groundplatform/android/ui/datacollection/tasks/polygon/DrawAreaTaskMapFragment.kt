/*
 * Copyright 2023 Google LLC
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
package org.groundplatform.android.ui.datacollection.tasks.polygon

import android.os.Bundle
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.groundplatform.android.ui.datacollection.tasks.AbstractTaskMapFragment
import org.groundplatform.android.ui.map.CameraPosition
import org.groundplatform.android.ui.map.Feature
import org.groundplatform.android.ui.map.gms.GmsExt.toBounds

@AndroidEntryPoint
class DrawAreaTaskMapFragment @Inject constructor() :
  AbstractTaskMapFragment<DrawAreaTaskViewModel>() {

  override fun onMapCameraMoved(position: CameraPosition) {
    super.onMapCameraMoved(position)
    if (!taskViewModel.isMarkedComplete()) {
      val mapCenter = position.coordinates
      taskViewModel.updateLastVertexAndMaybeCompletePolygon(mapCenter) { c1, c2 ->
        map.getDistanceInPixels(c1, c2)
      }
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    lifecycleScope.launch {
      combine(taskViewModel.isMarkedComplete, taskViewModel.isTooClose) { isComplete, tooClose ->
          !tooClose && !isComplete
        }
        .collect { shouldShow -> setCenterMarkerVisibility(shouldShow) }
    }
  }

  override fun setDefaultViewPort() {
    val feature = taskViewModel.draftArea.value
    val geometry = feature?.geometry ?: return
    val bounds = listOf(geometry).toBounds() ?: return
    moveToBounds(bounds, padding = 200, shouldAnimate = false)
  }

  override fun renderFeatures(): LiveData<Set<Feature>> =
    taskViewModel.draftArea
      .map { feature: Feature? -> if (feature == null) setOf() else setOf(feature) }
      .asLiveData()
}
