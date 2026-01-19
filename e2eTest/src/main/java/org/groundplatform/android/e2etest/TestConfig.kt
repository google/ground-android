/*
 * Copyright 2024 Google LLC
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
package org.groundplatform.android.e2etest

import org.groundplatform.android.model.task.Task

object TestConfig {
  const val DEFAULT_TIMEOUT = 10000L
  const val SURVEY_NAME = "Ground app E2E test"
  const val TEST_JOB_ALL_TASK_TYPES_EXCEPT_DRAW_AREA = "Test all task types except draw area"
  val TEST_LIST_ALL_TASK_TYPES_EXCEPT_DRAW_AREA =
    listOf(
      TestTask(taskType = Task.Type.DROP_PIN, isRequired = true),
      TestTask(Task.Type.INSTRUCTIONS),
      TestTask(Task.Type.TEXT),
      TestTask(taskType = Task.Type.MULTIPLE_CHOICE, selectIndexes = listOf(1)),
      TestTask(taskType = Task.Type.MULTIPLE_CHOICE, selectIndexes = (0..3).toList()),
      TestTask(Task.Type.NUMBER),
      TestTask(Task.Type.PHOTO),
      TestTask(Task.Type.DATE),
      TestTask(Task.Type.TIME),
      TestTask(taskType = Task.Type.CAPTURE_LOCATION, isRequired = true),
    )
  const val TEST_JOB_DRAW_AREA = "Test draw area"
  val TEST_LIST_DRAW_AREA = listOf(TestTask(taskType = Task.Type.DRAW_AREA, isRequired = true))
  const val LOI_NAME = "Test location"
  const val TEST_PHOTO_FILE = "e2e_test_photo.webp"
}
