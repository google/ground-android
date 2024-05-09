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

import com.google.android.ground.model.job.Job
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.remote.firebase.schema.ConditionConverter.toCondition
import com.google.android.ground.persistence.remote.firebase.schema.MultipleChoiceConverter.toMultipleChoice
import timber.log.Timber

/** Converts between Firestore nested objects and [Task] instances. */
internal object TaskConverter {

  fun toTask(id: String, em: TaskNestedObject): Task? {
    val type = toTaskType(em.type)
    if (type == Task.Type.UNKNOWN) {
      Timber.e("Unsupported task type: ${em.type}")
      return null
    }
    // Default index to -1 to degrade gracefully on older dev db instances and surveys.
    val multipleChoice: MultipleChoice? =
      if (type == Task.Type.MULTIPLE_CHOICE) toMultipleChoice(em) else null
    return Task(
      id,
      em.index ?: -1,
      type,
      em.label!!,
      em.required != null && em.required,
      multipleChoice,
      em.addLoiTask ?: false,
      em.condition?.toCondition(),
    )
  }

  // Note: Key value must be in sync with web app.
  private fun toTaskType(typeStr: String?): Task.Type =
    when (typeStr) {
      "text_field" -> Task.Type.TEXT
      "multiple_choice" -> Task.Type.MULTIPLE_CHOICE
      "photo" -> Task.Type.PHOTO
      "drop_pin" -> Task.Type.DROP_PIN
      "draw_area" -> Task.Type.DRAW_AREA
      "number" -> Task.Type.NUMBER
      "date" -> Task.Type.DATE
      "date_time" -> Task.Type.TIME
      "capture_location" -> Task.Type.CAPTURE_LOCATION
      else -> Task.Type.UNKNOWN
    }

  fun toStrategy(strategyStr: String): Job.DataCollectionStrategy =
    try {
      Job.DataCollectionStrategy.valueOf(strategyStr)
    } catch (e: IllegalArgumentException) {
      Timber.e("unknown data collection strategy", e)
      Job.DataCollectionStrategy.UNKNOWN
    }
}
