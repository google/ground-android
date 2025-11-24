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

package org.groundplatform.android.data.remote.firebase.schema

import org.groundplatform.android.data.remote.firebase.schema.ConditionConverter.toCondition
import org.groundplatform.android.data.remote.firebase.schema.MultipleChoiceConverter.toMultipleChoice
import org.groundplatform.android.model.task.Condition
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.proto.Task as TaskProto
import org.groundplatform.android.proto.Task.DataCollectionLevel
import org.groundplatform.android.proto.Task.DrawGeometry.Method
import org.groundplatform.android.proto.Task.TaskTypeCase

/** Converts between Firestore nested objects and [Task] instances. */
internal object TaskConverter {

  private fun TaskProto.toTaskType(): Task.Type =
    when (taskTypeCase) {
      TaskTypeCase.TEXT_QUESTION -> Task.Type.TEXT
      TaskTypeCase.NUMBER_QUESTION -> Task.Type.NUMBER
      TaskTypeCase.DATE_TIME_QUESTION -> dateTimeToTaskType()
      TaskTypeCase.MULTIPLE_CHOICE_QUESTION -> Task.Type.MULTIPLE_CHOICE
      TaskTypeCase.DRAW_GEOMETRY -> drawGeometryToTaskType()
      TaskTypeCase.CAPTURE_LOCATION -> Task.Type.CAPTURE_LOCATION
      TaskTypeCase.TAKE_PHOTO -> Task.Type.PHOTO
      TaskTypeCase.INSTRUCTIONS -> Task.Type.INSTRUCTIONS
      TaskTypeCase.TASKTYPE_NOT_SET,
      null -> Task.Type.UNKNOWN
      else -> Task.Type.UNKNOWN
    }

  private fun TaskProto.dateTimeToTaskType(): Task.Type =
    when (dateTimeQuestion?.type) {
      TaskProto.DateTimeQuestion.Type.DATE_ONLY -> Task.Type.DATE
      TaskProto.DateTimeQuestion.Type.TIME_ONLY -> Task.Type.TIME
      else -> Task.Type.DATE
    }

  private fun TaskProto.drawGeometryToTaskType(): Task.Type =
    if (drawGeometry?.allowedMethodsList?.contains(Method.DRAW_AREA) == true) {
      Task.Type.DRAW_AREA
    } else {
      Task.Type.DROP_PIN
    }

  fun toTask(task: TaskProto): Task =
    with(task) {
      val taskType = task.toTaskType()
      val multipleChoice =
        if (taskType == Task.Type.MULTIPLE_CHOICE) {
          task.multipleChoiceQuestion?.let { toMultipleChoice(it) }
        } else {
          null
        }
      // Merge list of condition expressions into one condition.
      val expressions = conditionsList.mapNotNull { it.toCondition()?.expressions }.flatten()
      val condition =
        if (expressions.isNotEmpty()) {
          Condition(Condition.MatchType.MATCH_ANY, expressions)
        } else {
          null
        }

      // Determine if user can move the point or if it's locked to device location
      val allowMovingPoint = getAllowMovingPoint(taskType, task)

      // Determine if user can manually override location lock for draw area tasks
      val allowManualOverride = getAllowManualOverride(taskType, task)

      Task(
        id,
        index,
        taskType,
        prompt,
        required,
        multipleChoice,
        task.level == DataCollectionLevel.LOI_METADATA,
        condition = condition,
        allowMovingPoint = allowMovingPoint,
        allowManualOverride = allowManualOverride,
      )
    }

  /**
   * Determines whether the user can pan/zoom the map to place a point anywhere.
   * - For DROP_PIN tasks: RWhether the user can pan/zoom the map to place a point anywhere (true),
   *   or can only capture
   *     * their GPS location (false). Only applies to DROP_PIN tasks.
   * - For CAPTURE_LOCATION tasks (legacy): Always locked to device location.
   * - For other tasks: Defaults to true (allow moving).
   */
  @Suppress("UnusedParameter")
  private fun getAllowMovingPoint(taskType: Task.Type, task: TaskProto): Boolean =
    when (taskType) {
      Task.Type.DROP_PIN -> {
        // TODO: Once the proto field is added, update this to read the actual value:
        // !task.drawGeometry.requireDeviceLocation
        // For now, default to true (allow moving point) for backward compatibility
        true
      }

      Task.Type.CAPTURE_LOCATION -> {
        false
      } // Legacy: always locked to device location
      else -> {
       true
      }
    }

  /**
   * Determines whether the user can manually override location lock by panning the map.
   * - For DRAW_AREA tasks: Reads from DrawGeometry.allowManualOverride. When true (default), the
   *   location lock starts enabled but can be disengaged by panning. When false, location lock
   *   cannot be turned off and map gestures are disabled.
   * - For other tasks: Defaults to true (not applicable).
   */
  @Suppress("UnusedParameter", "FunctionOnlyReturningConstant", "ExpressionBodySyntax")
  private fun getAllowManualOverride(taskType: Task.Type, task: TaskProto): Boolean {
    // TODO: Read actual value from proto once allowManualOverride is available.
    // For now, always allow manual override for backward compatibility.
    return true
  }
}
