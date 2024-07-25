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

package com.google.android.ground.persistence.remote.firebase.schema

import com.google.android.ground.model.task.Condition
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.remote.firebase.schema.ConditionConverter.toCondition
import com.google.android.ground.persistence.remote.firebase.schema.MultipleChoiceConverter.toMultipleChoice
import com.google.android.ground.proto.Task as TaskProto
import com.google.android.ground.proto.Task.DataCollectionLevel
import com.google.android.ground.proto.Task.DrawGeometry.Method
import com.google.android.ground.proto.Task.TaskTypeCase

/** Converts between Firestore nested objects and [Task] instances. */
internal object TaskConverter {

  private fun TaskProto.toTaskType() =
    when (taskTypeCase) {
      TaskTypeCase.TEXT_QUESTION -> Task.Type.TEXT
      TaskTypeCase.NUMBER_QUESTION -> Task.Type.NUMBER
      TaskTypeCase.DATE_TIME_QUESTION ->
        when (dateTimeQuestion?.type) {
          TaskProto.DateTimeQuestion.Type.DATE_ONLY -> Task.Type.DATE
          TaskProto.DateTimeQuestion.Type.TIME_ONLY -> Task.Type.TIME
          else -> Task.Type.DATE
        }
      TaskTypeCase.MULTIPLE_CHOICE_QUESTION -> Task.Type.MULTIPLE_CHOICE
      TaskTypeCase.DRAW_GEOMETRY ->
        if (drawGeometry?.allowedMethodsList?.contains(Method.DRAW_AREA) == true) {
          Task.Type.DRAW_AREA
        } else {
          Task.Type.DROP_PIN
        }
      TaskTypeCase.CAPTURE_LOCATION -> Task.Type.CAPTURE_LOCATION
      TaskTypeCase.TAKE_PHOTO -> Task.Type.PHOTO
      TaskTypeCase.TASKTYPE_NOT_SET -> Task.Type.UNKNOWN
      null -> Task.Type.UNKNOWN
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
      Task(
        id,
        index,
        taskType,
        prompt,
        required,
        multipleChoice,
        task.level == DataCollectionLevel.LOI_METADATA,
        condition = condition,
      )
    }
}
