/*
 * Copyright 2026 Google LLC
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
package org.groundplatform.ui.mapper

import ground_android.core.ui.generated.resources.Res
import ground_android.core.ui.generated.resources.accuracy
import ground_android.core.ui.generated.resources.altitude
import ground_android.core.ui.generated.resources.east
import ground_android.core.ui.generated.resources.north
import ground_android.core.ui.generated.resources.other
import ground_android.core.ui.generated.resources.skipped
import ground_android.core.ui.generated.resources.south
import ground_android.core.ui.generated.resources.west
import kotlin.math.absoluteValue
import kotlin.math.round
import org.groundplatform.domain.model.geometry.Point
import org.groundplatform.domain.model.submission.CaptureLocationTaskData
import org.groundplatform.domain.model.submission.DateTimeTaskData
import org.groundplatform.domain.model.submission.MultipleChoiceTaskData
import org.groundplatform.domain.model.submission.NumberTaskData
import org.groundplatform.domain.model.submission.SkippedTaskData
import org.groundplatform.domain.model.submission.TaskData
import org.groundplatform.domain.model.submission.TextTaskData
import org.groundplatform.domain.model.task.Task
import org.groundplatform.domain.util.toFixedDecimals
import org.groundplatform.ui.util.DateFormatter
import org.jetbrains.compose.resources.getString

object TaskValueMapper {
  private const val DEGREES_DECIMALS = 6

  /** Renders a [TaskData] value as plain text. */
  suspend fun map(task: Task, value: TaskData, dateFormatter: DateFormatter): String =
    when (value) {
      is SkippedTaskData -> getString(Res.string.skipped)
      is TextTaskData -> value.text
      is NumberTaskData -> value.number
      is DateTimeTaskData -> formatTaskDateTime(task, value.timeInMillis, dateFormatter)
      is MultipleChoiceTaskData -> formatMultipleChoice(task, value)
      is CaptureLocationTaskData -> formatCaptureLocation(value)
      else -> ""
    }

  /** Date for DATE tasks, time for TIME, date + time otherwise. */
  private fun formatTaskDateTime(task: Task, millis: Long, dateFormatter: DateFormatter): String =
    when (task.type) {
      Task.Type.DATE -> dateFormatter.formatDate(millis)
      Task.Type.TIME -> dateFormatter.formatTime(millis)
      else -> "${dateFormatter.formatDate(millis)} ${dateFormatter.formatTime(millis)}"
    }

  private suspend fun formatMultipleChoice(task: Task, value: MultipleChoiceTaskData): String {
    val options = task.multipleChoice?.options.orEmpty()
    val selectedLabels =
      value.getSelectedOptionsIdsExceptOther().map { id ->
        options.firstOrNull { it.id == id }?.label ?: id
      }
    val withOther =
      if (value.isOtherTextSelected()) {
        selectedLabels + "${getString(Res.string.other)}: ${value.getOtherText()}"
      } else {
        selectedLabels
      }
    return withOther.joinToString("; ")
  }

  private suspend fun formatCaptureLocation(value: CaptureLocationTaskData): String {
    val lines = mutableListOf(formatPoint(value.location))
    value.altitude?.let { lines.add(getString(Res.string.altitude, formatMeters(it))) }
    value.accuracy?.let { lines.add(getString(Res.string.accuracy, formatMeters(it))) }
    return lines.joinToString("\n")
  }

  private suspend fun formatPoint(point: Point): String {
    val lat = point.coordinates.lat
    val lng = point.coordinates.lng
    val latDir = if (lat >= 0) getString(Res.string.north) else getString(Res.string.south)
    val lngDir = if (lng >= 0) getString(Res.string.east) else getString(Res.string.west)
    return "${formatDegrees(lat)} $latDir, ${formatDegrees(lng)} $lngDir"
  }

  private fun formatDegrees(value: Double): String =
    "${value.absoluteValue.toFixedDecimals(DEGREES_DECIMALS)}°"

  private fun formatMeters(value: Double): String = round(value).toLong().toString()
}
