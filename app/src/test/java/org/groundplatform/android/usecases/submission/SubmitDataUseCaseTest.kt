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
package org.groundplatform.android.usecases.submission

import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.test.assertFailsWith
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData.newTask
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.submission.ValueDelta
import org.groundplatform.android.model.task.Task
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SubmitDataUseCaseTest : BaseHiltTest() {

  @Inject lateinit var submitDataUseCase: SubmitDataUseCase

  @Test
  fun `Invoke with invalid AddLoi task type throws error`() {
    val task = newTask(id = "1", type = Task.Type.TEXT, isAddLoiTask = true)
    val taskValue = ValueDelta(taskId = task.id, taskType = task.type, newTaskData = null)

    val exception =
      assertFailsWith<IllegalStateException> {
        runWithTestDispatcher {
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

    assertThat(exception.message).isEqualTo("Invalid AddLoi task")
  }
}
