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
package org.groundplatform.android.ui.datacollection.tasks.point

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.groundplatform.android.data.local.LocalValueStore
import org.groundplatform.android.data.uuid.OfflineUuidGenerator
import org.groundplatform.android.model.geometry.Point
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.job.getDefaultColor
import org.groundplatform.android.model.submission.DropPinTaskData
import org.groundplatform.android.model.submission.TaskData
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.ui.datacollection.tasks.AbstractMapTaskViewModel
import org.groundplatform.android.ui.map.Feature

class DropPinTaskViewModel
@Inject
constructor(
  private val uuidGenerator: OfflineUuidGenerator,
  private val localValueStore: LocalValueStore,
) : AbstractMapTaskViewModel() {

  private var pinColor: Int = 0
  val features: MutableLiveData<Set<Feature>> = MutableLiveData()
  /** Whether the instructions dialog has been shown or not. */
  var instructionsDialogShown: Boolean by localValueStore::dropPinInstructionsShown
  var captureLocation: Boolean = false

  override fun initialize(job: Job, task: Task, taskData: TaskData?) {
    super.initialize(job, task, taskData)
    pinColor = job.getDefaultColor()

    // Drop a marker for current value
    (taskData as? DropPinTaskData)?.let { dropMarker(it.location) }
  }

  override fun clearResponse() {
    super.clearResponse()
    features.postValue(setOf())
  }

  private fun updateResponse(point: Point) {
    setValue(DropPinTaskData(point))
    dropMarker(point)
  }

  private fun dropMarker(point: Point) =
    viewModelScope.launch {
      val feature = createFeature(point)
      features.postValue(setOf(feature))
    }

  /** Creates a new map [Feature] representing the point placed by the user. */
  private suspend fun createFeature(point: Point): Feature =
    Feature(
      id = uuidGenerator.generateUuid(),
      type = Feature.Type.USER_POINT,
      geometry = point,
      style = Feature.Style(pinColor),
      clusterable = false,
      selected = true,
    )

  fun dropPin() {
    getLastCameraPosition()?.let { updateResponse(Point(it.coordinates)) }
  }

  fun shouldShowInstructionsDialog() = !instructionsDialogShown && !captureLocation
}
