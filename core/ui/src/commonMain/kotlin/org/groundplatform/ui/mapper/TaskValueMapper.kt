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

import androidx.annotation.VisibleForTesting
import ground_android.core.ui.generated.resources.Res
import ground_android.core.ui.generated.resources.east
import ground_android.core.ui.generated.resources.north
import ground_android.core.ui.generated.resources.other
import ground_android.core.ui.generated.resources.pdf_accuracy
import ground_android.core.ui.generated.resources.pdf_altitude
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
import org.groundplatform.domain.model.task.PhotoTaskData
import org.groundplatform.domain.model.task.Task
import org.groundplatform.domain.util.toFixedDecimals
import org.groundplatform.ui.model.SubmissionPdfDocument.Answer
import org.groundplatform.ui.util.DateFormatter
import org.groundplatform.ui.util.StringResolver

object TaskValueMapper {
  private const val DEGREES_DECIMALS = 6

  /** Maps a [TaskData] value to the [Answer] to be rendered in the submission PDF. */
  suspend fun map(
    task: Task,
    value: TaskData,
    dateFormatter: DateFormatter,
    strings: StringResolver,
  ): Answer =
    when (value) {
      is SkippedTaskData -> Answer.Text(listOf(strings.resolve(Res.string.skipped)))
      is TextTaskData -> Answer.Text(listOf(value.text))
      is NumberTaskData -> Answer.Text(listOf(value.number))
      is DateTimeTaskData ->
        Answer.Text(listOfNotNull(formatTaskDateTime(task, value.timeInMillis, dateFormatter)))
      is MultipleChoiceTaskData -> Answer.Text(formatMultipleChoice(task, value, strings))
      is CaptureLocationTaskData -> Answer.Text(formatCaptureLocation(value, strings))
      is PhotoTaskData -> Answer.Photo(value.remoteFilename)
      else -> Answer.Text(emptyList())
    }

  private fun formatTaskDateTime(task: Task, millis: Long, dateFormatter: DateFormatter): String? =
    when (task.type) {
      Task.Type.DATE -> dateFormatter.formatDate(millis)
      Task.Type.TIME -> dateFormatter.formatTime(millis)
      else -> null
    }

  private suspend fun formatMultipleChoice(
    task: Task,
    value: MultipleChoiceTaskData,
    strings: StringResolver,
  ): List<String> {
    val options = task.multipleChoice?.options.orEmpty()
    val selectedLabels =
      value.getSelectedOptionsIdsExceptOther().map { id ->
        options.firstOrNull { it.id == id }?.label ?: id
      }
    return if (value.isOtherTextSelected()) {
      selectedLabels + "${strings.resolve(Res.string.other)}: ${value.getOtherText()}"
    } else {
      selectedLabels
    }
  }

  /** Coordinates first, then optional altitude and accuracy lines. */
  private suspend fun formatCaptureLocation(
    value: CaptureLocationTaskData,
    strings: StringResolver,
  ): List<String> {
    val lines = mutableListOf(formatPoint(value.location, strings))
    value.altitude?.let { lines.add(strings.resolve(Res.string.pdf_altitude, formatMeters(it))) }
    value.accuracy?.let { lines.add(strings.resolve(Res.string.pdf_accuracy, formatMeters(it))) }
    return lines
  }

  private suspend fun formatPoint(point: Point, strings: StringResolver): String {
    val lat = point.coordinates.lat
    val lng = point.coordinates.lng
    val latDir =
      if (lat >= 0) strings.resolve(Res.string.north) else strings.resolve(Res.string.south)
    val lngDir =
      if (lng >= 0) strings.resolve(Res.string.east) else strings.resolve(Res.string.west)
    return "${formatDegrees(lat)} $latDir, ${formatDegrees(lng)} $lngDir"
  }

  @VisibleForTesting
  fun formatDegrees(value: Double): String =
    "${value.absoluteValue.toFixedDecimals(DEGREES_DECIMALS)}°"

  @VisibleForTesting fun formatMeters(value: Double): String = round(value).toLong().toString()
}
