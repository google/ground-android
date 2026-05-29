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
package org.groundplatform.feature.pdf.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.geometry.Point
import org.groundplatform.domain.model.submission.CaptureLocationTaskData
import org.groundplatform.domain.model.submission.DateTimeTaskData
import org.groundplatform.domain.model.submission.DropPinTaskData
import org.groundplatform.domain.model.submission.MultipleChoiceTaskData
import org.groundplatform.domain.model.submission.NumberTaskData
import org.groundplatform.domain.model.submission.SkippedTaskData
import org.groundplatform.domain.model.submission.TextTaskData
import org.groundplatform.domain.model.task.MultipleChoice
import org.groundplatform.domain.model.task.Option
import org.groundplatform.domain.model.task.PhotoTaskData
import org.groundplatform.domain.model.task.Task
import org.groundplatform.feature.pdf.helpers.FakeDateFormatter
import org.groundplatform.feature.pdf.helpers.FakeStringResolver
import org.groundplatform.feature.pdf.model.SubmissionPdfDocument
import org.groundplatform.testing.FakeDataGenerator

class TaskValueMapperTest {

  private val dateFormatter = FakeDateFormatter
  private val stringResolver = FakeStringResolver
  private val mapper = TaskValueMapper(strings = stringResolver, dateFormatter = dateFormatter)

  @Test
  fun `TEXT task maps to the same value`() = runTest {
    assertEquals(
      SubmissionPdfDocument.Answer.Text(listOf("free text")),
      mapper.map(task(Task.Type.TEXT), TextTaskData("free text")),
    )
  }

  @Test
  fun `NUMBER task maps to the same value`() = runTest {
    assertEquals(
      SubmissionPdfDocument.Answer.Text(listOf("42")),
      mapper.map(task(Task.Type.NUMBER), NumberTaskData("42")),
    )
  }

  @Test
  fun `Skipped value renders the skipped label`() = runTest {
    assertEquals(
      SubmissionPdfDocument.Answer.Text(listOf("skipped")),
      mapper.map(task(Task.Type.TEXT), SkippedTaskData()),
    )
  }

  @Test
  fun `PHOTO task maps to a photo answer`() = runTest {
    assertEquals(
      SubmissionPdfDocument.Answer.Photo("path/to/photo.jpg"),
      mapper.map(task(Task.Type.PHOTO), PhotoTaskData("path/to/photo.jpg")),
    )
  }

  @Test
  fun `unsupported value maps to an empty answer`() = runTest {
    val value = DropPinTaskData(Point(Coordinates(1.0, 2.0)))
    assertEquals(
      SubmissionPdfDocument.Answer.Text(emptyList()),
      mapper.map(task(Task.Type.DROP_PIN), value),
    )
  }

  @Test
  fun `DATE task renders date only`() = runTest {
    val millis = 987654321L
    assertEquals(
      SubmissionPdfDocument.Answer.Text(listOf(dateFormatter.formatDate(millis))),
      mapper.map(task(Task.Type.DATE), DateTimeTaskData(millis)),
    )
  }

  @Test
  fun `TIME task renders time only`() = runTest {
    val millis = 987654321L
    assertEquals(
      SubmissionPdfDocument.Answer.Text(listOf(dateFormatter.formatTime(millis))),
      mapper.map(task(Task.Type.TIME), DateTimeTaskData(millis)),
    )
  }

  @Test
  fun `non date or time task renders empty answer`() = runTest {
    val millis = 987654321L
    assertEquals(
      SubmissionPdfDocument.Answer.Text(emptyList()),
      mapper.map(task(Task.Type.NUMBER), DateTimeTaskData(millis)),
    )
  }

  @Test
  fun `MULTIPLE_CHOICE renders each selected option label on its own line`() = runTest {
    val value = MultipleChoiceTaskData(multipleChoice(), selectedOptionIds = listOf("a", "b"))
    assertEquals(
      SubmissionPdfDocument.Answer.Text(listOf("Apple", "Banana")),
      mapper.map(task(Task.Type.MULTIPLE_CHOICE, multipleChoice()), value),
    )
  }

  @Test
  fun `MULTIPLE_CHOICE appends the other free text as a trailing line`() = runTest {
    val other = "${MultipleChoiceTaskData.OTHER_PREFIX}custom${MultipleChoiceTaskData.OTHER_SUFFIX}"
    val value = MultipleChoiceTaskData(multipleChoice(), selectedOptionIds = listOf("a", other))
    assertEquals(
      SubmissionPdfDocument.Answer.Text(listOf("Apple", "other: custom")),
      mapper.map(task(Task.Type.MULTIPLE_CHOICE, multipleChoice()), value),
    )
  }

  @Test
  fun `CAPTURE_LOCATION renders coordinates with directions and altitude and accuracy`() = runTest {
    val value =
      CaptureLocationTaskData(Point(Coordinates(1.5, -2.25)), altitude = 10.0, accuracy = 3.0)

    val result = mapper.map(task(Task.Type.CAPTURE_LOCATION), value)

    val expected =
      SubmissionPdfDocument.Answer.Text(
        listOf(
          "${mapper.formatDegrees(1.5)} north, ${mapper.formatDegrees(2.25)} west",
          "pdf_altitude(${mapper.formatMeters(10.0)})",
          "pdf_accuracy(${mapper.formatMeters(3.0)})",
        )
      )
    assertEquals(expected, result)
  }

  @Test
  fun `CAPTURE_LOCATION omits altitude and accuracy when absent`() = runTest {
    val value =
      CaptureLocationTaskData(Point(Coordinates(-1.0, 2.0)), altitude = null, accuracy = null)

    val result = mapper.map(task(Task.Type.CAPTURE_LOCATION), value)

    assertEquals(
      SubmissionPdfDocument.Answer.Text(
        listOf("${mapper.formatDegrees(1.0)} south, ${mapper.formatDegrees(2.0)} east")
      ),
      result,
    )
  }

  private fun task(type: Task.Type, multipleChoice: MultipleChoice? = null) =
    FakeDataGenerator.newTask(type = type, multipleChoice = multipleChoice)

  private fun multipleChoice() =
    MultipleChoice(
      options =
        persistentListOf(
          Option(id = "a", code = "A", label = "Apple"),
          Option(id = "b", code = "B", label = "Banana"),
        ),
      cardinality = MultipleChoice.Cardinality.SELECT_MULTIPLE,
    )
}
