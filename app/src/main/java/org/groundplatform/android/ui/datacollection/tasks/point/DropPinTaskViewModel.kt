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

import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.groundplatform.android.data.local.LocalValueStore
import org.groundplatform.android.data.uuid.OfflineUuidGenerator
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.ButtonActionState
import org.groundplatform.android.ui.datacollection.tasks.AbstractMapTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.DataCollectionEvent
import org.groundplatform.android.ui.datacollection.tasks.TaskPositionInterface
import org.groundplatform.android.ui.map.Feature
import org.groundplatform.android.ui.util.getDefaultColor
import org.groundplatform.domain.model.geometry.Point
import org.groundplatform.domain.model.job.Job
import org.groundplatform.domain.model.locationofinterest.LocationOfInterest
import org.groundplatform.domain.model.submission.DropPinTaskData
import org.groundplatform.domain.model.submission.TaskData
import org.groundplatform.domain.model.submission.isNullOrEmpty
import org.groundplatform.domain.model.task.Task
import org.groundplatform.domain.repository.LocationOfInterestRepositoryInterface
import org.groundplatform.domain.repository.SurveyRepositoryInterface

class DropPinTaskViewModel
@Inject
constructor(
  private val uuidGenerator: OfflineUuidGenerator,
  private val localValueStore: LocalValueStore,
  private val loiRepository: LocationOfInterestRepositoryInterface,
  private val surveyRepository: SurveyRepositoryInterface,
) : AbstractMapTaskViewModel() {

  private var pinColor: Int = 0
  private val _features = MutableStateFlow<Set<Feature>>(emptySet())
  val features: StateFlow<Set<Feature>> = _features.asStateFlow()

  /**
   * Features representing LOIs that already exist in the survey, shown as background context while
   * the user drops a new pin.
   */
  val existingLoiFeatures: StateFlow<Set<Feature>> =
    surveyRepository.activeSurveyFlow
      .filterNotNull()
      .flatMapLatest { survey -> loiRepository.getValidLois(survey) }
      .map { lois -> lois.map { loi -> loi.toExistingFeature() }.toSet() }
      .stateIn(viewModelScope, WhileSubscribed(5_000), emptySet())

  private fun LocationOfInterest.toExistingFeature(): Feature =
    Feature(
      id = id,
      type = Feature.Type.LOCATION_OF_INTEREST,
      geometry = geometry,
      style = Feature.Style(job.getDefaultColor()),
      clusterable = false,
      selected = false,
    )

  /** Whether the instructions dialog has been shown or not. */
  internal var instructionsDialogShown: Boolean by localValueStore::dropPinInstructionsShown



  override fun initialize(
    job: Job,
    task: Task,
    taskData: TaskData?,
    taskPositionInterface: TaskPositionInterface,
    surveyId: String,
    eventReporter: (DataCollectionEvent) -> Unit,
  ) {
    super.initialize(job, task, taskData, taskPositionInterface, surveyId, eventReporter)
    pinColor = job.getDefaultColor()

    // Drop a marker for current value
    (taskData as? DropPinTaskData)?.let { placeMarker(it.location) }
  }

  override fun getButtonStates(taskData: TaskData?): List<ButtonActionState> =
    listOf(
      getPreviousButton(),
      getSkipButton(taskData),
      getUndoButton(taskData),
      ButtonActionState(
        action = ButtonAction.DROP_PIN,
        isEnabled = true,
        isVisible = taskData.isNullOrEmpty(),
      ),
      getNextButton(taskData, hideIfEmpty = true),
    )

  override fun clearResponse() {
    super.clearResponse()
    _features.value = setOf()
  }

  override fun onButtonClick(action: ButtonAction) {
    if (action == ButtonAction.DROP_PIN) {
      getLastCameraPosition()?.let { cameraPosition ->
        val point = Point(cameraPosition.coordinates)
        setValue(DropPinTaskData(point))
        placeMarker(point)
      }
    } else {
      super.onButtonClick(action)
    }
  }

  fun dismissDropPinInstructions() {
    instructionsDialogShown = true
    dismissInstructions()
  }

  fun maybeShowInstructions() {
    if (!instructionsDialogShown) {
      showInstructions()
    }
  }

  private fun placeMarker(point: Point) = viewModelScope.launch {
    val feature = createFeature(point)
    _features.value = setOf(feature)
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
}
