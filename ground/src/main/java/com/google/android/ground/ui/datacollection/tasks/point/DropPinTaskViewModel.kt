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
package com.google.android.ground.ui.datacollection.tasks.point

import androidx.lifecycle.MutableLiveData
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.job.getDefaultColor
import com.google.android.ground.model.submission.DropPinTaskData
import com.google.android.ground.model.submission.TaskData
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.local.LocalValueStore
import com.google.android.ground.persistence.uuid.OfflineUuidGenerator
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskViewModel
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.map.Feature
import com.google.android.ground.ui.map.FeatureType
import javax.inject.Inject

class DropPinTaskViewModel
@Inject
constructor(
  private val uuidGenerator: OfflineUuidGenerator,
  private val localValueStore: LocalValueStore,
) : AbstractTaskViewModel() {

  private var pinColor: Int = 0
  private var lastCameraPosition: CameraPosition? = null
  val features: MutableLiveData<Set<Feature>> = MutableLiveData()
  /** Whether the instructions dialog has been shown or not. */
  var instructionsDialogShown: Boolean by localValueStore::dropPinInstructionsShown

  override fun initialize(job: Job, task: Task, taskData: TaskData?) {
    super.initialize(job, task, taskData)
    pinColor = job.getDefaultColor()

    // Drop a marker for current value
    // TODO: The restored marker appears to be slightly shifted. Check for accuracy of lat/lng.
    (taskData as? DropPinTaskData)?.let { dropMarker(it.location) }
  }

  fun updateCameraPosition(position: CameraPosition) {
    lastCameraPosition = position
  }

  override fun clearResponse() {
    super.clearResponse()
    features.postValue(setOf())
  }

  fun updateResponse(point: Point) {
    setValue(DropPinTaskData(point))
    dropMarker(point)
  }

  private fun dropMarker(point: Point) {
    val feature = createFeature(point)
    features.postValue(setOf(feature))
  }

  /** Creates a new map [Feature] representing the point placed by the user. */
  private fun createFeature(point: Point): Feature =
    Feature(
      id = uuidGenerator.generateUuid(),
      type = FeatureType.USER_POINT.ordinal,
      geometry = point,
      // TODO: Set correct pin color.
      style = Feature.Style(pinColor),
      clusterable = false,
      selected = true,
    )

  fun dropPin() {
    lastCameraPosition?.let { updateResponse(Point(it.coordinates)) }
  }
}
