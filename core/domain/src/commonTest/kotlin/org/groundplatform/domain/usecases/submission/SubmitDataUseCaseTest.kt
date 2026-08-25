/*
 * Copyright 2025 Google LLC
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
package org.groundplatform.domain.usecases.submission

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.geometry.Point
import org.groundplatform.domain.model.job.Job
import org.groundplatform.domain.model.submission.CaptureLocationTaskData
import org.groundplatform.domain.model.submission.DropPinTaskData
import org.groundplatform.domain.model.submission.ValueDelta
import org.groundplatform.domain.model.task.Task
import org.groundplatform.testing.FakeDataGenerator
import org.groundplatform.testing.FakeLocationOfInterestRepository
import org.groundplatform.testing.FakeSubmissionRepository

class SubmitDataUseCaseTest {

  private val locationOfInterestRepository = FakeLocationOfInterestRepository()
  private val submissionRepository = FakeSubmissionRepository()

  private val submitDataUseCase: SubmitDataUseCase =
    SubmitDataUseCase(locationOfInterestRepository, submissionRepository)

  @Test
  fun `Invoke with invalid AddLoi task type throws error`() {
    val task = FakeDataGenerator.newTask(id = "1", type = Task.Type.TEXT, isAddLoiTask = true)
    val taskValue = ValueDelta(taskId = task.id, taskType = task.type, newTaskData = null)

    val exception =
      assertFailsWith<IllegalStateException> {
        runTest {
          submitDataUseCase.invoke(
            selectedLoiId = null,
            job = Job(id = "1", tasks = mapOf(task.id to task)),
            surveyId = "survey",
            deltas = listOf(taskValue),
            loiName = null,
            collectionId = "",
          )
        }
      }

    assertEquals("Invalid AddLoi task", exception.message)
  }

  @Test
  fun `Invoke with CAPTURE_LOCATION task and CaptureLocationTaskData succeeds`() = runTest {
    val task =
      FakeDataGenerator.newTask(id = "1", type = Task.Type.CAPTURE_LOCATION, isAddLoiTask = true)
    val location = Point(Coordinates(10.0, 20.0))
    val taskData = CaptureLocationTaskData(location, null, null)
    val taskValue = ValueDelta(taskId = task.id, taskType = task.type, newTaskData = taskData)

    val result =
      submitDataUseCase.invoke(
        selectedLoiId = SUBMITTED_LOI_ID,
        job = Job(id = "1", tasks = mapOf(task.id to task)),
        surveyId = "survey",
        deltas = listOf(taskValue),
        loiName = "LOI Name",
        collectionId = "collectionId",
      )

    assertEquals(SUBMITTED_LOI_ID, result)
  }

  @Test
  fun `Invoke with CAPTURE_LOCATION task and DropPinTaskData succeeds`() = runTest {
    val task =
      FakeDataGenerator.newTask(id = "1", type = Task.Type.CAPTURE_LOCATION, isAddLoiTask = true)
    val location = Point(Coordinates(10.0, 20.0))
    val taskData = DropPinTaskData(location)
    val taskValue = ValueDelta(taskId = task.id, taskType = task.type, newTaskData = taskData)

    val result =
      submitDataUseCase.invoke(
        selectedLoiId = SUBMITTED_LOI_ID,
        job = Job(id = "1", tasks = mapOf(task.id to task)),
        surveyId = "survey",
        deltas = listOf(taskValue),
        loiName = "LOI Name",
        collectionId = "collectionId",
      )

    assertEquals(SUBMITTED_LOI_ID, result)
  }

  private companion object {
    const val SUBMITTED_LOI_ID = "submittedLoiId"
  }
}
